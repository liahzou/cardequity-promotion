package com.youyu.cardequity.promotion.biz.strategy.activity;

import com.youyu.cardequity.common.base.annotation.StatusAndStrategyNum;
import com.youyu.cardequity.common.base.converter.BeanPropertiesConverter;
import com.youyu.cardequity.common.base.util.BeanPropertiesUtils;
import com.youyu.cardequity.promotion.biz.dal.dao.ActivityQuotaRuleMapper;
import com.youyu.cardequity.promotion.biz.dal.dao.ActivityRefProductMapper;
import com.youyu.cardequity.promotion.biz.dal.dao.ActivityStageCouponMapper;
import com.youyu.cardequity.promotion.biz.dal.entity.ActivityProfitEntity;
import com.youyu.cardequity.promotion.biz.dal.entity.ActivityQuotaRuleEntity;
import com.youyu.cardequity.promotion.biz.dal.entity.ActivityRefProductEntity;
import com.youyu.cardequity.promotion.biz.dal.entity.ActivityStageCouponEntity;
import com.youyu.cardequity.promotion.dto.*;
import com.youyu.cardequity.promotion.dto.other.ClientCoupStatisticsQuotaDto;
import com.youyu.cardequity.promotion.dto.other.CommonBoolDto;
import com.youyu.cardequity.promotion.dto.other.OrderProductDetailDto;
import com.youyu.cardequity.promotion.enums.dict.ApplyProductFlag;
import com.youyu.cardequity.promotion.enums.dict.TriggerByType;
import com.youyu.cardequity.promotion.vo.rsp.UseActivityRsp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 任选活动策略
 * Created by caiyi on 2018/12/26.
 */
@Slf4j
@StatusAndStrategyNum(superClass = ActivityStrategy.class, number = "0", describe = "任选N件减M元")
@Component
public class MaxQuotaStrategy extends ActivityStrategy {
    @Autowired
    private ActivityRefProductMapper activityRefProductMapper;

    @Autowired
    private ActivityStageCouponMapper activityStageCouponMapper;

    @Autowired
    private ActivityQuotaRuleMapper activityQuotaRuleMapper;

    @Override
    public UseActivityRsp applyActivity(ActivityProfitEntity item, List<OrderProductDetailDto> productList) {

        log.info("进入任选限额活动处理策略，策略编号为{}", item.getId());

        //1.装箱返回数据
        UseActivityRsp rsp = new UseActivityRsp();
        ActivityProfitDto dto = new ActivityProfitDto();
        BeanUtils.copyProperties(item, dto);
        rsp.setActivity(dto);
        rsp.setProfitAmount(item.getProfitValue());

        //2.获取活动阶梯
        List<ActivityStageCouponEntity> activityProfitDetail = activityStageCouponMapper.findActivityProfitDetail(item.getId());
        //任选N件最多M元一定要设置门槛
        if (activityProfitDetail.size() <= 0) {
            return null;
        }

        //3.校验券的额度限制是否满足:只校验参与活动前是否有剩余额度，最终额度再和封顶值配合控制
        //检查指定客户的额度信息
        ActivityQuotaRuleEntity quota = activityQuotaRuleMapper.findActivityQuotaRuleById(item.getId());
        CommonBoolDto<ClientCoupStatisticsQuotaDto> boolDto = checkActivityPersonQuota(quota, item.getId());
        //校验不通过直接返回
        if (!boolDto.getSuccess()) {
            log.info("客户本人使用额度受限，详情：{}", boolDto.getDesc());
            return null;
        }

        //检查所有客户领取额度情况
        boolDto = checkActivityAllQuota(quota);
        //校验不通过直接返回
        if (!boolDto.getSuccess()) {
            log.info("所有客户使用额度受限，详情：{}", boolDto.getDesc());
            return null;
        }

        //拷贝适用活动的订单中商品
        BigDecimal totalNum = BigDecimal.ZERO;
        List<OrderProductDetailDto> copyProductList = new ArrayList<>();
        for (OrderProductDetailDto productItem : productList) {
            if (!ApplyProductFlag.ALL.getDictValue().equals(item.getApplyProductFlag())) {
                //该商品属性是否允许参与活动
                ActivityRefProductEntity entity = activityRefProductMapper.findByActivityAndSkuId(item.getId(), productItem.getProductId(), productItem.getSkuId());
                if (entity == null) {
                    continue;
                }
                copyProductList.add(productItem);
                totalNum=totalNum.add(productItem.getAppCount());
            }else {
                copyProductList.add(productItem);
            }
        }

        //降序排序
        Collections.sort(copyProductList, new Comparator<OrderProductDetailDto>() {
            @Override
            public int compare(OrderProductDetailDto entity1, OrderProductDetailDto entity2) {//如果是折扣、任选、优惠价从小到大
                return entity2.getPrice().compareTo(entity1.getPrice());
            }
        });

        List<ActivityStageCouponEntity> validActivityStageList = new ArrayList<>();
        ActivityStageCouponEntity specialStage=null;
        //移除掉无效的阶梯
        for (ActivityStageCouponEntity stage : activityProfitDetail) {
            //任选活动必须设置有效限额，限额设置为0的情况丢弃
            if (BigDecimal.ZERO.compareTo(stage.getProfitValue()) >= 0)
                continue;
            //任选活动必须设置有效的活动门槛，BeginValue设置为0的情况丢弃
            if (BigDecimal.ZERO.compareTo(stage.getBeginValue()) >= 0)
                continue;
            //任选不会出现按金额统计，废数据进行丢弃
            if (TriggerByType.CAPITAL.getDictValue().equals(stage.getTriggerByType())) {
                continue;
            }
            if (totalNum.compareTo(stage.getBeginValue())>=0){
                validActivityStageList.add(stage);
            }

        }

        //限额适用的商品
        List<OrderProductDetailDto> temproductLsit = new ArrayList<>();
        //3.检验门槛值：所有活动在定义适用商品时都不会重叠
        for (ActivityStageCouponEntity stage : validActivityStageList) {
            BigDecimal countCondition = BigDecimal.ZERO;
            BigDecimal amountCondition = BigDecimal.ZERO;
            BigDecimal diff = BigDecimal.ZERO;
            BigDecimal applyNum = BigDecimal.ZERO;
            BigDecimal profitAmount = BigDecimal.ZERO;
            rsp.getProductList().clear();
            for (OrderProductDetailDto productItem : copyProductList) {

                //一个商品如果存在适用多个活动时需要单独开辟内存
                OrderProductDetailDto product = new OrderProductDetailDto();
                BeanUtils.copyProperties(productItem, product);
                product.setTotalAmount(product.getAppCount().multiply(product.getPrice()));

                //已经适配到的不再处理,任选3件99元，哪怕有6个商品满足也只使用一次,即前3件会使用
                // （折扣策略是根据适用商品是否订单中活动定义所有商品都打折决定后续是否继续）

                //门槛差值
                diff = stage.getBeginValue().subtract(countCondition);
                //已经适配过的不再处理：理论上不会出现该情况，
                if (diff.compareTo(BigDecimal.ZERO) <= 0)
                    continue;
                //满足门槛条件情况下
                if (product.getAppCount().compareTo(diff) >= 0) {

                    applyNum = diff;
                    //涉及适用的数量
                    product.setProfitCount(applyNum);

                    //优惠金额=涉及适用的总金额-指定限制的总额
                    profitAmount = amountCondition.add(applyNum.multiply(product.getPrice())).subtract(stage.getProfitValue());
                    //有优惠时才算适用
                    if (profitAmount.compareTo(BigDecimal.ZERO) > 0) {
                        //比上一个阶梯优惠还小的不需要考虑，始终只考虑优惠力度最大的
                        if (rsp.getStage() != null && profitAmount.compareTo(rsp.getProfitAmount()) <= 0) {
                            break;
                        }
                        ActivityStageCouponDto stageDto = new ActivityStageCouponDto();
                        BeanUtils.copyProperties(stage, stageDto);
                        rsp.setStage(stageDto);
                        rsp.setProfitAmount(profitAmount);
                        //保留此阶梯适配的商品列表,只保留最近一次匹配到的
                        temproductLsit.clear();
                        temproductLsit.addAll(rsp.getProductList());
                        temproductLsit.add(product);
                        log.info("任选限额券满足使用条件处理;活动编号：" + item.getId() + ";门槛编号：" + stage.getId() + ";商品编号" + product.getProductId() + ";子商品编号" + product.getSkuId());
                        break;
                    }
                }

                //满足条件后加入
                product.setProfitCount(product.getAppCount());
                rsp.getProductList().add(product);
                countCondition = countCondition.add(product.getAppCount());
                amountCondition = amountCondition.add(product.getTotalAmount());
            }
        }

        //找到最终适用的阶梯后，将适用情况重置为对应点
        if (rsp.getStage() != null) {
            rsp.setProductList(temproductLsit);
            BigDecimal totalRealAmount = rsp.getProductList().stream().map(OrderProductDetailDto::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalRealAmount.compareTo(BigDecimal.ZERO) > 0) {
                //每种商品优惠的金额是按适用金额比例来的
                for (OrderProductDetailDto product : rsp.getProductList()) {
                    product.setProfitAmount(rsp.getProfitAmount().multiply(product.getTotalAmount().divide(totalRealAmount, 4, RoundingMode.DOWN)));
                }
            }
            return rsp;
        }

        return null;
    }



}