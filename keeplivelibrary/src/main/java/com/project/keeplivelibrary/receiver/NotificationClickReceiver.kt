package com.project.keeplivelibrary.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.project.keeplivelibrary.util.KeepLive


class NotificationClickReceiver  : BroadcastReceiver() {
    companion object {
        val Notification_Click_Action="CLICK_NOTIFICATION"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action=intent?.action
         Log.i("KeepLive","接收到NotificationClickReceiver："+action)
        if(Notification_Click_Action==action){
            KeepLive.mForegroundNotification?.foregroundNotificationClickListener?.foregroundNotificationClick(context,intent)
        }
    }
}