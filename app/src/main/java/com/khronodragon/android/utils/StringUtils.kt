package com.khronodragon.android.utils

inline operator fun String.times(num: Int): String {
    return repeat(num)
}