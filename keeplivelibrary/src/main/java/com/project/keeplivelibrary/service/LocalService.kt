package com.project.keeplivelibrary.service

import android.app.Service
import android.content.*
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import com.project.keepalive.GuardAidl
import com.project.keeplivelibrary.R
import com.project.keeplivelibrary.notification.NotificationUtils
import com.project.keeplivelibrary.receiver.NotificationClickReceiver
import com.project.keeplivelibrary.receiver.ScreenReceiver
import com.project.keeplivelibrary.util.KeepLive
import com.project.keeplivelibrary.util.ServiceUtils
import com.project.keeplivelibrary.util.Util

class LocalService : Service() {

    companion object {
        const  val  ACTION_SCREEN_OFF="_ACTION_SCREEN_OFF"
        const  val  ACTION_SCREEN_ON="_ACTION_SCREEN_ON"

    }

    /**屏幕唤醒/锁屏广播*/
    private val mScreenReceiver: BroadcastReceiver  by lazy {
        ScreenReceiver()
    }

    private val mScreenIntentFilter: IntentFilter  by lazy {
        val intentFilter= IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter
    }

    /**屏幕状态发生改变的广播*/
    private val mScreenStateReceiver: ScreenStateReceiver  by lazy {
        ScreenStateReceiver()
    }

    private val mScreenStateilter: IntentFilter  by lazy {
        val intentFilter= IntentFilter()
        intentFilter.addAction(ACTION_SCREEN_OFF)
        intentFilter.addAction(ACTION_SCREEN_ON)
        intentFilter
    }

    /**控制音乐播放/暂停*/
    private var isPause = true

    private val mMediaPlay:MediaPlayer  by  lazy {
        MediaPlayer.create(Util.getContext(), R.raw.novioce)
    }


    private val myBinder:MyBinder by lazy {
        MyBinder()
    }

    /**handler*/
    private val mHandler:Handler  by lazy {
        Handler(Looper.getMainLooper())
    }


    private var mIsBoundRemoteService: Boolean=false

    override fun onCreate() {
        super.onCreate()
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        isPause = pm.isScreenOn
        Log.i("LocalService","开启LocalService")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (KeepLive.useSilenceMusice){
            mMediaPlay?.let {
                it.setVolume(0f,0f)
                it.setOnCompletionListener {
                    if(!isPause){
                        if (KeepLive.mRunMode == KeepLive.RunMode.ROGUE){
                            playMusic()
                        }else{
                            mHandler?.postDelayed({
                                playMusic()
                            },5000)
                        }
                    }
                }
                playMusic()
            }
        }
        //注册屏幕锁屏或解锁广播
        registerReceiver(mScreenReceiver,mScreenIntentFilter)
        //注册屏幕状态发生改变的广播
        registerReceiver(mScreenStateReceiver,mScreenStateilter)
        //启用前台服务，提升优先级
        KeepLive.mForegroundNotification?.let {
                val intent2 = Intent(Util.getContext(), NotificationClickReceiver::class.java)
                intent2.action = NotificationClickReceiver.Notification_Click_Action
                val notification =
                    NotificationUtils.createNotification(this, it.title, it.description, it.iconRes, intent2)
                startForeground(13691, notification)
         }

        //开启守护进程
        try {
            val intent3 = Intent(this, RemoteService::class.java)
            mIsBoundRemoteService = this.bindService(intent3, mMuliteProcessConnection, Context.BIND_ABOVE_CLIENT)
            Log.i("LocalService","绑定远程服务")
        } catch (e: Exception) {
        }
        //隐藏服务通知
        try {
           if (Build.VERSION.SDK_INT < 25) {
            startService(Intent(this, HideForgroundService::class.java))
            }
        } catch (e: Exception) {
        }

        KeepLive.mKeepLiveService?.let {
            it.onServiceWorking()
        }
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
      return myBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        mScreenReceiver?.let {
            unregisterReceiver(mScreenReceiver)
        }
        mScreenStateReceiver?.let {
            unregisterReceiver(mScreenStateReceiver)
        }
        mMuliteProcessConnection?.let {
            if(mIsBoundRemoteService) {
                unbindService(mMuliteProcessConnection)
            }
        }
        KeepLive.mKeepLiveService?.let {
           it.onServiceStop()
        }
        Log.i("LocalService","关闭LocalService")
    }


    private   class  MyBinder : GuardAidl.Stub(){
        override fun wakeUp(title: String?, discription: String?, iconRes: Int) {
            Log.i("LocalService","双进程服务之本地进程唤醒")
         }
    }

    private  inner class   ScreenStateReceiver : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action=intent?.action
            when(action){
                ACTION_SCREEN_OFF ->{
                    isPause = false
                    playMusic()
                }
                ACTION_SCREEN_ON ->{
                    isPause = true
                   pauseMusic()
                }
            }
        }
    }


    private var  mMuliteProcessConnection =object :ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            if (ServiceUtils.isServiceRunning(Util.getContext(), "com.project.keeplivelibrary.service.LocalService")) {
                val remoteService = Intent(this@LocalService, RemoteService::class.java)
                this@LocalService.startService(remoteService)
                val intent = Intent(this@LocalService, RemoteService::class.java)
                mIsBoundRemoteService = this@LocalService.bindService(intent,this, Context.BIND_ABOVE_CLIENT)
            }
            val pm = Util.getContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            val isScreenOn = pm.isScreenOn
            if (isScreenOn) {
                sendBroadcast(Intent(LocalService.ACTION_SCREEN_ON))
            } else {
                sendBroadcast(Intent(LocalService.ACTION_SCREEN_OFF))
            }
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            KeepLive.mForegroundNotification?.let {
              val mGuardAidl=  GuardAidl.Stub.asInterface(service)
              mGuardAidl.wakeUp(it.title,it.description,it.iconRes)
            }

         }
    }


    fun  playMusic(){
        if(!mMediaPlay?.isPlaying&&KeepLive.useSilenceMusice){
            mMediaPlay.start()
            Log.i("LocalService","播放音乐")
        }
    }

    fun  pauseMusic(){
        if(mMediaPlay?.isPlaying&&KeepLive.useSilenceMusice){
            mMediaPlay.pause()
            Log.i("LocalService","暂停播放音乐")
        }
    }

}