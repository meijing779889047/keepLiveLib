##  Android 保活方案   

####  问题

  * 随着系统资源的消耗，当Android系统的内存不足时，会杀掉一些进程以此来释放内存，那我们的程序也很可能在后台被干掉，那么在用户体验度上就有所降低了，所以有时候需要想办法提高程序的级别，尽可能的让我们的程序能够长久存活下去，而不被销毁。

####  解决方案

  *  方案1

      * 使用1像素的Activity，当屏幕锁屏时，我们监听锁屏广播开启一个Activity，但是它只有一个像素，在屏幕解锁时结束掉Activity，以此来提升进程的优先级

  * 方案2

      * 双进程守护，使用两个进程相互唤起，因进程的回收是一个一个的回收的，那么我们可以在一个进程被杀掉后，使用另一个来重启，但是在8.0之后对启动服务做了限制，需要使用startForegroundService，但是会有一个通知栏，这一点不是很友好

 * 方案3

       使用JobService，利用系统调度去开启一个服务，同时会在解锁屏幕，充电，重启等操作去重新执行里面的job，不过也可能被杀死


####  实现

   * 1.创建一个Activity，这个Activity只有一个像素，并且设置主题背景为透明
			
			
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



   *  在接收到锁屏广播时，启动该界面；当接收到屏幕解锁广播时，关闭该界面

			
			import android.app.PendingIntent
			import android.content.BroadcastReceiver
			import android.content.Context
			import android.content.Intent
			import android.os.Handler
			import android.os.Looper
			import android.util.Log
			import com.project.keepalive.keepLive.service.LocalService
			import com.project.keepalive.ui.OnePixelActivity
			import com.project.keepalive.utils.HnAppManager
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
			                   val mIntent=Intent(context,OnePixelActivity::class.java)
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


  * 设置Activity的主题

        //1.style.xml

		
		    <!--1像素保活透明Activity-->
		    <style name="onePixelActivity" parent="@style/Theme.AppCompat.Light.NoActionBar">
		        <item name="android:windowIsTranslucent">true</item>
		        <item name="android:windowBackground">@android:color/transparent</item>
		    </style>

         //2.在清单文件中设置
	
	     <activity android:name=".ui.OnePixelActivity"
	        android:theme="@style/onePixelActivity"
	        android:launchMode="singleInstance"
	        android:excludeFromRecents="true"/>




   * 2.使用JobService来开启前台服务，同时通过startService（）启动本地服务和远程服务以及前台进程服务（因为在8.0以上系统的前台服务通知栏不能隐藏，会影响体验感，所以前台服务通知栏只能适配到8.0，暂时没有找到好的解决方案），注意JobService在5.0以上才支持



			import android.app.Service
			import android.app.job.JobInfo
			import android.app.job.JobParameters
			import android.app.job.JobScheduler
			import android.app.job.JobService
			import android.content.ComponentName
			import android.content.Context
			import android.content.Intent
			import android.os.Build
			import android.os.IBinder
			import android.support.annotation.RequiresApi
			import android.util.Log
			import com.project.keepalive.keepLive.notification.NotificationUtils
			import com.project.keepalive.keepLive.receiver.NotificationClickReceiver
			import com.project.keepalive.keepLive.util.KeepLive
			import com.project.keepalive.keepLive.util.ServiceUtils
			import com.project.keepalive.utils.Util
			
			@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
			class JobHandlerService : JobService(){
			
			    companion object {
			        var mIsStartService=false
			    }
			
			
			    private val mJobScheduler:JobScheduler  by lazy {
			       Util.getContext().getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
			   }
			
			      override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
			        Log.i("JobHandlerService","启动了JobHandlerService")
			        startService()
			        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
			            val jobId=startId+1
			            val builder=JobInfo.Builder(jobId, ComponentName(packageName, JobHandlerService::class.java.name))
			            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
			                builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS) //执行的最小延迟时间
			                builder.setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS)  //执行的最长延时时间
			                builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS)
			                builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR)//线性重试方案
			            }else{
			                builder.setPeriodic(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS)
			            }
			            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
			            builder.setRequiresCharging(true) // 当插入充电器，执行该任务
			            mJobScheduler.schedule(builder.build())
			        }
			        return Service.START_STICKY
			    }
			
			    override fun onStartJob(jobParameters: JobParameters): Boolean {
			        if (!ServiceUtils.isServiceRunning(Util.getContext(), "com.project.keepalive.keepLive.service.LocalService") || !ServiceUtils.isRunningTaskExist(Util.getContext(), "$packageName:remote")) {
			             startService()
			        }
			        return false
			    }
			
			    override fun onStopJob(jobParameters: JobParameters): Boolean {
			        if (!ServiceUtils.isServiceRunning(Util.getContext(), "com.project.keepalive.keepLive.service.LocalService") || !ServiceUtils.isRunningTaskExist(Util.getContext(), "$packageName:remote")) {
	
			            mIsStartService=false
			            startService()
			        }
			        return false
			    }
			
			    /**
			     * 启动服务
			     */
			    private fun startService(){
			        if(!mIsStartService) {
			            mIsStartService=true
			            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
			                KeepLive.mForegroundNotification?.let {
			                    val intent = Intent(Util.getContext(), NotificationClickReceiver::class.java)
			                    intent.action = NotificationClickReceiver.Notification_Click_Action
			                    val mNotification = NotificationUtils.createNotification(
			                        Util.getContext(),
			                        it.title,
			                        it.description,
			                        it.iconRes,
			                        intent
			                    )
			                    startForeground(13691, mNotification)
			                }
			            }
			            val intent1 = Intent(Util.getContext(), LocalService::class.java)
			            Util.getContext().startService(intent1)
			            val intent2 = Intent(Util.getContext(), RemoteService::class.java)
			            Util.getContext().startService(intent2)
			        }
			    }
			
			    override fun onDestroy() {
			        super.onDestroy()
			        mIsStartService=false
			    }
			
			}


    * 3.使用AIDL实现本地服务和远程服务的双向绑定，同时接收屏幕解锁/屏幕锁屏广播播放音乐，设置声音为0

        * 本地服务  


				import android.app.Service
				import android.content.*
				import android.media.MediaPlayer
				import android.os.*
				import android.util.Log
				import com.project.keepalive.GuardAidl
				import com.project.keepalive.R
				import com.project.keepalive.keepLive.notification.NotificationUtils
				import com.project.keepalive.keepLive.receiver.NotificationClickReceiver
				import com.project.keepalive.keepLive.receiver.ScreenReceiver
				import com.project.keepalive.keepLive.util.KeepLive
				import com.project.keepalive.keepLive.util.ServiceUtils
				import com.project.keepalive.utils.Util
				
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
				            val notification = NotificationUtils.createNotification(this, it.title, it.description, it.iconRes, intent2)
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
				            if (ServiceUtils.isServiceRunning(Util.getContext(), "com.project.keepalive.keepLive.service.LocalService")) {
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



      *  远程服务


				
				
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
				import com.project.keepalive.keepLive.notification.NotificationUtils
				import com.project.keepalive.keepLive.receiver.NotificationClickReceiver
				import com.project.keepalive.keepLive.util.ServiceUtils
				import com.project.keepalive.utils.Util
				
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
				        if (Build.VERSION.SDK_INT <Build.VERSION_CODES.N_MR1) {
				            val intent = Intent(Util.getContext(), NotificationClickReceiver::class.java)
				            intent.action = NotificationClickReceiver.Notification_Click_Action
				            val notification = NotificationUtils.createNotification(this@RemoteService, title!!, discription!!, iconRes, intent)
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
