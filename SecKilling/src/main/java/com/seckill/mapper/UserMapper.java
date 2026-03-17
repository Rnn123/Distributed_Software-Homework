package com.seckill.mapper;

import com.seckill.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("select * from user where id = #{id}")
    public User getById(@Param("id") long id);

    @Select("select * from user where phone = #{phone}")
    public User getByPhone(@Param("phone") String phone);

    @Insert("insert into user(username, password, phone, create_time) values(#{username}, #{password}, #{phone}, #{createTime})")
    public int insert(User user);
}
