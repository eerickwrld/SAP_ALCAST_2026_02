package com.example.appar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appar.data.PreferencesRepository
import com.example.appar.databinding.ActivityMainBinding
import com.example.appar.ui.AppDialogs
import com.example.appar.update.AppUpdateManager
import com.example.appar.util.NetworkUtils
import com.example.appar.util.UrlValidator
import com.example.appar.web.SapSessionManager
import com.example.appar.web.SapWebViewClient
import com.example.appar.web.TabCookieManager

/**
 * Controladora principal da interface do aplicativo AppAR.
 *
 * Gerencia a navegação em abas duplas independentes do SAP ITS Mobile em coletores Android,
 * articulando serviços de persistência, diálogo, sessão web e isolamento de cookies por aba.
 */
class MainActivity : AppCompatActivity(), SapWebViewClient.Listener, AppDialogs.MenuCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsRepository: PreferencesRepository
    private lateinit var sessionManager: SapSessionManager
    private lateinit var clientTab1: SapWebViewClient
    private lateinit var clientTab2: SapWebViewClient
    private lateinit var appDialogs: AppDialogs
    private lateinit var appUpdateManager: AppUpdateManager
    private val tabCookieManager = TabCookieManager()

    private var activeTab: Int = 1
    private var tab2Loaded: Boolean = false
    private var currentUrlTab1: String = UrlValidator.DEFAULT_SAP_URL
    private var currentUrlTab2: String = UrlValidator.DEFAULT_SAP_URL
    private var backPressedTimestamp: Long = 0L

    private var batteryReceiver: BroadcastReceiver? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val activeWebView: WebView
        get() = if (activeTab == 1) binding.sapWebView1 else binding.sapWebView2

    private val activeClient: SapWebViewClient
        get() = if (activeTab == 1) clientTab1 else clientTab2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        prefsRepository = PreferencesRepository(this)
        sessionManager = SapSessionManager()
        clientTab1 = SapWebViewClient(tabIndex = 1, listener = this)
        clientTab2 = SapWebViewClient(tabIndex = 2, listener = this)
        appDialogs = AppDialogs(this)
        appUpdateManager = AppUpdateManager(this)

        currentUrlTab1 = UrlValidator.sanitizeInitialUrl(intent.getStringExtra("urlInicial"))

        setupWebViews()
        setupListeners()
        setupBackPressedHandler()
        setupSystemMonitors()
        updateBackButton()

        if (savedInstanceState != null) {
            activeTab = savedInstanceState.getInt(KEY_ACTIVE_TAB, 1)
            tab2Loaded = savedInstanceState.getBoolean(KEY_TAB2_LOADED, false)
        } else {
            // Verifica automaticamente se há atualização na Vercel ao abrir o aplicativo
            appUpdateManager.checkForUpdates(manualCheck = false)
        }

        if (savedInstanceState == null || binding.sapWebView1.restoreState(savedInstanceState) == null) {
            binding.sapWebView1.loadUrl(currentUrlTab1)
        }

        if (activeTab == 2) {
            selectTab(2)
        }
    }

    private fun setupWebViews() {
        val initialScale1 = if (isLoginOrSsoUrl(currentUrlTab1)) 100 else prefsRepository.textScale
        sessionManager.configureDefaults(binding.sapWebView1, initialScale1)
        binding.sapWebView1.webViewClient = clientTab1

        val initialScale2 = if (isLoginOrSsoUrl(currentUrlTab2)) 100 else prefsRepository.textScale
        sessionManager.configureDefaults(binding.sapWebView2, initialScale2)
        binding.sapWebView2.webViewClient = clientTab2
    }

    private fun setupListeners() {
        binding.btnTab1.setOnClickListener { selectTab(1) }
        binding.btnTab2.setOnClickListener { selectTab(2) }

        binding.btnMenu.setOnClickListener { appDialogs.showMainMenu(this) }
        binding.btnReload.setOnClickListener { reload() }
        binding.btnRetry.setOnClickListener { reload() }
        binding.btnVoltar.setOnClickListener { navigateBack() }
        binding.btnConfirmar.setOnClickListener { confirmFocusedField() }
    }

    private fun selectTab(tabIndex: Int) {
        if (activeTab == tabIndex && tabIndex == 2 && !tab2Loaded) {
            // Permitir primeiro carregamento da aba 2
        } else if (activeTab == tabIndex) {
            return
        }

        tabCookieManager.switchToTabCookies(tabIndex, UrlValidator.DEFAULT_SAP_URL) {
            activeTab = tabIndex

            if (tabIndex == 1) {
                binding.sapWebView1.visibility = View.VISIBLE
                binding.sapWebView2.visibility = View.GONE
            } else {
                binding.sapWebView1.visibility = View.GONE
                binding.sapWebView2.visibility = View.VISIBLE

                if (!tab2Loaded) {
                    tab2Loaded = true
                    binding.sapWebView2.loadUrl(currentUrlTab2)
                }
            }

            applyDynamicZoom(activeWebView, activeWebView.url)
            updateTabButtonsUI(tabIndex)
            updateStatusAndNavigation()
        }
    }

    private fun updateTabButtonsUI(activeTab: Int) {
        val activeBg = ContextCompat.getColor(this, R.color.tab_active_bg)
        val activeText = ContextCompat.getColor(this, R.color.tab_active_text)
        val inactiveBg = ContextCompat.getColor(this, R.color.tab_inactive_bg)
        val inactiveText = ContextCompat.getColor(this, R.color.tab_inactive_text)

        if (activeTab == 1) {
            binding.btnTab1.backgroundTintList = ColorStateList.valueOf(activeBg)
            binding.btnTab1.setTextColor(activeText)
            binding.btnTab2.backgroundTintList = ColorStateList.valueOf(inactiveBg)
            binding.btnTab2.setTextColor(inactiveText)
        } else {
            binding.btnTab1.backgroundTintList = ColorStateList.valueOf(inactiveBg)
            binding.btnTab1.setTextColor(inactiveText)
            binding.btnTab2.backgroundTintList = ColorStateList.valueOf(activeBg)
            binding.btnTab2.setTextColor(activeText)
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeWebView.canGoBack()) {
                    navigateBack()
                } else {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - backPressedTimestamp < 2000L) {
                        finish()
                    } else {
                        backPressedTimestamp = currentTime
                        Toast.makeText(this@MainActivity, "Pressione Voltar novamente para sair do SAP", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun reload() {
        binding.errorPanel.visibility = View.GONE
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.txtStatus.text = getString(R.string.status_loading)

        val targetUrl = activeWebView.url?.takeIf { it.isNotBlank() && it != "about:blank" }
            ?: if (activeTab == 1) currentUrlTab1 else currentUrlTab2

        activeWebView.loadUrl(targetUrl)
    }

    private fun navigateBack() {
        if (!activeWebView.canGoBack()) return

        val history = activeWebView.copyBackForwardList()
        val currentIndex = history.currentIndex
        if (currentIndex > 0) {
            val prevUrl = history.getItemAtIndex(currentIndex - 1)?.url.orEmpty()
            // Se a tela anterior for o endpoint de autenticação do SAML2/IdP, voltar mais um passo
            // para evitar o erro de reenvio de token expirado do SAML2
            if (prevUrl.contains("saml2", ignoreCase = true) || prevUrl.contains("sso", ignoreCase = true)) {
                if (currentIndex > 1) {
                    activeWebView.goBackOrForward(-2)
                    return  //sap é uma porcaria
                }
            }
        }
        activeWebView.goBack()
    }

    private fun confirmFocusedField() {
        binding.btnConfirmar.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        activeWebView.requestFocus()
        activeWebView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        activeWebView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun updateBackButton() {
        val canGoBack = activeWebView.canGoBack()
        binding.btnVoltar.isEnabled = canGoBack
        binding.btnVoltar.alpha = if (canGoBack) 1.0f else 0.55f
    }

    private fun updateStatusAndNavigation() {
        updateBackButton()
        if (activeClient.mainFrameFailed) {
            binding.errorPanel.visibility = View.VISIBLE
            binding.txtStatus.text = getString(R.string.status_load_failed)
        } else {
            binding.errorPanel.visibility = View.GONE
            binding.txtStatus.text = if (isNetworkAvailable()) {
                getString(R.string.status_network_available)
            } else {
                getString(R.string.status_no_network)
            }
        }
    }

    // --- SapWebViewClient.Listener Callbacks ---

    override fun onPageStarted(tabIndex: Int, url: String?) {
        if (!url.isNullOrEmpty()) {
            if (tabIndex == 1) currentUrlTab1 = url else currentUrlTab2 = url
        }
        val targetWebView = if (tabIndex == 1) binding.sapWebView1 else binding.sapWebView2
        applyDynamicZoom(targetWebView, url)

        if (tabIndex == activeTab) {
            binding.loadingIndicator.visibility = View.VISIBLE
            binding.errorPanel.visibility = View.GONE
            binding.txtStatus.text = getString(R.string.status_loading)
            updateBackButton()
        }
    }

    override fun onPageFinished(tabIndex: Int, url: String?) {
        tabCookieManager.updateCookiesForTab(tabIndex, UrlValidator.DEFAULT_SAP_URL)
        val targetWebView = if (tabIndex == 1) binding.sapWebView1 else binding.sapWebView2
        applyDynamicZoom(targetWebView, url)

        if (tabIndex == activeTab) {
            binding.loadingIndicator.visibility = View.GONE
            updateStatusAndNavigation()
        }
    }

    override fun onMainFrameError(tabIndex: Int, errorMessage: String) {
        if (tabIndex == activeTab) {
            binding.loadingIndicator.visibility = View.GONE
            binding.errorPanel.visibility = View.VISIBLE
            binding.errorText.text = errorMessage
            binding.txtStatus.text = getString(R.string.status_load_failed)
        }
    }

    override fun onSslBypassed(tabIndex: Int) {
        if (tabIndex == activeTab) {
            binding.errorPanel.visibility = View.GONE
            binding.txtStatus.text = getString(R.string.status_ssl_allowed)
        }
    }

    override fun isNetworkAvailable(): Boolean {
        return NetworkUtils.isNetworkAvailable(this)
    }

    // --- Dynamic Zoom Management (100% on Login/SSO, Custom scale on SAP ITS Mobile) ---

    private fun applyDynamicZoom(webView: WebView, url: String?) {
        val isLogin = isLoginOrSsoUrl(url)
        val targetScale = if (isLogin) 100 else prefsRepository.textScale
        if (webView.settings.textZoom != targetScale) {
            webView.settings.textZoom = targetScale
        }
    }

    private fun isLoginOrSsoUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()
        return lower.contains("login.microsoftonline") ||
                lower.contains("login.microsoft") ||
                lower.contains("login.live.com") ||
                lower.contains("microsoft") ||
                lower.contains("msft") ||
                lower.contains("saml2") ||
                lower.contains("adfs") ||
                lower.contains("/sso/") ||
                lower.contains("oauth") ||
                lower.contains("signin")
    }

    // --- AppDialogs.MenuCallback Callbacks ---

    override fun onReload() {
        reload()
    }

    override fun onChangeScale() {
        appDialogs.showScaleChooser(prefsRepository.textScale) { newScale ->
            prefsRepository.textScale = newScale
            applyDynamicZoom(binding.sapWebView1, binding.sapWebView1.url ?: currentUrlTab1)
            applyDynamicZoom(binding.sapWebView2, binding.sapWebView2.url ?: currentUrlTab2)
        }
    }

    override fun onReturnToStart() {
        appDialogs.showReturnToStartConfirmation {
            activeWebView.loadUrl(UrlValidator.DEFAULT_SAP_URL)
        }
    }

    override fun onCheckUpdates() {
        appUpdateManager.checkForUpdates(manualCheck = true)
    }

    override fun onShowAbout() {
        appDialogs.showAboutDialog(
            appUpdateManager.getCurrentVersionName(),
            appUpdateManager.getCurrentVersionCode()
        )
    }

    override fun onLogoffRequested() {
        appDialogs.showLogoffConfirmation {
            binding.txtStatus.text = getString(R.string.status_logging_off)
            tabCookieManager.clearTabCookies(activeTab)
            sessionManager.performLogoff(activeWebView) {
                activeClient.clearHistoryPending = true
                if (activeTab == 1) {
                    currentUrlTab1 = UrlValidator.DEFAULT_SAP_URL
                } else {
                    currentUrlTab2 = UrlValidator.DEFAULT_SAP_URL
                }
                binding.errorPanel.visibility = View.GONE
            }
        }
    }

    // --- System Status Monitors (Battery & Wi-Fi) ---

    private fun setupSystemMonitors() {
        // Monitoramento de Bateria
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let { updateBatteryUI(it) }
            }
        }
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialBattery = registerReceiver(batteryReceiver, batteryFilter)
        initialBattery?.let { updateBatteryUI(it) }

        // Monitoramento de Wi-Fi / Rede
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread { updateWifiUI(isConnected = true) }
            }

            override fun onLost(network: Network) {
                runOnUiThread { updateWifiUI(isConnected = false) }
            }
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback as ConnectivityManager.NetworkCallback)
        updateWifiUI(NetworkUtils.isNetworkAvailable(this))
    }

    private fun updateBatteryUI(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        if (level >= 0 && scale > 0) {
            val pct = (level * 100) / scale
            val icon = if (isCharging) "⚡" else when {
                pct > 60 -> "🔋"
                pct > 20 -> "🪫"
                else -> "⚠️"
            }
            binding.txtBattery.text = "$icon $pct%"
            binding.txtBattery.setTextColor(
                when {
                    pct <= 15 && !isCharging -> Color.parseColor("#FF5252") // Vermelho alerta
                    pct <= 30 && !isCharging -> Color.parseColor("#FFD600") // Amarelo atenção
                    else -> Color.parseColor("#A0B0C0") // Normal
                }
            )
        }
    }

    private fun updateWifiUI(isConnected: Boolean) {
        if (isConnected) {
            binding.txtWifi.text = "📶 Wi-Fi OK"
            binding.txtWifi.setTextColor(Color.parseColor("#4CAF50")) // Verde
        } else {
            binding.txtWifi.text = "❌ Sem Wi-Fi"
            binding.txtWifi.setTextColor(Color.parseColor("#FF5252")) // Vermelho
        }
    }

    // --- Activity Lifecycle ---

    override fun onResume() {
        super.onResume()
        appUpdateManager.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.sapWebView1.saveState(outState)
        outState.putInt(KEY_ACTIVE_TAB, activeTab)
        outState.putBoolean(KEY_TAB2_LOADED, tab2Loaded)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        batteryReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        networkCallback?.let {
            try { connectivityManager?.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        tabCookieManager.saveActiveTabCookies(UrlValidator.DEFAULT_SAP_URL)
        CookieManager.getInstance().flush()
        super.onDestroy()
    }

    companion object {
        private const val KEY_ACTIVE_TAB = "activeTab"
        private const val KEY_TAB2_LOADED = "tab2Loaded"
    }
}
