package com.fbiego.dt78.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import timber.log.Timber

/**
 * Invoked after the system boots up
 */
class OnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val startAtBoot = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SettingsActivity.PREF_KEY_START_AT_BOOT, false)
        if (startAtBoot) {

            Thread(Runnable {
                try {
                    // Lets wait for 10 seconds
                    Thread.sleep((30 * 1000).toLong())
                } catch (e:InterruptedException) {
                    Timber.d("Onboot error: $e")
                }
                // Start your application here
                val intentService = Intent(context, ForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intentService)
                } else {
                    context.startService(intentService)
                }
            }).start()
        }

    }
}