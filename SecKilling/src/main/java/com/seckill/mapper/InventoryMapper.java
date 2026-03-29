package com.seckill.mapper;

import com.seckill.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface InventoryMapper {

    @Select("select * from inventory where product_id = #{productId}")
    Inventory getByProductId(@Param("productId") long productId);

    @Select("select * from inventory order by product_id")
    List<Inventory> listAll();

    @Update("""
            update inventory
            set available_stock = available_stock - 1,
                frozen_stock = frozen_stock + 1,
                version = version + 1
            where product_id = #{productId}
              and available_stock > 0
            """)
    int deductStock(@Param("productId") long productId);
}
