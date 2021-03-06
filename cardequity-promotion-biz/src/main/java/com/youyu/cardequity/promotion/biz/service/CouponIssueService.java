package com.youyu.cardequity.promotion.biz.service;

import com.youyu.cardequity.promotion.dto.req.*;
import com.youyu.cardequity.promotion.dto.rsp.*;
import com.youyu.common.api.PageData;

import java.util.List;


/**
 * @author panqingqing
 * @version v1.0
 * @date 2019年04月25日 15:00:00
 * @work 优惠券发放service
 */
public interface CouponIssueService {

    /**
     * 创建发放优惠券
     *
     * @param couponIssueReq
     * @return
     */
    CouponIssueRsp createIssue(CouponIssueReq couponIssueReq);

    /**
     * 触发器到点正式执行发券操作
     */
    void processIssue(CouponIssueMsgDetailsReq couponIssueMsgDetailsReq);

    /**
     * 查询优惠券发放流水
     *
     * @param couponIssueHistoryQueryReq
     * @return
     */
    PageData<CouponIssueHistoryQueryRep> getCouponIssueHistory(CouponIssueHistoryQueryReq couponIssueHistoryQueryReq);


    /**
     * 根据查询条件查询优惠券发放列表
     *
     * @param couponIssueQueryReq
     * @return
     */
    PageData<CouponIssueQueryRsp> getCouponIssueQuery(CouponIssueQueryReq couponIssueQueryReq);

    /**
     * 拿到优惠券发放记录中应该被补偿的列表
     *
     * @return
     */
    List<CouponIssueDetailRsp> getCouponIssueCompensate();


    /**
     * 根据查询条件查询发放明细
     *
     * @param couponIssueDetailReq
     * @return
     */
    CouponIssueDetailRsp getCouponIssueDetail(CouponIssueDetailReq couponIssueDetailReq);

    /**
     * 根据条件删除
     *
     * @param couponIssueDeleteReq
     * @return
     */
    void delete(CouponIssueDeleteReq couponIssueDeleteReq);

    /**
     * 设置优惠券上下架
     *
     * @param couponIssueVisibleReq
     */
    void setVisible(CouponIssueVisibleReq couponIssueVisibleReq);

    /**
     * 优惠券发放编辑
     *
     * @param couponIssueEditReq
     * @return
     */
    CouponIssueEditRsp edit(CouponIssueEditReq couponIssueEditReq);
}
