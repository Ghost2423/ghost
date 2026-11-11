# ghost
用于学习记录Java


## 一键启动

项目根目录：`D:\workSpace\ghost\ghost-main`（Git 仓库根在上一级 `D:\workSpace\ghost`，索引路径带 `ghost-main/` 前缀；同步脚本在项目根运行即可）。双击运行根目录的 **`一键启动.bat`**，即可自动拉起后端（Spring Boot :8125）与前端（Vue3 + Vite :5173），并自动打开 http://localhost:5173 （后端 http://localhost:8125）。

- 首次运行会自动完成 Maven 打包与 npm 依赖安装（约 1-2 分钟），并按序执行 4 个幂等建表脚本：`cangyuan.sql`、`cultivation.sql`、`admin_home.sql`、`register_account.sql`
- 前后端均绑定 `0.0.0.0`：`localhost` 与局域网实际 IP 都可访问，换 IP 不会导致启动失败；**不要在配置里写死局域网 IP**
- 改过 Java 代码后需重新打包：运行 `springBootTest\start-backend.bat rebuild`（或删掉 `target` 下 jar 再启动）
- 停止服务：双击 **`一键停止.bat`**；单服务可用 `springBootTest\stop-backend.bat`、`cangyuantu-vue\stop-frontend.bat`
- 环境要求：JDK 17+、Node.js 16+、MySQL 8（root/123456，库 `cangyuan`）
- 更完整的启动说明与踩坑记录见 `springBootTest\AI-启动指南.md`（前端目录有同步副本）

目录：`springBootTest/`（后端）、`cangyuantu-vue/`（前端）、`springBootTest/sql/`（建表脚本，均幂等）

## 后台管理系统

入口：首页导航栏右侧 **「⚙ 后台管理」** → 后台登录页（**仅登录，不开放注册**）。

- 入口按钮**只对 `permission=0` 的管理员渲染**；默认用户（`permission=1`）与游客看不到按钮，直接调接口也会被 403 拒绝
- 默认管理员：`沧元祖师 / admin123`；权限由管理员在「用户管理」中授予或收回，即时生效
- 注册（`/api/forge`）写入的用户一律为 `permission=1`，请求体里传 `permission=0` 也会被服务端强制改回 1
- 账号密码存放在 `avatar_foundation.spell`，按需求**明文存储、不做加密**；但公开查询接口 `/api/avatar` 不下发该字段，仅 `/api/login` 内部比对
- **每次注册都会留痕**：注册成功时在同一事务里写 `avatar_foundation` + `user_register_record`（账号、密码、地域、神尊名号、权限快照、来源 IP、注册时间）。任一步失败整个事务回滚，前端不会显示「根基已成」
- `user_register_record` 是独立的注册历史表：后台删除用户、改权限或改名都不会抹掉这条记录；建表前的旧账号由脚本自动回填（`source=migrate`）
- 查看：后台「注册记录」页签，或 `SELECT * FROM cangyuan.user_register_record ORDER BY id DESC;`
- 建表脚本：`springBootTest/sql/register_account.sql`（幂等，一键启动时自动执行）
- 登录后进入后台控制台，管理首页全部展示数据：

| 模块 | 作用 | 数据表 |
| --- | --- | --- |
| 数据概览 | 图鉴/用户/动态统计与最近操作 | — |
| 图鉴管理 | 神尊/功法/妖魔/秘境条目增删改、上下架、排序、分页、多选批量上下架 | `cy_codex` |
| 核心界域 | 首页四个展示框的标题/图标/主色/简介/显隐，编辑弹窗内右侧**实时预览卡片效果** | `cy_home_section` |
| 元界动态 | 首页滚动动态条 | `cy_home_news` |
| 站点文案 | 品牌名/首屏标题/检索占位/热门标签/页脚，左侧编辑右侧**实时预览首屏** | `cy_site_setting` |
| 用户管理 | 用户列表、权限变更、删除 | `avatar_foundation` |
| 注册记录 | 每次注册账号的独立留痕（含密码、IP、时间），删用户仍保留 | `user_register_record` |
| 操作日志 | 后台操作审计 | `cy_admin_log` |

后台相关建表脚本：`springBootTest/sql/admin_home.sql`；注册留痕表：`springBootTest/sql/register_account.sql`（均幂等，一键启动与 `start-backend.bat` 都会自动执行）。
首页配置接口：`GET /api/home/config`；后台接口：`POST /api/admin/*`（均校验管理员凭证），注册记录查询为 `POST /api/admin/register/records`。

### 图鉴分页与批量上下架

- 列表接口支持分页与筛选：`page`、`page_size`（10/20/50/100，上限 100）、`category`、`keyword`、`status`（1=上架 / 0=下架 / 不传=全部）
  返回 `items / total / page / page_size / pages / counts（上架数） / counts_all（总数）`
- 页码越界会自动纠正到最后一页（例如删除末页最后一条后不会停在空页）
- 批量上下架：`POST /api/admin/codex/status/batch { name, spell, ids:[...], status }`，单次上限 200 条，写入一条「批量上架/下架」操作日志
- 前端：分类页签角标显示各分类总数；表头复选框「全选本页」；选中后出现批量操作条；切换分类/搜索/筛选会清空选中，翻页则保留

### 首页实时预览对照

改文案/改卡片不用再「保存 → 回首页刷新 → 再回来」：

- **站点文案**：表单左侧编辑，右侧吸附（sticky）一块「首页首屏」示意，徽标 / 主标题（鎏金渐变字）/ 副标题 / 检索占位 / 热门标签 / 品牌印章 / 页脚随输入实时变化；留空字段显示首页实际会用的兜底文案，清空热门标签会提示「首页该行隐藏」
- **核心界域**：编辑弹窗改为左右两栏，右侧按首页卡片样式等比渲染标题、图标、主色（连左边色条与光晕一起）、简介与「收录 N」；状态选「隐藏」时卡片上出现「已隐藏 · 首页不显示」角标
- 预览纯前端渲染，**不写库**：点「取消」或「重新读取」即回到数据库原值
