package com.fbiego.dt78.data

import android.graphics.drawable.Drawable


class AppsData(
    var icon: Drawable,
    var name: String,
    var packageName: String,
    var channel: Int,
    var enabled: Boolean,
    var expanded: Boolean
)