package com.android.rockchip;

import java.io.*; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.IllegalArgumentException;

import android.util.Log;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.DiskInfo;
import android.os.storage.StorageVolume;

/** 
 *StorageUtils is use to get correct storage path between ics and gingerbread.
 *add by lijiehong
 */ 
public class StorageUtils { 
    public static String TAG = "StorageUtils";
    public static String mUsbDirs;
    public static String mSDcardDir;

    
    public static boolean isMountSD(StorageManager storageManager) {

        final List<VolumeInfo> volumes = storageManager.getVolumes();
        //Collections.sort(volumes, VolumeInfo.getDescriptionComparator());

        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                DiskInfo disk = vol.getDisk();
                if (disk != null && disk.isSd()) {
                    // sdcard dir
                    int status = vol.getState();
                    Log.d(TAG,"isMountSD()--status-->" + status);
                    if (status == VolumeInfo.STATE_MOUNTED) {
                        return true;
                    }
                    //continue;
                
                }
            }
        }
        return false;
    }
   
    public static String getSDcardDir(StorageManager storageManager) {

        final List<VolumeInfo> volumes = storageManager.getVolumes();
        //Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                DiskInfo disk = vol.getDisk();
                if (disk != null) {
                    if (disk.isSd()) {
                        // usb dir
//                        StorageVolume sv = vol.buildStorageVolume(context, context.getUserId(),
//                                false);
                        mSDcardDir = vol.path;
                    }
                }
            }
        }
		
		if (null != mSDcardDir) {
            int end = mSDcardDir.lastIndexOf('/');
            if (end > 0)    // case mSDcardDir = /xxx/xxx
                return mSDcardDir.substring(0, end);
            else            // case mSDcardDir = /xxx
                return mSDcardDir;
			//return mSDcardDir;
        } else {
            return null;
        }
 
    }
    
    public static List<String> getSdCardPaths(StorageManager storageManager){
    	List<String> sdPaths = new ArrayList<String>();
        final List<VolumeInfo> volumes = storageManager.getVolumes();
        //Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                DiskInfo disk = vol.getDisk();
                if (disk != null) {
                    if (disk.isSd()) {
                        sdPaths.add(vol.path);
                    }
                }
            }
        }
		
		return sdPaths;
    
    }
    
    
    public static List<String> getUsbPaths(StorageManager storageManager){

    	List<String> usbPaths = new ArrayList<String>();
        final List<VolumeInfo> volumes = storageManager.getVolumes();
        File sataDir = new File(getSataDir());
        //Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        for (VolumeInfo vol : volumes) {
        	if (RockExplorer.SATA_ENABLED && vol.path != null && new File(vol.path).getName().equals(sataDir.getName())) {
        		Log.d(TAG, "volume path maybe sata, skip " + vol.path);
        		continue;
        	}
        	
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                DiskInfo disk = vol.getDisk();
                if (disk != null) {
                    if (disk.isUsb()) {
                    	usbPaths.add(vol.path);
                    }
                }
            }
        }
		
		return usbPaths;
    
    }
    
    public static boolean isMountUSB(StorageManager storageManager) {

        final List<VolumeInfo> volumes = storageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());

        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                DiskInfo disk = vol.getDisk();
                if (disk != null) {
                    if (disk.isUsb()) {
                        // usb dir
                        int status = vol.getState();
                        Log.d(TAG,"isMountUSB()--status-->" + status);
                        if (status == VolumeInfo.STATE_MOUNTED) {
                            return true;
                        }
                        return false;
                    }
                }
            }
        }
        return false;
    }
    
    public static String  getSSDDir(StorageManager storageManager){
    	return "/mnt/media_rw/ssd_inode";
    }
    
    public static String getSSDDirOld(StorageManager storageManager) {

        final List<VolumeInfo> volumes = storageManager.getVolumes();
        //Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        for (VolumeInfo vol : volumes) {
        	  Log.d("YHX", "VolumeInfo.Type=" + vol.getType());
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                Log.d("YHX", "VolumeInfo.TYPE_PUBLIC");
                Log.d("YHX", "Volume path:" + vol.getPath());
                DiskInfo disk = vol.getDisk();
                if (disk != null) {
                	/*
                    if (disk.isPcie()) {
                        // usb dir
//                        StorageVolume sv = vol.buildStorageVolume(context, context.getUserId(),
//                                false);
                        mUsbDirs = vol.path;
                    }
                    */
                }
            }
        }
        
        if (null != mUsbDirs) {
          int end = mUsbDirs.lastIndexOf('/');
            if (end > 0)    // case mUsbDirs = /xxx/xxx
                return mUsbDirs.substring(0, end);
            else            // case mUsbDirs = /xxx
                return mUsbDirs;
        	//return mUsbDirs;
        } else {
            return null;
        }
    }
    
    
    public static List<String> getSSDPaths(StorageManager storageManager){

    	List<String> ssdPaths = new ArrayList<String>();
        final List<VolumeInfo> volumes = storageManager.getVolumes();
        File sataDir = new File(getSataDir());
        //Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        for (VolumeInfo vol : volumes) {
        	if (RockExplorer.SATA_ENABLED && vol.path != null && new File(vol.path).getName().equals(sataDir.getName())) {
        		Log.d(TAG, "volume path maybe sata, skip " + vol.path);
        		continue;
        	}
        	
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                DiskInfo disk = vol.getDisk();
                if (disk != null) {
                	/*
                    if (disk.isPcie()) {
                    	ssdPaths.add(vol.path);
                    }
                    */
                }
            }
        }
		
		return ssdPaths;
    
    }

    public static String getUsbDir(StorageManager storageManager) {

        final List<VolumeInfo> volumes = storageManager.getVolumes();
        //Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                Log.d("YHX", "VolumeInfo.TYPE_PUBLIC");
                Log.d("YHX", "Volume path:" + vol.getPath());
                DiskInfo disk = vol.getDisk();
                if (disk != null) {
                    if (disk.isUsb()) {
                        // usb dir
//                        StorageVolume sv = vol.buildStorageVolume(context, context.getUserId(),
//                                false);
                        mUsbDirs = vol.path;
                    }
                }
            }
        }
        
        if (null != mUsbDirs) {
          int end = mUsbDirs.lastIndexOf('/');
            if (end > 0)    // case mUsbDirs = /xxx/xxx
                return mUsbDirs.substring(0, end);
            else            // case mUsbDirs = /xxx
                return mUsbDirs;
        	//return mUsbDirs;
        } else {
            return null;
        }
    }
    
    public static String getFlashDir(){
        if(android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.GINGERBREAD){
            Log.d(TAG,"%%%%%%%%%%-------- using at gingerbread2.3.1 ----------!!!");
            return ((File)invokeStaticMethod("android.os.Environment","getFlashStorageDirectory",null)).getPath();
        }else if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD){
            Log.d(TAG,"%%%%%%%%%%-------- using at ics4.0.3 ----------!!!");
            return ((File)invokeStaticMethod("android.os.Environment","getExternalStorageDirectory",null)).getPath();
        }
        return null;
    }

    public static String getFlashState(){
        if(android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.GINGERBREAD){
            Log.d(TAG,"%%%%%%%%%%-------- using at gingerbread2.3.1 ----------!!!");
            return (String)invokeStaticMethod("android.os.Environment","getFlashStorageState",null);
        }else if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD){
            Log.d(TAG,"%%%%%%%%%%-------- using at ics4.0.3 ----------!!!");
            return (String)invokeStaticMethod("android.os.Environment","getExternalStorageState",null);
        }
        return Environment.MEDIA_REMOVED;
    }
    
    public static String getSataDir(){
    	if (!RockExplorer.SATA_ENABLED) {
    		return "";
    	}
    	
    	File mntFile = new File("/mnt/media_rw");
        if (!mntFile.exists() || !mntFile.isDirectory()) {
            return "";
        }

        File[] files = mntFile.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            Log.d(TAG, "sata - list mnt media_rw storage: " + fileName);
            if (fileName.length() > 10 && fileName.indexOf("-") == -1) {
                return file.getAbsolutePath();
            }
            //return file.getAbsolutePath();
        }

        return "";
    }

    public static String getSataState() {
    	String satadir = getSataDir();
    	if (satadir == null || "".equals(satadir)) {
    		return Environment.MEDIA_REMOVED;
    	} else {
    		return Environment.MEDIA_MOUNTED;
    	}
    }

    public static Object invokeStaticMethod(Class<?> cls, String methodName, Object... arguments) {
        try {
            Class<?>[] parameterTypes = null;
            if(arguments!=null){
                parameterTypes = new Class<?>[arguments.length];
                for(int i=0; i<arguments.length; i++){
                    parameterTypes[i] = arguments[i].getClass();
                }
            }
            Method method = cls.getMethod(methodName, parameterTypes);
            return method.invoke(null, arguments);
        }catch (Exception ex) {
            Log.d(TAG,"Invoke method error. " + ex.getMessage());
            //handleReflectionException(ex);
            return null;
        }
    }

    public static Object invokeStaticMethod(String className, String methodName, Object... arguments) {
        try {
            Class<?> cls = Class.forName(className);
            return invokeStaticMethod(cls, methodName, arguments);
        }catch (Exception ex) {
            Log.d(TAG,"Invoke method error. " + ex.getMessage());
            //handleReflectionException(ex);
            return null;
        }
    }

    public static Object invokeStaticMethod(String className, String methodName, Class<?>[] types , Object... arguments) {
        try {
            Class<?> cls = Class.forName(className);
            return invokeStaticMethod(cls, methodName, types, arguments);
        }catch (Exception ex) {
            Log.d(TAG,"Invoke method error. " + ex.getMessage());
            //handleReflectionException(ex);
            return null;
        }
    }

    public static Object invokeStaticMethod(Class<?> cls, String methodName, Class<?>[] types, Object... arguments) {
        try {
            Method method = cls.getMethod(methodName, types);
            return method.invoke(null, arguments);
        }catch (Exception ex) {
            Log.d(TAG,"Invoke method error. " + ex.getMessage());
            //handleReflectionException(ex);
            return null;
        }
    }

} 


 

