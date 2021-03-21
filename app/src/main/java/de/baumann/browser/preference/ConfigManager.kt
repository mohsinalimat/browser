package de.baumann.browser.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class ConfigManager(private val context: Context) {
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var touchAreaHint: Boolean
    get() = sp.getBoolean("sp_touch_area_hint", true)
    set(value) {sp.edit { putBoolean("sp_touch_area_hint", value) } }

    var boldFontStyle: Boolean
        get() = sp.getBoolean("sp_bold_font", false)
        set(value) {sp.edit { putBoolean("sp_bold_font", value) } }
}