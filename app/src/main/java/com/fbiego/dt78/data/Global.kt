package com.fbiego.dt78.data

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.fbiego.dt78.BuildConfig
import com.fbiego.dt78.R
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException
import kotlin.experimental.and

fun Byte.toPInt() = toInt() and 0xFF

fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) {
        pos -> ints[pos].toByte()
}

fun colors(col: Int): Int {
    return when (col) {
        0 -> Color.parseColor("#FF0000")
        1 -> Color.parseColor("#FFFF00")
        2 -> Color.parseColor("#00FF00")
        3 -> Color.parseColor("#008000")
        4 -> Color.parseColor("#00FFFF")
        5 -> Color.parseColor("#008080")
        6 -> Color.parseColor("#0000FF")
        7 -> Color.parseColor("#000080")
        8 -> Color.parseColor("#FF00FF")
        9 -> Color.parseColor("#800080")
        else -> Color.parseColor("#FFFFFF")
    }
}

fun distance(dis: Int, unit: Boolean, context: Context): String{
    val dist = dis.toFloat()
    return if (unit){
        "%.1f".format(dist/100000)+" "+context.resources.getString(R.string.km)
    } else {
        "%.1f".format(dist/160900)+" "+context.resources.getString(R.string.miles)
    }

}

fun unit(unit: Boolean, context: Context): String{
    return if (unit){
        context.resources.getString(R.string.metric)+" ("+context.resources.getString(R.string.km)+")"
    } else {
        context.resources.getString(R.string.imperial)+" ("+context.resources.getString(R.string.miles)+")"
    }

}

fun icons(id: Int): Int{
    return when (id) {
        0 -> R.raw.sms
        1 -> R.raw.whatsapp
        2 -> R.raw.twitter
        4 -> R.raw.instagram
        5 -> R.raw.facebook
        6 -> R.raw.messenger
        7 -> R.raw.skype
        8 -> R.raw.penguin
        9 -> R.raw.wechat
        10 -> R.raw.line
        11 -> R.raw.weibo
        12 -> R.raw.kakao
        13 -> R.raw.telegram
        14 -> R.raw.viber
        else -> R.raw.sms
    }
}

fun spinner(id: Int): Int{
    return if (id < 3){
        id
    } else {
        id-1
    }
}

fun parseApps(list : MutableSet<String>): ArrayList<Channel>{
    var array = ArrayList<Channel>()
    if (list.isNotEmpty()){
        list.forEach {
            val start = it.indexOf(",")
            val id = if (start != -1){
                it.substring(0, it.indexOf(","))
            } else {
                "0"
            }

            val name = it.substring(it.indexOf(",")+1, it.length)
            val no = try {
                id.toInt()
            } catch (e: NumberFormatException){
                0
            } catch (e: IllegalArgumentException){
                0
            }
            array.add(Channel(no, name))
        }
    }
    return array
}

fun battery(percentage: Int, bool: Boolean): Int{
    return if (bool){
        battery1(percentage)
    } else {
        battery2(percentage)
    }
}

fun battery1(percentage: Int): Int{
    return when (percentage){
        100 -> R.drawable.ic_bat100w
        80 -> R.drawable.ic_bat80w
        60 -> R.drawable.ic_bat60w
        40 -> R.drawable.ic_bat40w
        20 -> R.drawable.ic_bat20w
        0 -> R.drawable.ic_bat00w
        -10 -> R.drawable.ic_disc
        else -> R.drawable.ic_watch
    }
}

fun battery2(percentage: Int): Int{
    return when (percentage){
        100 -> R.drawable.ic_per100
        80 -> R.drawable.ic_per80
        60 -> R.drawable.ic_per60
        40 -> R.drawable.ic_per40
        20 -> R.drawable.ic_per20
        0 -> R.drawable.ic_per0
        -10 -> R.drawable.ic_disc
        else -> R.drawable.ic_watch
    }
}

fun priority(pr: Int): Int{
    return when (pr){
        0 -> NotificationCompat.PRIORITY_MIN
        1 -> NotificationCompat.PRIORITY_LOW
        2 -> NotificationCompat.PRIORITY_DEFAULT
        3 -> NotificationCompat.PRIORITY_HIGH
        4 -> NotificationCompat.PRIORITY_MAX
        else -> NotificationCompat.PRIORITY_DEFAULT
    }
}

fun color(percentage: Int): Int{
    return when (percentage){
        100 -> Color.parseColor("#008000")
        80 -> Color.parseColor("#308000")
        60 -> Color.parseColor("#508000")
        40 -> Color.parseColor("#F08000")
        20 -> Color.parseColor("#F04000")
        0 -> Color.parseColor("#F00000")
        else -> Color.parseColor("#2abf34")
    }
}

fun hasBat(bat: Int): Boolean{
    return when (bat){
        100, 80, 60, 40, 20, 0 -> true
        else -> false
    }
}

fun type(bool: Boolean): String{
    return if (bool){
        " DT78"
    } else {
        " DT92"
    }
}

fun checkPackage(packageName: String, dt78: Boolean): Int{
    return when (packageName) {
        "com.whatsapp" -> 1
        "com.whatsapp.w4b"  -> 1
        "com.twitter.android" -> 2
        "com.instagram.android" -> 4
        "com.facebook.katana" -> 5
        "com.facebook.lite" -> 5
        "com.facebook.orca" -> 6
        "com.facebook.mlite" -> 6
        "com.skype.raider" -> 7
        "com.skype.m2" -> 7
        "com.tencent.mobileqq" -> 8
        "com.tencent.mm" -> 9
        "jp.naver.line.android" -> 10
        "com.linecorp.linelite" -> 10
        "com.weico.international" -> 11
        "com.sina.weibo" -> 11
        "com.kakao.talk" -> 12
        "org.telegram.messenger" -> {
            if (!dt78){
                13
            } else {
                0
            }
        }
        "com.viber.voip" -> {
            if (!dt78){
                 14
            } else {
                0
            }
        }
        else -> 0
    }
}


fun alarmRepeat(x: Int, context: Context): String{
    return when (x){
        128 -> {
            context.resources.getString(R.string.once)
        }
        127 -> {
            context.resources.getString(R.string.everyday)
        }
        31 -> {
            context.resources.getString(R.string.mon_fri)
        }
        else -> {
            var out = ""
            var y = x
            if (y/64 == 1){
                out = " "+context.resources.getString(R.string.sun)+out
            }
            y %= 64
            if (y/32 == 1){
                out = " "+context.resources.getString(R.string.sat)+out
            }
            y %= 32
            if (y/16 == 1){
                out = " "+context.resources.getString(R.string.fri)+out
            }
            y %= 16
            if (y/8 == 1){
                out = " "+context.resources.getString(R.string.thur)+out
            }
            y %= 8
            if (y/4 == 1){
                out = " "+context.resources.getString(R.string.wed)+out
            }
            y %= 4
            if (y/2 == 1){
                out = " "+context.resources.getString(R.string.tue)+out
            }
            y %= 2
            if (y/1 == 1){
                out = " "+context.resources.getString(R.string.mon)+out
            }
            out
        }
    }
}