package com.example.appar.util

import androidx.core.net.toUri

object UrlValidator {
    const val DEFAULT_SAP_URL = "https://s4.alcast.com.br/sap/bc/gui/sap/its/zitsmobile"
    private const val ALLOWED_HOST = "s4.alcast.com.br"
    private const val ALLOWED_SCHEME = "https"

    fun sanitizeInitialUrl(urlInput: String?): String {
        if (urlInput.isNullOrBlank()) return DEFAULT_SAP_URL
        return try {
            val uri = urlInput.toUri()
            if (uri.scheme.equals(ALLOWED_SCHEME, ignoreCase = true) &&
                uri.host.equals(ALLOWED_HOST, ignoreCase = true)
            ) {
                urlInput
            } else {
                DEFAULT_SAP_URL
            }
        } catch (_: Exception) {
            DEFAULT_SAP_URL
        }
    }
}
