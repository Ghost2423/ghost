package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.AvatarFoundation;
import com.fenglin.springboottest.entity.UserRegisterRecord;

import java.util.List;

/**
 * 注册账号记录 Service
 * 保证「注册成功」与「账号留痕」在同一个事务里完成，绝不会出现只写了一张表的情况。
 */
public interface RegisterRecordService {

    /** 注册：写入神尊根基 + 独立注册账号表（同一事务） */
    void saveRegistration(AvatarFoundation af, String clientIp);

    /** 最近的注册记录（按注册时间倒序） */
    List<UserRegisterRecord> listRecent(int limit);
}
