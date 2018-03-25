package com.kdragon.android.utils

import java.util.*

private val random = Random()

fun<E> List<E>.random(): E {
    return get(random.nextInt(size))
}