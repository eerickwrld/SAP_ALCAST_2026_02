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
            val errorCode = error?.errorCode ?: 0
            // ERROR_CONNECT = -2, ERROR_HOST_LOOKUP = -6, ERROR_TIMEOUT = -8, ERROR_IO = -1
            // Se for erro de cache/POST resubmission (ERROR_UNKNOWN = -1 ou similar ao voltar), tentar reload automático
            if (errorCode == -1 || error?.description?.contains("CACHE_MISS", ignoreCase = true) == true) {
                view?.post { view.reload() }
                return
            }

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

    override fun onFormResubmission(
        view: WebView?,
        dontResend: android.os.Message?,
        resend: android.os.Message?
    ) {
        // Resolve o erro ERR_CACHE_MISS ao clicar em Voltar em páginas geradas por formulários POST do SAP
        resend?.sendToTarget()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        if (clearHistoryPending) {
            view?.clearHistory()
            clearHistoryPending = false
        }
        CookieManager.getInstance().flush()

        // Auto-foco inteligente: foca no primeiro campo editável da tela do SAP automaticamente
        view?.evaluateJavascript(
            """
            (function() {
                try {
                    var inputs = document.querySelectorAll('input:not([type="hidden"]):not([readonly]):not([disabled])');
                    for (var i = 0; i < inputs.length; i++) {
                        var inp = inputs[i];
                        if (inp.offsetWidth > 0 || inp.offsetHeight > 0) {
                            inp.focus();
                            if (inp.type === 'text') { inp.select(); }
                            break;
                        }
                    }
                } catch(e) {}
            })();
            """.trimIndent(),
            null
        )

        if (!mainFrameFailed) {
            listener.onPageFinished(tabIndex, url)
        }
    }
}
