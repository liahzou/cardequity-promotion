package com.youyu.cardequity.promotion.biz.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.youyu.cardequity.common.base.converter.BeanPropertiesConverter;
import com.youyu.cardequity.common.base.converter.OrikaBeanPropertiesConverter;
import com.youyu.cardequity.common.base.uidgenerator.UidGenerator;
import com.youyu.cardequity.common.base.util.BeanPropertiesUtils;
import com.youyu.cardequity.common.base.util.StringUtil;
import com.youyu.cardequity.common.spring.service.BatchService;
import com.youyu.cardequity.promotion.biz.dal.dao.*;
import com.youyu.cardequity.promotion.biz.dal.entity.*;
import com.youyu.cardequity.promotion.biz.enums.CouponTargetFlagEnum;
import com.youyu.cardequity.promotion.biz.enums.CouponTypeEnum;
import com.youyu.cardequity.promotion.biz.enums.CouponUnitEnum;
import com.youyu.cardequity.promotion.biz.enums.CouponValidTimeTypeEnum;
import com.youyu.cardequity.promotion.biz.service.ClientCouponService;
import com.youyu.cardequity.promotion.biz.service.CouponRefProductService;
import com.youyu.cardequity.promotion.biz.service.ProductCouponService;
import com.youyu.cardequity.promotion.biz.utils.CommonUtils;
import com.youyu.cardequity.promotion.constant.CommonConstant;
import com.youyu.cardequity.promotion.dto.*;
import com.youyu.cardequity.promotion.dto.other.CommonBoolDto;
import com.youyu.cardequity.promotion.dto.other.CouponDetailDto;
import com.youyu.cardequity.promotion.dto.other.ObtainCouponViewDto;
import com.youyu.cardequity.promotion.dto.other.ShortCouponDetailDto;
import com.youyu.cardequity.promotion.dto.req.AddCouponReq2;
import com.youyu.cardequity.promotion.dto.req.EditCouponReq2;
import com.youyu.cardequity.promotion.dto.req.MemberProductMaxCouponReq;
import com.youyu.cardequity.promotion.dto.rsp.MemberProductMaxCouponRsp;
import com.youyu.cardequity.promotion.enums.CommonDict;
import com.youyu.cardequity.promotion.enums.dict.*;
import com.youyu.cardequity.promotion.vo.req.*;
import com.youyu.cardequity.promotion.vo.rsp.CouponPageQryRsp;
import com.youyu.cardequity.promotion.vo.rsp.FullClientCouponRsp;
import com.youyu.cardequity.promotion.vo.rsp.GatherInfoRsp;
import com.youyu.common.api.PageData;
import com.youyu.common.exception.BizException;
import com.youyu.common.service.AbstractService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.youyu.cardequity.common.base.util.CollectionUtils.isEmpty;
import static com.youyu.cardequity.common.base.util.EnumUtil.getCardequityEnum;
import static com.youyu.cardequity.common.base.util.LocalDateUtils.date2LocalDateTime;
import static com.youyu.cardequity.common.base.util.LocalDateUtils.localDateTime2Date;
import static com.youyu.cardequity.common.base.util.MoneyUtil.*;
import static com.youyu.cardequity.common.base.util.PaginationUtils.convert;
import static com.youyu.cardequity.common.base.util.StringUtil.eq;
import static com.youyu.cardequity.promotion.enums.ResultCode.*;
import static com.youyu.cardequity.promotion.enums.dict.CouponGetType.HANLD;
import static com.youyu.cardequity.promotion.enums.dict.CouponStrategyType.equalstage;
import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.time.DateUtils.addDays;


/**
 * 代码生成器
 *
 * @author 技术平台
 * @date 2018-12-07
 * 开发日志
 * V1.0-V1 1004244-徐长焕-20181207 新建代码，findEnableGetCoupon：获取客户可领取的券
 */
@Slf4j
@Service
public class ProductCouponServiceImpl extends AbstractService<String, ProductCouponDto, ProductCouponEntity, ProductCouponMapper> implements ProductCouponService {

    /**
     * 默认最大值
     */
    public static final BigDecimal MAX_VALUE = new BigDecimal("999999999.00");

    @Autowired
    private ProductCouponMapper productCouponMapper;

    @Autowired
    private CouponStageRuleMapper couponStageRuleMapper;

    @Autowired
    private CouponGetOrUseFreqRuleMapper couponGetOrUseFreqRuleMapper;

    @Autowired
    private CouponAndActivityLabelMapper couponAndActivityLabelMapper;

    @Autowired
    private CouponQuotaRuleMapper couponQuotaRuleMapper;

    @Autowired
    private ClientCouponMapper clientCouponMapper;

    @Autowired
    private BatchService batchService;

    @Autowired
    private CouponRefProductService couponRefProductService;

    @Autowired
    private ClientCouponService clientCouponService;

    @Autowired
    private UidGenerator uidGenerator;

    /**
     * 1004259-徐长焕-20181210 新增
     * 功能：查询指定商品可领取的优惠券
     *
     * @param qryProfitCommonReq 优惠查询请求体
     * @return 优惠券详情列表
     * 开发日志
     * 1004244-徐长焕-20181207 新建
     */
    @Override
    public List<CouponDetailDto> findEnableGetCoupon(QryProfitCommonReq qryProfitCommonReq) {
        List<ProductCouponEntity> productCouponlist = null;
        List<CouponDetailDto> result = new ArrayList<>();

        if ("12".equals(qryProfitCommonReq.getClientType())) {
            qryProfitCommonReq.setClientType("11");
        }
        // DateParam dateParam=getMaxMonthDate();
        if (CommonConstant.EXCLUSIONFLAG_ALL.equals(qryProfitCommonReq.getExclusionFlag())) {
            //获取满足条件的优惠券：1.满足对应商品属性(指定商品或组)、客户属性(指定客户类型)、订单属性(指定客户类型)；2.满足券额度(券每日领取池，券总金额池，券总量池)
            productCouponlist = productCouponMapper.findEnableGetCouponList(qryProfitCommonReq.getProductId(), qryProfitCommonReq.getEntrustWay(), qryProfitCommonReq.getClientType());
        } else {
            productCouponlist = productCouponMapper.findEnableGetCouponListByCommon(qryProfitCommonReq.getProductId(), qryProfitCommonReq.getEntrustWay(), qryProfitCommonReq.getClientType());
        }

        List<ProductCouponEntity> finnalCouponlist = new ArrayList<>();
        //优惠卷阶梯规则
        Map<String, List<CouponStageRuleDto>> finnalStageMap = new HashedMap();
        //根据客户对上述券领取情况，以及该券领取频率限制进行排除
        for (ProductCouponEntity item : productCouponlist) {
            //如果非新注册用户,排除掉新用户专享的
            if (!CommonConstant.USENEWREGISTER_YES.equals(qryProfitCommonReq.getNewRegisterFlag())) {
                if (UsedStage.Register.getDictValue().equals(item.getGetType())) {
                    continue;
                }
            }
            //商品详情和商品列表的需要排除掉运费券
            if (!StringUtil.isEmpty(qryProfitCommonReq.getProductId())) {
                if (CouponType.TRANSFERFARE.getDictValue().equals(item.getCouponType()) || CouponType.FREETRANSFERFARE.getDictValue().equals(item.getCouponType()))
                    continue;
            }

            //查询子券信息

            //优惠卷阶梯规则
            List<CouponStageRuleEntity> stageList = couponStageRuleMapper.findStageByCouponId(item.getId());
            //优惠券与阶段的简短传输实体   stageId   couponId
            List<ShortCouponDetailDto> shortStageList = new ArrayList<>();
            //获取不满足领取频率的数据:不能反向查找，反向查询返回结果为空并不代表该券不可用
            if (!CommonConstant.EXCLUSIONFLAG_ALL.equals(qryProfitCommonReq.getExclusionFlag())) {
                //检查指定客户的额度信息
                CouponQuotaRuleEntity quota = couponQuotaRuleMapper.findCouponQuotaRuleById(item.getId());
                //检查所有客户领取额度情况
                CommonBoolDto boolDto = clientCouponService.checkCouponAllQuota(quota);
                //校验不通过直接返回
                if (!boolDto.getSuccess()) {
                    continue;
                }
                if (!CommonUtils.isEmptyorNull(qryProfitCommonReq.getClientId())) {
                    boolDto = clientCouponService.checkCouponPersonQuota(quota, qryProfitCommonReq.getClientId());
                    //校验不通过直接返回
                    if (!boolDto.getSuccess()) {
                        continue;
                    }
                    //查询客户受频率限制的券
                    shortStageList = couponGetOrUseFreqRuleMapper.findClinetFreqForbidCouponDetailListById(qryProfitCommonReq.getClientId(), item.getId(), "", OpCouponType.GETRULE.getDictValue());
                }
            }

            //子券因领取频率受限的
            if (!stageList.isEmpty() && !shortStageList.isEmpty()) {
                List<CouponStageRuleDto> couponStageList = new ArrayList<>();

                for (CouponStageRuleEntity stageItem : stageList) {
                    //排除用户领取频率限制的
                    boolean isExsit = false;
                    for (ShortCouponDetailDto shortItem : shortStageList) {
                        if (stageItem.getUuid().equals(shortItem.getStageId())) {
                            isExsit = true;
                            shortStageList.remove(shortItem);//线程安全
                            log.info(String.format("该有门槛优惠券{0}的门槛{1}不能被领取，因为其领取频率超限", item.getId(), shortItem.getStageId()));

                            break;
                        }
                    }
                    //该阶梯没有领取频率限制则可领取
                    if (!isExsit)
                        couponStageList.add(BeanPropertiesConverter.copyProperties(stageItem, CouponStageRuleDto.class));
                }
                //有阶梯的优惠券，如果有0个阶梯能领取该券可领取，否则该券不能领取
                if (couponStageList.size() > 0) {
                    finnalCouponlist.add(item);
                    finnalStageMap.put(item.getId(), couponStageList);
                } else {
                    log.info(String.format("该有门槛优惠券{0}不能被领取，因为其领取频率超限", item.getId()));
                }
            }
            //没有领取频率受限的
            else if (shortStageList.isEmpty()) {
                finnalCouponlist.add(item);//不需要重置门槛
            }
            //没有子券，受领取频率限制
            else {
                boolean isLimit = false;
                //正常数据没有阶梯的券，此处只会有一条在shortStageList中，循环是为了容错
                for (ShortCouponDetailDto shortItem : shortStageList) {
                    if (item.getUuid().equals(shortItem.getCouponId()) &&
                            CommonUtils.isEmptyorNull(shortItem.getStageId())) {
                        isLimit = true;
                        break;
                    }
                }
                //该券没有领取频率限制则可领取
                if (!isLimit) {
                    finnalCouponlist.add(item);//不需要重置门槛
                } else {
                    log.info(String.format("该无门槛优惠券{0}不能被领取，因为其领取频率超限", item.getId()));
                }
            }
        }

        log.info("可领取的优惠券List:[{}]" + JSONObject.toJSONString(finnalCouponlist));
        result = combinationCoupon(finnalCouponlist);
        log.info("可领取的优惠券结果:[{}]" + JSONObject.toJSONString(result));
        for (CouponDetailDto item : result) {
            if (finnalStageMap.get(item.getProductCouponDto().getId()) != null) {
                item.setStageList(finnalStageMap.get(item.getProductCouponDto().getId()));
            }
        }
        //清除缓存
        finnalStageMap.clear();
        log.info("最终可领取的优惠券结果:[{}]" + JSONObject.toJSONString(result));
        return result;

    }

    /**
     * 添加优惠券
     *
     * @param req 优惠券详情
     * @return 执行数量
     */
    // @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonBoolDto<CouponDetailDto> addCoupon2(CouponDetailDto req) {
        //权益中心标志为3，活动表标识为1，ProductCoupon表标识为2
        //SnowflakeIdWorker stageWorker = new SnowflakeIdWorker(3, 2);
        CommonBoolDto<CouponDetailDto> result = new CommonBoolDto<>(false);
        result.setCode(PARAM_ERROR.getCode());
        List<CouponStageRuleEntity> stageList = new ArrayList<>();
        List<CouponGetOrUseFreqRuleEntity> freqRuleList = new ArrayList<>();
        int sqlresult = 0;

        ProductCouponDto dto = req.getProductCouponDto();
        if (dto == null) {
            result.setDesc("没有指定编辑信息");
            return result;
        }

        if (dto.getAllowGetBeginDate() == null) {
            if (dto.getAllowUseBeginDate() == null) {
                dto.setAllowUseBeginDate(now());
            }
            dto.setAllowGetBeginDate(dto.getAllowUseBeginDate());
        } else {
            //参数保护，使用日期为空则等于领取有效起始日
            if (dto.getAllowUseBeginDate() == null) {
                dto.setAllowUseBeginDate(dto.getAllowGetBeginDate());
            }
        }

        if (dto.getAllowGetBeginDate() == null) {
            dto.setAllowGetBeginDate(now());
        }
        if (dto.getAllowGetEndDate() == null) {
            dto.setAllowGetEndDate(LocalDateTime.of(2099, 12, 31, 0, 0, 0));
        }
        if (dto.getAllowUseBeginDate() == null) {
            dto.setAllowUseBeginDate(now());
        }
      /*  if (dto.getAllowUseEndDate() == null) {
            dto.setAllowUseEndDate(LocalDateTime.of(2099, 12, 31, 0, 0, 0));
        }*/
        //参数保护，默认使用日期为最大
        if (dto.getAllowUseEndDate() == null) {
            dto.setAllowUseEndDate(LocalDateTime.of(2099, 12, 31, 0, 0, 0));
        }
        if (dto.getAllowGetEndDate() == null) {
            dto.setAllowGetEndDate(dto.getAllowUseEndDate());
        }

        //参数保护，默认期限为无期限
        if (dto.getValIdTerm() == null)
            dto.setValIdTerm(0);

        //参数保护，实际有效日期=领取日+期限
        if (dto.getUseGeEndDateFlag() == null)
            dto.setUseGeEndDateFlag(UseGeEndDateFlag.NO.getDictValue());

        if (StringUtil.isEmpty(dto.getUsedStage()))
            dto.setUsedStage(UsedStage.AfterPay.getDictValue());

        if (StringUtil.isEmpty(dto.getGetStage()))
            dto.setGetStage(UsedStage.Other.getDictValue());

        if (StringUtil.isEmpty(dto.getGetType()))
            dto.setGetType(HANLD.getDictValue());

        if (dto.getAllowUseEndDate().compareTo(dto.getAllowUseBeginDate()) < 0) {
            result.setDesc("优惠券使用日期无效：起始值" + dto.getAllowUseBeginDate() + "；结束值" + dto.getAllowUseEndDate());
            return result;
        }

        if (dto.getAllowGetEndDate().compareTo(dto.getAllowGetBeginDate()) < 0) {
            result.setDesc("优惠券领取日期无效：起始值" + dto.getAllowGetBeginDate() + "；结束值" + dto.getAllowGetEndDate());
            return result;
        }

        if (CouponType.FREETRANSFERFARE.getDictValue().equals(dto.getCouponType()) || CouponType.TRANSFERFARE.getDictValue().equals(dto.getCouponType())) {
            dto.setCouponLevel(CouponActivityLevel.GLOBAL.getDictValue());
            dto.setApplyProductFlag(ApplyProductFlag.ALL.getDictValue());
        } else if (dto.getCouponLevel() == null) {
            result.setDesc("优惠券等级参数为空：参数值" + dto.getCouponLevel());
            return result;
        }

        if (dto.getCouponStrategyType() == null) {
            result.setDesc("优惠券策略类型没有设置：参数值" + dto.getCouponStrategyType());
            return result;
        }

        if (CouponStrategyType.discount.getDictValue().equals(dto.getCouponStrategyType())) {
            if (BigDecimal.ONE.compareTo(dto.getProfitValue()) <= 0) {
                result.setDesc("折扣优惠券优惠折扣不能高于1，参数值" + dto.getProfitValue());
                return result;
            }
        }
        if (!CommonUtils.isGtZeroDecimal(dto.getProfitValue())) {
            result.setDesc("折扣优惠券优惠折扣不能低于0，参数值" + dto.getProfitValue());
            return result;
        }

        if (dto.getLabelDto() == null) {
            result.setDesc("没有指定标签");
            return result;
        }
        CouponAndActivityLabelEntity labelById = couponAndActivityLabelMapper.findLabelById(dto.getLabelDto().getId());
        if (labelById == null) {
            result.setDesc("指定标签不存在" + dto.getLabelDto().getId());
            return result;
        }

        //如果指定商品集合，默认为自定义配置
        if (req.getProductList() != null && !req.getProductList().isEmpty()) {
            dto.setApplyProductFlag(ApplyProductFlag.APPOINTPRODUCT.getDictValue());
        }
        //生成优惠编号
        //dto.setId(stageWorker.nextId() + "");
        dto.setId(uidGenerator.getUID2());
        //【处理阶梯】
        if (req.getStageList() != null && !req.getStageList().isEmpty()) {

            //组装子券信息
            for (CouponStageRuleDto stage : req.getStageList()) {
                if (stage.getBeginValue() == null)
                    stage.setBeginValue(BigDecimal.ZERO);
                if (!CommonUtils.isGtZeroDecimal(stage.getEndValue())) {
                    stage.setEndValue(CommonConstant.IGNOREVALUE);
                }
                if (!CommonUtils.isGtZeroDecimal(stage.getCouponValue()) && CommonUtils.isGtZeroDecimal(dto.getProfitValue())) {
                    stage.setCouponValue(dto.getProfitValue());
                }
                stage.setCouponId(dto.getId());//将先生成的id设置到门槛阶梯
                if (CommonUtils.isEmptyorNull(stage.getCouponShortDesc())) {
                    stage.setCouponShortDesc(dto.getCouponShortDesc());
                }
                if (CommonUtils.isEmptyorNull(stage.getTriggerByType())) {
                    stage.setTriggerByType(TriggerByType.CAPITAL.getDictValue());
                }

                CouponStageRuleEntity stageRuleEntity = BeanPropertiesUtils.copyProperties(stage, CouponStageRuleEntity.class);
                stageRuleEntity.setCreateAuthor(req.getOperator());
                stageRuleEntity.setUpdateAuthor(req.getOperator());
                stageRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
                stageRuleEntity.setId(CommonUtils.getUUID());
                stage.setId(stageRuleEntity.getId());
                stageList.add(stageRuleEntity);
            }
            batchService.batchDispose(stageList, CouponStageRuleMapper.class, "insert");
        }

        //【处理频率】
        //逻辑删除  通过优惠id  更改IsEnable值为0
        couponGetOrUseFreqRuleMapper.logicDelByCouponId(dto.getId());
        if (req.getFreqRuleList() != null) {
            for (CouponGetOrUseFreqRuleDto item : req.getFreqRuleList()) {
                item.setValue(1);//默认不支持多频率
                item.setCouponId(dto.getId());//将先生成的id设置到门槛阶梯
                if (CommonUtils.isEmptyorNull(item.getOpCouponType()))
                    item.setOpCouponType(OpCouponType.GETRULE.getDictValue());

                if (CommonUtils.isEmptyorNull(item.getStageId())) {
                    if (req.getStageList() != null && req.getStageList().size() == 1) {
                        item.setStageId(req.getStageList().get(0).getId());
                    }
                }

                CouponGetOrUseFreqRuleEntity freqRuleEntity = BeanPropertiesUtils.copyProperties(item, CouponGetOrUseFreqRuleEntity.class);
                freqRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
                freqRuleEntity.setId(CommonUtils.getUUID());
                item.setId(freqRuleEntity.getId());//返回生成的id
                freqRuleList.add(freqRuleEntity);

            }
            batchService.batchDispose(freqRuleList, CouponGetOrUseFreqRuleMapper.class, "insert");
        }

        //【处理限额】
        if (req.getQuotaRule() != null) {
            req.getQuotaRule().setCouponId(dto.getId());//将先生成的id设置到额度

            CouponQuotaRuleEntity quotaRuleEntity = BeanPropertiesUtils.copyProperties(req.getQuotaRule(), CouponQuotaRuleEntity.class);
            quotaRuleEntity.setCreateAuthor(req.getOperator());
            quotaRuleEntity.setUpdateAuthor(req.getOperator());
            quotaRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
            sqlresult = couponQuotaRuleMapper.insert(quotaRuleEntity);
            if (sqlresult <= 0) {
                throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("新增优惠额度信息错误，编号" + quotaRuleEntity.getId()));
            }
        }

        //【配置适用商品】
        if (req.getProductList() != null) {
            BatchRefProductReq refProductReq = new BatchRefProductReq();
            refProductReq.setId(dto.getId());//将先生成的id设置到商品
            refProductReq.setProductList(req.getProductList());
            couponRefProductService.batchAddCouponRefProduct(refProductReq);
        }

        //【基本信息】
        ProductCouponEntity entity = BeanPropertiesUtils.copyProperties(dto, ProductCouponEntity.class);
        entity.setCouponLable(dto.getLabelDto().getId());
        entity.setUpdateAuthor(req.getOperator());
        entity.setCreateAuthor(req.getOperator());
        entity.setIsEnable(CommonDict.IF_YES.getCode());
        sqlresult = productCouponMapper.insert(entity);
        if (sqlresult <= 0) {
            throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("新增优惠信息错误，编号" + entity.getId()));
        }
        result.setSuccess(true);
        result.setCode(NET_ERROR.getCode());
        result.setData(req);

        return result;
    }

    /**
     * 有效时间的检验
     *
     * @return
     * @Param req 优惠券详情
     */
    public void checkAllowUseDate(CouponDetailDto req) {
        ProductCouponDto dto = req.getProductCouponDto();
        //处理领取时间 有效日期
        if (HANLD.getDictValue().equals(dto.getGetType())) {
            //按有效日期
            Boolean flag = dto.getAllowUseBeginDate() != null && dto.getAllowUseEndDate() != null;
            if (flag) {
                if (dto.getAllowUseBeginDate().compareTo(dto.getAllowGetBeginDate()) < 0 || dto.getAllowUseEndDate().compareTo(dto.getAllowGetEndDate()) < 0) {
                    throw new BizException(WRONG_DATE_SPECIFICATION);
                }
            } else {
                //按当月有效
                if (dto.getMonthValid()) {
                    dto.setAllowUseBeginDate(dto.getAllowGetBeginDate());
                    dto.setAllowUseEndDate(lastMonthDay());
                }
                //按天数
                if (dto.getValIdTerm() != null) {
                    dto.setAllowUseBeginDate(dto.getAllowGetBeginDate());
                    dto.setAllowUseEndDate(numToDate(req));
                }
                if (dto.getAllowGetBeginDate().compareTo(now()) < 0) {
                    throw new BizException(DATA_CHECK_FAILED);
                }
            }

        }
        if (CouponGetType.AUTO.getDictValue().equals(dto.getGetType())) {
            //按天数
            if (dto.getMonthValid()) {
                dto.setAllowUseBeginDate(now());
                dto.setAllowUseEndDate(lastMonthDay());

            }
            //按当月有效
            if (dto.getValIdTerm() != null) {
                dto.setAllowUseBeginDate(now());
                dto.setAllowUseEndDate(numToDate(req));

            }
        }

    }

    /**
     * 添加优惠券
     *
     * @param req 优惠券详情
     * @return 执行数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonBoolDto<CouponDetailDto> addCoupon(CouponDetailDto req) {
        //权益中心标志为3，活动表标识为1，ProductCoupon表标识为2
        //SnowflakeIdWorker stageWorker = new SnowflakeIdWorker(3, 2);
        CommonBoolDto<CouponDetailDto> result = new CommonBoolDto<>(false);
        result.setCode(PARAM_ERROR.getCode());
        List<CouponStageRuleEntity> stageList = new ArrayList<>();
        List<CouponGetOrUseFreqRuleEntity> freqRuleList = new ArrayList<>();
        int sqlresult = 0;
        ProductCouponDto dto = req.getProductCouponDto();
        if (dto == null) {
            result.setDesc("没有指定编辑信息");
            return result;
        }

        //参数保护，实际有效日期=领取日+期限
        if (dto.getUseGeEndDateFlag() == null)
            dto.setUseGeEndDateFlag(UseGeEndDateFlag.NO.getDictValue());

        if (StringUtil.isEmpty(dto.getUsedStage()))
            dto.setUsedStage(UsedStage.AfterPay.getDictValue());

        if (StringUtil.isEmpty(dto.getGetStage()))
            dto.setGetStage(UsedStage.Other.getDictValue());

        if (StringUtil.isEmpty(dto.getGetType()))
            dto.setGetType(HANLD.getDictValue());
        if (CouponType.FREETRANSFERFARE.getDictValue().equals(dto.getCouponType()) || CouponType.TRANSFERFARE.getDictValue().equals(dto.getCouponType())) {
            dto.setCouponLevel(CouponActivityLevel.GLOBAL.getDictValue());
            dto.setApplyProductFlag(ApplyProductFlag.ALL.getDictValue());
        } else if (dto.getCouponLevel() == null) {
            result.setDesc("优惠券等级参数为空：参数值" + dto.getCouponLevel());
            return result;
        }

        if (dto.getCouponStrategyType() == null) {
            result.setDesc("优惠券策略类型没有设置：参数值" + dto.getCouponStrategyType());
            return result;
        }

        if (CouponStrategyType.discount.getDictValue().equals(dto.getCouponStrategyType())) {
            if (BigDecimal.ONE.compareTo(dto.getProfitValue()) <= 0) {
                result.setDesc("折扣优惠券优惠折扣不能高于1，参数值" + dto.getProfitValue());
                return result;
            }
        }
        if (!CommonUtils.isGtZeroDecimal(dto.getProfitValue())) {
            result.setDesc("折扣优惠券优惠折扣不能低于0，参数值" + dto.getProfitValue());
            return result;
        }

        if (dto.getLabelDto() == null) {
            result.setDesc("没有指定标签");
            return result;
        }
        CouponAndActivityLabelEntity labelById = couponAndActivityLabelMapper.findLabelById(dto.getLabelDto().getId());
        if (labelById == null) {
            result.setDesc("指定标签不存在" + dto.getLabelDto().getId());
            return result;
        }

        //如果指定商品集合，默认为自定义配置
        if (req.getProductList() != null && !req.getProductList().isEmpty()) {
            dto.setApplyProductFlag(ApplyProductFlag.APPOINTPRODUCT.getDictValue());
        }
        //生成优惠编号
        //dto.setId(stageWorker.nextId() + "");
        dto.setId(uidGenerator.getUID2());
        //【处理阶梯】
        if (req.getStageList() != null && !req.getStageList().isEmpty()) {

            //组装子券信息
            for (CouponStageRuleDto stage : req.getStageList()) {
                if (stage.getBeginValue() == null)
                    stage.setBeginValue(BigDecimal.ZERO);
                if (!CommonUtils.isGtZeroDecimal(stage.getEndValue())) {
                    stage.setEndValue(CommonConstant.IGNOREVALUE);
                }
                if (!CommonUtils.isGtZeroDecimal(stage.getCouponValue()) && CommonUtils.isGtZeroDecimal(dto.getProfitValue())) {
                    stage.setCouponValue(dto.getProfitValue());
                }
                stage.setCouponId(dto.getId());//将先生成的id设置到门槛阶梯
                if (CommonUtils.isEmptyorNull(stage.getCouponShortDesc())) {
                    stage.setCouponShortDesc(dto.getCouponShortDesc());
                }
                if (CommonUtils.isEmptyorNull(stage.getTriggerByType())) {
                    stage.setTriggerByType(TriggerByType.CAPITAL.getDictValue());
                }

                CouponStageRuleEntity stageRuleEntity = BeanPropertiesUtils.copyProperties(stage, CouponStageRuleEntity.class);
                stageRuleEntity.setCreateAuthor(req.getOperator());
                stageRuleEntity.setUpdateAuthor(req.getOperator());
                stageRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
                stageRuleEntity.setId(CommonUtils.getUUID());
                stage.setId(stageRuleEntity.getId());
                stageList.add(stageRuleEntity);
            }
            batchService.batchDispose(stageList, CouponStageRuleMapper.class, "insert");
        }

        //领取时间，有效时间校验
        checkAllowUseDate(req);

        //手动领取的优惠券要进行领取频率、领取额度、及领取时间的判断
        if (HANLD.getDictValue().equals(dto.getGetType())) {

            //【处理频率】逻辑删除  通过优惠id
            couponGetOrUseFreqRuleMapper.logicDelByCouponId(dto.getId());
            if (req.getFreqRuleList() != null) {
                for (CouponGetOrUseFreqRuleDto item : req.getFreqRuleList()) {
                    item.setValue(1);//默认不支持多频率
                    item.setCouponId(dto.getId());//将先生成的id设置到门槛阶梯
                    if (CommonUtils.isEmptyorNull(item.getOpCouponType()))
                        item.setOpCouponType(OpCouponType.GETRULE.getDictValue());

                    if (CommonUtils.isEmptyorNull(item.getStageId())) {
                        if (req.getStageList() != null && req.getStageList().size() == 1) {
                            item.setStageId(req.getStageList().get(0).getId());
                        }
                    }

                    CouponGetOrUseFreqRuleEntity freqRuleEntity = BeanPropertiesUtils.copyProperties(item, CouponGetOrUseFreqRuleEntity.class);
                    freqRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
                    freqRuleEntity.setId(CommonUtils.getUUID());
                    item.setId(freqRuleEntity.getId());//返回生成的id
                    freqRuleList.add(freqRuleEntity);

                }
                batchService.batchDispose(freqRuleList, CouponGetOrUseFreqRuleMapper.class, "insert");
            }
            //【处理限额】
            /*if (req.getQuotaRule() != null) {
                req.getQuotaRule().setCouponId(dto.getId());//将先生成的id设置到额度

                CouponQuotaRuleEntity quotaRuleEntity = BeanPropertiesUtils.copyProperties(req.getQuotaRule(), CouponQuotaRuleEntity.class);
                quotaRuleEntity.setCreateAuthor(req.getOperator());
                quotaRuleEntity.setUpdateAuthor(req.getOperator());
                quotaRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
                sqlresult = couponQuotaRuleMapper.insert(quotaRuleEntity);
                if (sqlresult <= 0) {
                    throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("新增优惠额度信息错误，编号" + quotaRuleEntity.getId()));
                }
            }*/

        }

        //【处理限额】
        if (req.getQuotaRule() != null) {
            req.getQuotaRule().setCouponId(dto.getId());//将先生成的id设置到额度

            CouponQuotaRuleEntity quotaRuleEntity = BeanPropertiesUtils.copyProperties(req.getQuotaRule(), CouponQuotaRuleEntity.class);
            quotaRuleEntity.setCreateAuthor(req.getOperator());
            quotaRuleEntity.setUpdateAuthor(req.getOperator());
            quotaRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
            sqlresult = couponQuotaRuleMapper.insert(quotaRuleEntity);
            if (sqlresult <= 0) {
                throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("新增优惠额度信息错误，编号" + quotaRuleEntity.getId()));
            }
        }

        if (dto.getAllowUseEndDate().compareTo(dto.getAllowUseBeginDate()) < 0) {
            result.setDesc("优惠券使用日期无效：起始值" + dto.getAllowUseBeginDate() + "；结束值" + dto.getAllowUseEndDate());
            return result;
        }
        //【配置适用商品】
        if (req.getProductList() != null) {
            BatchRefProductReq refProductReq = new BatchRefProductReq();
            refProductReq.setId(dto.getId());//将先生成的id设置到商品
            refProductReq.setProductList(req.getProductList());
            couponRefProductService.batchAddCouponRefProduct(refProductReq);
        }

        //【基本信息】
        ProductCouponEntity entity = BeanPropertiesUtils.copyProperties(dto, ProductCouponEntity.class);
        entity.setCouponLable(dto.getLabelDto().getId());
        entity.setUpdateAuthor(req.getOperator());
        entity.setCreateAuthor(req.getOperator());
        entity.setIsEnable(CommonDict.IF_YES.getCode());
        sqlresult = productCouponMapper.insert(entity);
        if (sqlresult <= 0) {
            throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("新增优惠信息错误，编号" + entity.getId()));
        }
        result.setSuccess(true);
        result.setCode(NET_ERROR.getCode());
        result.setData(req);

        return result;
    }

    @Override
    @Transactional
    public void addCoupon2(AddCouponReq2 addCouponReq2) {
        ProductCouponEntity productCouponEntity = createProductCouponEntity(addCouponReq2);

        createCouponQuotaRuleEntity(addCouponReq2, productCouponEntity);

        String stageId = createCouponStageRuleEntity(addCouponReq2, productCouponEntity);

        createCouponGetOrUseFreqRuleEntity(addCouponReq2, productCouponEntity, stageId);

        productCouponMapper.insertSelective(productCouponEntity);
    }

    /**
     * 编辑优惠券
     *
     * @param req 优惠券详情
     * @return 执行数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonBoolDto<CouponDetailDto> editCoupon(CouponDetailDto req) {
        CommonBoolDto<CouponDetailDto> result = new CommonBoolDto<CouponDetailDto>(false);
        result.setCode(PARAM_ERROR.getCode());
        //先删后插需要同步处理CouponGetOrUseFreqRule：因为这表中存有对应字段
        List<CouponStageRuleEntity> stageList = new ArrayList<>();
        List<CouponGetOrUseFreqRuleEntity> freqRuleList = new ArrayList<>();
        int sqlresult = 0;

        //商品优惠券Dto
        ProductCouponDto dto = req.getProductCouponDto();
        if (dto == null) {
            result.setDesc("没有指定编辑信息");
            return result;
        }

        if (dto.getAllowGetBeginDate() == null) {
            if (dto.getAllowUseBeginDate() == null) {
                dto.setAllowUseBeginDate(now());
            }
            dto.setAllowGetBeginDate(dto.getAllowUseBeginDate());
        } else {
            //参数保护，使用日期为空则等于领取有效起始日
            if (dto.getAllowUseBeginDate() == null) {
                dto.setAllowUseBeginDate(dto.getAllowGetBeginDate());
            }
        }

        //参数保护，默认使用日期为最大
        if (dto.getAllowUseEndDate() == null) {
            dto.setAllowUseEndDate(LocalDateTime.of(2099, 12, 31, 0, 0, 0));
        }
        if (dto.getAllowGetEndDate() == null) {
            dto.setAllowGetEndDate(dto.getAllowUseEndDate());
        }

        //参数保护，默认期限为无期限
        if (dto.getValIdTerm() == null)
            dto.setValIdTerm(0);

        //参数保护，实际有效日期=领取日+期限
        if (dto.getUseGeEndDateFlag() == null)
            dto.setUseGeEndDateFlag(UseGeEndDateFlag.NO.getDictValue());

        if (StringUtil.isEmpty(dto.getUsedStage()))//付后-0  确认收货后-1
            dto.setUsedStage(UsedStage.AfterPay.getDictValue());

        if (StringUtil.isEmpty(dto.getGetStage()))
            dto.setGetStage(UsedStage.Other.getDictValue());

        if (StringUtil.isEmpty(dto.getGetType()))
            dto.setGetType(HANLD.getDictValue());

        if (dto.getAllowUseEndDate().compareTo(dto.getAllowUseBeginDate()) < 0) {
            result.setDesc("优惠券使用日期无效：起始值" + dto.getAllowUseBeginDate() + "；结束值" + dto.getAllowUseEndDate());
            return result;
        }

        if (dto.getAllowGetEndDate().compareTo(dto.getAllowGetBeginDate()) < 0) {
            result.setDesc("优惠券领取日期无效：起始值" + dto.getAllowGetBeginDate() + "；结束值" + dto.getAllowGetEndDate());
            return result;
        }

        //运费券的登记为全局
        if (CouponType.FREETRANSFERFARE.getDictValue().equals(dto.getCouponType()) || CouponType.TRANSFERFARE.getDictValue().equals(dto.getCouponType())) {
            dto.setCouponLevel(CouponActivityLevel.GLOBAL.getDictValue());
            dto.setApplyProductFlag(ApplyProductFlag.ALL.getDictValue());
        } else {
            if (dto.getCouponLevel() == null) {
                result.setDesc("优惠券等级参数为空：参数值" + dto.getCouponLevel());
                return result;
            }
        }

        //必须要指定为折扣、满减等策略
        if (dto.getCouponStrategyType() == null) {
            result.setDesc("优惠券策略类型没有设置：参数值" + dto.getCouponStrategyType());
            return result;
        }

        //折扣的值必须在(0,1)之间
        if (CouponStrategyType.discount.getDictValue().equals(dto.getCouponStrategyType())) {
            if (BigDecimal.ONE.compareTo(dto.getProfitValue()) <= 0) {
                result.setDesc("折扣优惠券优惠折扣不能高于1，参数值" + dto.getProfitValue());
                return result;
            }
        }

        if (!CommonUtils.isGtZeroDecimal(dto.getProfitValue())) {
            result.setDesc("折扣优惠券优惠折扣不能低于0，参数值" + dto.getProfitValue());
            return result;
        }

        //校验标签是否存在
        if (dto.getLabelDto() == null) {
            result.setDesc("没有指定标签");
            return result;
        }
        CouponAndActivityLabelEntity labelById = couponAndActivityLabelMapper.findLabelById(dto.getLabelDto().getId());
        if (labelById == null) {
            result.setDesc("指定标签不存在" + dto.getLabelDto().getId());
            return result;
        }

        //编辑主体必须存在
        ProductCouponEntity entity = productCouponMapper.findProductCouponById(dto.getId());
        if (entity == null) {
            result.setDesc("指定编辑优惠券不存在");
            return result;
        }

        //如果指定商品集合，默认为自定义配置
        if (req.getProductList() != null && !req.getProductList().isEmpty()) {
            dto.setApplyProductFlag(ApplyProductFlag.APPOINTPRODUCT.getDictValue());
        }

        //【处理阶梯】

        if (req.getStageList() != null && !req.getStageList().isEmpty()) {
            //组装子券信息
            for (CouponStageRuleDto stage : req.getStageList()) {
                //优惠券阶梯规则实体
                CouponStageRuleEntity stageRuleEntity = new CouponStageRuleEntity();
                if (stage.getBeginValue() == null)
                    stage.setBeginValue(BigDecimal.ZERO);
                if (!CommonUtils.isGtZeroDecimal(stage.getEndValue())) {
                    stage.setEndValue(CommonConstant.IGNOREVALUE);
                }

                if (!CommonUtils.isGtZeroDecimal(stage.getCouponValue()) && CommonUtils.isGtZeroDecimal(dto.getProfitValue())) {
                    stage.setCouponValue(dto.getProfitValue());
                }

                if (CommonUtils.isEmptyorNull(stage.getCouponShortDesc())) {
                    stage.setCouponShortDesc(dto.getCouponShortDesc());
                }
                if (CommonUtils.isEmptyorNull(stage.getTriggerByType())) {
                    stage.setTriggerByType(TriggerByType.CAPITAL.getDictValue());
                }

                stage.setCouponId(dto.getId());
                BeanPropertiesUtils.copyProperties(stage, stageRuleEntity);
                stageRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
                if (CommonUtils.isEmptyorNull(stage.getId())) {

                    stageRuleEntity.setId(CommonUtils.getUUID());
                    stage.setId(stageRuleEntity.getId());
                    stageRuleEntity.setCreateAuthor(req.getOperator());
                    stageRuleEntity.setUpdateAuthor(req.getOperator());
                    sqlresult = couponStageRuleMapper.insert(stageRuleEntity);
                    if (sqlresult <= 0) {
                        throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("插入优惠门槛错误，子券id" + stageRuleEntity.getId()));
                    }
                } else {
                    stageRuleEntity.setUpdateAuthor(req.getOperator());
                    sqlresult = couponStageRuleMapper.updateByPrimaryKeySelective(stageRuleEntity);
                    if (sqlresult <= 0) {
                        throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("更新优惠门槛错误，子券id" + stageRuleEntity.getId()));
                    }
                }
                stageList.add(stageRuleEntity);
            }

        }

        //【处理频率】   优惠卷获得或使用频率规则-删除通过优惠id    dto商品优惠券
        couponGetOrUseFreqRuleMapper.deleteByCouponId(dto.getId());
        //couponGetOrUseFreqRuleMapper.logicDelByCouponId(dto.getId());
        //领取或使用频率规则
        if (req.getFreqRuleList() != null) {
            for (CouponGetOrUseFreqRuleDto item : req.getFreqRuleList()) {
                item.setValue(1);//默认不支持多频率
                item.setCouponId(dto.getId());
                if (CommonUtils.isEmptyorNull(item.getOpCouponType())) //操作方式 0-获取 1-使用
                    item.setOpCouponType(OpCouponType.GETRULE.getDictValue());
                //优惠阶梯编号
                if (CommonUtils.isEmptyorNull(item.getStageId())) {
                    if (req.getStageList() != null && req.getStageList().size() == 1) {
                        item.setStageId(req.getStageList().get(0).getId());
                    }
                }

                item.setCouponId(dto.getId()); //领取编号
                //优惠券领取使用频率规则实体
                CouponGetOrUseFreqRuleEntity freqRuleEntity = BeanPropertiesUtils.copyProperties(item, CouponGetOrUseFreqRuleEntity.class);
                freqRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());

                if (CommonUtils.isEmptyorNull(item.getId())) {
                    freqRuleEntity.setId(CommonUtils.getUUID());
                    item.setId(freqRuleEntity.getId());
                    sqlresult = couponGetOrUseFreqRuleMapper.insert(freqRuleEntity);
                    if (sqlresult <= 0) {
                        throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("插入优惠适用频率错误，编号" + freqRuleEntity.getId()));
                    }
                } else {
                    sqlresult = couponGetOrUseFreqRuleMapper.updateByPrimaryKey(freqRuleEntity);

                    if (sqlresult <= 0) {
                        throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("更新优惠适用频率错误，编号" + freqRuleEntity.getId()));
                    }
                }

                freqRuleList.add(freqRuleEntity);
            }
        }

        //【处理限额】    //优惠劵额度规则-
        sqlresult = couponQuotaRuleMapper.logicDelByCouponId(dto.getId());
        if (req.getQuotaRule() != null) {

            req.getQuotaRule().setCouponId(dto.getId());
            CouponQuotaRuleEntity quotaRuleEntity = BeanPropertiesUtils.copyProperties(req.getQuotaRule(), CouponQuotaRuleEntity.class);

            quotaRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
            quotaRuleEntity.setUpdateAuthor(req.getOperator());
            if (sqlresult > 0) {
                sqlresult = couponQuotaRuleMapper.updateByPrimaryKey(quotaRuleEntity);
            } else {
                quotaRuleEntity.setCreateAuthor(req.getOperator());
                sqlresult = couponQuotaRuleMapper.insert(quotaRuleEntity);
            }
            if (sqlresult <= 0) {
                throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("处理优惠额度信息错误，编号" + quotaRuleEntity.getId()));
            }
        }

        //【配置适用商品】:传入了代表着需要更新配置

        //商品优惠券Dto
        // ProductCouponDto dto = req.getProductCouponDto();

        BatchRefProductReq refProductReq = new BatchRefProductReq();
        refProductReq.setId(dto.getId());
        if (req.getProductList() != null) {
            refProductReq.setProductList(req.getProductList());
            couponRefProductService.batchAddCouponRefProduct(refProductReq);
        } else if (req.getDelProductList() != null) {
            refProductReq.setDelProductList(req.getDelProductList());
            couponRefProductService.batchDeleteCouponRefProduct(refProductReq);
        } else {
            BeanPropertiesUtils.copyProperties(dto, entity);  //enity-（商品优惠券实体）编辑实体必须存在   dto-商品优惠券dto
            entity.setCouponLable(dto.getLabelDto().getId());
            entity.setUpdateAuthor(req.getOperator());
            entity.setIsEnable(CommonDict.IF_YES.getCode());

            sqlresult = productCouponMapper.updateByPrimaryKey(entity);
            if (sqlresult <= 0) {
                throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("更新优惠信息错误，编号" + entity.getId()));
            }
        }
        result.setSuccess(true);
        result.setCode(NET_ERROR.getCode());
        result.setData(req);

        return result;
    }

    @Override
    @Transactional
    public void editCoupon2(EditCouponReq2 editCouponReq2) {
        String uuid = editCouponReq2.getUuid();
        ProductCouponEntity existProductCouponEntity = productCouponMapper.selectByPrimaryKey(uuid);
        ProductCouponEntity productCouponEntity = createEditProductCouponEntity(editCouponReq2, existProductCouponEntity);
        productCouponMapper.updateByPrimaryKeySelective(productCouponEntity);

        editCouponGetOrUseFreqRuleEntity(editCouponReq2, productCouponEntity);
    }

    @Override
    public MemberProductMaxCouponRsp getMemberProductMaxCoupon(MemberProductMaxCouponReq productMaxCouponReq) {
        MemberProductMaxCouponRsp productMaxCouponRsp = productCouponMapper.getMemberProductMaxCoupon(productMaxCouponReq);
        return productMaxCouponRsp;
    }

    /**
     * 优惠券领取使用频率规则
     *
     * @param editCouponReq2
     * @param productCouponEntity
     */
    private void editCouponGetOrUseFreqRuleEntity(EditCouponReq2 editCouponReq2, ProductCouponEntity productCouponEntity) {
        List<CouponGetOrUseFreqRuleEntity> couponGetOrUseFreqRuleEntities = couponGetOrUseFreqRuleMapper.findByCouponId(productCouponEntity.getUuid());
        if (isEmpty(couponGetOrUseFreqRuleEntities)) {
            return;
        }
        CouponGetOrUseFreqRuleEntity existCouponGetOrUseFreqRuleEntity = couponGetOrUseFreqRuleEntities.get(0);
        CouponGetOrUseFreqRuleEntity couponGetOrUseFreqRuleEntity = new CouponGetOrUseFreqRuleEntity();
        couponGetOrUseFreqRuleEntity.setUuid(existCouponGetOrUseFreqRuleEntity.getUuid());
        couponGetOrUseFreqRuleEntity.setUnit(editCouponReq2.getUnit());
        couponGetOrUseFreqRuleEntity.setAllowCount(editCouponReq2.getAllowCount());
        CouponUnitEnum couponUnitEnum = getCardequityEnum(CouponUnitEnum.class, editCouponReq2.getUnit());
        couponUnitEnum.setPersonTotalNum(couponGetOrUseFreqRuleEntity, editCouponReq2);
        couponGetOrUseFreqRuleMapper.updateByPrimaryKeySelective(couponGetOrUseFreqRuleEntity);
    }

    /**
     * 创建编辑优惠券发放对象
     *
     * @param editCouponReq2
     * @param existProductCouponEntity
     * @return
     */
    private ProductCouponEntity createEditProductCouponEntity(EditCouponReq2 editCouponReq2, ProductCouponEntity existProductCouponEntity) {
        ProductCouponEntity productCouponEntity = new ProductCouponEntity();
        productCouponEntity.setUuid(existProductCouponEntity.getUuid());
        productCouponEntity.setCouponName(editCouponReq2.getCouponName());
        productCouponEntity.setCouponShortDesc(editCouponReq2.getCouponShortDesc());
        productCouponEntity.setCouponDesc(editCouponReq2.getCouponDesc());

        CouponValidTimeTypeEnum couponValidTimeTypeEnum = getCardequityEnum(CouponValidTimeTypeEnum.class, existProductCouponEntity.getValidTimeType());
        couponValidTimeTypeEnum.calcValidDate4Edit(editCouponReq2, existProductCouponEntity, productCouponEntity);
        return productCouponEntity;
    }

    /**
     * 批量删除优惠券
     *
     * @param req 批量优惠券编号
     * @return 执行情况
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonBoolDto<Integer> batchDelCoupon(BatchBaseCouponReq req) {
        CommonBoolDto<Integer> result = new CommonBoolDto<>(false);
        if (req.getBaseCouponList() == null || req.getBaseCouponList().isEmpty()) {
            result.setDesc("没有指定删除的优惠券");
            result.setCode(PARAM_ERROR.getCode());
            return result;
        }


        List<String> coupons = new ArrayList<>();
        for (BaseCouponReq baseCoupon : req.getBaseCouponList()) {
            coupons.add(baseCoupon.getCouponId());
        }

        //检查是否存在客户未使用的优惠券
        int couponCount = clientCouponMapper.findAllClientValidCouponCount(coupons);
        if (couponCount > 0) {
            result.setCode(PARAM_ERROR.getCode());
            result.setDesc("不能删除，存在客户领取该优惠券有效数量" + couponCount);
            return result;
        }

        //检查是否有有效的已领取的优惠券
        batchService.batchDispose(req.getBaseCouponList(), ProductCouponMapper.class, "logicDelById");

        //逻辑删除门槛
        batchService.batchDispose(coupons, CouponStageRuleMapper.class, "logicDelByCouponId");

        //逻辑删除频率
        batchService.batchDispose(coupons, CouponGetOrUseFreqRuleMapper.class, "logicDelByCouponId");

        //逻辑删除适用商品
        batchService.batchDispose(coupons, CouponRefProductMapper.class, "deleteByCouponId");

        //逻辑删除额度
        batchService.batchDispose(coupons, CouponQuotaRuleMapper.class, "logicDelByCouponId");

        result.setSuccess(true);
        result.setCode(NET_ERROR.getCode());
        return result;
    }

    /**
     * 上架优惠券
     *
     * @param req 批量优惠券编号
     * @return 执行情况
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonBoolDto<Integer> upCoupon(BatchBaseCouponReq req) {
        CommonBoolDto<Integer> result = new CommonBoolDto<>(true);
        result.setData(0);
        result.setCode(NET_ERROR.getCode());
        result.setDesc("");

        if (req == null || req.getBaseCouponList() == null || req.getBaseCouponList().isEmpty()) {
            throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("必须指定一个优惠券"));
        }
        List<String> ids = new ArrayList<>();
        for (BaseCouponReq item : req.getBaseCouponList()) {
            ids.add(item.getCouponId());
        }
        List<ProductCouponEntity> entities = productCouponMapper.findCouponListByIds(ids);
        List<ProductCouponEntity> dealList = new ArrayList<>();
        for (ProductCouponEntity item : entities) {
            if (CouponStatus.YES.getDictValue().equals(item.getStatus())) {
                result.setDesc(result.getDesc() + item.getId() + "状态已上架，无需处理|");
                continue;
            }
            item.setStatus(CouponStatus.YES.getDictValue());
            item.setRemark("上架优惠券");
            item.setUpdateAuthor(req.getOperator());
            result.setData(result.getData() + 1);
            dealList.add(item);
        }
        batchService.batchDispose(dealList, ProductCouponMapper.class, "updateByPrimaryKeySelective");


        return result;
    }

    /**
     * 下架优惠券
     *
     * @param req 批量优惠券编号
     * @return 执行情况
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonBoolDto<Integer> downCoupon(BatchBaseCouponReq req) {
        CommonBoolDto<Integer> result = new CommonBoolDto<>(true);
        result.setData(0);
        result.setCode(NET_ERROR.getCode());
        result.setDesc("");

        if (req == null || req.getBaseCouponList() == null || req.getBaseCouponList().isEmpty()) {
            throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("必须指定一个优惠券"));
        }
        List<String> ids = new ArrayList<>();
        for (BaseCouponReq item : req.getBaseCouponList()) {
            ids.add(item.getCouponId());
        }

        List<ProductCouponEntity> entities = productCouponMapper.findCouponListByIds(ids);
        List<ProductCouponEntity> dealList = new ArrayList<>();
        for (ProductCouponEntity item : entities) {
            if (!CouponStatus.YES.getDictValue().equals(item.getStatus())) {
                result.setDesc(result.getDesc() + item.getId() + "状态已下架，无需处理|");
                continue;
            }
            item.setStatus(CouponStatus.NO.getDictValue());
            item.setRemark("下架优惠券");
            item.setUpdateAuthor(req.getOperator());
            result.setData(result.getData() + 1);
            dealList.add(item);
        }
        batchService.batchDispose(dealList, ProductCouponMapper.class, "updateByPrimaryKeySelective");

        return result;


    }

    /**
     * 【上架且有效期内的】查看商品对应优惠券列表
     *
     * @param req 商品编号
     * @return 优惠券详情列表
     */
    @Override
    public List<CouponDetailDto> findCouponListByProduct(FindCouponListByProductReq req) {
        if (req == null || CommonUtils.isEmptyorNull(req.getProductId()))
            throw new BizException(PARAM_ERROR.getCode(), PARAM_ERROR.getFormatDesc("商品编号未指定"));
        //查询有效期内的上架的消费券
        List<ProductCouponEntity> entities = null;
        List<ProductCouponEntity> couponList = new ArrayList<>();
        //包含的券类型0-红包 1-消费券 2-运费券，传多个，无分隔符默认值为01
        if (StringUtil.isEmpty(req.getContainsCouponType()))
            req.setContainsCouponType(CouponType.MONEYBAG.getDictValue() + CouponType.COUPON.getDictValue());
        //查询优惠信息时是否排除掉因额度和领取频率限制的 -0
        if (CommonConstant.EXCLUSIONFLAG_ALL.equals(req.getExclusionFlag())) {
            entities = productCouponMapper.findInQuotaCouponListByProduct("1", "3", req.getProductId(), req.getSkuId(), req.getContainsCouponType());
        } else {
            entities = productCouponMapper.findCouponListByProduct("1", "3", req.getProductId(), req.getSkuId(), req.getContainsCouponType(), HANLD.getDictValue()); //手动
        }
        for (ProductCouponEntity couponEntity : entities) {
            //剔除没有上架的
            if (CouponStatus.YES.getDictValue().equals(couponEntity.getStatus())) {
                couponList.add(couponEntity);
            }
        }
        return combinationCoupon(couponList);
    }

    /**
     * 查询所有优惠券列表
     *
     * @param req 请求体
     * @return 分页优惠券详情
     */
    @Override
    public CouponPageQryRsp findCouponListByCommon(BaseQryCouponReq req) {
        if (req == null)
            req = new BaseQryCouponReq();

        CouponPageQryRsp result = new CouponPageQryRsp();
        PageHelper.startPage(req.getPageNo(), req.getPageSize());
        // 获取活动分页信息
        PageInfo<ProductCouponEntity> entitiesPage = new PageInfo<>(productCouponMapper.findCouponListByCommon(req));
        List<CouponDetailDto> dtoList = combinationCoupon(entitiesPage.getList());

        List<GatherInfoRsp> gatherInfoRspList = productCouponMapper.findGatherCouponList(req);
        result.setGatherResult(gatherInfoRspList);

        PageData<CouponDetailDto> pageresult = convert(entitiesPage, dtoList);
        result.setResult(pageresult);
        return result;
    }

    /**
     * 模糊查询所有优惠券列表
     *
     * @param req
     * @return
     */
    @Override
    public CouponPageQryRsp findCouponList(BaseQryCouponReq req) {
        if (req == null)
            req = new BaseQryCouponReq();
        CouponPageQryRsp result = new CouponPageQryRsp();
        // pagination
        PageHelper.startPage(req.getPageNo(), req.getPageSize());
        // 获取活动分页信息
        PageInfo<ProductCouponEntity> entitiesPage = new PageInfo<>(productCouponMapper.findCouponList(req));
        List<CouponDetailDto> dtoList = combinationCoupon(entitiesPage.getList());
        PageData<CouponDetailDto> pageresult = convert(entitiesPage, dtoList);
        result.setResult(pageresult);

        List<GatherInfoRsp> gatherInfoRspList = productCouponMapper.findGatherCouponList(req);
        result.setGatherResult(gatherInfoRspList);

        return result;
    }

    /**
     * 查询优惠汇总信息
     *
     * @param req 普通优惠活动请求体
     * @return 优惠汇总列表
     */
    @Override
    public List<GatherInfoRsp> findGatherCouponByCommon(BaseQryCouponReq req) {
        if (req == null)
            req = new BaseQryCouponReq();
        return productCouponMapper.findGatherCouponList(req);
    }

    /**
     * 查询所有优惠券列表
     *
     * @param req
     * @return
     */
    @Override
    public CouponDetailDto findCouponById(BaseCouponReq req) {
        CouponDetailDto result = null;

        ProductCouponEntity entity = productCouponMapper.findProductCouponById(req.getCouponId());
        if (entity != null) {
            result = combinationCoupon(entity);
        }
        return result;
    }


    /**
     * 查看商品对应优惠券列表
     *
     * @param req
     * @return
     */
    @Override
    public List<CouponDetailDto> findCouponListByIds(List<String> req) {
        List<CouponDetailDto> dtoList = new ArrayList<>();
        if (req == null || req.isEmpty())
            return dtoList;
        List<ProductCouponEntity> entities = productCouponMapper.findCouponListByIds(req);
        dtoList = combinationCoupon(entities);

        return dtoList;
    }

    /**
     * 拼装优惠券详情
     *
     * @param entities 优惠券主体列表
     * @return 优惠券详情：含限额、频率、子券信息
     */
    @Override
    public List<CouponDetailDto> combinationCoupon(List<ProductCouponEntity> entities) {
        if (entities == null || entities.isEmpty())
            return new ArrayList<>();

        //使用LinkedHashMap保留添加顺序
        Map<String, CouponDetailDto> result = new LinkedHashMap();
        List<String> labelIdList = new ArrayList<>();
        for (ProductCouponEntity item : entities) {
            System.out.println(item + "-----------------");
            CouponDetailDto detailDto = new CouponDetailDto();

            ProductCouponDto productCouponDto = BeanPropertiesUtils.copyProperties(item, ProductCouponDto.class);
            productCouponDto.setCouponStatus(getCouponStatus(item));
            detailDto.setProductCouponDto(productCouponDto);

            if (!CommonUtils.isEmptyorNull(item.getCouponLable())) {
                productCouponDto.setLabelDto(new CouponAndActivityLabelDto());
                productCouponDto.getLabelDto().setId(item.getCouponLable());
                labelIdList.add(item.getCouponLable());
            }
            result.put(item.getId(), detailDto);
        }

        List<String> idList = new ArrayList<>(result.keySet());
        //查询限额
        List<CouponQuotaRuleEntity> quotaRuleEntityList = couponQuotaRuleMapper.findCouponQuotaRuleByIds(idList);
        for (CouponQuotaRuleEntity item : quotaRuleEntityList) {
            if (result.containsKey(item.getCouponId())) {
                result.get(item.getCouponId()).setQuotaRule(BeanPropertiesUtils.copyProperties(item, CouponQuotaRuleDto.class));
            }
        }

        //查询领取频率
        List<CouponGetOrUseFreqRuleEntity> freqRuleEntities = couponGetOrUseFreqRuleMapper.findByCouponIds(idList);
        for (CouponGetOrUseFreqRuleEntity item : freqRuleEntities) {
            if (result.containsKey(item.getCouponId())) {
                if (result.get(item.getCouponId()).getFreqRuleList() == null) {
                    result.get(item.getCouponId()).setFreqRuleList(new ArrayList<>());
                }
                result.get(item.getCouponId()).getFreqRuleList().add(BeanPropertiesUtils.copyProperties(item, CouponGetOrUseFreqRuleDto.class));
            }
        }

        //查询子券信息
        List<CouponStageRuleEntity> stageRuleEntities = couponStageRuleMapper.findStageByCouponIds(idList);
        for (CouponStageRuleEntity item : stageRuleEntities) {
            if (result.containsKey(item.getCouponId())) {
                if (result.get(item.getCouponId()).getStageList() == null) {
                    result.get(item.getCouponId()).setStageList(new ArrayList<>());
                }
                result.get(item.getCouponId()).getStageList().add(BeanPropertiesUtils.copyProperties(item, CouponStageRuleDto.class));

            }
        }

        //标签信息
        List<CouponAndActivityLabelEntity> labelByIds = couponAndActivityLabelMapper.findLabelByIds(labelIdList);
        for (CouponAndActivityLabelEntity item : labelByIds) {
            for (CouponDetailDto dto : result.values()) {
                if (dto.getProductCouponDto().getLabelDto() != null && item.getId().equals(dto.getProductCouponDto().getLabelDto().getId())) {
                    dto.getProductCouponDto().setLabelDto(BeanPropertiesUtils.copyProperties(item, CouponAndActivityLabelDto.class));
                }
            }
        }

        List<CouponDetailDto> resultList = new ArrayList<>(result.values());
        //清除缓存
        result.clear();
        result = null;

        log.info("判断后的优惠券结果:[{}]" + JSONObject.toJSONString(resultList));
        System.out.println(resultList + "=====");
        return resultList;

    }

    /**
     * 获取优惠券状态
     *
     * @param productCouponEntity
     * @return
     */
    private String getCouponStatus(ProductCouponEntity productCouponEntity) {
        CouponValidTimeTypeEnum couponValidTimeTypeEnum = getCardequityEnum(CouponValidTimeTypeEnum.class, productCouponEntity.getValidTimeType());
        String couponStatus = couponValidTimeTypeEnum.getCouponStatus(productCouponEntity);
        return couponStatus;
    }

    /**
     * 拼装优惠券详情
     *
     * @param entity 优惠券主体
     * @return 优惠券详情：含限额、频率、子券信息
     */
    @Override
    public CouponDetailDto combinationCoupon(ProductCouponEntity entity) {
        CouponDetailDto result = new CouponDetailDto();

        ProductCouponDto productCouponDto = BeanPropertiesUtils.copyProperties(entity, ProductCouponDto.class);
        if (!CommonUtils.isEmptyorNull(entity.getCouponLable())) {
            CouponAndActivityLabelEntity labelEntity = couponAndActivityLabelMapper.findLabelById(entity.getCouponLable());
            if (labelEntity != null) {
                CouponAndActivityLabelDto labelDto = BeanPropertiesUtils.copyProperties(labelEntity, CouponAndActivityLabelDto.class);
                productCouponDto.setLabelDto(labelDto);
            }
        }
        result.setProductCouponDto(productCouponDto);
        //查询限额
        CouponQuotaRuleEntity quotaRuleEntity = couponQuotaRuleMapper.findCouponQuotaRuleById(entity.getId());
        result.setQuotaRule(BeanPropertiesConverter.copyProperties(quotaRuleEntity, CouponQuotaRuleDto.class));

        //查询领取频率
        List<CouponGetOrUseFreqRuleEntity> freqRuleEntities = couponGetOrUseFreqRuleMapper.findByCouponId(entity.getId());
        result.setFreqRuleList(BeanPropertiesConverter.copyPropertiesOfList(freqRuleEntities, CouponGetOrUseFreqRuleDto.class));

        //查询子券信息（优惠劵阶梯规则表）
        List<CouponStageRuleEntity> stageRuleEntities = couponStageRuleMapper.findStageByCouponId(entity.getId());
        result.setStageList(BeanPropertiesConverter.copyPropertiesOfList(stageRuleEntities, CouponStageRuleDto.class));
        return result;
    }


    public int myCompare(CouponDetailDto entity1, CouponDetailDto entity2) {//如果是折扣、任选、优惠价从小到大
        //券类型升序排列
        int sortresult = entity1.getProductCouponDto().getCouponType().compareTo(entity2.getProductCouponDto().getCouponType());
        if (sortresult != 0)
            return sortresult;
        //券类型降序排列
        sortresult = entity2.getProductCouponDto().getCouponLevel().compareTo(entity1.getProductCouponDto().getCouponLevel());
        if (sortresult != 0)
            return sortresult;
        //券优惠金额降序排列
        return entity2.getProductCouponDto().getProfitValue().compareTo(entity1.getProductCouponDto().getProfitValue());
    }


    public int myCompare(ObtainCouponViewDto entity1, ObtainCouponViewDto entity2) {//如果是折扣、任选、优惠价从小到大
        //已过期的排最后
        if (entity1.getValidEndDate().toLocalDate().isBefore(LocalDate.now()) && !entity2.getValidEndDate().toLocalDate().isBefore(LocalDate.now()))
            return -1;
        //券类型升序排列
        int sortresult = entity1.getCouponViewType().compareTo(entity2.getCouponViewType());
        if (sortresult != 0)
            return sortresult;
        //券类型降序排列
        sortresult = entity2.getCouponLevel().compareTo(entity1.getCouponLevel());
        if (sortresult != 0)
            return sortresult;
        //券优惠金额降序排列
        return entity2.getProfitValue().compareTo(entity1.getProfitValue());
    }

    public int myCompare(ClientCouponEntity entity1, ClientCouponEntity entity2) {//如果是折扣、任选、优惠价从小到大
        //已过期的排最后
        if (entity1.getValidEndDate().toLocalDate().isBefore(LocalDate.now()) && !entity2.getValidEndDate().toLocalDate().isBefore(LocalDate.now()))
            return -1;
        //消费券->运费券
        int sortresult = entity1.getCouponType().compareTo(entity2.getCouponType());
        if (sortresult != 0)
            return sortresult;
        //大鱼券->小鱼券
        sortresult = entity2.getCouponLevel().compareTo(entity1.getCouponLevel());
        if (sortresult != 0)
            return sortresult;
        //金额降序
        return entity2.getCouponAmout().compareTo(entity1.getCouponAmout());
    }

    /**
     * 查询H5首页会员专享权益优惠券
     *
     * @param req 查询请求体
     * @return
     */
    @Override
    public List<ObtainCouponViewDto> findFirstPageVipCoupon(PageQryProfitCommonReq req) {
        int retInt = req.getPageSize() <= 0 ? 999 : req.getPageSize();
        //获得优惠卷视图
        List<ObtainCouponViewDto> result = new ArrayList<>();
        //已使用过的券  改动
        List<ObtainCouponViewDto> overresult = new ArrayList<>();

        //1.获取当月可领取的优惠券

        // List<ProductCouponEntity> nextMonthEntities = productCouponMapper.findSpacifyMonthEnableGetCouponsByCommon(req.getProductId(), req.getEntrustWay(), req.getClientType(),0,lastMonthDay());
        //组装CouponDetailDto
        //List<CouponDetailDto> couponDetailDtos = combinationCoupon(nextMonthEntities);

        //优惠卷一期固定自然月  当天可领的即当月
        List<CouponDetailDto> enableGetCoupon = findEnableGetCoupon(req);

        //消费券排前面,2级券排前面

        Collections.sort(enableGetCoupon, new Comparator<CouponDetailDto>() {
            @Override
            public int compare(CouponDetailDto entity1, CouponDetailDto entity2) {//如果是折扣、任选、优惠价从小到大
                return myCompare(entity1, entity2);
            }
        });
        //可以领取的优惠券  改动
        //int t = enableGetCoupon.size() >= retInt ? retInt : enableGetCoupon.size();
        for (int i = 0; i < enableGetCoupon.size(); i++) {
            //商品优惠券
            ProductCouponDto dto = enableGetCoupon.get(i).getProductCouponDto();
            if (!ClientType.MEMBER.getDictValue().equals(dto.getClientTypeSet()))
                continue;
            //获得优惠劵视图
            ObtainCouponViewDto item = new ObtainCouponViewDto();
            BeanPropertiesUtils.copyProperties(enableGetCoupon.get(i).switchToView(), item);
            item.setLabelDto(dto.getLabelDto());//优惠标签:标签：满返券、促销等
            item.setObtainState(CommonConstant.OBTAIN_STATE_NO); //领取状态   0-可领取
            if (dto.getValIdTerm() <= 0) {  //有效期限
                item.setValidStartDate(dto.getAllowUseBeginDate());
                item.setValidEndDate(dto.getAllowUseEndDate());
            } else {
                item.setValidStartDate(now());
                item.setValidEndDate(now().plusDays(dto.getValIdTerm()));
            }
            item.setProductList(enableGetCoupon.get(i).getProductList());
            //result  获得优惠卷视图list
            result.add(item);
        }

        int size = result.size();
        if (size >= 5) {
            return result.subList(0, 5);
        }
        //2.获取已领取的优惠券
        QryComonClientCouponReq innerReq = new QryComonClientCouponReq();
        innerReq.setClientId(req.getClientId());
        //0-可领取 1-已领取 2-已使用 3-过期未使用 4-未开始 5-可使用
        List<ObtainCouponViewDto> clientCoupon = clientCouponService.findClientCoupon(innerReq);
        if (!CollectionUtils.isEmpty(clientCoupon)) {
            clientCoupon = clientCoupon.stream().filter(obtainCouponViewDto -> eq(obtainCouponViewDto.getGetType(), HANLD.getDictValue())).collect(toList());
        }

        List<ObtainCouponViewDto> resultDto = new ArrayList<>();
        for (ObtainCouponViewDto item : clientCoupon) {
            if (!CommonDict.FRONDEND_MEMBER.getCode().equals(item.getTargetFlag()))
                continue;
            if (CommonConstant.OBTAIN_STATE_OVERDUE.equals(item.getObtainState()) ||
                    CommonConstant.OBTAIN_STATE_UNSTART.equals(item.getObtainState())) {
                continue;
            }
            //改动
            if (CommonConstant.OBTAIN_STATE_USE.equals(item.getObtainState())) {
                overresult.add(item);
            } else {
                resultDto.add(item);
            }
        }
        //消费券排前面,2级券排前面
        Collections.sort(resultDto, new Comparator<ObtainCouponViewDto>() {
            @Override
            public int compare(ObtainCouponViewDto entity1, ObtainCouponViewDto entity2) {//如果是折扣、任选、优惠价从小到大
                return myCompare(entity1, entity2);
            }
        });
        resultDto.addAll(overresult);
        //刪除resultDto和result的重复值
        //  resultDto.stream().forEach(r -> result.removeIf(obtainCouponViewDto -> obtainCouponViewDto.getUuid().equals(r.getUuid())));
        result.addAll(resultDto);

        if (result.size() >= 5) {
            return result.subList(0, 5);
        }
        return result;
    }

    /**
     * 查询H5指定月会员专享可领优惠券
     *
     * @param req 查询请求体
     * @return
     */

    //已使用过的劵不显示， 显示所有起始时间在本月的劵    排序
    @Override
    public List<ObtainCouponViewDto> findEnableObtainCouponByMonth(FindEnableObtainCouponByMonthReq req) {
        log.info("优惠券请求参数:[{}]" + JSONObject.toJSONString(req));
        if (req == null)
            req = new FindEnableObtainCouponByMonthReq();
        if (req.getMonthNum() < 0)
            req.setMonthNum(0);

        //获得优惠卷视图
        List<ObtainCouponViewDto> result = new ArrayList<>();
        //已使用过的券
        List<ObtainCouponViewDto> overresult = new ArrayList<>();

        //monthNumFlag   截止第几月或指定第几月标志 0-截止第几月 1-指定第几月
        //monthNum        第几月可领的券的指定月数
        if (req.getMonthNum() == 0 || req.getMonthNumFlag() == 0) {
            //*************当月可领的***********
            //List<ProductCouponEntity> nextMonthEntities = productCouponMapper.findSpacifyMonthEnableGetCouponsByCommon(req.getProductId(), req.getEntrustWay(), req.getClientType(),0,lastMonthDay());
            //List<CouponDetailDto> couponDetailDtos = combinationCoupon(nextMonthEntities);

            //1.其他数据补充 当天可领的（自然月性质）
            List<CouponDetailDto> enableGetCoupon = findEnableGetCoupon(req);

            //2.排序：消费券排前面,2级券排前面  当月可领取的
            Collections.sort(enableGetCoupon, new Comparator<CouponDetailDto>() {
                @Override
                public int compare(CouponDetailDto entity1, CouponDetailDto entity2) {//如果是折扣、任选、优惠价从小到大
                    return myCompare(entity1, entity2);
                }
            });

            //3.转换视图
            for (CouponDetailDto item : enableGetCoupon) {
                // if (ClientType.MEMBER.getDictValue().equals(item.getProductCouponDto().getClientTypeSet()) || StringUtil.eq("*", item.getProductCouponDto().getClientTypeSet())) {}
                if (!ClientType.MEMBER.getDictValue().equals(item.getProductCouponDto().getClientTypeSet()))
                    continue;
                //获得优惠卷视图
                ObtainCouponViewDto viewDto = BeanPropertiesUtils.copyProperties(item.switchToView(), ObtainCouponViewDto.class);
                viewDto.setProductList(item.getProductList());
                viewDto.setLabelDto(item.getProductCouponDto().getLabelDto());
                viewDto.setObtainState(CommonConstant.OBTAIN_STATE_NO);  //可领取

                if (viewDto.getValIdTerm() <= 0) {   //有效日期
                    viewDto.setValidStartDate(item.getProductCouponDto().getAllowUseBeginDate());   //起始时间
                    viewDto.setValidEndDate(item.getProductCouponDto().getAllowUseEndDate());       //结束时间
                } else {
                    viewDto.setValidStartDate(now());
                    viewDto.setValidEndDate(now().plusDays(viewDto.getValIdTerm()));
                }
                result.add(viewDto);
                log.info("转换视图后的优惠券信息:{}" + JSONObject.toJSONString(result));

            }

            //***********当月已领的 ;去掉过期未使用的优惠卷***************************
            List<ClientCouponEntity> obtainCoupon = clientCouponMapper.findCurrMonthObtainCoupon(req.getClientId(), "");

            //1.排序
            Collections.sort(obtainCoupon, new Comparator<ClientCouponEntity>() {
                @Override
                public int compare(ClientCouponEntity entity1, ClientCouponEntity entity2) {//如果是折扣、任选、优惠价从小到大
                    return myCompare(entity1, entity2);
                }
            });

            //2.补充数据
            log.info("已领取组装参数" + obtainCoupon);
            List<FullClientCouponRsp> fullClientCouponRsps = clientCouponService.combClientFullObtainCouponList(obtainCoupon);

            //3.视图转换及过滤
            List<ObtainCouponViewDto> resultDto = new ArrayList<>();
            for (FullClientCouponRsp item : fullClientCouponRsps) {
                // if (ClientType.MEMBER.getDictValue().equals(item.getCoupon().getProductCouponDto().getClientTypeSet()) ||
                //       StringUtil.eq("*", item.getCoupon().getProductCouponDto().getClientTypeSet())) {}
                log.info("视图转换后的值" + item);
                if (!ClientType.MEMBER.getDictValue().equals(item.getCoupon().getProductCouponDto().getClientTypeSet()))
                    continue;

                ObtainCouponViewDto viewDto = BeanPropertiesUtils.copyProperties(item.getCoupon().switchToView(), ObtainCouponViewDto.class);

                viewDto.setProductList(item.getCoupon().getProductList());
                viewDto.setLabelDto(item.getCoupon().getProductCouponDto().getLabelDto());
                viewDto.setObtainId(item.getClientCoupon().getId());
                viewDto.setValidEndDate(item.getClientCoupon().getValidEndDate());
                viewDto.setValidStartDate(item.getClientCoupon().getValidStartDate());
                viewDto.setObtainState(item.getClientCoupon().refreshObtainState());
                //改动
                if (!CommonConstant.OBTAIN_STATE_OVERDUE.equals(viewDto.getObtainState())) {
                    if (!CommonConstant.OBTAIN_STATE_USE.equals(viewDto.getObtainState())) {
                        resultDto.add(viewDto);
                    } else {
                        overresult.add(viewDto);
                    }
                }

            }
            resultDto.addAll(overresult);

            log.info("已领取的优惠券信息:{}" + JSONObject.toJSONString(resultDto));

            //  resultDto.stream().forEach(r-> result.removeIf(obtainCouponViewDto -> obtainCouponViewDto.getUuid().equals(r.getUuid())));


            result.addAll(resultDto);

        }/* else {//查询指定月可以领的券

            List<ProductCouponEntity> nextMonthEntities = productCouponMapper.findSpacifyMonthEnableGetCouponsByCommon(req.getProductId(), req.getEntrustWay(), req.getClientType(), 1);

            //消费券排前面,2级券排前面
           *//* Collections.sort(nextMonthEntities, new Comparator<ProductCouponEntity>() {
                @Override
                public int compare(ProductCouponEntity entity1, ProductCouponEntity entity2) {//如果是折扣、任选、优惠价从小到大
                    int sortresult = entity1.getCouponType().compareTo(entity2.getCouponType());
                    if (sortresult != 0)
                        return sortresult;
                    sortresult = entity2.getCouponLevel().compareTo(entity1.getCouponLevel());
                    if (sortresult != 0)
                        return sortresult;
                    return entity2.getProfitValue().compareTo(entity1.getProfitValue());
                }
            });*//*

            List<CouponDetailDto> detailDtos = combinationCoupon(nextMonthEntities);

            for (CouponDetailDto item : detailDtos) {
                if (!ClientType.MEMBER.getDictValue().equals(item.getProductCouponDto().getClientTypeSet()))
                    continue;
                ObtainCouponViewDto obtainDto = BeanPropertiesUtils.copyProperties(item.switchToView(), ObtainCouponViewDto.class);
                obtainDto.setProductList(item.getProductList());
                obtainDto.setLabelDto(item.getProductCouponDto().getLabelDto());
                obtainDto.setObtainState(CommonConstant.OBTAIN_STATE_NO);
                if (obtainDto.getValIdTerm() <= 0) {  //有效期限  以天为单位
                    obtainDto.setValidStartDate(item.getProductCouponDto().getAllowUseBeginDate());
                    obtainDto.setValidEndDate(item.getProductCouponDto().getAllowUseEndDate());
                } else {
                    obtainDto.setValidStartDate(LocalDateTime.now());
                    obtainDto.setValidEndDate(LocalDateTime.now().plusDays(obtainDto.getValIdTerm()));
                }
                if (obtainDto.getValidEndDate().toLocalDate().isBefore(LocalDate.now())) {
                    overresult.add(obtainDto);
                }else{
                     result.add(obtainDto)     ;
                }
            }
            result.addAll(overresult);
        }*/
        return result;
    }

    /**
     * 查询每月份的最后一天
     *
     * @param
     * @return LocalTimeDate
     */
    public LocalDateTime lastMonthDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Calendar cale = Calendar.getInstance();
        cale.add(Calendar.MONTH, 1);
        cale.set(Calendar.DAY_OF_MONTH, 0);
        LocalDateTime ldt = LocalDateTime.parse(sdf.format(cale.getTime()), df);
        LocalDateTime localDateTime = OrikaBeanPropertiesConverter.copyProperties(ldt, LocalDateTime.class);

        return localDateTime;
    }

    /**
     * 生效日期+天数=截止日期
     *
     * @param req
     * @return LocalDateTime
     */
    public LocalDateTime numToDate(CouponDetailDto req) {

        LocalDateTime allowGetBeginDate = req.getProductCouponDto().getAllowGetBeginDate();
        if (isNull(allowGetBeginDate)) {
            allowGetBeginDate = now();
        }
        return date2LocalDateTime(addDays(localDateTime2Date(allowGetBeginDate), req.getProductCouponDto().getValIdTerm()));
    }

    /**
     * 创建优惠券领取使用频率规则
     *
     * @param addCouponReq2
     * @param productCouponEntity
     * @param stageId
     */
    private void createCouponGetOrUseFreqRuleEntity(AddCouponReq2 addCouponReq2, ProductCouponEntity productCouponEntity, String stageId) {
        if (!eq(HANLD.getDictValue(), addCouponReq2.getGetType())) {
            return;
        }

        couponGetOrUseFreqRuleMapper.logicDelByCouponId(productCouponEntity.getUuid());
        CouponGetOrUseFreqRuleEntity couponGetOrUseFreqRuleEntity = new CouponGetOrUseFreqRuleEntity();
        couponGetOrUseFreqRuleEntity.setUuid(uidGenerator.getUID2());
        couponGetOrUseFreqRuleEntity.setCouponId(productCouponEntity.getUuid());
        couponGetOrUseFreqRuleEntity.setOpCouponType(OpCouponType.GETRULE.getDictValue());
        couponGetOrUseFreqRuleEntity.setUnit(addCouponReq2.getUnit());
        couponGetOrUseFreqRuleEntity.setValue(1);
        couponGetOrUseFreqRuleEntity.setAllowCount(addCouponReq2.getAllowCount());
        couponGetOrUseFreqRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
        couponGetOrUseFreqRuleEntity.setStageId(stageId);
        CouponUnitEnum couponUnitEnum = getCardequityEnum(CouponUnitEnum.class, addCouponReq2.getUnit());
        couponUnitEnum.setPersonTotalNum(couponGetOrUseFreqRuleEntity, addCouponReq2);
        couponGetOrUseFreqRuleMapper.insertSelective(couponGetOrUseFreqRuleEntity);
    }

    /**
     * 创建优惠阶梯使用或获取规则
     *
     * @param addCouponReq2
     * @param productCouponEntity
     * @return
     */
    private String createCouponStageRuleEntity(AddCouponReq2 addCouponReq2, ProductCouponEntity productCouponEntity) {

        BigDecimal conditionValue = addCouponReq2.getConditionValue();
        if (!gtZero(conditionValue)) {
            return null;
        }

        CouponStageRuleEntity couponStageRuleEntity = new CouponStageRuleEntity();
        couponStageRuleEntity.setUuid(uidGenerator.getUID2());
        couponStageRuleEntity.setCouponId(productCouponEntity.getUuid());
        couponStageRuleEntity.setCouponShortDesc(productCouponEntity.getCouponShortDesc());
        couponStageRuleEntity.setCouponValue(addCouponReq2.getProfitValue());
        couponStageRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
        couponStageRuleEntity.setTriggerByType(TriggerByType.CAPITAL.getDictValue());
        couponStageRuleEntity.setBeginValue(ltEqZero(addCouponReq2.getConditionValue()) ? BigDecimal.ZERO : addCouponReq2.getConditionValue());
        BigDecimal perProfitTopValue = addCouponReq2.getPerProfitTopValue();
        BigDecimal endValue = BigDecimal.ZERO;
        boolean flag = gtZero(perProfitTopValue) && lt(perProfitTopValue, CommonConstant.IGNOREVALUE) && gt(perProfitTopValue, addCouponReq2.getProfitValue());
        if (flag) {
            productCouponEntity.setCouponStrategyType(equalstage.getDictValue());
            endValue = perProfitTopValue.divide(addCouponReq2.getProfitValue(), 0, RoundingMode.DOWN).multiply(addCouponReq2.getConditionValue());
        }
        couponStageRuleEntity.setEndValue(ltEqZero(endValue) ? CommonConstant.IGNOREVALUE : endValue);
        couponStageRuleMapper.insertSelective(couponStageRuleEntity);

        return couponStageRuleEntity.getUuid();
    }

    /**
     * 创建优惠券数量
     *
     * @param addCouponReq2
     * @param productCouponEntity
     */
    private void createCouponQuotaRuleEntity(AddCouponReq2 addCouponReq2, ProductCouponEntity productCouponEntity) {
        Integer maxCount = addCouponReq2.getMaxCount();
        if (isNull(maxCount) || maxCount < 0) {
            return;
        }

        CouponQuotaRuleEntity couponQuotaRuleEntity = new CouponQuotaRuleEntity();
        couponQuotaRuleEntity.setMaxCount(addCouponReq2.getMaxCount());
        couponQuotaRuleEntity.setPerMaxAmount(MAX_VALUE);
        couponQuotaRuleEntity.setPerDateAndAccMaxAmount(MAX_VALUE);
        couponQuotaRuleEntity.setPerDateMaxAmount(MAX_VALUE);
        couponQuotaRuleEntity.setPersonMaxAmount(MAX_VALUE);
        couponQuotaRuleEntity.setMaxAmount(MAX_VALUE);
        couponQuotaRuleEntity.setIsEnable(CommonDict.IF_YES.getCode());
        couponQuotaRuleEntity.setCouponId(productCouponEntity.getUuid());
        couponQuotaRuleMapper.insertSelective(couponQuotaRuleEntity);
    }

    /**
     * 创建优惠券对象
     *
     * @param addCouponReq2
     * @return
     */
    private ProductCouponEntity createProductCouponEntity(AddCouponReq2 addCouponReq2) {
        ProductCouponEntity productCouponEntity = new ProductCouponEntity();
        CouponTypeEnum couponTypeEnum = getCardequityEnum(CouponTypeEnum.class, addCouponReq2.getCouponViewType());
        productCouponEntity.setCouponLevel(addCouponReq2.getCouponLevel());
        if (couponTypeEnum.canGlobal()) {
            productCouponEntity.setCouponLevel(CouponActivityLevel.GLOBAL.getDictValue());
            productCouponEntity.setApplyProductFlag(ApplyProductFlag.ALL.getDictValue());
        }

        CouponTargetFlagEnum couponTargetFlagEnum = getCardequityEnum(CouponTargetFlagEnum.class, addCouponReq2.getTargetFlag());
        couponTargetFlagEnum.setClientTypeSet(productCouponEntity, addCouponReq2);
        CouponValidTimeTypeEnum couponValidTimeTypeEnum = getCardequityEnum(CouponValidTimeTypeEnum.class, addCouponReq2.getValidTimeType());
        couponValidTimeTypeEnum.calcValidDate4Create(productCouponEntity, addCouponReq2);

        productCouponEntity.setUuid(uidGenerator.getUID2());
        productCouponEntity.setCouponStrategyType(CouponStrategyType.stage.getDictValue());
        productCouponEntity.setCouponType(addCouponReq2.getCouponViewType());
        productCouponEntity.setCouponName(addCouponReq2.getCouponName());
        productCouponEntity.setCouponLable(addCouponReq2.getCouponLabel());
        productCouponEntity.setCouponShortDesc(addCouponReq2.getCouponShortDesc());
        productCouponEntity.setCouponDesc(addCouponReq2.getCouponDesc());
        productCouponEntity.setUseGeEndDateFlag(UseGeEndDateFlag.NO.getDictValue());
        productCouponEntity.setProfitValue(addCouponReq2.getProfitValue());
        productCouponEntity.setStatus(addCouponReq2.getStatus());
        productCouponEntity.setUsedStage("0");
        productCouponEntity.setGetType(addCouponReq2.getGetType());
        productCouponEntity.setIsEnable(CommonDict.IF_YES.getCode());
        return productCouponEntity;
    }

}


