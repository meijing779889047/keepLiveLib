package com.project.keeplivelibrary.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.project.keepalive.GuardAidl
import com.project.keeplivelibrary.notification.NotificationUtils
import com.project.keeplivelibrary.receiver.NotificationClickReceiver
import com.project.keeplivelibrary.util.ServiceUtils
import com.project.keeplivelibrary.util.Util

/**
 * 守护进程
 * 1.绑定本地服务
 * 2.
 */
class RemoteService : Service() {

    private var mIsBoundLocalService: Boolean = false

    private val myBinder: RemoteBinder by lazy {
        RemoteBinder()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return myBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("RemoteService","启动RemoteService")
        bindService()
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (mIsBoundLocalService) {
                unbindService(mRemoteConnect)
            }
        } catch (e: java.lang.Exception) {

        }
    }


    private var mRemoteConnect = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            if (ServiceUtils.isRunningTaskExist(Util.getContext(), packageName + ":remote")) {
                val intent = Intent(Util.getContext(), LocalService::class.java)
                Util.getContext().startService(intent)
                bindService()
            }
            val pm = Util.getContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            val isScreenOn = pm.isScreenOn
            if (isScreenOn) {
                sendBroadcast(Intent(LocalService.ACTION_SCREEN_ON))
            } else {
                sendBroadcast(Intent(LocalService.ACTION_SCREEN_OFF))
            }
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}

    }


    private inner class RemoteBinder : GuardAidl.Stub() {
        override fun wakeUp(title: String?, discription: String?, iconRes: Int) {
            startForeground(title, discription, iconRes)
            Log.i("RemoteService","唤醒")
        }
    }

    /**
     * 开启一个前台通知
     */
    fun startForeground(title: String?, discription: String?, iconRes: Int) {
        if (Build.VERSION.SDK_INT < 25) {
            val intent = Intent(Util.getContext(), NotificationClickReceiver::class.java)
            intent.action = NotificationClickReceiver.Notification_Click_Action
            val notification =
                NotificationUtils.createNotification(this@RemoteService, title!!, discription!!, iconRes, intent)
            this@RemoteService.startForeground(13691, notification)
        }
    }

    fun bindService() {
        try {
            mIsBoundLocalService = this.bindService(Intent(this@RemoteService, LocalService::class.java), mRemoteConnect, Context.BIND_ABOVE_CLIENT)
            Log.i("RemoteService","绑定本地服务")
        } catch (e: Exception) {
        }
    }
}