package com.citybooking.server.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("select avg(r.score) from review r join biz_order o on r.order_id=o.id " +
            "where o.merchant_id=#{merchantId} and r.deleted=0")
    Double avgMerchantScore(Long merchantId);

    @Select("select avg(r.score) from review r join biz_order o on r.order_id=o.id " +
            "where o.technician_id=#{technicianId} and r.deleted=0")
    Double avgTechnicianScore(Long technicianId);
}
