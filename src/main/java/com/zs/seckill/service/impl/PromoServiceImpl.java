package com.zs.seckill.service.impl;

import com.zs.seckill.dao.PromoDOMapper;
import com.zs.seckill.dataObject.PromoDO;
import com.zs.seckill.error.BusinessErrEnum;
import com.zs.seckill.error.BusinessException;
import com.zs.seckill.service.ItemService;
import com.zs.seckill.service.PromoService;
import com.zs.seckill.service.UserService;
import com.zs.seckill.service.model.ItemModel;
import com.zs.seckill.service.model.PromoModel;
import com.zs.seckill.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        if (promoDO == null) {
            return null;
        }
        PromoModel promoModel = convertPromoDOToModel(promoDO);
        // 0 - 已结束，1 - 未开始， 2 - 进行中
        if (promoModel.getStartTime().isAfterNow()) {
            promoModel.setStatus(1);
        } else if (promoModel.getEndTime().isBeforeNow()) {
            promoModel.setStatus(0);
        } else {
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO.getItemId() == null || promoDO.getItemId() == 0) {
            return;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());

        // 将库存同步到redis
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());

        // 将大闸的容量设置到redis内
        redisTemplate.opsForValue().set("promo_door_count_" + promoId, itemModel.getStock().intValue() * 5);
    }

    @Override
    public String generateSeckillToken(Integer promoId, Integer itemId, Integer userId) {
        // 判断库存是否已售罄
        if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
            return null;
        }

        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel == null) {
            return null;
        }

        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            return null;
        }

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO == null) {
            return null;
        }
        if (promoDO.getStartTime().after(new Date())) {
            return null;
        } else if (promoDO.getEndTime().before(new Date())) {
            return null;
        }

        // 获取大闸数量并减1，如果剩余大闸容量小于0，则无法获取到秒杀令牌
        long doorRemain = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if (doorRemain < 0) {
            return null;
        }

        // 生成令牌并存入redis
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("promotoken_" + promoId + "_userid_" + userId + "_itemid_" + itemId, token,
                5, TimeUnit.MINUTES);

        return token;
    }

    private PromoModel convertPromoDOToModel(PromoDO promoDO) {
        if (promoDO == null) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setStartTime(new DateTime(promoDO.getStartTime()));
        promoModel.setEndTime(new DateTime(promoDO.getEndTime()));
        return promoModel;
    }

}
