package com.android.rockchip.nfs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.rockchip.EnumConstent;
import com.android.rockchip.FileInfo;
import com.android.rockchip.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by balu on 2017/7/30.
 */

public class NfsController {

    public static final String NFS_MOUNT_ROOT_DIR = "/data/nfs/";
    private Context mContext;
    private NfsService mNfsService;
    private AlertDialog mDialog;
    private ProgressBar mProgressBar;
    private WindowManager mWindowManager;
    private boolean mHasInited;
    private OnNfsListener mOnNfsListener;

    public interface OnNfsListener {
        public void onNfsAdd(NfsInfo info);
    }

    public NfsController(Context context) {
        mContext = context;
        mNfsService = new NfsService(mContext);
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mHasInited = false;
    }

    public void initMountList() {
        if (mHasInited) {
            return;
        }
        mHasInited = true;
        List<NfsInfo> infos = mNfsService.getNfsInfoList();
        ALog.d("init mount nfs list " + infos.size());
        for (NfsInfo info : infos) {
            File file  = new File(info.getLocalPath());
            if (!file.exists()) {
                mNfsService.removeNfsInfo(info.getLocalPath());
                continue;
            }

            ALog.d("init mount nfs path " + info.getLocalPath());
            new AsyncTask<NfsInfo, Integer, Boolean>(){
                protected  Boolean doInBackground(NfsInfo[] params) {
                    NfsInfo ninfo = params[0];
                    boolean result = mNfsService.mountNfsDevice(ninfo.getServerAddr(), ninfo.getLocalPath());
                    if (!result) {
                        mNfsService.removeNfsInfo(ninfo.getLocalPath());
                    }
                    return result;
                };
            }.execute(info);
        }
    }

    public void showAddNfsDialog() {
    	showAddOrEditNfsDialog(null , null);
    }
    
    public void showEditNfsDialog(String serverAddr, String localPath) {
    	showAddOrEditNfsDialog(serverAddr, localPath);
    }
    
    public void showAddOrEditNfsDialog(final String serverAddr, final String localPath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View dialogView = LayoutInflater.from(mContext).inflate(R.layout.nsf_add_dialog, null);
        final EditText addrEdt = (EditText)dialogView.findViewById(R.id.nfs_edt_remote_addr);
        if (serverAddr!=null) {
        	addrEdt.setText(serverAddr);
            builder.setTitle(R.string.edit_nfs_device);
        } else {
            builder.setTitle(R.string.add_nfs_device);
        }
        builder.setView(dialogView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                String remoteAddr = addrEdt.getText().toString();
                if (remoteAddr.equals(serverAddr)) {
                	ALog.d("server addr unchanged " + remoteAddr);
                	return;
                }
                
                addOrEditNfsDevice(remoteAddr, localPath);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        mDialog = builder.create();
        mDialog.show();
    }

    private void addOrEditNfsDevice(final String remoteAddr, final String lastMountPath) {
    	ALog.d("addOrEditNfsDevice " + remoteAddr);
        if(TextUtils.isEmpty(remoteAddr)){
            Toast.makeText(mContext, R.string.nfs_addr_null, Toast.LENGTH_LONG).show();
            return;
        }

        if(!isNFSAddress(remoteAddr)){
            Toast.makeText(mContext, R.string.nfs_addr_err, Toast.LENGTH_LONG).show();
            return;
        }

        int pathSplitIdx = remoteAddr.lastIndexOf("/");
        if (pathSplitIdx < 0) {
            pathSplitIdx = remoteAddr.lastIndexOf("\\");
        }
        if (pathSplitIdx <= 0) {
            Toast.makeText(mContext, R.string.nfs_addr_err, Toast.LENGTH_LONG).show();
            ALog.e("Invalid server address");
            return;
        }

        final String newMountPath = NFS_MOUNT_ROOT_DIR + remoteAddr.substring(pathSplitIdx+1);
        showProgress();
        new AsyncTask<String, Integer, Boolean>(){
            protected  Boolean doInBackground(String[] params) {
            	//remove last mount path if existed
                if (lastMountPath != null) {
                	doRemoveNfsDevice(lastMountPath);
                }
                
                String mountPath = newMountPath;
                File mountDir = new File(mountPath);
                ALog.d("Nfs mount path " + mountPath);
                if (mountDir.exists()) {
                    int result = mNfsService.checkMount(remoteAddr, mountPath);
                    //mount to new local path
                    if (result == NfsService.NFS_MOUNT_OK && lastMountPath != null) {
                    	result = NfsService.NFS_MOUNT_EXISTED;
                    }
                    if (result == NfsService.NFS_MOUNT_OK) {
                        ALog.d("Already nfs mounted path " + mountPath);
                        String lopath = mapToMountPath(remoteAddr);
                        if (lopath == null) {
                            ALog.d("but nfs local path no existed");
        	                mNfsService.addNfsInfo(remoteAddr, mountPath);
        	                if (mOnNfsListener != null) {
        	                	mOnNfsListener.onNfsAdd(new NfsInfo(remoteAddr, mountPath));
        	                }
                        }
                        //Toast.makeText(mContext, R.string.nfs_mount_ok, Toast.LENGTH_LONG).show();
                        return true;
                    } else if (result == NfsService.NFS_MOUNT_EXISTED) {
                        mountPath += System.currentTimeMillis();
                        mountDir = new File(mountPath);
                        mountDir.mkdirs();
                        //umountNFS(mountPath);
                    }
                } else {
                    mountDir.mkdirs();
                }
                
                boolean result = mNfsService.mountNfsDevice(remoteAddr, mountPath);
                if (!result) {
                    ALog.d("Mount failed ,try again " + remoteAddr);
                    result = mNfsService.mountNfsDevice(remoteAddr, mountPath);
                }
                if (result) {
                    mNfsService.addNfsInfo(remoteAddr, mountPath);
                    if (mOnNfsListener != null) {
                    	mOnNfsListener.onNfsAdd(new NfsInfo(remoteAddr, mountPath));
                    }
                }
                return result;
            };

            protected void onPostExecute(Boolean result) {
                if (result) {
                    Toast.makeText(mContext, R.string.nfs_mount_ok, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(mContext, mContext.getString(R.string.nfs_mount_failed, remoteAddr), Toast.LENGTH_LONG).show();
                }
                dismissProgress();
            }
        }.execute();
    }

    public boolean removeNfsDevice(String path) {
    	ALog.d("removeNfsDevice " + path);
    	//should running int another thread?
        return doRemoveNfsDevice(path);
    }
    
    public boolean doRemoveNfsDevice(String path) {
    	ALog.d("removeNfsDeviceInternal " + path);
        mNfsService.removeNfsInfo(path);
        boolean result = mNfsService.umountNfsDevice(path);
        new File(path).delete();
        return result;
    }
    
    public ArrayList<FileInfo> getNfsFileInfoList() {
    	ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
    	List<NfsInfo> infos = mNfsService.getNfsInfoList();
        ALog.d("getNfsFileInfoList nfs list " + infos.size());
        for (NfsInfo info : infos) {
            File file  = new File(info.getLocalPath());
            if (!file.exists()) {
                mNfsService.removeNfsInfo(info.getLocalPath());
                continue;
            }
            
        	FileInfo fileInfo = new FileInfo();
        	fileInfo.mFile = new File(EnumConstent.mDirNfs + "/" + info.getServerAddr());
        	fileInfo.mDescription = info.getServerAddr();
        	fileInfo.mIsDir = true;
        	fileInfo.mIcon = mContext.getDrawable(R.drawable.icon_smb);
        	fileInfo.setMMountpoint(info.getLocalPath());
        	fileList.add(fileInfo);
        }

        ALog.d("getNfsFileInfoList file list " + fileList.size());
    	return fileList;
    }
    
    public String mapToMountPath(String serverAddr) {
    	List<NfsInfo> infos = mNfsService.getNfsInfoList();
        for (NfsInfo info : infos) {
        	if (info.getServerAddr().equals(serverAddr)) {
        		return info.getLocalPath();
        	}
        }
        return null;
    }
    
    public String mapToServerAddr(String localPath) {
    	List<NfsInfo> infos = mNfsService.getNfsInfoList();
        for (NfsInfo info : infos) {
        	if (localPath!=null && localPath.contains(info.getLocalPath())) {
        		return info.getServerAddr();
        	}
        }
        return null;
    }
    
    public void setOnNfsListener(OnNfsListener listener) {
    	mOnNfsListener = listener;
    }

    public static boolean isNFSAddress(String address){
        String patter = "^(\\d{1,3}\\.){3}\\d{1,3}:(/[^(/\\)]+)*$";
        return Pattern.matches(patter, address);
    }

    private void showProgress() {
        mProgressBar = new ProgressBar(mContext, null, android.R.attr.progressBarStyleInverse);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION,
                0,  PixelFormat.TRANSLUCENT);
        lp.windowAnimations = 0;
        lp.gravity = Gravity.CENTER;
        mWindowManager.addView(mProgressBar, lp);
    }

    private void dismissProgress() {
        if (mProgressBar != null)
            mWindowManager.removeView(mProgressBar);
    }

}
