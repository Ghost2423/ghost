package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.ForumPost;
import com.fenglin.springboottest.entity.ForumReply;

import java.util.List;
import java.util.Map;

/**
 * 沧元界 · 论道论坛业务接口
 */
public interface ForumService {

    /** 板块常量（前端与后台共用） */
    List<String> CATEGORIES = List.of("论道", "切磋", "妖情", "闲谈");

    /** 帖子列表：分类 / 关键词 / 排序 / 分页，viewer 用于标记是否已共鸣 */
    Map<String, Object> listPosts(String category, String keyword, String sort,
                                  int page, int pageSize, String viewer);

    /** 帖子详情：主帖 + 回帖列表，同时累加阅卷数 */
    Map<String, Object> detail(long postId, String viewer);

    ForumPost createPost(AvatarFoundation user, String category, String title, String content);

    ForumReply createReply(AvatarFoundation user, long postId, String content);

    /** 共鸣（点赞）切换：返回切换后是否已点赞 */
    boolean toggleLike(AvatarFoundation user, long postId);

    /** 作者本人或管理员隐藏帖子（软删除，保留数据） */
    boolean hidePost(AvatarFoundation user, long postId);

    /** 论坛总览统计 */
    Map<String, Object> stats();

    /** 后台：含隐藏帖的分页列表 */
    Map<String, Object> adminList(String keyword, int page, int pageSize);

    /** 后台：置顶 / 恢复 / 屏蔽 */
    boolean adminUpdate(long id, Integer pinned, Integer status);

    /** 后台：隐藏回帖 */
    boolean adminHideReply(long replyId);

    /** 后台：查看某帖全部回帖（含隐藏） */
    List<ForumReply> adminReplies(long postId);
}
