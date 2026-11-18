package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.BoardGameLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 太虚弈局对局记录 Mapper */
@Mapper
public interface BoardGameLogMapper extends BaseMapper<BoardGameLog> {

    /** 登顶榜：按通关次数 -> 累计铜币 排序 */
    @Select("SELECT a.deity_name AS name, COUNT(*) AS plays, " +
            "SUM(CASE WHEN g.pass = 1 THEN 1 ELSE 0 END) AS wins, " +
            "SUM(g.gain_copper) AS copper, SUM(g.gain_power) AS power " +
            "FROM board_game_log g JOIN avatar_foundation a ON a.id = g.user_id " +
            "WHERE g.status = 'finished' " +
            "GROUP BY g.user_id, a.deity_name " +
            "ORDER BY wins DESC, copper DESC LIMIT 10")
    List<Map<String, Object>> rankByWins();

    /** 财富榜：按累计获得铜币排序 */
    @Select("SELECT a.deity_name AS name, COUNT(*) AS plays, " +
            "SUM(g.gain_copper) AS copper, SUM(g.gain_power) AS power " +
            "FROM board_game_log g JOIN avatar_foundation a ON a.id = g.user_id " +
            "WHERE g.status = 'finished' " +
            "GROUP BY g.user_id, a.deity_name " +
            "ORDER BY copper DESC LIMIT 10")
    List<Map<String, Object>> rankByCopper();
}
