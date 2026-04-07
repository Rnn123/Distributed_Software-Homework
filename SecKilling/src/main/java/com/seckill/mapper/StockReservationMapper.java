package com.seckill.mapper;

import com.seckill.entity.StockReservation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockReservationMapper {

    @Insert("""
            insert into stock_reservation(order_id, product_id, user_id, quantity, status)
            values(#{orderId}, #{productId}, #{userId}, #{quantity}, #{status})
            """)
    int insert(StockReservation reservation);

    @Select("select * from stock_reservation where order_id = #{orderId} limit 1")
    StockReservation getByOrderId(@Param("orderId") long orderId);

    @Update("""
            update stock_reservation
            set status = #{targetStatus}
            where order_id = #{orderId}
              and status = #{sourceStatus}
            """)
    int updateStatus(@Param("orderId") long orderId,
                     @Param("sourceStatus") int sourceStatus,
                     @Param("targetStatus") int targetStatus);
}
