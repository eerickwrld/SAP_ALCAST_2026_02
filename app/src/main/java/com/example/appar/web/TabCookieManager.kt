package com.example.appar.web

import android.webkit.CookieManager
import androidx.core.net.toUri

/**
 * Gerenciador responsável por isolar as sessões do SAP entre abas independentes.
 *
 * Como o Android CookieManager é singleton e compartilhado por todas as WebViews do processo,
 * esta classe captura e restaura os cookies específicos de cada aba durante a alternância.
 */
class TabCookieManager {

    private var tab1Cookies: String? = null
    private var tab2Cookies: String? = null
    private var activeTab: Int = 1

    /**
     * Salva os cookies da aba atualmente visível.
     */
    fun saveActiveTabCookies(domainUrl: String) {
        val cookies = CookieManager.getInstance().getCookie(domainUrl)
        if (!cookies.isNullOrBlank()) {
            if (activeTab == 1) {
                tab1Cookies = cookies
            } else {
                tab2Cookies = cookies
            }
        }
    }

    /**
     * Atualiza os cookies em cache de uma aba específica após a conclusão de uma requisição.
     */
    fun updateCookiesForTab(tabIndex: Int, domainUrl: String) {
        val cookies = CookieManager.getInstance().getCookie(domainUrl)
        if (!cookies.isNullOrBlank()) {
            if (tabIndex == 1) {
                tab1Cookies = cookies
            } else {
                tab2Cookies = cookies
            }
        }
    }

    /**
     * Alterna os cookies globais para a aba de destino com formatação adequada de Domain e Path=/.
     */
    fun switchToTabCookies(targetTab: Int, domainUrl: String, onComplete: () -> Unit) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        // 1. Salva a sessão da aba atual
        saveActiveTabCookies(domainUrl)

        activeTab = targetTab
        val targetCookies = if (targetTab == 1) tab1Cookies else tab2Cookies
        val host = domainUrl.toUri().host ?: DEFAULT_HOST
        val cookieTargetUrl = "https://$host"

        if (!targetCookies.isNullOrBlank()) {
            restoreCookiesForHost(cookieManager, cookieTargetUrl, host, targetCookies)
        } else {
            expireSessionCookiesForHost(cookieManager, cookieTargetUrl, host)
        }

        cookieManager.flush()
        onComplete()
    }

    /**
     * Injeta os cookies formatados com Domain e Path=/ para evitar erros de rotas no SAP.
     */
    private fun restoreCookiesForHost(
        cookieManager: CookieManager,
        targetUrl: String,
        host: String,
        cookies: String
    ) {
        val cookieList = cookies.split(";")
        for (cookie in cookieList) {
            val trimmed = cookie.trim()
            if (trimmed.isNotEmpty()) {
                val formattedCookie = if (trimmed.contains("Domain=", ignoreCase = true)) {
                    trimmed
                } else {
                    "$trimmed; Path=/; Domain=$host"
                }
                cookieManager.setCookie(targetUrl, formattedCookie)
            }
        }
    }

    /**
     * Expira os cookies de sessão anterior para forçar uma tela de login limpa ao abrir uma nova aba.
     */
    private fun expireSessionCookiesForHost(
        cookieManager: CookieManager,
        targetUrl: String,
        host: String
    ) {
        val currentCookies = cookieManager.getCookie(targetUrl) ?: return
        val parts = currentCookies.split(";")
        for (part in parts) {
            val keyVal = part.trim().split("=")
            if (keyVal.isNotEmpty()) {
                val key = keyVal[0].trim()
                if (key.startsWith("SAP_SESSIONID", ignoreCase = true) ||
                    key.equals("MYSAPSSO2", ignoreCase = true)
                ) {
                    cookieManager.setCookie(
                        targetUrl,
                        "$key=; Path=/; Domain=$host; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
                    )
                }
            }
        }
    }

    /**
     * Limpa o cache de cookies da aba no momento do logoff.
     */
    fun clearTabCookies(tabIndex: Int) {
        if (tabIndex == 1) {
            tab1Cookies = null
        } else {
            tab2Cookies = null
        }
    }

    companion object {
        private const val DEFAULT_HOST = "s4.alcast.com.br"
    }
}
