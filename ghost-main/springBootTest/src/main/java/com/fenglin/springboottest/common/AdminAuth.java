package com.fenglin.springboottest.common;

import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.service.AvatarFoundationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 后台管理系统 · 管理员凭证校验
 *
 * 权限规则：avatar_foundation.permission —— 1=默认用户（注册默认），0=管理员。
 * 仅 permission=0 的用户可登录后台并调用管理接口。
 */
@Component
public class AdminAuth {

    /** 默认用户权限 */
    public static final int PERM_USER = 1;
    /** 管理员权限 */
    public static final int PERM_ADMIN = 0;

    @Autowired
    private AvatarFoundationService avatarService;

    /**
     * 校验管理员凭证（账号存在 + 密码明文一致 + 权限为 0）。
     *
     * @return 校验通过返回管理员实体，否则 null
     */
    public AvatarFoundation requireAdmin(String name, String spell) {
        if (name == null || name.isBlank() || spell == null) {
            return null;
        }
        AvatarFoundation af = avatarService.getByDeityName(name.trim());
        if (af == null || !spell.equals(af.getSpell())) {
            return null;
        }
        if (af.getPermission() == null || af.getPermission() != PERM_ADMIN) {
            return null;
        }
        return af;
    }
}
