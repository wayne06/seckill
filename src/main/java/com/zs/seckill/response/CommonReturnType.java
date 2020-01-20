package com.zs.seckill.response;

public class CommonReturnType {

    /**
     * 请求的返回处理结果：success或fail
     */
    private String status;

    /**
     * 若status为success，则data是前端需要的json数据
     * 若status为fail，则data是通用的错误码格式
     */
    private Object data;

    public static CommonReturnType create(Object result) {
        return CommonReturnType.create(result, "success");
    }

    public static CommonReturnType create(Object result, String status) {
        CommonReturnType commonReturnType = new CommonReturnType();
        commonReturnType.setStatus(status);
        commonReturnType.setData(result);
        return commonReturnType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
