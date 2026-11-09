package com.example.appar.data

import android.content.Context
import androidx.core.content.edit

class PreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var textScale: Int
        get() = prefs.getInt(KEY_TEXT_SCALE, DEFAULT_SCALE)
        set(value) = prefs.edit { putInt(KEY_TEXT_SCALE, value) }

    companion object {
        private const val PREFS_NAME = "appar_interface"
        private const val KEY_TEXT_SCALE = "text_scale"
        const val DEFAULT_SCALE = 100
        val AVAILABLE_SCALES = intArrayOf(85, 100, 115, 150)
        val SCALE_LABELS = arrayOf(
            "Compacto • 85%",
            "Normal • 100%",
            "Maior • 115%",
            "Extra grande • 150%"
        )
    }
}
