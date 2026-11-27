package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.UserRegisterRecord;
import com.fenglin.springboottest.mapper.UserRegisterRecordMapper;
import com.fenglin.springboottest.service.AvatarFoundationService;
import com.fenglin.springboottest.service.CultivationService;
import com.fenglin.springboottest.service.RegisterRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegisterRecordServiceImpl implements RegisterRecordService {

    @Autowired
    private AvatarFoundationService avatarService;

    @Autowired
    private UserRegisterRecordMapper recordMapper;

    @Autowired
    private CultivationService cultivationService;

    /**
     * 一次注册 = 神尊根基表一条 + 注册账号表一条。
     * 任一步失败整个事务回滚，前端会收到失败响应，不会误以为账号已保存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRegistration(AvatarFoundation af, String clientIp) {
        avatarService.save(af); // MyBatis-Plus 回填自增 id

        UserRegisterRecord r = new UserRegisterRecord();
        r.setAvatarId(af.getId());
        r.setAccount(af.getDeityName());
        r.setPassword(af.getSpell());
        r.setRegion(af.getRegion());
        r.setMythName(af.getMythName());
        r.setMythStory(af.getMythStory());
        r.setAgreed(af.getAgreed());
        r.setPermission(af.getPermission());
        r.setRegisterIp(clientIp);
        r.setSource("forge");
        recordMapper.insert(r);

        // 注册即落库修炼根基：新账号从诞生起就有持久化的修炼进度（筑基/0/200），
        // 幂等实现，若已存在绝不覆盖；避免进主页后才惰性创建、看起来像「数据被重置」。
        cultivationService.ensureInitialized(af.getId());
    }

    @Override
    public List<UserRegisterRecord> listRecent(int limit) {
        int size = Math.max(1, Math.min(limit, 1000));
        return recordMapper.selectList(new QueryWrapper<UserRegisterRecord>()
                .orderByDesc("id")
                .last("LIMIT " + size));
    }
}
