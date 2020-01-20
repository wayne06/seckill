package com.zs.seckill.controller;

import com.zs.seckill.error.BusinessErrEnum;
import com.zs.seckill.error.BusinessException;
import com.zs.seckill.response.CommonReturnType;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public CommonReturnType doError(HttpServletRequest request, HttpServletResponse response, Exception e) {
        e.printStackTrace();
        Map<String, Object> responseData = new HashMap<>();
        if (e instanceof BusinessException) {
            BusinessException businessException = (BusinessException) e;
            responseData.put("errCode", businessException.getErrCode());
            responseData.put("errMsg", businessException.getErrMsg());
        } else if (e instanceof ServletRequestBindingException) {
            responseData.put("errCode", BusinessErrEnum.UNKNOWN_ERROR.getErrCode());
            responseData.put("errMsg", "URL路由绑定错误");
        } else if (e instanceof NoHandlerFoundException) {
            responseData.put("errCode", BusinessErrEnum.UNKNOWN_ERROR.getErrCode());
            responseData.put("errMsg", "访问路径不存在");
        } else {
            responseData.put("errCode", BusinessErrEnum.UNKNOWN_ERROR.getErrCode());
            responseData.put("errMsg", BusinessErrEnum.UNKNOWN_ERROR.getErrMsg());
        }
        return CommonReturnType.create(responseData, "fail");
    }

}
