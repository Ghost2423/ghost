-- =============================================================
-- 旧库升级脚本：用户表新增权限字段
-- 权限定义：1 = 默认用户（注册默认），0 = 管理员（可登录后台管理系统）
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql\upgrade_permission.sql
-- 说明：若 cangyuan.sql 已全新建库（含 permission 列），无需执行本脚本。
-- =============================================================

USE cangyuan;

-- 1) 新增权限列（默认 1 = 默认用户）
ALTER TABLE avatar_foundation
  ADD COLUMN permission TINYINT(1) NOT NULL DEFAULT 1 COMMENT '权限：1=默认用户，0=管理员' AFTER agreed;

-- 2) 内置管理员账号（账号 沧元祖师，密码 admin123，明文存储，permission=0）
INSERT IGNORE INTO avatar_foundation (region, deity_name, myth_name, spell, myth_story, agreed, permission) VALUES
('天枢殿', '沧元祖师', '元界天帝', 'admin123', '元界天帝：执掌天枢，统御元界后台。', 1, 0);

-- 3) 如需把某个已有用户提升为管理员，执行（替换 deity_name 为该用户账号）：
-- UPDATE avatar_foundation SET permission = 0 WHERE deity_name = '你的账号';

-- 4) 校验
SELECT deity_name, spell, permission FROM avatar_foundation ORDER BY id;
