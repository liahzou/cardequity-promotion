package com.youyu.cardequity.promotion.biz.service;

import com.github.pagehelper.PageInfo;
import com.youyu.cardequity.promotion.biz.dal.entity.ProductCouponEntity;
import com.youyu.cardequity.promotion.dto.other.CommonBoolDto;
import com.youyu.cardequity.promotion.dto.other.CouponDetailDto;
import com.youyu.cardequity.promotion.dto.ProductCouponDto;
import com.youyu.cardequity.promotion.vo.req.*;
import com.youyu.cardequity.promotion.vo.rsp.CouponPageQryRsp;
import com.youyu.cardequity.promotion.vo.rsp.FindCouponListByOrderDetailRsp;
import com.youyu.cardequity.promotion.vo.rsp.GatherInfoRsp;
import com.youyu.common.api.PageData;
import com.youyu.common.service.IService;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 *  代码生成器
 *
 * @author 技术平台
 * @date 2018-12-07
 */
public interface ProductCouponService extends IService<ProductCouponDto, ProductCouponEntity> {

    /**
     * 1004259-徐长焕-20181210 新增
     * 功能：查询【有效期内、上架的】指定商品可领取的优惠券
     * @return
     */
     List<CouponDetailDto> findEnableGetCoupon(QryProfitCommonReq qryProfitCommonReq);



    /**
     * 添加优惠券
     *
     * @param req
     * @return
     */
    CommonBoolDto<CouponDetailDto> addCoupon(CouponDetailDto req);


    /**
     * 编辑优惠券
     *
     * @param req
     * @return
     */
    CommonBoolDto<CouponDetailDto>  editCoupon(CouponDetailDto req);

    /**
     * 批量删除优惠券
     *
     * @param req
     * @return
     */
     CommonBoolDto<Integer> batchDelCoupon( BatchBaseCouponReq req);

    /**
     * 开始发放
     * @param req
     * @return
     */
    CommonBoolDto<ProductCouponDto> startGetCoupon(BaseCouponReq req);

    /**
     * 结束发放
     * @param req
     * @return
     */
    CommonBoolDto<ProductCouponDto> stopGetCoupon(BaseCouponReq req);

    /**
     * 查看商品对应优惠券列表
     *
     * @param req
     * @return
     */
    List<CouponDetailDto> findCouponListByProduct(BaseProductReq req);

    /**
     * 查询所有优惠券列表
     *
     * @param req
     * @return
     */
    CouponPageQryRsp findCouponListByCommon(BaseQryCouponReq req);

    /**
     * 模糊查询所有优惠券列表
     *
     * @param req
     * @return
     */
    CouponPageQryRsp findCouponList(BaseQryCouponReq req);

    /**
     * 查询指定优惠券详情
     * @param req
     * @return
     */
    CouponDetailDto findCouponById(BaseCouponReq req);

    /**
     * 查询优惠汇总信息
     *
     * @param req 普通优惠活动请求体
     * @return 优惠汇总列表
     */
    List<GatherInfoRsp> findGatherCouponByCommon(BaseQryCouponReq req);


    /**
     * 查看商品对应优惠券列表
     *
     * @param req
     * @return
     */
    List<CouponDetailDto> findCouponListByIds(List<String> req);

}




