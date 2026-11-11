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
        const val DEFAULT_SCALE = 160
        val AVAILABLE_SCALES = intArrayOf(85, 100, 120, 140, 160, 180, 200)
        val SCALE_LABELS = arrayOf(
            "Pequeno • 85%",
            "Normal • 100%",
            "Médio • 120%",
            "Grande • 140%",
            "Muito grande • 160%",
            "Extra grande • 180%",
            "Máximo • 200%"
        )
    }
}
