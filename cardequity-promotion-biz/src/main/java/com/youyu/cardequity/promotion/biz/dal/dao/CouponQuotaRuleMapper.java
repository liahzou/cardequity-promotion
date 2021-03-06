package com.youyu.cardequity.promotion.biz.dal.dao;

import com.youyu.cardequity.promotion.biz.dal.entity.CouponQuotaRuleEntity;
import com.youyu.common.mapper.YyMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *  代码生成器
 *
 * @author 技术平台
 * @date 2018-12-07
 */
public interface CouponQuotaRuleMapper extends YyMapper<CouponQuotaRuleEntity> {

    /**
     * 根据优惠券id获取额度规则
     * @param couponId
     * @return
     */
    CouponQuotaRuleEntity findCouponQuotaRuleById(@Param("couponId") String couponId );

    /**
     * 根据优惠券id获取额度规则
     * @param idList
     * @return
     */
    List<CouponQuotaRuleEntity> findCouponQuotaRuleByIds(@Param("idList") List<String> idList );


    /**
     * 逻辑删除通过优惠id
     * @param couponId
     * @return
     */
    int logicDelByCouponId(@Param("couponId") String couponId);

}




