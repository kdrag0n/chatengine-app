package com.khronodragon.android.utils

import java.text.DateFormat
import java.util.*

object TimeUtils {
    lateinit var dateFormat: DateFormat
}

fun Date.formatAsTime() : String {
    return TimeUtils.dateFormat.format(this) ?: "?"
}