package com.android.rockchip.nfs;

import java.io.Serializable;

/**
 * Created by balu on 2017/8/5.
 */

public class NfsInfo implements Serializable {

    private String serverAddr;
    private String localPath;
    
    public NfsInfo(){
    	
    }
    
    public NfsInfo(String serverAddr, String localPath){
    	this.serverAddr = serverAddr;
    	this.localPath = localPath;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NfsInfo nfsInfo = (NfsInfo) o;

        if (!getServerAddr().equals(nfsInfo.getServerAddr())) return false;
        return getLocalPath().equals(nfsInfo.getLocalPath());

    }

    @Override
    public int hashCode() {
        int result = getServerAddr().hashCode();
        result = 31 * result + getLocalPath().hashCode();
        return result;
    }
}
