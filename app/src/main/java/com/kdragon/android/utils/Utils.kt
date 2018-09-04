@file:Suppress("NOTHING_TO_INLINE")

package com.kdragon.android.utils

import android.annotation.SuppressLint
import android.os.AsyncTask
import java.text.DateFormat
import java.util.*

val random = Random()

inline fun<E> List<E>.random(): E {
    return get(random.nextInt(size))
}

inline fun Date.formatAsTime(fmt: DateFormat) : String {
    return fmt.format(this) ?: "?"
}

inline operator fun String.times(num: Int): String {
    return repeat(num)
}

@SuppressLint("StaticFieldLeak")
inline fun asyncExec(crossinline func: () -> Unit) {
    object : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) = func()
    }.execute()
}