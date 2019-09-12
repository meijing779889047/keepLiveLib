package com.project.keeplivelibrary.ui

import android.os.Bundle
import android.util.Log
import android.view.Gravity


/**
 * 说明：
 * 1.1像素的Activity
 * 2.需要设置Activity的style为透明
 */
class OnePixelActivity  : BaseActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mWindow=window
        mWindow.setGravity(Gravity.TOP or Gravity.START)
        val param= mWindow.attributes
        param.x=0
        param.y=0
        param.height=1
        param.width=1
        mWindow.attributes=param
        Log.i("OnePixelActivity","启动OnePixelActivity")
    }


}