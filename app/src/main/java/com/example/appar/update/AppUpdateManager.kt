package com.example.appar.update

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Gerenciador de atualizações OTA (Over-The-Air) via Vercel + GitHub Releases.
 *
 * Verifica o arquivo `version.json` hospedado na Vercel, compara com a versão instalada
 * no coletor e realiza o download e instalação do APK atualizado.
 */
@SuppressLint("SetTextI18n")
class AppUpdateManager(private val activity: Activity) {

    data class UpdateInfo(
        val versionCode: Long,
        val versionName: String,
        val apkUrl: String,
        val releaseNotes: String,
        val forceUpdate: Boolean,
    )

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingApkFile: File? = null

    /**
     * Verifica em segundo plano se há uma nova versão disponível na Vercel.
     *
     * @param manualCheck Se true, exibe avisos caso já esteja atualizado ou ocorra erro.
     */
    fun checkForUpdates(manualCheck: Boolean = false) {
        if (manualCheck) {
            Toast.makeText(activity, "Verificando atualizações...", Toast.LENGTH_SHORT).show()
        }

        executor.execute {
            try {
                val url = URL("$UPDATE_JSON_URL?t=${System.currentTimeMillis()}")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    useCaches = false
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)

                    val updateInfo = UpdateInfo(
                        versionCode = json.optLong("versionCode", 0L),
                        versionName = json.optString("versionName", ""),
                        apkUrl = json.optString("apkUrl", ""),
                        releaseNotes = json.optString("releaseNotes", "Melhorias de estabilidade e correções."),
                        forceUpdate = json.optBoolean("forceUpdate", false),
                    )

                    val currentVersionCode = getCurrentVersionCode()

                    mainHandler.post {
                        if (activity.isFinishing || activity.isDestroyed) return@post

                        if ((updateInfo.versionCode > currentVersionCode) && updateInfo.apkUrl.isNotBlank()) {
                            showUpdateDialog(updateInfo)
                        } else if (manualCheck) {
                            showAlreadyUpdatedDialog(currentVersionCode, getCurrentVersionName())
                        }
                    }
                } else if (manualCheck) {
                    mainHandler.post {
                        Toast.makeText(
                            activity,
                            "Não foi possível verificar atualizações (HTTP $responseCode).",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
                if (manualCheck) {
                    mainHandler.post {
                        Toast.makeText(
                            activity,
                            "Erro ao buscar atualização: verifique a conexão.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun showUpdateDialog(info: UpdateInfo) {
        val builder = AlertDialog.Builder(activity)
            .setTitle("Nova Atualização Disponível (${info.versionName})")
            .setMessage(
                "Uma nova versão do SAP ALCAST está disponível para este coletor.\n\n" +
                        "Novidades:\n${info.releaseNotes}\n\n" +
                        "Deseja baixar e instalar agora?"
            )
            .setCancelable(!info.forceUpdate)
            .setPositiveButton("Atualizar Agora") { _, _ ->
                downloadAndInstallApk(info)
            }

        if (!info.forceUpdate) {
            builder.setNegativeButton("Depois", null)
        }

        builder.show()
    }

    private fun showAlreadyUpdatedDialog(versionCode: Long, versionName: String) {
        AlertDialog.Builder(activity)
            .setTitle("Aplicativo Atualizado")
            .setMessage("Este coletor já está com a versão mais recente instalada ($versionName - código $versionCode).")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun downloadAndInstallApk(info: UpdateInfo) {
        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
        }
        val statusText = TextView(activity).apply {
            text = "Iniciando download... 0%"
            setPadding(0, 24, 0, 0)
        }
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 48, 64, 24)
            addView(progressBar)
            addView(statusText)
        }

        val progressDialog = AlertDialog.Builder(activity)
            .setTitle("Baixando versão ${info.versionName}")
            .setView(container)
            .setCancelable(false)
            .create()

        progressDialog.show()

        executor.execute {
            try {
                val updatesDir = File(activity.getExternalFilesDir(null) ?: activity.cacheDir, "updates")
                if (!updatesDir.exists()) {
                    updatesDir.mkdirs()
                }
                val apkFile = File(updatesDir, "appar-update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                var currentUrl = info.apkUrl
                var redirects = 0
                var conn: HttpURLConnection

                while (true) {
                    conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 15000
                        readTimeout = 15000
                    }
                    val status = conn.responseCode
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == HttpURLConnection.HTTP_SEE_OTHER ||
                        status == 307 || status == 308
                    ) {
                        val newUrl = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (newUrl.isNullOrBlank() || redirects >= 5) break
                        currentUrl = newUrl
                        redirects++
                    } else {
                        break
                    }
                }

                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Servidor retornou código HTTP ${conn.responseCode}")
                }

                val fileLength = conn.contentLength
                conn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var total: Long = 0
                        var count: Int
                        while (input.read(buffer).also { count = it } != -1) {
                            total += count
                            output.write(buffer, 0, count)
                            if (fileLength > 0) {
                                val percent = ((total * 100) / fileLength).toInt()
                                mainHandler.post {
                                    progressBar.progress = percent
                                    statusText.text = "Baixando pacote... $percent%"
                                }
                            } else {
                                val mb = String.format(Locale.US, "%.1f", total / (1024f * 1024f))
                                mainHandler.post {
                                    progressBar.isIndeterminate = true
                                    statusText.text = "Baixando pacote... $mb MB"
                                }
                            }
                        }
                        output.flush()
                    }
                }
                conn.disconnect()

                mainHandler.post {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    promptInstall(apkFile)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    AlertDialog.Builder(activity)
                        .setTitle("Falha no Download")
                        .setMessage("Não foi possível baixar a atualização:\n${e.localizedMessage ?: "Erro de conexão"}")
                        .setPositiveButton("Tentar Novamente") { _, _ -> downloadAndInstallApk(info) }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }
    }

    private fun promptInstall(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                pendingApkFile = apkFile
                AlertDialog.Builder(activity)
                    .setTitle("Permissão Necessária")
                    .setMessage("Para atualizar automaticamente, autorize a instalação de aplicativos desta fonte na tela a seguir e depois volte ao app.")
                    .setCancelable(false)
                    .setPositiveButton("Autorizar") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            "package:${activity.packageName}".toUri()
                        )
                        activity.startActivity(intent)
                    }
                    .show()
                return
            }
        }
        launchApkInstaller(apkFile)
    }

    /**
     * Retoma a instalação caso o operador tenha acabado de voltar da tela de permissão do Android.
     */
    fun onResume() {
        val file = pendingApkFile ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity.packageManager.canRequestPackageInstalls() &&
            file.exists()
        ) {
            pendingApkFile = null
            launchApkInstaller(file)
        }
    }

    private fun launchApkInstaller(apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Erro ao abrir instalador: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun getCurrentVersionCode(): Long {
        return try {
            val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            PackageInfoCompat.getLongVersionCode(pInfo)
        } catch (_: Exception) {
            0L
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            pInfo.versionName ?: "Desconhecida"
        } catch (_: Exception) {
            "Desconhecida"
        }
    }

    companion object {
        /**
         * URL do arquivo `version.json` hospedado na Vercel.
         */
        const val UPDATE_JSON_URL = "https://sap-alcast.vercel.app/version.json"
    }
}
