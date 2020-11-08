package com.fbiego.dt78.app

import android.content.Context
import android.content.SharedPreferences
import android.support.multidex.MultiDexApplication
import timber.log.Timber
import com.fbiego.dt78.BuildConfig

/**
 *
 */
class MainApplication : MultiDexApplication() {

    companion object {
        const val PREFS_KEY_ALLOWED_PACKAGES = "PREFS_KEY_ALLOWED_PACKAGES"
        const val PREFS_KEY_APP_CHANNELS = "PREFS_KEY_APP_CHANNELS"
        lateinit var sharedPrefs: SharedPreferences
    }

    override fun onCreate() {
        super.onCreate()

        sharedPrefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            //Timber.plant(FileLog(this, "timber.txt"))
        }
        Timber.plant(FileLog(this, "error.txt"))
    }


}