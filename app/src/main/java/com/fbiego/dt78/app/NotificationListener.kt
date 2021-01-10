package com.fbiego.dt78.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.text.SpannableString
import com.fbiego.dt78.app.ForegroundService.Companion.dt78
import com.fbiego.dt78.data.Channel
import com.fbiego.dt78.data.parseApps
import timber.log.Timber

/**
 *
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        const val EXTRA_ACTION = "ESP"
        const val EXTRA_PACKAGE = "EXTRA_PACKAGE"
        const val EXTRA_NOTIFICATION_DISMISSED = "EXTRA_NOTIFICATION_DISMISSED"
        const val EXTRA_APP_NAME = "EXTRA_APP_NAME"
        const val EXTRA_NOTIFICATION_ID_INT = "EXTRA_NOTIFICATION_ID_INT"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_BODY = "EXTRA_BODY"
        const val EXTRA_TIMESTAMP_LONG = "EXTRA_TIMESTAMP_LONG"
        const val EXTRA_ICON = "EXTRA_ICON"
    }

    /**
     * Implement this method to learn about new notifications as they are posted by apps.
     *
     * @param sbn A data structure encapsulating the original [android.app.Notification]
     * object as well as its identifying information (tag and id) and source
     * (package name).
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val ticker = notification?.tickerText
        val bundle: Bundle? = notification?.extras
        val titleObj = bundle?.get("android.title")
        val title: String
        title = when (titleObj) {
            is String -> titleObj
            is SpannableString -> titleObj.toString()
            else -> null.toString()
        }
        val body: String? = bundle?.getCharSequence("android.text").toString()

        val appInfo = applicationContext.packageManager.getApplicationInfo(sbn.packageName, PackageManager.GET_META_DATA)
        val appName = applicationContext.packageManager.getApplicationLabel(appInfo)
        Timber.d("onNotificationPosted {app=${appName},id=${sbn.id},ticker=$ticker,title=$title,body=$body,posted=${sbn.postTime},package=${sbn.packageName}}")

        val allowedPackages: MutableSet<String> = MainApplication.sharedPrefs.getStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, mutableSetOf())!!

        val appsChannels = parseApps(allowedPackages)
        val me: Channel? = appsChannels.singleOrNull {
            it.app == sbn.packageName
        }

        // broadcast StatusBarNotication (exclude own notifications)
        if (sbn.id != ForegroundService.SERVICE_ID && me != null) {
            if (me.icon == 13 || me.icon == 14){
                if (dt78){
                    me.icon = 0
                }
            }
            val intent = Intent(EXTRA_ACTION)
            intent.putExtra(EXTRA_NOTIFICATION_ID_INT, sbn.id)
            intent.putExtra(EXTRA_PACKAGE, sbn.packageName)
            intent.putExtra(EXTRA_APP_NAME, appName)
            intent.putExtra(EXTRA_TITLE, title)
            intent.putExtra(EXTRA_BODY, body)
            intent.putExtra(EXTRA_ICON, me.icon)
            intent.putExtra(EXTRA_NOTIFICATION_DISMISSED, false)
            intent.putExtra(EXTRA_TIMESTAMP_LONG, sbn.postTime)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        val notification = sbn.notification
        val ticker = notification?.tickerText
        val bundle: Bundle? = notification?.extras
        val titleObj = bundle?.get("android.title")
        val title: String
        title = when (titleObj) {
            is String -> titleObj
            is SpannableString -> titleObj.toString()
            else -> "undefined"
        }
        val body: String? = bundle?.getCharSequence("android.text").toString()

        val appInfo = applicationContext.packageManager.getApplicationInfo(sbn.packageName, PackageManager.GET_META_DATA)
        val appName = applicationContext.packageManager.getApplicationLabel(appInfo)
        Timber.d("onNotificationPosted {app=${appName},id=${sbn.id},ticker=$ticker,title=$title,body=$body,posted=${sbn.postTime},package=${sbn.packageName}}")

        val allowedPackages: MutableSet<String> = MainApplication.sharedPrefs.getStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, mutableSetOf())!!

        val appsChannels = parseApps(allowedPackages)
        val me: Channel? = appsChannels.singleOrNull {
            it.app == sbn.packageName
        }

        Timber.d("onNotificationRemoved {app=${applicationContext.packageManager.getApplicationLabel(appInfo)},id=${sbn.id},ticker=$ticker,title=$title,body=$body,posted=${sbn.postTime},package=${sbn.packageName}}")

        // broadcast StatusBarNotication (exclude own notifications)
        if (sbn.id != ForegroundService.SERVICE_ID && me != null) {
            if (me.icon == 13 || me.icon == 14){
                if (dt78){
                    me.icon = 0
                }
            }
            val intent = Intent(EXTRA_ACTION)
            intent.putExtra(EXTRA_NOTIFICATION_ID_INT, sbn.id)
            intent.putExtra(EXTRA_APP_NAME, appName)
            intent.putExtra(EXTRA_TITLE, title)
            intent.putExtra(EXTRA_BODY, body)
            intent.putExtra(EXTRA_ICON, me.icon)
            intent.putExtra(EXTRA_NOTIFICATION_DISMISSED, true)
            intent.putExtra(EXTRA_TIMESTAMP_LONG, sbn.postTime)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }


}