package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.BoardPvpMatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 太虚弈局 PVP 对战 Mapper */
@Mapper
public interface BoardPvpMatchMapper extends BaseMapper<BoardPvpMatch> {

    /** PVP 名将榜：按胜场排序（合并挑战者 / 应战者两个视角） */
    @Select("SELECT name, SUM(plays) AS plays, SUM(wins) AS wins FROM (" +
            "  SELECT a.deity_name AS name, COUNT(*) AS plays, " +
            "         SUM(CASE WHEN m.winner='challenger' THEN 1 ELSE 0 END) AS wins " +
            "  FROM board_pvp_match m JOIN avatar_foundation a ON a.id=m.challenger_id " +
            "  WHERE m.status='finished' GROUP BY m.challenger_id, a.deity_name " +
            "  UNION ALL " +
            "  SELECT a.deity_name AS name, COUNT(*) AS plays, " +
            "         SUM(CASE WHEN m.winner='defender' THEN 1 ELSE 0 END) AS wins " +
            "  FROM board_pvp_match m JOIN avatar_foundation a ON a.id=m.defender_id " +
            "  WHERE m.status='finished' AND m.defender_id IS NOT NULL " +
            "  GROUP BY m.defender_id, a.deity_name " +
            ") t GROUP BY name ORDER BY wins DESC, plays DESC LIMIT 10")
    List<Map<String, Object>> rankPvp();
}
