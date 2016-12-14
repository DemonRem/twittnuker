package org.mariotaku.ktextension

fun String?.toLong(def: Long): Long {
    try {
        return this?.toLong() ?: def
    } catch (e: NumberFormatException) {
        return def
    }
}

fun String?.toInt(def: Int): Int {
    try {
        return this?.toInt() ?: def
    } catch (e: NumberFormatException) {
        return def
    }
}

fun String.toDoubleOrNull(): Double? {
    try {
        return toDouble()
    } catch (e: NumberFormatException) {
        return null
    }
}