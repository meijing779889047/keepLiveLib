package com.project.keepalive.ui
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.project.keepalive.R
import com.project.keeplivelibrary.impl.KeepLiveService
import com.project.keeplivelibrary.notification.ForegroundNotification
import com.project.keeplivelibrary.ui.BaseActivity
import com.project.keeplivelibrary.util.KeepLive
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity(), KeepLiveService {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvName.text="111"
        tvName.setOnClickListener {
            val intent =   Intent("send_action");
            intent.component = ComponentName("com.project.keepalive", "send_action");
            sendBroadcast(intent);
        }
        val mForegroundNotification= ForegroundNotification("正在运行","",R.mipmap.ic_launcher)
        mForegroundNotification.foregroundNotificationClickListener { context, intent ->
            Log.i("KeepLive","点击了通知栏")
        }
        KeepLive.startKeepLive(mForegroundNotification,this)
    }

    override fun onServiceWorking() {
        Log.i("KeepLive","onServiceWorking")
    }

    override fun onServiceStop() {
        Log.i("KeepLive","onServiceStop")
    }


}
