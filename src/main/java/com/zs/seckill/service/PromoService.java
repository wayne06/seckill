package com.zs.seckill.service;

import com.zs.seckill.service.model.PromoModel;

public interface PromoService {

    /**
     * 根据itemId获取活动模型
     * @param itemId
     * @return
     */
    PromoModel getPromoByItemId(Integer itemId);

    /**
     * 活动发布
     * @param promoId
     */
    void publishPromo(Integer promoId);

    /**
     * 生成秒杀令牌
     * @param promoId
     * @param itemId
     * @param userId
     * @return
     */
    String generateSeckillToken(Integer promoId, Integer itemId, Integer userId);

}
