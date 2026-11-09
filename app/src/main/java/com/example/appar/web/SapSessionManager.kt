package com.example.appar.web

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import com.example.appar.util.UrlValidator

class SapSessionManager {

    fun performLogoff(targetWebView: WebView, onComplete: () -> Unit) {
        targetWebView.stopLoading()
        targetWebView.loadUrl("about:blank")

        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            targetWebView.clearCache(true)
            targetWebView.clearFormData()
            targetWebView.clearHistory()
            targetWebView.loadUrl(UrlValidator.DEFAULT_SAP_URL)
            onComplete()
        }
    }

    fun configureDefaults(targetWebView: WebView, textScale: Int) {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(targetWebView, true)
        }
        targetWebView.settings.apply {
            @Suppress("SetJavaScriptEnabled")
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            textZoom = textScale
            cacheMode = WebSettings.LOAD_DEFAULT
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
        }
        targetWebView.isFocusableInTouchMode = true
    }
}
