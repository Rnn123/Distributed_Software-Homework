package com.seckill.mapper;

import com.seckill.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("""
            insert into `order`(id, user_id, product_id, order_no, amount, status, create_time, pay_time)
            values(#{id}, #{userId}, #{productId}, #{orderNo}, #{amount}, #{status}, #{createTime}, #{payTime})
            """)
    int insert(Order order);

    @Select("select * from `order` where id = #{id}")
    Order getById(@Param("id") long id);

    @Select("select * from `order` where user_id = #{userId} order by create_time desc")
    List<Order> listByUserId(@Param("userId") long userId);

    @Select("select * from `order` where user_id = #{userId} and product_id = #{productId} limit 1")
    Order getByUserIdAndProductId(@Param("userId") long userId, @Param("productId") long productId);

    @Select("select count(1) from `order` where user_id = #{userId} and product_id = #{productId}")
    int countByUserIdAndProductId(@Param("userId") long userId, @Param("productId") long productId);
}
