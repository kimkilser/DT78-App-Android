package com.fbiego.dt78.app

import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.fbiego.dt78.R
import java.util.regex.Pattern

/**
 *
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        val PREF_KEY_RUN_AS_A_SERVICE = "pref_as_bg_service"
        val PREF_KEY_REMOTE_MAC_ADDRESS = "pref_remote_mac_address"
        val PREF_KEY_START_AT_BOOT = "pref_start_at_boot"
        val PREF_KEY_NOTIFY_DISCONNECT = "pref_notify_disconnect"
        val PREF_CURRENT_STEPS = "current_steps"
        val PREF_CURRENT_CALORIES = "current_calories"
        val MAC_PATTERN = Pattern.compile("^([A-F0-9]{2}[:]?){5}[A-F0-9]{2}$")
        val PREF_SYNC = "last_sync"

        class SettingsFragment : PreferenceFragment() {

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                addPreferencesFromResource(R.xml.preferences)
                //
                // apply persisted value
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
                setRemoteMACAddressPrefSummary(sharedPref.getString(PREF_KEY_REMOTE_MAC_ADDRESS, "00:00:00:00:00:00"))
                //
                // validate updates and apply is valid
                findPreference(PREF_KEY_REMOTE_MAC_ADDRESS).setOnPreferenceChangeListener { _: Preference?, value: Any? ->
                    val mac = (value as String).trim()
                    if (MAC_PATTERN.matcher(mac).find()) {
                        setRemoteMACAddressPrefSummary(mac)
                        true
                    } else {
                        Toast.makeText(activity, R.string.mac_format_error, Toast.LENGTH_LONG).show()
                        false
                    }
                }
            }

            private fun setRemoteMACAddressPrefSummary(summary: String) {
                val pref = findPreference(PREF_KEY_REMOTE_MAC_ADDRESS)
                pref.summary = summary
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)

        fragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
    }

    override fun onStart() {
        super.onStart()
        stopService(Intent(this, ForegroundService::class.java))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}