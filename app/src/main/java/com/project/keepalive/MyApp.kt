package com.project.keepalive

import android.app.Application
import com.project.keeplivelibrary.util.Util

class MyApp : Application() {


    override fun onCreate() {
        super.onCreate()
        Util.initContext(this)
    }

}