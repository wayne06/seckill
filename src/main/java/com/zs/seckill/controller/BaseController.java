package com.zs.seckill.controller;

import com.zs.seckill.error.BusinessErrEnum;
import com.zs.seckill.error.BusinessException;
import com.zs.seckill.response.CommonReturnType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class BaseController {

    public static final String CONTENT_TYPE_FORMED = "application/x-www-form-urlencoded";

    ///**
    // * 定义exceptionHandler解决未被controller层处理的exception
    // * @param request
    // * @param e
    // * @return
    // */
    //@ExceptionHandler(Exception.class)
    //@ResponseStatus(HttpStatus.OK)
    //@ResponseBody
    //public CommonReturnType handlerException(HttpServletRequest request, Exception e) {
    //    e.printStackTrace();
    //    Map<String, Object> responseData = new HashMap<>();
    //    if (e instanceof BusinessException) {
    //        BusinessException businessException = (BusinessException) e;
    //        responseData.put("errCode", businessException.getErrCode());
    //        responseData.put("errMsg", businessException.getErrMsg());
    //    } else {
    //        responseData.put("errCode", BusinessErrEnum.UNKNOWN_ERROR.getErrCode());
    //        responseData.put("errMsg", BusinessErrEnum.UNKNOWN_ERROR.getErrMsg());
    //    }
    //    return CommonReturnType.create(responseData, "fail");
    //}

}
