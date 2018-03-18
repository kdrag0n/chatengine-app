package com.khronodragon.android.utils

import android.annotation.SuppressLint
import android.os.AsyncTask

@SuppressLint("StaticFieldLeak")
fun asyncExec(func: () -> Unit) {
     object : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) = func()
    }.execute()
}