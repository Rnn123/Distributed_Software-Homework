package com.seckill.mapper;

import com.seckill.entity.PaymentRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentRecordMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("""
            insert into payment_record(order_id, user_id, amount, status, pay_time)
            values(#{orderId}, #{userId}, #{amount}, #{status}, #{payTime})
            """)
    int insert(PaymentRecord paymentRecord);

    @Select("select * from payment_record where order_id = #{orderId} limit 1")
    PaymentRecord getByOrderId(@Param("orderId") long orderId);
}
