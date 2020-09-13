package com.fbiego.dt78.data

import android.graphics.Color
import kotlin.experimental.and

fun Byte.toPInt() = toInt() and 0xFF

fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) {
        pos -> ints[pos].toByte()
}

fun colors(col: Int): Int {
    return when (col) {
        0 -> Color.parseColor("#44FF0000")
        1 -> Color.parseColor("#44FFFF00")
        2 -> Color.parseColor("#4400FF00")
        3 -> Color.parseColor("#44008000")
        4 -> Color.parseColor("#4400FFFF")
        5 -> Color.parseColor("#44008080")
        6 -> Color.parseColor("#440000FF")
        7 -> Color.parseColor("#44000080")
        8 -> Color.parseColor("#44FF00FF")
        9 -> Color.parseColor("#44800080")
        else -> Color.parseColor("#44FFFFFF")
    }
}