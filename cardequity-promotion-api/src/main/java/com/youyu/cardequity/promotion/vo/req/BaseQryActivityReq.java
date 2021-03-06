package com.youyu.cardequity.promotion.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by caiyi on 2019/1/3.
 */
@Data
public class BaseQryActivityReq {
    @ApiModelProperty(value = "指定活动id")
    private String activityId;

    @ApiModelProperty(value = "上下架状态:0-下架 1-上架")
    private String upAndDownStatus;

    @ApiModelProperty(value = "促销状态:0-未开始 1-活动中 2-已结束 3-未过期 4-未开始")
    private String status;

    @ApiModelProperty(value = "优惠券名称：可模糊")
    private String activityName;

    @ApiModelProperty(value = "商品id")
    private String productId;

    @ApiModelProperty(value = "子商品id")
    private String skuId;

    @ApiModelProperty(value = "可多选，直接拼接串如“013”，选项如下：0-限额任选 1-折扣 2-优惠价 3-现金立减 4-自动返券(暂无)")
    private String activityCouponType;

    @ApiModelProperty(value = "页码：从1开始")
    private int pageNo;

    @ApiModelProperty(value = "每页数量：从1开始")
    private int pageSize;
}
