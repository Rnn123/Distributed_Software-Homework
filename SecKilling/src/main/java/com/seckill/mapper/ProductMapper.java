package com.seckill.mapper;

import com.seckill.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("select * from product where id = #{id}")
    Product getById(@Param("id") long id);

    @Select("select * from product")
    List<Product> list();
}
