package com.zs.seckill.controller;

import com.alibaba.fastjson.JSON;
import com.zs.seckill.controller.viewobject.ItemVO;
import com.zs.seckill.error.BusinessException;
import com.zs.seckill.response.CommonReturnType;
import com.zs.seckill.service.CacheService;
import com.zs.seckill.service.ItemService;
import com.zs.seckill.service.PromoService;
import com.zs.seckill.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller("item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class ItemController extends BaseController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PromoService promoService;

    @RequestMapping(value = "/getAll", method = RequestMethod.GET)
    @ResponseBody
    public CommonReturnType getAll() {
        List<ItemModel> itemModelList = itemService.getAll();

        // 解决java.util.LinkedHashMap cannot be cast to xxx
        //List<ItemVO> itemVOList = new ArrayList<>();
        //ItemVO itemVO;
        //for (Object object : itemModelList) {
        //    String s = JSON.toJSONString(object);
        //    ItemModel itemModel = JSON.parseObject(s, ItemModel.class);
        //    itemVO = convertItemModelToVO(itemModel);
        //    itemVOList.add(itemVO);
        //}

        //List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
        //    ItemVO itemVO = convertItemModelToVO(itemModel);
        //    return itemVO;
        //}).collect(Collectors.toList());

        ItemVO itemVO;
        List<ItemVO> itemVOList = new ArrayList<>();
        for (ItemModel itemModel : itemModelList) {
            itemVO = convertItemModelToVO(itemModel);
            itemVOList.add(itemVO);
        }
        return CommonReturnType.create(itemVOList);
    }

    @RequestMapping(value = "/publishPromo", method = RequestMethod.GET)
    @ResponseBody
    public CommonReturnType publishPromo(Integer id) {
        promoService.publishPromo(id);
        return CommonReturnType.create(null);
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ResponseBody
    public CommonReturnType getItem(@RequestParam("id") Integer id) {
        ItemModel itemModel = null;

        itemModel = (ItemModel) cacheService.getFromCommonCache("item_" + id);
        if (itemModel == null) {
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_" + id);
            if (itemModel == null) {
                itemModel = itemService.getItemById(id);
                redisTemplate.opsForValue().set("item_" + id, itemModel, 10, TimeUnit.MINUTES);
                cacheService.setCommonCache("item_" + id, itemModel);
            }
        }

        ItemVO itemVO = convertItemModelToVO(itemModel);
        return CommonReturnType.create(itemVO);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST, consumes = CONTENT_TYPE_FORMED)
    @ResponseBody
    public CommonReturnType createItem(@RequestParam("title") String title,
                                       @RequestParam("price") BigDecimal price,
                                       @RequestParam("stock") Integer stock,
                                       @RequestParam("description") String description,
                                       @RequestParam("imgUrl") String imgUrl)
            throws BusinessException {

        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setDescription(description);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn = itemService.createItem(itemModel);
        ItemVO itemVO = convertItemModelToVO(itemModelForReturn);

        return CommonReturnType.create(itemVO);
    }

    private ItemVO convertItemModelToVO(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);

        if (itemModel.getPromoModel() != null) {
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartTime(itemModel.getPromoModel().getStartTime().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            itemVO.setPromoStatus(0);
        }

        return itemVO;
    }

}
