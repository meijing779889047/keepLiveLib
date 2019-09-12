package  com.project.keeplivelibrary.util

import android.content.Context

object Util {

    private var mContext:Context?=null


    fun  initContext(mContext:Context){
        Util.mContext =mContext
    }

    fun  getContext():Context{
        return  mContext!!
    }



}