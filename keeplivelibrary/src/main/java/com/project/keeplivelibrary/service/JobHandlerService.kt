package com.project.keeplivelibrary.service

import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import com.project.keeplivelibrary.notification.NotificationUtils
import com.project.keeplivelibrary.receiver.NotificationClickReceiver
import com.project.keeplivelibrary.util.KeepLive
import com.project.keeplivelibrary.util.ServiceUtils
import com.project.keeplivelibrary.util.Util

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
        if (!ServiceUtils.isServiceRunning(Util.getContext(), "com.project.keeplivelibrary.service.LocalService") || !ServiceUtils.isRunningTaskExist(
                Util.getContext(), "$packageName:remote")) {
            Log.i("JobHandlerService","开启服务222")
             startService()
        }
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        if (!ServiceUtils.isServiceRunning(Util.getContext(), "com.project.keeplivelibrary.service.LocalService") || !ServiceUtils.isRunningTaskExist(
                Util.getContext(), "$packageName:remote")) {
            Log.i("JobHandlerService","开启服务333")
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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