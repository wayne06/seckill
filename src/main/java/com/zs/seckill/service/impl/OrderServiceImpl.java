package com.zs.seckill.service.impl;

import com.zs.seckill.dao.OrderDOMapper;
import com.zs.seckill.dao.SequenceDOMapper;
import com.zs.seckill.dao.StockLogDOMapper;
import com.zs.seckill.dataObject.OrderDO;
import com.zs.seckill.dataObject.SequenceDO;
import com.zs.seckill.dataObject.StockLogDO;
import com.zs.seckill.error.BusinessErrEnum;
import com.zs.seckill.error.BusinessException;
import com.zs.seckill.service.ItemService;
import com.zs.seckill.service.OrderService;
import com.zs.seckill.service.UserService;
import com.zs.seckill.service.model.ItemModel;
import com.zs.seckill.service.model.OrderModel;
import com.zs.seckill.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author wayne
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId)
            throws BusinessException {
        // 1.校验下单状态：商品是否存在，用户是否合法，购买数量是否正确
        //UserModel userModel = userService.getUserById(userId);
        //UserModel userModel = userService.getUserByIdInCache(userId);
        //if (userModel == null) {
        //    throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "用户信息不正确");
        //}
        //ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }
        if (amount <= 0 || amount > 99) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "购买数量不正确");
        }

        // 校验活动信息
        //if (promoId != null) {
        //    if (promoId.intValue() != itemModel.getPromoModel().getId()) {
        //        throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
        //    } else if (itemModel.getPromoModel().getStatus() != 2) {
        //        throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "活动还未开始");
        //    }
        //}

        // 2.落单减库存(另一种是支付减库存)
        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result) {
            throw new BusinessException(BusinessErrEnum.INSUFFICIENT_AMOUNT);
        }

        // 3.订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setId(generateOrderNo(itemId));
        orderModel.setItemId(itemId);
        orderModel.setUserId(userId);

        if (promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
            orderModel.setPromoId(promoId);
        } else {
            orderModel.setItemPrice(itemModel.getPrice());
        }

        orderModel.setAmount(amount);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(BigDecimal.valueOf(amount)));
        OrderDO orderDO = covertOrderModelToDO(orderModel);
        orderDOMapper.insertSelective(orderDO);

        itemService.increaseSales(itemId, amount);

        // 设置库存流水状态为成功 2019.06.06
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if (stockLogDO == null) {
            throw new BusinessException(BusinessErrEnum.UNKNOWN_ERROR);
        }
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

        // 异步更新数据库的库存数量
        //boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
        //if (!mqResult) {
        //    itemService.increaseStock(itemId, amount);
        //    throw new BusinessException(BusinessErrEnum.MQ_SEND_FAILED);
        //}

        // 方法最近的一个@Transactional标签事务被成功commit之后，才会执行afterCommit()方法 2019.06.05
        //TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
        //    @Override
        //    public void afterCommit() {
        //        // 异步减库存 2019.06.05
        //        boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
        //        //if (!mqResult) {
        //        //    itemService.increaseStock(itemId, amount);
        //        //    throw new BusinessException(BusinessErrEnum.MQ_SEND_FAILED);
        //        //}
        //    }
        //});

        // 4.返回前端
        return orderModel;
    }

    private OrderDO covertOrderModelToDO(OrderModel orderModel) {
        if (orderModel == null) {
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        return orderDO;
    }


    /**
     * 订单号：16位，前8位为时间信息，中间6位为自增序列，最后2位为分库分表位
     * @param itemId
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateOrderNo(Integer itemId) {
        // 前八位日期信息
        StringBuilder stringBuilder = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);

        // 中间六位自增序列
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        int sequence = 0;
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequence + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        // 最后两位分库分表信息
        stringBuilder.append("00");

        return stringBuilder.toString();
    }

}
