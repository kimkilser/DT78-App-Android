package com.fbiego.dt78.data

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import com.fbiego.dt78.AppsActivity
import com.fbiego.dt78.MainActivity
import com.fbiego.dt78.app.ForegroundService.Companion.dt78
import com.fbiego.dt78.app.MainApplication
import timber.log.Timber
import java.util.ArrayList

class AppsLoader internal constructor (activity: AppsActivity): Runnable {

    private val mActivity: AppsActivity = activity

    override fun run() {

        val installedApps = mActivity.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        installedApps.sortWith(kotlin.Comparator { a, b ->
            val nameA = mActivity.packageManager.getApplicationLabel(a).toString()
            val nameB = mActivity.packageManager.getApplicationLabel(b).toString()
            nameA.compareTo(nameB)
        })
        val names: Array<String> = installedApps.map { applicationInfo -> mActivity.packageManager.getApplicationLabel(applicationInfo).toString() }.toTypedArray()

        val prefsPackages: MutableSet<String> = MainApplication.sharedPrefs.getStringSet(
            MainApplication.PREFS_KEY_ALLOWED_PACKAGES, mutableSetOf())

        val appChannels = parseApps(prefsPackages)
        val prefsAllowedPackages = ArrayList<String>()
        if (appChannels.isNotEmpty()){
            appChannels.forEach {
                prefsAllowedPackages.add(it.app)
            }
        }
        var channels = ArrayList<Int>()
        if (names.isNotEmpty()){
            names.forEach {
                channels.add(0)
            }
        }
        Timber.d("names = ${names.size}, channels = ${channels.size}, appChannels = ${appChannels.size}, installedApps = ${installedApps.size}")
        val checkedItems = BooleanArray(installedApps.size)

        for (i in names.indices) {

            if (prefsAllowedPackages.contains(installedApps[i].packageName)){
                channels[i] = appChannels[prefsAllowedPackages.indexOf(installedApps[i].packageName)].icon
                if (channels[i] == 13 || channels[i] == 14){
                    if (dt78){
                        channels[i] = 0
                    }
                }
                if (channels[i] == 0){
                    channels[i] = checkPackage(installedApps[i].packageName, dt78)
                }

                Timber.d("package: ${installedApps[i].packageName} channel: ${channels[i]}")
            }
            checkedItems[i] = prefsAllowedPackages.contains(installedApps[i].packageName)
        }

        val modifiedList: ArrayList<String> = arrayListOf()
        modifiedList.addAll(prefsAllowedPackages)

        mActivity.appsLoaded(names, installedApps, modifiedList, channels, checkedItems)

    }
}