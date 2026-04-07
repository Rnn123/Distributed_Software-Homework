package com.seckill.mapper;

import com.seckill.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
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

    @Select("select * from `order` where user_id = #{userId} and product_id = #{productId} order by create_time desc limit 1")
    Order getByUserIdAndProductId(@Param("userId") long userId, @Param("productId") long productId);

    @Select("select count(1) from `order` where user_id = #{userId} and product_id = #{productId} and status <> 2")
    int countActiveByUserIdAndProductId(@Param("userId") long userId, @Param("productId") long productId);

    @Update("""
            update `order`
            set status = 0
            where id = #{orderId}
              and status = -1
            """)
    int markUnpaid(@Param("orderId") long orderId);

    @Update("""
            update `order`
            set status = 1,
                pay_time = #{payTime}
            where id = #{orderId}
              and status = 0
            """)
    int markPaid(@Param("orderId") long orderId, @Param("payTime") Date payTime);

    @Update("""
            update `order`
            set status = 2
            where id = #{orderId}
              and status = -1
            """)
    int markCanceled(@Param("orderId") long orderId);
}
