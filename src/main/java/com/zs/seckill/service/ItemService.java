package com.zs.seckill.service;

import com.zs.seckill.error.BusinessException;
import com.zs.seckill.service.model.ItemModel;

import java.util.List;

public interface ItemService {

    /**
     * 新增商品
     * @param itemModel
     * @return
     * @throws BusinessException
     */
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    /**
     * 根据商品id获取商品信息
     * @param id
     * @return
     */
    ItemModel getItemById(Integer id);

    /**
     * 获取所有商品
     * @return
     */
    List<ItemModel> getAll();

    /**
     * 库存扣减
     * @param itemId
     * @param amount
     * @return
     */
    boolean decreaseStock(Integer itemId, Integer amount);

    /**
     * 库存回补
     * @param itemId
     * @param amount
     * @return
     */
    boolean increaseStock(Integer itemId, Integer amount);

    /**
     * 增加销量
     * @param itemId
     * @param amout
     */
    void increaseSales(Integer itemId, Integer amout);

    /**
     * 根据商品id获取缓存中的商品信息
     * @param itemId
     * @return
     */
    ItemModel getItemByIdInCache(Integer itemId);

    /**
     * 异步减库存
     * @param itemId
     * @param amount
     * @return
     */
    boolean asyncDecreaseStock(Integer itemId, Integer amount);

    /**
     * 初始化库存流水
     * @param itemId
     * @param amount
     */
    String initStockLog(Integer itemId, Integer amount);

}
