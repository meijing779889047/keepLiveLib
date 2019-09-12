package com.project.keeplivelibrary.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.project.keeplivelibrary.util.HnAppManager


open abstract  class BaseActivity :  AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HnAppManager.getInstance().addActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("KeepLive","结束界面:"+this.javaClass)
//        HnAppManager.getInstance().finishActivity(this)
    }
}