package com.order.main.mapper;

import com.order.main.dto.TShopGoodsPublishedDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 已发布商品信息Mapper接口
 */
@Mapper
public interface TShopGoodsPublishedMapper {

    /**
     * 根据ID查询
     *
     * @param id 主键ID
     * @return 已发布商品信息
     */
    TShopGoodsPublishedDto selectById(@Param("id") Long id);

    /**
     * 根据平台商品id查询
     * @param trilateralId
     * @return
     */
    List<TShopGoodsPublishedDto> selectByTrilateralId(Long trilateralId);

}