package com.android.rockchip.nfs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by balu on 2017/7/30.
 */

public class NfsService {

    private static final String CMD_MOUNT_NFS = "mountnfs";
    private static final String CMD_UNMOUNT_NFS = "umountnfs";
    public static final int NFS_MOUNT_NONE = 0;
    public static final int NFS_MOUNT_OK = 1;
    public static final int NFS_MOUNT_EXISTED = 2;
    private Context mContext;

    public NfsService(Context context) {
        mContext = context;
    }

    public boolean mountNfsDevice(String remoteNfsAddr, String mountPath) {
        ALog.d("mount nfs server in");
        setSysProperty("cvnfs.addr", remoteNfsAddr);
        setSysProperty("cvnfs.path", mountPath);
        if (!runSambaCmd(CMD_MOUNT_NFS)) {
            ALog.e("Failed to nfs server");
            return false;
        }

        if (!hasMount(remoteNfsAddr, mountPath)) {
            ALog.e("Failed to mount nfs server");
            return false;
        }
        ALog.d("mount nfs server ok");
        return true;
    }

    public boolean umountNfsDevice(String mountPath) {
        ALog.d("unmount nfs path in");
        setSysProperty("cvnfs.path", mountPath);
        if (!runSambaCmd(CMD_UNMOUNT_NFS)) {
            ALog.e("Failed to unmount nfs path");
            return false;
        }

        ALog.d("unmount nfs path ok");
        return true;
    }

    public synchronized void addNfsInfo(String serverAddr, String localPath) {
        ALog.d("add nfs info path " + localPath);
        NfsInfo info = new NfsInfo();
        info.setServerAddr(serverAddr);
        info.setLocalPath(localPath);
        List<NfsInfo> nfsList = getNfsInfoList();
        nfsList.add(info);
        saveNfsInfoList(nfsList);
    }

    public synchronized void removeNfsInfo(String localPath) {
        if (TextUtils.isEmpty(localPath)) {
            return;
        }

        ALog.d("remove nfs info path " + localPath);
        List<NfsInfo> nfsList = getNfsInfoList();
        Iterator<NfsInfo> it = nfsList.iterator();
        while (it.hasNext()) {
            NfsInfo info = it.next();
            if (localPath.equals(info.getLocalPath())) {
                it.remove();
            }
        }
        saveNfsInfoList(nfsList);
    }

    public synchronized void saveNfsInfoList(List<NfsInfo> nfsList) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            JSONArray ja = new JSONArray();
            for (int i = 0; i < nfsList.size(); i++) {
                NfsInfo info = nfsList.get(i);
                JSONObject jo = new JSONObject();
                jo.put("serverAddr", info.getServerAddr());
                jo.put("localPath", info.getLocalPath());
                ja.put(jo);
            }
            ALog.d("save nfs device list size: " + ja.length());
            String nfslistStr = ja.length() > 0 ? ja.toString() : "";
            sp.edit().putString("KEY_NFS_LIST", nfslistStr).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized List<NfsInfo> getNfsInfoList() {
        List<NfsInfo> nfsList = new ArrayList<>();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        String nfslistStr = sp.getString("KEY_NFS_LIST", null);
        if (TextUtils.isEmpty(nfslistStr)) {
            return nfsList;
        }
        try {
            JSONArray ja = new JSONArray(nfslistStr);
            for(int i = 0; i < ja.length(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                NfsInfo info = new NfsInfo();
                info.setServerAddr(jo.getString("serverAddr"));
                info.setLocalPath(jo.getString("localPath"));
                nfsList.add(info);
            }
        } catch (Exception e) {
            sp.edit().putString("KEY_NFS_LIST", "").commit();
            e.printStackTrace();
        }
        return nfsList;
    }



    public static void waitCmdFinished(String svc, String cmd) {
        ALog.d("wait service command " + svc + " - " + cmd);
        int timeout = 0;
        while (timeout < 20) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }

            String state = getSysProperty("init.svc." + svc, "");
            if ("stopped".equals(state)) {
                ALog.d("wait service command " + svc + " ok");
                return;
            }
            timeout++;
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
    }

    public static boolean runSambaCmd(String cmd) {
        setSysProperty("cvsmbd.exec", cmd);
        setSysProperty("cvsmbd.cmd", "1");
        waitCmdFinished("cvsmbsvc", cmd);
        return true;
    }

    public static boolean hasMount(String networkPath, String localPath) {
        return (checkMount(networkPath, localPath) == NFS_MOUNT_OK);
    }

    public static int checkMount(String networkPath, String localPath) {
        //File dirFile = new File(localPath);
        //String[] fileNames = dirFile.list();
        //boolean hasFile = (fileNames != null && fileNames.length > 0);
        boolean hasMountRemote = false;
        boolean hasMountLocal = false;
        List<String> lines = execMountCmd();
        for (String line : lines) {
            if (containPath(line, localPath)) {
                hasMountLocal = true;
                if (containPath(line, networkPath)) {
                    hasMountRemote = true;
                }
                break;
            }
        }

        int result = NFS_MOUNT_NONE;
        if (hasMountRemote && hasMountLocal) {
            result = NFS_MOUNT_OK;
        } else if (hasMountLocal) {
            result = NFS_MOUNT_EXISTED;
        }
        ALog.d("Check mount result " + result);
        return result;
    }

    private static boolean containPath(String path, String destPath) {
        if (path.contains(destPath)) {
            return true;
        }

        String replaceLine = path.replaceAll("134", "");
        replaceLine = replaceLine.replaceAll("\\\\", "/");
        if (replaceLine.contains(destPath)) {
            return true;
        }
        return false;
    }

    public static boolean isUMountSuccess(String mountDir){
        File dirFile = new File(mountDir);
        /* 可能共享目录就是空的
        String[] fileNames = dirFile.list();
        if (fileNames != null && fileNames.length > 0) {
            return false;
        }*/

        List<String> lines = execDfCmd();
        for(String line : lines){
            if(line.contains(mountDir))
                return false;
        }
        return true;
    }

    public static List<String> execMountCmd() {
        return execLinuxCmd("mount");
    }

    public static List<String> execDfCmd() {
        return execLinuxCmd("df");
    }

    public synchronized static List<String> execLinuxCmd(String cmd) {
        String line = null;
        BufferedReader br = null;
        List<String> strlist = new ArrayList<String>();
        try {
            Process pro = Runtime.getRuntime().exec(cmd);
            br = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            while ((line = br.readLine())!=null){
                strlist.add(line);
                ALog.d(cmd + ": " + line);
            }
        }catch (Exception e) {
            ALog.e("Failed to exec " + cmd + " command result " + e.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return strlist;
    }

    public static void setSysProperty(String key, String value) {
        ReflectionUtils.invokeStaticMethod("android.os.SystemProperties", "set", new Class[]{String.class, String.class}, key, value);
    }

    public static String getSysProperty(String key, String defVal) {
        return (String) ReflectionUtils.invokeStaticMethod("android.os.SystemProperties", "get", new Class[]{String.class, String.class}, key, defVal);
    }

}
