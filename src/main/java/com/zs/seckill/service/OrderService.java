package com.zs.seckill.service;

import com.zs.seckill.error.BusinessException;
import com.zs.seckill.service.model.OrderModel;

/**
 * @author wayne
 */
public interface OrderService {
    OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException;
}
