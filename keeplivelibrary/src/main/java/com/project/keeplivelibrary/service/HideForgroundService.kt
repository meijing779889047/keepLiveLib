package com.project.keeplivelibrary.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.project.keeplivelibrary.notification.NotificationUtils
import com.project.keeplivelibrary.receiver.NotificationClickReceiver
import com.project.keeplivelibrary.util.KeepLive
import com.project.keeplivelibrary.util.Util


/**
 * 隐藏前台通知的服务
 */
class HideForgroundService  : Service() {

    /**handler*/
    private val mHandler: Handler  by lazy {
        Handler(Looper.getMainLooper())
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        mHandler?.postDelayed({
            stopForeground(true)
            stopSelf()
            Log.i("HideForgroundService","隐藏前台通知的服务")
        },8000)
        return START_NOT_STICKY
    }

    private fun startForeground() {
        //启用前台服务，提升优先级
        KeepLive.mForegroundNotification?.let {
                val intent2 = Intent(Util.getContext(), NotificationClickReceiver::class.java)
                intent2.action = NotificationClickReceiver.Notification_Click_Action
                val notification =
                    NotificationUtils.createNotification(this, it.title, it.description, it.iconRes, intent2)
                startForeground(13691, notification)

            }

    }


    override fun onBind(intent: Intent?): IBinder? {
         return null
    }
}