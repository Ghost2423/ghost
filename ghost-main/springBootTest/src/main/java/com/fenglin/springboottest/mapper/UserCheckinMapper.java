package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.UserCheckin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/** 用户每日签到 Mapper */
@Mapper
public interface UserCheckinMapper extends BaseMapper<UserCheckin> {

    /**
     * 原子领取当日签到：仅当该用户最近签到日期早于今天（或从未签到）时写入成功。
     * 返回受影响行数 1 = 领取成功，0 = 今天已签过（并发下也不会重复发奖）。
     */
    @Update("UPDATE user_checkin SET checkin_date = #{today}, streak = #{streak}, total = total + 1 " +
            "WHERE user_id = #{userId} AND (checkin_date IS NULL OR checkin_date < #{today})")
    int claim(@Param("userId") Long userId,
              @Param("today") LocalDate today,
              @Param("streak") int streak);
}
