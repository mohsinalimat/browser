package info.plateaukao.einkbro.fragment

import androidx.preference.PreferenceFragmentCompat
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import info.plateaukao.einkbro.R
import android.content.SharedPreferences
import androidx.preference.ListPreference
import info.plateaukao.einkbro.view.GestureType

class FragmentSettingsGesture : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener, FragmentTitleInterface {

    override fun getTitleId() = R.string.setting_gestures

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_gesture, rootKey)

        findPreference<ListPreference>("setting_multitouch_up")?.setGestureEntries()
        findPreference<ListPreference>("setting_multitouch_down")?.setGestureEntries()
        findPreference<ListPreference>("setting_multitouch_left")?.setGestureEntries()
        findPreference<ListPreference>("setting_multitouch_right")?.setGestureEntries()

        findPreference<ListPreference>("setting_gesture_nav_up")?.setGestureEntries()
        findPreference<ListPreference>("setting_gesture_nav_down")?.setGestureEntries()
        findPreference<ListPreference>("setting_gesture_nav_left")?.setGestureEntries()
        findPreference<ListPreference>("setting_gesture_nav_right")?.setGestureEntries()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        if (key == "sp_gestures_use" || key == "sp_gesture_action") {
            sp.edit().putBoolean("restart_changed", true).apply()
        }
    }

    private fun ListPreference.setGestureEntries() {
        entries = GestureType.values().map { context.getString(it.resId) }.toTypedArray()
        entryValues = GestureType.values().map { it.value }.toTypedArray()
    }
}