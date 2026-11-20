package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.CultivationRules;
import com.fenglin.springboottest.entity.AdminLog;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.BoardGameLog;
import com.fenglin.springboottest.entity.BoardItem;
import com.fenglin.springboottest.entity.BoardPvpMatch;
import com.fenglin.springboottest.entity.ForumLike;
import com.fenglin.springboottest.entity.ForumPost;
import com.fenglin.springboottest.entity.ForumReply;
import com.fenglin.springboottest.entity.SparLog;
import com.fenglin.springboottest.entity.UserCardView;
import com.fenglin.springboottest.entity.UserCultivation;
import com.fenglin.springboottest.entity.UserGongfa;
import com.fenglin.springboottest.entity.UserRegisterRecord;
import com.fenglin.springboottest.mapper.AdminLogMapper;
import com.fenglin.springboottest.mapper.BoardGameLogMapper;
import com.fenglin.springboottest.mapper.BoardItemMapper;
import com.fenglin.springboottest.mapper.BoardPvpMatchMapper;
import com.fenglin.springboottest.mapper.ForumLikeMapper;
import com.fenglin.springboottest.mapper.ForumPostMapper;
import com.fenglin.springboottest.mapper.ForumReplyMapper;
import com.fenglin.springboottest.mapper.SparLogMapper;
import com.fenglin.springboottest.mapper.UserCardViewMapper;
import com.fenglin.springboottest.mapper.UserCultivationMapper;
import com.fenglin.springboottest.mapper.UserGongfaMapper;
import com.fenglin.springboottest.mapper.UserRegisterRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 *   user_cultivation                                            管理员可编辑境界/神尊力/铜币/称号（单个或全部账号）
 *   user_cultivation / user_gongfa / spar_log / user_card_view   删除用户时按 user_id 级联清理
 *   board_game_log / board_item / board_pvp_match               删除用户时清理其桌游数据与对局
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
    @Autowired private UserCardViewMapper cardViewMapper;
    @Autowired private BoardGameLogMapper boardGameLogMapper;
    @Autowired private BoardItemMapper boardItemMapper;
    @Autowired private BoardPvpMatchMapper boardPvpMatchMapper;
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

    /* ==================== 修炼数据（管理员编辑，支持指定账号 / 全部账号） ==================== */

    /**
     * 后台修炼数据列表：全部账号一行一条，未开辟修炼的按默认值展示（不改变数据库）。
     */
    public List<Map<String, Object>> listCultivations() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (AvatarFoundation af : avatarService.list()) {
            UserCultivation uc = cultivationMapper.selectOne(
                    new QueryWrapper<UserCultivation>().eq("user_id", af.getId()));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("user_id", af.getId());
            m.put("deity_name", af.getDeityName());
            m.put("realm", uc == null ? "筑基" : uc.getRealm());
            m.put("power", uc == null || uc.getPower() == null ? 0 : uc.getPower());
            m.put("copper", uc == null || uc.getCopper() == null ? 0 : uc.getCopper());
            m.put("title", uc == null || uc.getTitle() == null ? "" : uc.getTitle());
            m.put("initialized", uc != null);
            out.add(m);
        }
        return out;
    }

    /**
     * 管理员编辑修炼数据：可指定单个账号，或一次性作用于全部账号。
     * <ul>
     *   <li>realm 必须在境界阶梯内；power/copper 可为 0 及以上整数；title 可显式覆盖</li>
     *   <li>只改 realm 而未显式给 title 时，按 {@link CultivationRules#REALM_TITLES} 自动授予对应最高称号</li>
     *   <li>账号尚无修炼行则按默认值新建；全部在同一事务内落库</li>
     * </ul>
     * @return 实际更新的账号数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateCultivation(Long targetId, boolean targetAll, Map<String, Object> item) {
        if (item == null || item.isEmpty()) {
            throw new IllegalArgumentException("缺少要修改的字段");
        }
        // 收集目标账号
        List<Long> ids;
        if (targetAll) {
            ids = avatarService.list().stream()
                    .map(AvatarFoundation::getId).filter(Objects::nonNull).toList();
        } else {
            if (targetId == null) throw new IllegalArgumentException("缺少 target_id");
            if (avatarService.getById(targetId) == null) throw new IllegalArgumentException("目标用户不存在");
            ids = List.of(targetId);
        }
        if (ids.isEmpty()) return 0;

        // 校验字段
        String realm = val(item, "realm");
        if (realm != null && !CultivationRules.LADDER.contains(realm)) {
            throw new IllegalArgumentException("境界必须为合法阶梯值");
        }
        Integer power = intVal(item, "power");
        Integer copper = intVal(item, "copper");
        if (power != null && power < 0) throw new IllegalArgumentException("神尊力不能为负");
        if (copper != null && copper < 0) throw new IllegalArgumentException("铜币不能为负");
        boolean hasTitle = item.containsKey("title");
        String title = hasTitle ? val(item, "title") : null;

        int n = 0;
        for (Long id : ids) {
            UserCultivation uc = cultivationMapper.selectOne(
                    new QueryWrapper<UserCultivation>().eq("user_id", id));
            if (uc == null) {
                uc = new UserCultivation();
                uc.setUserId(id);
                uc.setRealm("筑基");
                uc.setPower(0);
                uc.setCopper(CultivationRules.INIT_COPPER);
                uc.setSecretCount(0);
            }
            if (realm != null) {
                uc.setRealm(realm);
                if (!hasTitle) uc.setTitle(autoTitleOf(realm));
            }
            if (hasTitle) uc.setTitle(title);
            if (power != null) uc.setPower(power);
            if (copper != null) uc.setCopper(copper);
            uc.setUpdatedAt(LocalDateTime.now());
            if (uc.getId() == null) {
                cultivationMapper.insert(uc);
            } else {
                cultivationMapper.updateById(uc);
            }
            n++;
        }
        return n;
    }

    /** 按境界自动授予的最高称号（暗星→封侯，无间及以上→封王，其余→null） */
    private static String autoTitleOf(String realm) {
        int idx = CultivationRules.indexOf(realm);
        int hou = CultivationRules.indexOf("暗星");
        int wang = CultivationRules.indexOf("无间");
        if (wang >= 0 && idx >= wang) return CultivationRules.TITLE_FENGWANG;
        if (hou >= 0 && idx >= hou) return CultivationRules.TITLE_FENGHOU;
        return null;
    }

    /**
     * 批量编辑账号资料：一次性作用于全部账号。
     * 仅允许非身份字段（地域 / 权限 / 心魔誓约 / 简介），避免把账号名/密码/名号改成相同值造成冲突。
     * 每个账号同步回写注册留痕，全部在同一事务内落库。
     * @return 实际更新的账号数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateUsersBatch(Map<String, Object> item) {
        if (item == null || item.isEmpty()) throw new IllegalArgumentException("缺少要修改的字段");
        String region = item.containsKey("region") ? val(item, "region") : null;
        Integer permission = intVal(item, "permission");
        Integer agreed = intVal(item, "agreed");
        String story = item.containsKey("myth_story") ? val(item, "myth_story") : null;
        if (region == null && permission == null && agreed == null && story == null) {
            throw new IllegalArgumentException("批量修改仅支持：地域 / 权限 / 心魔誓约 / 神话简介");
        }
        if (permission != null && permission != 0 && permission != 1) {
            throw new IllegalArgumentException("权限仅允许 0（管理员）或 1（默认用户）");
        }

        int n = 0;
        for (AvatarFoundation af : avatarService.list()) {
            if (region != null) af.setRegion(region);
            if (permission != null) af.setPermission(permission);
            if (agreed != null) af.setAgreed(agreed);
            if (story != null) af.setMythStory(story);
            avatarService.updateById(af);

            UpdateWrapper<UserRegisterRecord> rw = new UpdateWrapper<>();
            rw.eq("avatar_id", af.getId()).or().eq("account", af.getDeityName());
            if (region != null) rw.set("region", region);
            if (permission != null) rw.set("permission", permission);
            if (agreed != null) rw.set("agreed", agreed);
            if (story != null) rw.set("myth_story", story);
            recordMapper.update(null, rw);
            n++;
        }
        return n;
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
        n += cardViewMapper.delete(new QueryWrapper<UserCardView>().eq("user_id", userId));
        n += sparLogMapper.delete(new QueryWrapper<SparLog>()
                .eq("initiator_id", userId).or().eq("target_id", userId));
        // 桌游：对局日志 / 道具背包 / PVP 对局（作为挑战者或应战者参与）一并清理
        n += boardGameLogMapper.delete(new QueryWrapper<BoardGameLog>().eq("user_id", userId));
        n += boardItemMapper.delete(new QueryWrapper<BoardItem>().eq("user_id", userId));
        n += boardPvpMatchMapper.delete(new QueryWrapper<BoardPvpMatch>()
                .eq("challenger_id", userId).or().eq("defender_id", userId));
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
