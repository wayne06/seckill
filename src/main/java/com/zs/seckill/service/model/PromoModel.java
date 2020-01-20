package com.zs.seckill.service.model;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

public class PromoModel implements Serializable {

    private Integer id;
    private String promoName;
    private DateTime startTime;
    private DateTime endTime;

    private Integer    itemId;
    private BigDecimal promoItemPrice;

    // 0 - 没有活动，1 - 未开始， 2 - 进行中
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(DateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPromoName() {
        return promoName;
    }

    public void setPromoName(String promoName) {
        this.promoName = promoName;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getPromoItemPrice() {
        return promoItemPrice;
    }

    public void setPromoItemPrice(BigDecimal promoItemPrice) {
        this.promoItemPrice = promoItemPrice;
    }
}
