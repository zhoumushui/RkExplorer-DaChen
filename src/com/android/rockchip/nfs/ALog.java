package com.android.rockchip.nfs;

import android.util.Log;

/**
 * Created by balu on 2017/7/9.
 */

public class ALog {
    public static final String TAG = "EasyNFS";

    public static void d(String str){
        Log.d(TAG, str);
    }

    public static void d2(String str){
        //Log.d(TAG, str);
    }

    public static void i(String str){
        Log.i(TAG, str);
    }

    public static void w(String str){
        Log.w(TAG, str);
    }

    public static void e(String str){
        Log.e(TAG, str);
    }
}
