package com.neusoft.neu23.neuhospital.pharmacy.dto;

public class DrugStockAdjustReq {
    // 调整数量，正数代表入库，负数代表盘亏减少
    private Integer adjustQuantity;

    public Integer getAdjustQuantity() { return adjustQuantity; }
    public void setAdjustQuantity(Integer adjustQuantity) { this.adjustQuantity = adjustQuantity; }
}
