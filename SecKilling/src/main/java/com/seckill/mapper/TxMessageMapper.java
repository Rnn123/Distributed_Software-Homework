package com.seckill.mapper;

import com.seckill.entity.TxMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface TxMessageMapper {

    @Insert("""
            insert into tx_message(message_key, topic, biz_type, biz_key, payload, status, retry_count, next_retry_time)
            values(#{messageKey}, #{topic}, #{bizType}, #{bizKey}, #{payload}, #{status}, #{retryCount}, #{nextRetryTime})
            """)
    int insert(TxMessage message);

    @Select("""
            select * from tx_message
            where status = 0
              and next_retry_time <= #{currentTime}
            order by id asc
            limit #{limit}
            """)
    List<TxMessage> listPending(@Param("currentTime") Date currentTime, @Param("limit") int limit);

    @Update("""
            update tx_message
            set status = 1
            where id = #{id}
              and status = 0
            """)
    int markSent(@Param("id") long id);

    @Update("""
            update tx_message
            set retry_count = retry_count + 1,
                next_retry_time = #{nextRetryTime}
            where id = #{id}
            """)
    int markRetryLater(@Param("id") long id, @Param("nextRetryTime") Date nextRetryTime);
}
