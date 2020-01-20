package com.zs.seckill.controller;

import com.alibaba.druid.util.StringUtils;
import com.zs.seckill.controller.viewobject.UserVO;
import com.zs.seckill.error.BusinessErrEnum;
import com.zs.seckill.error.BusinessException;
import com.zs.seckill.response.CommonReturnType;
import com.zs.seckill.service.UserService;
import com.zs.seckill.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

        /**
         * 用户登录接口
         * @param telphone
         * @param password
         * @return
         */
        @RequestMapping(value = "/login", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
        @ResponseBody
        public CommonReturnType login(@RequestParam("telphone") String telphone,
                @RequestParam("password") String password)
            throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
            if (StringUtils.isEmpty(telphone) || StringUtils.isEmpty(password)) {
                throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "用户名或密码不能为空");
            }
            UserModel userModel = userService.loginValidation(telphone, encodeByMd5(password));

        // 登录信息存入session
        //this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
        //this.httpServletRequest.getSession().setAttribute("LOGIN_USER", userModel);
        //return CommonReturnType.create(null);

        // 登录信息以token形式存入redis
        String userToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(userToken, userModel, 1, TimeUnit.HOURS);

        return CommonReturnType.create(userToken);
    }

    /**
     * 用户注册接口
     * @param telphone
     * @param otp
     * @param name
     * @param password
     * @param age
     * @param gender
     * @return
     */
    @RequestMapping(value = "/register", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam("telphone") String telphone,
                                     @RequestParam("otp") String otp,
                                     @RequestParam("name") String name,
                                     @RequestParam("password") String password,
                                     @RequestParam("age") Integer age,
                                     @RequestParam("gender") Integer gender)
            throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        String otpInSession = (String) httpServletRequest.getSession().getAttribute(telphone);
        if (!StringUtils.equals(otp, otpInSession)) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "验证码不正确");
        }

        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(encodeByMd5(password));
        userService.register(userModel);

        return CommonReturnType.create(null);
    }

    private String encodeByMd5(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();
        String encrypt = base64Encoder.encode(messageDigest.digest(password.getBytes("utf-8")));
        return encrypt;
    }

    /**
     * 用户获取otp短信接口
     * @param telphone
     * @return
     */
    @RequestMapping(value = "/getotp", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam("telphone") String telphone) {
        // 按一定规则生成OTP验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 10000;
        String otpCode = String.valueOf(randomInt);

        // 将OTP验证码同对应用户手机号关联，使用httpsession的方式绑定手机号和code（企业级项目中使用redis：键值+过期时间）
        httpServletRequest.getSession().setAttribute(telphone, otpCode);

        // 将OTP验证码通过短信通道发送给用户，暂略
        System.out.println("telphone = " + telphone + " & otpCode = " + otpCode);

        return CommonReturnType.create(null);
    }


    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id") Integer id) throws BusinessException {
        UserModel userModel = userService.getUserById(id);

        if (userModel == null) {
            throw new BusinessException(BusinessErrEnum.USER_NOT_EXIST);
        }

        UserVO userVO = convertFromModel(userModel);
        return CommonReturnType.create(userVO);
    }

    private UserVO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }



}
