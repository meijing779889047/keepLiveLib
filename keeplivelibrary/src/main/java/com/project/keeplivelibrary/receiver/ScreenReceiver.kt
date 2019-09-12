package com.project.keeplivelibrary.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.project.keeplivelibrary.service.LocalService
import com.project.keeplivelibrary.ui.OnePixelActivity
import com.project.keeplivelibrary.util.HnAppManager
import java.lang.Exception

/**
 * 屏幕锁屏和解锁广播监听器
 */
class ScreenReceiver : BroadcastReceiver() {

    private val mHandler:Handler  by  lazy {
        Handler(Looper.getMainLooper())
    }
    private var mScreenOn=true

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.e("mj", "屏幕锁屏和解锁广播监听器：$action")
        if (Intent.ACTION_SCREEN_OFF == action) {
            mScreenOn=false
            mHandler.postDelayed({
                if(!mScreenOn){
                   val mIntent=Intent(context, OnePixelActivity::class.java)
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP )
                   val mPendingIntent=PendingIntent.getActivity(context,0,mIntent,0)
                   try {
                       mPendingIntent.send()
                   } catch (e:Exception){
                       e.printStackTrace()
                   }
                }
            },1000)


            context.sendBroadcast(Intent(LocalService.ACTION_SCREEN_OFF))
        } else if (Intent.ACTION_SCREEN_ON == action) {
            mScreenOn=true
            HnAppManager.getInstance().finishActivity(OnePixelActivity::class.java)
            context.sendBroadcast(Intent(LocalService.ACTION_SCREEN_ON))
        }
    }
}
