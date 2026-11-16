package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.entity.AdminLog;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.ForumLike;
import com.fenglin.springboottest.entity.ForumPost;
import com.fenglin.springboottest.entity.ForumReply;
import com.fenglin.springboottest.entity.SparLog;
import com.fenglin.springboottest.entity.UserCultivation;
import com.fenglin.springboottest.entity.UserGongfa;
import com.fenglin.springboottest.entity.UserRegisterRecord;
import com.fenglin.springboottest.mapper.AdminLogMapper;
import com.fenglin.springboottest.mapper.ForumLikeMapper;
import com.fenglin.springboottest.mapper.ForumPostMapper;
import com.fenglin.springboottest.mapper.ForumReplyMapper;
import com.fenglin.springboottest.mapper.SparLogMapper;
import com.fenglin.springboottest.mapper.UserCultivationMapper;
import com.fenglin.springboottest.mapper.UserGongfaMapper;
import com.fenglin.springboottest.mapper.UserRegisterRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 沧元图 · 后台数据编辑与「一处改动、全库同步」
 *
 * 后台管理系统里对同一份数据的所有副本统一在这里维护，避免改了主表、
 * 数据库里其它表还留着旧值：
 *
 *   avatar_foundation          用户主表
 *   user_register_record       注册留痕（账号 / 密码 / 地域 / 名号 / 权限）
 *   cy_forum_post / reply      论坛作者账号 author_name、展示名号 author_alias
 *   cy_forum_like              共鸣账号 user_name（带 (post_id,user_name) 唯一键）
 *   cy_admin_log               操作日志里的管理员账号
 *   user_cultivation / user_gongfa / spar_log   删除用户时按 user_id 级联清理
 *
 * 所有写方法都在同一个事务里，任一步失败整体回滚，不会出现主表改了、关联表没改。
 */
@Service
public class AdminDataSyncService {

    @Autowired private AvatarFoundationService avatarService;
    @Autowired private UserRegisterRecordMapper recordMapper;
    @Autowired private ForumPostMapper postMapper;
    @Autowired private ForumReplyMapper replyMapper;
    @Autowired private ForumLikeMapper likeMapper;
    @Autowired private AdminLogMapper logMapper;
    @Autowired private UserCultivationMapper cultivationMapper;
    @Autowired private UserGongfaMapper gongfaMapper;
    @Autowired private SparLogMapper sparLogMapper;
    @Autowired private HomeContentService contentService;

    /* ==================== 用户资料 ==================== */

    /**
     * 编辑用户资料，并把新值同步到注册记录、论坛、共鸣、操作日志等所有关联表。
     * @return 更新后的用户实体
     */
    @Transactional(rollbackFor = Exception.class)
    public AvatarFoundation updateUser(Long targetId, Map<String, Object> item) {
        AvatarFoundation af = avatarService.getById(targetId);
        if (af == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }
        String oldName = af.getDeityName();
        String oldMyth = af.getMythName();

        String deityName = val(item, "deity_name");
        if (deityName != null) {
            if (deityName.isEmpty()) {
                throw new IllegalArgumentException("账号不能为空");
            }
            AvatarFoundation dup = avatarService.getByDeityName(deityName);
            if (dup != null && !Objects.equals(dup.getId(), targetId)) {
                throw new IllegalArgumentException("账号「" + deityName + "」已被占用");
            }
            af.setDeityName(deityName);
        }
        String spell = val(item, "spell");
        if (spell != null) {
            if (spell.isEmpty()) {
                throw new IllegalArgumentException("密码（召唤语）不能为空");
            }
            af.setSpell(spell);
        }
        if (item.containsKey("region")) af.setRegion(val(item, "region"));
        if (item.containsKey("myth_name")) af.setMythName(val(item, "myth_name"));
        if (item.containsKey("myth_story")) af.setMythStory(val(item, "myth_story"));
        Integer permission = intVal(item, "permission");
        if (permission != null) {
            if (permission != 0 && permission != 1) {
                throw new IllegalArgumentException("权限仅允许 0（管理员）或 1（默认用户）");
            }
            af.setPermission(permission);
        }
        Integer agreed = intVal(item, "agreed");
        if (agreed != null) af.setAgreed(agreed);
        avatarService.updateById(af);

        // 注册留痕：按 avatar_id 或旧账号定位，同步为当前资料
        UpdateWrapper<UserRegisterRecord> rw = new UpdateWrapper<>();
        rw.eq("avatar_id", targetId).or().eq("account", oldName);
        rw.set("account", af.getDeityName())
          .set("password", af.getSpell())
          .set("region", af.getRegion())
          .set("myth_name", af.getMythName())
          .set("myth_story", af.getMythStory())
          .set("agreed", af.getAgreed())
          .set("permission", af.getPermission());
        recordMapper.update(null, rw);

        // 账号改名 → 所有以账号为标识的表同步
        if (!Objects.equals(oldName, af.getDeityName())) {
            syncAccountName(oldName, af.getDeityName());
        }
        // 神尊名号变动 → 论坛里沿用旧名号作为展示名的同步更新
        if (!Objects.equals(oldMyth, af.getMythName())
                && oldMyth != null && af.getMythName() != null) {
            postMapper.update(null, new UpdateWrapper<ForumPost>()
                    .eq("author_name", af.getDeityName()).eq("author_alias", oldMyth)
                    .set("author_alias", af.getMythName()));
            replyMapper.update(null, new UpdateWrapper<ForumReply>()
                    .eq("author_name", af.getDeityName()).eq("author_alias", oldMyth)
                    .set("author_alias", af.getMythName()));
        }
        return af;
    }

    /** 权限按钮单独调用：同步注册留痕里的权限字段 */
    @Transactional(rollbackFor = Exception.class)
    public void syncPermission(Long userId, String account, Integer permission) {
        UpdateWrapper<UserRegisterRecord> rw = new UpdateWrapper<>();
        rw.eq("avatar_id", userId).or().eq("account", account);
        rw.set("permission", permission);
        recordMapper.update(null, rw);
    }

    /**
     * 删除用户并级联清理数据库里的相应内容。
     * 论坛帖子/回帖不删除（保留内容资产），作者账号统一改为「已注销神尊」；
     * 注册留痕按设计保留，仅不再指向有效用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserCascade(Long userId, String account) {
        int n = 0;
        n += cultivationMapper.delete(new QueryWrapper<UserCultivation>().eq("user_id", userId));
        n += gongfaMapper.delete(new QueryWrapper<UserGongfa>().eq("user_id", userId));
        n += sparLogMapper.delete(new QueryWrapper<SparLog>()
                .eq("initiator_id", userId).or().eq("target_id", userId));
        if (account != null && !account.isBlank()) {
            n += likeMapper.delete(new QueryWrapper<ForumLike>().eq("user_name", account));
            n += postMapper.update(null, new UpdateWrapper<ForumPost>()
                    .eq("author_name", account).set("author_name", "已注销神尊"));
            n += replyMapper.update(null, new UpdateWrapper<ForumReply>()
                    .eq("author_name", account).set("author_name", "已注销神尊"));
        }
        avatarService.removeById(userId);
        return n;
    }

    /* ==================== 注册记录 ==================== */

    /**
     * 编辑一条注册留痕；若该记录仍关联现存用户，则同时把改动写回 avatar_foundation，
     * 并同步论坛作者账号，保证「后台改的」和「数据库存的」始终一致。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserRegisterRecord updateRegisterRecord(Long recordId, Map<String, Object> item) {
        UserRegisterRecord r = recordMapper.selectById(recordId);
        if (r == null) {
            throw new IllegalArgumentException("注册记录不存在");
        }
        String oldAccount = r.getAccount();

        String account = val(item, "account");
        if (account != null) {
            if (account.isEmpty()) throw new IllegalArgumentException("账号不能为空");
            r.setAccount(account);
        }
        String password = val(item, "password");
        if (password != null) {
            if (password.isEmpty()) throw new IllegalArgumentException("密码不能为空");
            r.setPassword(password);
        }
        if (item.containsKey("region")) r.setRegion(val(item, "region"));
        if (item.containsKey("myth_name")) r.setMythName(val(item, "myth_name"));
        if (item.containsKey("myth_story")) r.setMythStory(val(item, "myth_story"));
        Integer permission = intVal(item, "permission");
        if (permission != null) {
            if (permission != 0 && permission != 1) throw new IllegalArgumentException("权限仅允许 0 或 1");
            r.setPermission(permission);
        }
        Integer agreed = intVal(item, "agreed");
        if (agreed != null) r.setAgreed(agreed);
        recordMapper.updateById(r);

        // 同步现存用户主表
        if (r.getAvatarId() != null) {
            AvatarFoundation af = avatarService.getById(r.getAvatarId());
            if (af != null && Objects.equals(af.getDeityName(), oldAccount)) {
                AvatarFoundation dup = avatarService.getByDeityName(r.getAccount());
                if (dup != null && !Objects.equals(dup.getId(), af.getId())) {
                    throw new IllegalArgumentException("账号「" + r.getAccount() + "」已被其它用户占用");
                }
                af.setDeityName(r.getAccount());
                af.setSpell(r.getPassword());
                af.setRegion(r.getRegion());
                af.setMythName(r.getMythName());
                af.setMythStory(r.getMythStory());
                af.setAgreed(r.getAgreed());
                af.setPermission(r.getPermission());
                avatarService.updateById(af);
            }
        }
        // 账号改名 → 论坛 / 共鸣 / 日志同步
        if (!Objects.equals(oldAccount, r.getAccount())) {
            syncAccountName(oldAccount, r.getAccount());
        }
        return r;
    }

    /* ==================== 论道内容 ==================== */

    /** 后台编辑论题：板块 / 标题 / 正文 */
    @Transactional(rollbackFor = Exception.class)
    public ForumPost updateForumPost(Long postId, Map<String, Object> item) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) throw new IllegalArgumentException("论题不存在");
        String category = val(item, "category");
        if (category != null && !category.isEmpty()) {
            if (!ForumService.CATEGORIES.contains(category)) {
                throw new IllegalArgumentException("板块仅允许：论道 / 切磋 / 妖情 / 闲谈");
            }
            p.setCategory(category);
        }
        String title = val(item, "title");
        if (title != null) {
            if (title.isEmpty()) throw new IllegalArgumentException("论题标题不能为空");
            p.setTitle(title);
        }
        String content = val(item, "content");
        if (content != null) {
            if (content.isEmpty()) throw new IllegalArgumentException("论题正文不能为空");
            p.setContent(content);
        }
        p.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(p);
        return p;
    }

    /** 后台编辑回帖正文 */
    @Transactional(rollbackFor = Exception.class)
    public ForumReply updateForumReply(Long replyId, Map<String, Object> item) {
        ForumReply r = replyMapper.selectById(replyId);
        if (r == null) throw new IllegalArgumentException("回帖不存在");
        String content = val(item, "content");
        if (content != null) {
            if (content.isEmpty()) throw new IllegalArgumentException("回帖正文不能为空");
            r.setContent(content);
            replyMapper.updateById(r);
        }
        return r;
    }

    /* ==================== 公共同步 ==================== */

    /**
     * 账号改名：同步所有以账号为标识的表。
     * cy_forum_like 有 (post_id, user_name) 唯一键，改名前先清掉新账号在
     * 同一批帖子上的重复共鸣，避免 DuplicateKeyException 导致整个事务回滚。
     */
    private void syncAccountName(String oldName, String newName) {
        if (oldName == null || newName == null || oldName.equals(newName) || oldName.isBlank()) {
            return;
        }
        List<Long> postIds = likeMapper.selectList(
                        new QueryWrapper<ForumLike>().eq("user_name", oldName))
                .stream().map(ForumLike::getPostId).filter(Objects::nonNull).distinct().toList();
        if (!postIds.isEmpty()) {
            likeMapper.delete(new QueryWrapper<ForumLike>()
                    .eq("user_name", newName).in("post_id", postIds));
        }
        likeMapper.update(null, new UpdateWrapper<ForumLike>()
                .eq("user_name", oldName).set("user_name", newName));
        postMapper.update(null, new UpdateWrapper<ForumPost>()
                .eq("author_name", oldName).set("author_name", newName));
        replyMapper.update(null, new UpdateWrapper<ForumReply>()
                .eq("author_name", oldName).set("author_name", newName));
        logMapper.update(null, new UpdateWrapper<AdminLog>()
                .eq("admin_name", oldName).set("admin_name", newName));
    }

    /** 写一条后台操作日志（复用内容服务的统一实现） */
    public void log(AvatarFoundation admin, String action, String targetType,
                    String targetId, String detail) {
        contentService.log(admin.getId(), admin.getDeityName(), action, targetType, targetId, detail);
    }

    /* ==================== 工具 ==================== */

    private static String val(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object o = m.get(key);
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? "" : s;
    }

    private static Integer intVal(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object o = m.get(key);
        if (o == null || String.valueOf(o).isBlank()) return null;
        try {
            return Integer.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " 必须是数字");
        }
    }
}
