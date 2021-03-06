package com.youyu.cardequity.promotion.vo.req;

import com.youyu.cardequity.promotion.dto.other.ActivityDetailDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 批量活动请求体：用于批量删除、更新等
 */
@Data
public class BatchActivityDetailDto {
    @ApiModelProperty(value = "活动详情列表")
    private List<ActivityDetailDto> activityDetailList;

    @ApiModelProperty(value = "操作者：用于更新产生者或更新者")
    private String operator;
}
