package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.ForumLike;
import com.fenglin.springboottest.entity.ForumPost;
import com.fenglin.springboottest.entity.ForumReply;
import com.fenglin.springboottest.mapper.ForumLikeMapper;
import com.fenglin.springboottest.mapper.ForumPostMapper;
import com.fenglin.springboottest.mapper.ForumReplyMapper;
import com.fenglin.springboottest.service.ForumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 论道论坛业务实现：帖子 / 回帖 / 共鸣 三表协同，写操作全部走事务。
 */
@Service
public class ForumServiceImpl implements ForumService {

    private static final int STATUS_OK = 1;
    private static final int STATUS_HIDDEN = 0;
    private static final int MAX_TITLE = 120;
    private static final int MAX_CONTENT = 2000;

    @Autowired
    private ForumPostMapper postMapper;
    @Autowired
    private ForumReplyMapper replyMapper;
    @Autowired
    private ForumLikeMapper likeMapper;

    /** 论坛业务异常（由 GlobalExceptionHandler 统一转 400 + {ok:false,msg}） */
    public static class ForumException extends RuntimeException {
        public ForumException(String message) { super(message); }
    }

    /* ============================ 读 ============================ */

    @Override
    public Map<String, Object> listPosts(String category, String keyword, String sort,
                                         int page, int pageSize, String viewer) {
        int p = Math.max(1, page);
        int size = Math.min(50, Math.max(1, pageSize));
        long total = postMapper.selectCount(condition(category, keyword, true));

        QueryWrapper<ForumPost> w = condition(category, keyword, true);
        w.orderByDesc("is_pinned");
        if ("hot".equals(sort)) {
            w.orderByDesc("like_count").orderByDesc("reply_count").orderByDesc("view_count");
        } else {
            w.orderByDesc("COALESCE(last_reply_at, created_at)");
        }
        w.orderByDesc("id").last("LIMIT " + size + " OFFSET " + ((p - 1) * size));

        List<ForumPost> items = postMapper.selectList(w);
        markLiked(items, viewer);

        Map<String, Object> r = result();
        r.put("items", items);
        r.put("total", total);
        r.put("page", p);
        r.put("page_size", size);
        r.put("pages", (total + size - 1) / size);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> detail(long postId, String viewer) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != STATUS_OK) {
            throw new ForumException("未寻得此论题，或已被撤下");
        }
        // 阅卷数 +1（同一事务内完成）
        post.setViewCount(nvl(post.getViewCount()) + 1);
        postMapper.updateById(post);

        List<ForumPost> one = new ArrayList<>();
        one.add(post);
        markLiked(one, viewer);

        List<ForumReply> replies = replyMapper.selectList(
                new QueryWrapper<ForumReply>()
                        .eq("post_id", postId)
                        .eq("status", STATUS_OK)
                        .orderByAsc("floor_no")
                        .last("LIMIT 500"));

        Map<String, Object> r = result();
        r.put("post", post);
        r.put("replies", replies);
        return r;
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> r = result();
        r.put("posts", postMapper.selectCount(new QueryWrapper<ForumPost>().eq("status", STATUS_OK)));
        r.put("replies", replyMapper.selectCount(new QueryWrapper<ForumReply>().eq("status", STATUS_OK)));
        r.put("likes", likeMapper.selectCount(null));
        r.put("today", postMapper.selectCount(new QueryWrapper<ForumPost>()
                .eq("status", STATUS_OK)
                .ge("created_at", LocalDate.now().atStartOfDay())));

        Map<String, Long> counts = new LinkedHashMap<>();
        for (String c : CATEGORIES) {
            counts.put(c, postMapper.selectCount(new QueryWrapper<ForumPost>()
                    .eq("status", STATUS_OK).eq("category", c)));
        }
        r.put("category_counts", counts);
        r.put("hot", postMapper.selectList(new QueryWrapper<ForumPost>()
                .eq("status", STATUS_OK)
                .orderByDesc("like_count").orderByDesc("reply_count").orderByDesc("view_count")
                .last("LIMIT 5")));
        return r;
    }

    /* ============================ 写 ============================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumPost createPost(AvatarFoundation user, String category, String title, String content) {
        String cat = text(category);
        String t = text(title);
        String c = text(content);
        if (!CATEGORIES.contains(cat)) throw new ForumException("请选择论道板块");
        if (t.length() < 2 || t.length() > MAX_TITLE) throw new ForumException("论题需 2–" + MAX_TITLE + " 字");
        if (c.length() < 2 || c.length() > MAX_CONTENT) throw new ForumException("正文需 2–" + MAX_CONTENT + " 字");

        LocalDateTime now = LocalDateTime.now();
        ForumPost p = new ForumPost();
        p.setCategory(cat);
        p.setTitle(t);
        p.setContent(c);
        p.setAuthorName(user.getDeityName());
        p.setAuthorAlias(user.getMythName() == null || user.getMythName().isBlank()
                ? user.getDeityName() : user.getMythName());
        p.setViewCount(0);
        p.setLikeCount(0);
        p.setReplyCount(0);
        p.setIsPinned(0);
        p.setStatus(STATUS_OK);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        p.setLiked(false);
        postMapper.insert(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumReply createReply(AvatarFoundation user, long postId, String content) {
        String c = text(content);
        if (c.length() < 2 || c.length() > MAX_CONTENT) throw new ForumException("回帖需 2–" + MAX_CONTENT + " 字");

        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != STATUS_OK) {
            throw new ForumException("此论题已不存在，无法回帖");
        }

        // 楼层 = 当前最大楼层 + 1（含隐藏回帖，避免楼层重复）
        ForumReply last = replyMapper.selectOne(new QueryWrapper<ForumReply>()
                .eq("post_id", postId)
                .orderByDesc("floor_no")
                .last("LIMIT 1"));
        int floor = (last == null || last.getFloorNo() == null) ? 2 : last.getFloorNo() + 1;

        LocalDateTime now = LocalDateTime.now();
        ForumReply r = new ForumReply();
        r.setPostId(postId);
        r.setAuthorName(user.getDeityName());
        r.setAuthorAlias(user.getMythName() == null || user.getMythName().isBlank()
                ? user.getDeityName() : user.getMythName());
        r.setContent(c);
        r.setFloorNo(floor);
        r.setStatus(STATUS_OK);
        r.setCreatedAt(now);
        replyMapper.insert(r);

        post.setReplyCount(nvl(post.getReplyCount()) + 1);
        post.setLastReplyAt(now);
        post.setUpdatedAt(now);
        postMapper.updateById(post);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(AvatarFoundation user, long postId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != STATUS_OK) {
            throw new ForumException("此论题已不存在");
        }
        ForumLike exist = likeMapper.selectOne(new QueryWrapper<ForumLike>()
                .eq("post_id", postId).eq("user_name", user.getDeityName()).last("LIMIT 1"));

        boolean liked;
        if (exist != null) {
            likeMapper.deleteById(exist.getId());
            post.setLikeCount(Math.max(0, nvl(post.getLikeCount()) - 1));
            liked = false;
        } else {
            ForumLike l = new ForumLike();
            l.setPostId(postId);
            l.setUserName(user.getDeityName());
            l.setCreatedAt(LocalDateTime.now());
            likeMapper.insert(l);
            post.setLikeCount(nvl(post.getLikeCount()) + 1);
            liked = true;
        }
        postMapper.updateById(post);
        return liked;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean hidePost(AvatarFoundation user, long postId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null) return false;
        boolean admin = user.getPermission() != null && user.getPermission() == 0;
        if (!admin && !user.getDeityName().equals(post.getAuthorName())) {
            throw new ForumException("仅发帖者或管理员可撤下此论题");
        }
        post.setStatus(STATUS_HIDDEN);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        return true;
    }

    /* ============================ 后台 ============================ */

    @Override
    public Map<String, Object> adminList(String keyword, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.min(100, Math.max(1, pageSize));
        long total = postMapper.selectCount(condition(null, keyword, false));

        QueryWrapper<ForumPost> w = condition(null, keyword, false);
        w.orderByDesc("is_pinned").orderByDesc("id")
                .last("LIMIT " + size + " OFFSET " + ((p - 1) * size));
        List<ForumPost> items = postMapper.selectList(w);

        Map<String, Object> r = result();
        r.put("items", items);
        r.put("total", total);
        r.put("page", p);
        r.put("page_size", size);
        r.put("pages", (total + size - 1) / size);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminUpdate(long id, Integer pinned, Integer status) {
        ForumPost post = postMapper.selectById(id);
        if (post == null) return false;
        if (pinned != null) post.setIsPinned(pinned == 1 ? 1 : 0);
        if (status != null) post.setStatus(status == 1 ? 1 : 0);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminHideReply(long replyId) {
        ForumReply reply = replyMapper.selectById(replyId);
        if (reply == null || (reply.getStatus() != null && reply.getStatus() == STATUS_HIDDEN)) return false;
        reply.setStatus(STATUS_HIDDEN);
        replyMapper.updateById(reply);

        ForumPost post = postMapper.selectById(reply.getPostId());
        if (post != null) {
            post.setReplyCount(Math.max(0, nvl(post.getReplyCount()) - 1));
            post.setUpdatedAt(LocalDateTime.now());
            postMapper.updateById(post);
        }
        return true;
    }

    @Override
    public List<ForumReply> adminReplies(long postId) {
        return replyMapper.selectList(new QueryWrapper<ForumReply>()
                .eq("post_id", postId).orderByAsc("floor_no").last("LIMIT 500"));
    }

    /* ============================ 工具 ============================ */

    private static Map<String, Object> result() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        return m;
    }

    private QueryWrapper<ForumPost> condition(String category, String keyword, boolean onlyVisible) {
        QueryWrapper<ForumPost> w = new QueryWrapper<>();
        if (onlyVisible) w.eq("status", STATUS_OK);
        if (has(category) && !"全部".equals(category.trim())) {
            w.eq("category", category.trim());
        }
        if (has(keyword)) {
            String kw = keyword.trim();
            w.and(q -> q.like("title", kw)
                    .or().like("content", kw)
                    .or().like("author_alias", kw)
                    .or().like("author_name", kw));
        }
        return w;
    }

    /** 标记当前浏览者是否已共鸣（一次 in 查询，避免 N+1） */
    private void markLiked(List<ForumPost> items, String viewer) {
        for (ForumPost p : items) p.setLiked(false);
        if (!has(viewer) || items.isEmpty()) return;

        List<Long> ids = new ArrayList<>();
        for (ForumPost p : items) ids.add(p.getId());
        List<ForumLike> likes = likeMapper.selectList(new QueryWrapper<ForumLike>()
                .eq("user_name", viewer.trim()).in("post_id", ids));
        Set<Long> liked = new HashSet<>();
        for (ForumLike l : likes) liked.add(l.getPostId());
        for (ForumPost p : items) p.setLiked(liked.contains(p.getId()));
    }

    private static boolean has(String s) { return s != null && !s.trim().isEmpty(); }
    private static String text(String s) { return s == null ? "" : s.trim(); }
    private static int nvl(Integer v) { return v == null ? 0 : v; }
}
