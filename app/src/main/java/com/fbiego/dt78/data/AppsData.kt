package com.fbiego.dt78.data

import android.graphics.drawable.Drawable


class AppsData(
    var icon: Drawable,
    var name: String,
    var packageName: String,
    var channel: Int,
    var enabled: Boolean,
    var expanded: Boolean,
    var filters: ArrayList<String>
) {
    fun hasFilter(): Boolean{
        return if (hideUnlocked()){
            filters.size > 1
        } else {
            filters.size > 0
        }
    }

    fun hideUnlocked(): Boolean{
        return filters.contains(sON)
    }
}