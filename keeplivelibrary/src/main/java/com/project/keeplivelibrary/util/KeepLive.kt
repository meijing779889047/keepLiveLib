package  com.project.keeplivelibrary.util

import android.content.Intent
import android.os.Build
import android.util.Log
import com.project.keeplivelibrary.impl.KeepLiveService
import com.project.keeplivelibrary.notification.ForegroundNotification
import com.project.keeplivelibrary.service.JobHandlerService
import com.project.keeplivelibrary.service.LocalService
import com.project.keeplivelibrary.service.RemoteService

object KeepLive {

    /**
     * 运行模式
     */
    enum class RunMode {
        /**
         * 省电模式
         * 省电一些，但保活效果会差一点
         */
        ENERGY,
        /**
         * 流氓模式
         * 相对耗电，但可造就不死之身
         */
        ROGUE
    }


    var mForegroundNotification: ForegroundNotification?=null
    var mKeepLiveService: KeepLiveService?=null
    var mRunMode: RunMode? = null
    var useSilenceMusice = true
    fun  startKeepLive(mForegroundNotification: ForegroundNotification?,mKeepLiveService: KeepLiveService){
        if(ServiceUtils.isMain(Util.getContext())) {
            this.mForegroundNotification=mForegroundNotification
            this.mKeepLiveService=mKeepLiveService
            //5.0以上的手机
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                val intent = Intent(Util.getContext(), JobHandlerService::class.java)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                    Util.getContext().startForegroundService(intent)
                } else {
                    Util.getContext().startService(intent)
                }
            } else {//5.0以下的手机  双进程服务
                val intent1 = Intent(Util.getContext(), LocalService::class.java)
                Util.getContext().startService(intent1)
                val intent2 = Intent(Util.getContext(), RemoteService::class.java)
                Util.getContext().startService(intent2)
            }
            Log.i("KeepLive", "启动保活")
        }
    }

}