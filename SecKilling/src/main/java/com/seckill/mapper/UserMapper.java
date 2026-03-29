package com.seckill.mapper;

import com.seckill.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("select * from `user` where id = #{id}")
    User getById(@Param("id") long id);

    @Select("select * from `user` where phone = #{phone}")
    User getByPhone(@Param("phone") String phone);

    @Select("select * from `user` where username = #{username}")
    User getByUsername(@Param("username") String username);

    @Insert("""
            insert into `user`(username, password, phone, email, create_time, update_time)
            values(#{username}, #{password}, #{phone}, #{email}, #{createTime}, #{updateTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
