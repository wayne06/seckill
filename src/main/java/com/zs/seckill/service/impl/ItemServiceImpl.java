package com.zs.seckill.service.impl;

import com.zs.seckill.dao.ItemDOMapper;
import com.zs.seckill.dao.ItemStockDOMapper;
import com.zs.seckill.dao.StockLogDOMapper;
import com.zs.seckill.dataObject.ItemDO;
import com.zs.seckill.dataObject.ItemStockDO;
import com.zs.seckill.dataObject.PromoDO;
import com.zs.seckill.dataObject.StockLogDO;
import com.zs.seckill.error.BusinessErrEnum;
import com.zs.seckill.error.BusinessException;
import com.zs.seckill.mq.MqProducer;
import com.zs.seckill.service.ItemService;
import com.zs.seckill.service.PromoService;
import com.zs.seckill.service.model.ItemModel;
import com.zs.seckill.service.model.PromoModel;
import com.zs.seckill.validator.ValidationResult;
import com.zs.seckill.validator.ValidatorImpl;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemDOMapper itemDOMapper;

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;

    @Autowired
    private PromoService  promoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        ValidationResult validationResult = validator.validate(itemModel);
        if (validationResult.isHasErrors()) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, validationResult.getErrMsg());
        }
        ItemDO itemDO = convertItemModelToDO(itemModel);
        itemDOMapper.insertSelective(itemDO);

        itemModel.setId(itemDO.getId());

        ItemStockDO itemStockDO = convertItemModelToStockDo(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);

        return getItemById(itemModel.getId());
    }

    @Override
    public ItemModel getItemById(Integer id) {
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (itemDO == null) {
            return null;
        }
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
        ItemModel itemModel = convertToItemModel(itemDO, itemStockDO);

        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if (promoModel != null && promoModel.getStatus() != 0) {
            itemModel.setPromoModel(promoModel);
        }

        return itemModel;
    }

    @Override
    //@Cacheable(value = "itemModelList")
    public List<ItemModel> getAll() {
        List<ItemDO> itemDOList = itemDOMapper.selectAll();
        List<ItemModel> itemModelList = itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = convertToItemModel(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) {
        //int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);
        //if (affectedRow > 0) {
        //    return true;
        //} else {
        //    return false;
        //}

        // 修改为减缓存库存 2019.06.01
        long remain = redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue() * -1);
        if (remain > 0) {
            // 下面的代码提取到asyncDecreaseStock()方法中 2019.06.05
            //boolean mqResult = mqProducer.asyncReduceStock(itemId, amount);
            //if (!mqResult) {
            //    redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
            //    return false;
            //}
            return true;
        } else if (remain == 0) {
            // 打上库存售罄的标识
            redisTemplate.opsForValue().set("promo_item_stock_invalid_" + itemId, "true");
            return true;
        } else {
            increaseStock(itemId, amount);
            return false;
        }
    }

    @Override
    public boolean increaseStock(Integer itemId, Integer amount) {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
        return true;
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amout) {
        itemDOMapper.increaseSales(itemId, amout);
    }

    @Override
    public ItemModel getItemByIdInCache(Integer itemId) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_" + itemId);
        if (itemModel == null) {
            itemModel = this.getItemById(itemId);
            redisTemplate.opsForValue().set("item_validate_" + itemId, itemModel, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        boolean mqResult = mqProducer.asyncReduceStock(itemId, amount);
        return mqResult;
    }

    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDO stockLogDO = new StockLogDO();
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmout(amount);
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replace("-", ""));
        stockLogDO.setStatus(1);
        stockLogDOMapper.insertSelective(stockLogDO);
        return stockLogDO.getStockLogId();
    }

    private ItemModel convertToItemModel(ItemDO itemDO, ItemStockDO itemStockDO) {
        if (itemDO == null || itemStockDO == null) {
            return null;
        }
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setStock(itemStockDO.getStock());
        return itemModel;
    }


    private ItemStockDO convertItemModelToStockDo(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());
        return itemStockDO;
    }

    private ItemDO convertItemModelToDO(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);
        return itemDO;
    }
}
