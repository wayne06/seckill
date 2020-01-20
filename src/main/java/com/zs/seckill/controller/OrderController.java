package com.zs.seckill.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.zs.seckill.error.BusinessErrEnum;
import com.zs.seckill.error.BusinessException;
import com.zs.seckill.mq.MqProducer;
import com.zs.seckill.response.CommonReturnType;
import com.zs.seckill.service.ItemService;
import com.zs.seckill.service.OrderService;
import com.zs.seckill.service.PromoService;
import com.zs.seckill.service.model.UserModel;
import com.zs.seckill.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.awt.im.InputMethodWindow;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.*;

@Controller("order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class OrderController extends BaseController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(20);
        orderCreateRateLimiter = RateLimiter.create(300);
    }

    @RequestMapping("/generateVerifyCode")
    @ResponseBody
    public void generateVerifyCode(HttpServletResponse response) throws BusinessException, IOException {
        // token方式验证用户是否已经登录
        String token= httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(BusinessErrEnum.User_NOT_LOGIN);
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(BusinessErrEnum.User_NOT_LOGIN);
        }

        Map<String,Object> map = CodeUtil.generateCodeAndPic();
        redisTemplate.opsForValue().set("verify_code_" + userModel.getId(), map.get("code"), 10, TimeUnit.MINUTES);
        ImageIO.write((RenderedImage) map.get("codePic"), "jpeg", response.getOutputStream());
    }

    @RequestMapping(value = "/generateToken", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam("itemId") Integer itemId,
                                          @RequestParam("promoId") Integer promoId,
                                          @RequestParam("verifyCode") String verifyCode)
            throws BusinessException {
        // token方式验证用户是否已经登录
        String token= httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(BusinessErrEnum.User_NOT_LOGIN);
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(BusinessErrEnum.User_NOT_LOGIN);
        }

        // 判断verifyCode的有效性
        String inRedisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_" + userModel.getId());
        if (StringUtils.isEmpty(inRedisVerifyCode)) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "请求非法");
        }
        if (!inRedisVerifyCode.equalsIgnoreCase(verifyCode)) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "验证码不正确");
        }

        // 生成秒杀令牌
        String promoToken = promoService.generateSeckillToken(promoId, itemId, userModel.getId());
        if (promoToken == null) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        }

        return CommonReturnType.create(promoToken);
    }

    @RequestMapping(value = "/createOrder", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam("itemId") Integer itemId,
                                        @RequestParam("amount") Integer amount,
                                        @RequestParam(value = "promoId", required = false) Integer promoId,
                                        @RequestParam(value = "promoToken", required = false) String promoToken)
            throws BusinessException {

        if (!orderCreateRateLimiter.tryAcquire()) {
            throw new BusinessException(BusinessErrEnum.RATELIMIT);
        }

        // session方式验证用户是否登录
        //Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        //if (isLogin == null || !isLogin.booleanValue()) {
        //    throw new BusinessException(BusinessErrEnum.User_NOT_LOGIN);
        //}
        //UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        // token方式验证用户是否已经登录
        String token= httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(BusinessErrEnum.User_NOT_LOGIN);
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(BusinessErrEnum.User_NOT_LOGIN);
        }

        // 校验令牌是否正确
        if (promoId != null) {
            String inRedisPromoToken = (String) redisTemplate.opsForValue()
                    .get("promotoken_" + promoId + "_userid_" + userModel.getId() + "_itemid_" + itemId);
            if (inRedisPromoToken == null) {
                throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
            if (!StringUtils.equals(promoToken, inRedisPromoToken)) {
                throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
        }

        // 同步调用线程池的submit方法
        // 拥塞窗口为20的等待队列，用来队列化泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                // 加入库存流水init状态
                String stockLogId = itemService.initStockLog(itemId, amount);

                // 改造成下单事务型消息机制 2019.06.05
                //OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);
                boolean result = mqProducer.transactionAsyncReduceStock(itemId, amount, userModel.getId(), promoId, stockLogId);
                if (!result) {
                    throw new BusinessException(BusinessErrEnum.UNKNOWN_ERROR, "下单失败");
                }
                return null;
            }
        });

        try {
            // future.get()如果获取不到，会一直阻塞
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new BusinessException(BusinessErrEnum.UNKNOWN_ERROR);
        }

        // 判断库存是否已售罄 (移到生成秒杀令牌的方法内)
        //if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
        //    throw new BusinessException(BusinessErrEnum.INSUFFICIENT_AMOUNT);
        //}
        //// 加入库存流水init状态
        //String stockLogId = itemService.initStockLog(itemId, amount);
        //
        //// 改造成下单事务型消息机制 2019.06.05
        ////OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);
        //boolean result = mqProducer.transactionAsyncReduceStock(itemId, amount, userModel.getId(), promoId, stockLogId);
        //if (!result) {
        //    throw new BusinessException(BusinessErrEnum.UNKNOWN_ERROR, "下单失败");
        //}
        return CommonReturnType.create(null);
    }

}