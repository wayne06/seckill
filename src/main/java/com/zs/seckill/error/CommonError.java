package com.zs.seckill.error;

public interface CommonError {
    int getErrCode();
    String getErrMsg();
    CommonError setErrMsg(String errMsg);
}
