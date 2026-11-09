package com.example.appar.web

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class SapWebViewClient(
    val tabIndex: Int,
    private val listener: Listener
) : WebViewClient() {

    interface Listener {
        fun onPageStarted(tabIndex: Int, url: String?)
        fun onPageFinished(tabIndex: Int, url: String?)
        fun onMainFrameError(tabIndex: Int, errorMessage: String)
        fun onSslBypassed(tabIndex: Int)
        fun isNetworkAvailable(): Boolean
    }

    var mainFrameFailed = false
        private set
    var clearHistoryPending = false

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        mainFrameFailed = false
        listener.onPageStarted(tabIndex, url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (request?.isForMainFrame == true) {
            mainFrameFailed = true
            val errorMessage = if (listener.isNetworkAvailable()) {
                "Não foi possível abrir o SAP. Verifique o servidor ou tente novamente."
            } else {
                "Sem conexão de rede. Verifique o Wi-Fi e tente novamente."
            }
            listener.onMainFrameError(tabIndex, errorMessage)
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        if (request?.isForMainFrame == true && errorResponse != null && errorResponse.statusCode in 500..599) {
            mainFrameFailed = true
            val errorMessage = "Servidor SAP indisponível (${errorResponse.statusCode}). Tente novamente."
            listener.onMainFrameError(tabIndex, errorMessage)
        }
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        handler?.proceed()
        listener.onSslBypassed(tabIndex)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        if (clearHistoryPending) {
            view?.clearHistory()
            clearHistoryPending = false
        }
        CookieManager.getInstance().flush()
        if (!mainFrameFailed) {
            listener.onPageFinished(tabIndex, url)
        }
    }
}
