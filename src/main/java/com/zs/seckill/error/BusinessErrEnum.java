package com.zs.seckill.error;

public enum BusinessErrEnum implements CommonError {
    // 统一错误类型10001开头
    PARAMETER_VALIDATION_ERROR(10001, "参数不合法"),

    UNKNOWN_ERROR(10002, "未知错误"),

    // 20000开头为用户信息相关错误定义
    USER_NOT_EXIST(20001, "用户不存在"),
    User_NOT_LOGIN(20003, "未登录"),

    // 30000开头为交易相关错误定义
    INSUFFICIENT_AMOUNT(30001, "库存不足"),
    MQ_SEND_FAILED(30002, "库存异步消息发送失败"),
    RATELIMIT(30003, "活动太火爆，请稍候再试")
    ;

    BusinessErrEnum(int errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    private int    errCode;
    private String errMsg;

    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
