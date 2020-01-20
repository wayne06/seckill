package com.zs.seckill;

import com.zs.seckill.controller.viewobject.ItemVO;
import com.zs.seckill.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class TestList {

    public static void main(String[] args) {
        ItemModel itemModel1 = new ItemModel();
        itemModel1.setId(1);
        itemModel1.setTitle("11111");
        ItemModel itemModel2 = new ItemModel();
        itemModel2.setId(2);
        itemModel2.setTitle("22222");

        List<ItemModel> itemModelList = new ArrayList<>();
        itemModelList.add(itemModel1);
        itemModelList.add(itemModel2);
        System.out.println(itemModelList);

        ItemVO itemVO;
        List<ItemVO> itemVOList = new ArrayList<>();
        for (ItemModel itemModel : itemModelList) {
            itemVO = convertItemModelToVO(itemModel);
            itemVOList.add(itemVO);
        }
        System.out.println(itemVOList);
    }

    private static ItemVO convertItemModelToVO(ItemModel itemModel) {
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
