# AI 快速启动指南（沧元图 / cangyuantu-vue）

> **给 AI 的说明**：用户说「启动项目」时，直接照本文件执行，**不要重新探索项目结构、不要重新设计方案**。
> 唯一工作副本：`D:\workSpace\ghost\ghost-main`。旧的 `C:\Users\Administrator\Desktop\ghost-main` 与
> `D:\workSpace\springBootTest`（不带 ghost）路径**已失效**，按旧路径找脚本是启动失败的首要原因。

## 一、启动（一键，首选）

```
双击运行  D:\workSpace\ghost\ghost-main\一键启动.bat
```

脚本顺序：环境检查 → MySQL 检测/启动 → 执行 4 个建表脚本（幂等）→ 后端（缺 jar 时自动 mvn 打包）:8125 → 前端（缺 node_modules 时自动 npm install）:5173 → 自动打开浏览器。
停止：双击 `D:\workSpace\ghost\ghost-main\一键停止.bat`。

浏览器访问：**http://localhost:5173**（后端 **http://localhost:8125**）。
后端与前端均绑定 `0.0.0.0`，局域网内其他设备用本机实际 IP 加端口也能访问；**不要把 IP 写死**（DHCP 换 IP 会导致 bind 失败）。

备用分步脚本（仅一键脚本异常时用）：
```
D:\workSpace\ghost\ghost-main\springBootTest\start-backend.bat            # 后端
D:\workSpace\ghost\ghost-main\springBootTest\start-backend.bat rebuild    # 改过 Java 代码后强制重打包再启动
D:\workSpace\ghost\ghost-main\cangyuantu-vue\start-frontend.bat           # 前端
```
对应 stop-backend.bat / stop-frontend.bat 停止单个服务。

## 二、项目全景

| 部分 | 位置 | 说明 |
|---|---|---|
| **项目根**（所有脚本/源码） | `D:\workSpace\ghost\ghost-main` | 一键启动.bat、README、前后端目录都在这里 |
| **Git 仓库根** | `D:\workSpace\ghost` | `.git` 在**上一级**；索引路径都带 `ghost-main/` 前缀，分支 main，remote Ghost2423/ghost。别在父目录执行 `git init` 或误删 .git |
| **前端** | `...\ghost-main\cangyuantu-vue` | Vue3 + Vite 5，端口 **5173**，`/api` 代理到 `http://127.0.0.1:8125` |
| **后端** | `...\ghost-main\springBootTest` | Spring Boot 3.4.5 (Java 21) + MyBatis-Plus 3.5.7，端口 **8125** |
| **数据库** | MySQL 8 | 库 `cangyuan`，root/123456，127.0.0.1:3306 |
| **SQL** | `springBootTest\sql\` | 一键启动按序执行：`cangyuan.sql` → `cultivation.sql` → `admin_home.sql` → `register_account.sql`（均幂等）；另有 `cangyuan_codex.sql`、`upgrade_*.sql` |
| 根目录 `index.html` | 历史单文件静态页 | 已废弃，勿动 |

后端包结构：`com.fenglin.springboottest` 下 entity / mapper / service / controller / dto / common / config。

## 三、API 契约（JSON 统一 `{ok, msg}`，字段 snake_case）

| 接口 | 说明 |
|---|---|
| `POST /api/login` `{name, spell}` | 登录，服务端明文比对召唤语；成功 `{ok, avatar}` |
| `POST /api/forge` `{region, deity_name, myth_name, spell, myth_story, agreed}` | 注册：**同一事务**写 `avatar_foundation` + `user_register_record`，成功 `{ok, id, register_record:true}`；重复 409 |
| `GET /api/avatar?name=神尊相` | 查根基，不下发 spell；404 未寻得 |
| `POST /api/admin/register/records` `{name, spell}` | 仅管理员，返回最近 500 条注册记录，字段 snake_case |
| `POST /api/admin/*` | 后台全部接口均校验管理员凭证 |

测试账号：`九天应元雷声普化天尊` / `诸般雷霆，遵吾号令`。
后台管理员：`沧元祖师` / `admin123`（`permission=0`）。

## 四、数据表

- `avatar_foundation`：`id` / `region` / `deity_name`(唯一，账号) / `myth_name` / `spell`(明文密码) / `myth_story` / `agreed` / `permission`(1 用户, 0 管理员) / `created_at`
- `user_register_record`：`id` / `avatar_id`(历史留痕) / `account` / `password` / `region` / `myth_name` / `myth_story` / `agreed` / `permission` / `register_ip` / `source`(`forge` / `migrate`) / `created_at`
  → **每次注册独立落库，删用户/改权限都不会抹掉**；后台「注册记录」页签可查。
- 其它：`cy_codex` / `cy_home_section` / `cy_home_news` / `cy_site_setting` / `cy_admin_log` / 修炼相关表。

## 五、本机环境事实（已验证，勿重复探测）

- **Java 21**（Zulu，PATH 内）；Maven 用项目 wrapper `mvnw.cmd -s maven-settings-aliyun.xml`。
- **MySQL 客户端**：`C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`（不在 PATH）。
- **Node** 在 PATH；npmmirror / 阿里云 Maven 镜像可直连。
- **构建产物**：`springBootTest\target\springBootTest-0.0.1-SNAPSHOT.jar`（约 29MB，只有几十 KB 说明打包失败）。
- **HTTPS 代理** `127.0.0.1:54311`，外部请求需时跳过证书校验。

## 六、重要陷阱（踩过的坑，务必绕开）

1. **环境变量 `SERVER__PORT` 污染**：会话会注入该变量（实测 55604），Spring Boot relaxed binding 把它当 `server.port`，直接 `java -jar` 会监听随机端口。启动必须显式 `--server.port=8125 --server.address=0.0.0.0`（三个启动脚本均已内置）。
2. **jar 被运行中的 java 进程占用** → `spring-boot-maven-plugin:repackage` 报 `Unable to rename ...jar.original`。先停后端再打包：PowerShell `Stop-Process -Id <pid> -Force`（PID 用 `Get-NetTCPConnection -LocalPort 8125 -State Listen`）。
3. **Git Bash 的 taskkill 参数会被 MSYS 转义破坏**（`//PID` 报「无效参数/选项」）→ 杀进程用 PowerShell，别用 `taskkill`。
4. **curl 测本地接口**加 `--noproxy "*" --max-time 5`，否则被代理劫持/挂起。
5. **bat 必须 GBK 编码**（`chcp 936`）；改中文时保持原编码，别用 UTF-8 覆盖。
6. **端口匹配** `findstr /C:":8125 "`（带尾随空格），避免误配 81250。
7. **SQL 列表要同步**：新增建表脚本时，`一键启动.bat` 与 `start-backend.bat` 都要加一行，否则单独启动后端会缺表（如注册接口报 `user_register_record` 不存在）。
8. **改过 Java 代码必须重新打包**：脚本只在缺 jar 时构建；用 `start-backend.bat rebuild`，或删 jar 后重启，或手动 `./mvnw.cmd -s maven-settings-aliyun.xml package -DskipTests -q`。
9. **前端改完 Vue 文件**：Vite dev 会热更新；要同步正式产物需 `cd cangyuantu-vue && npm run build`（覆盖 `dist/`）。
10. **批处理延迟**用 `ping -n 3 127.0.0.1 >nul`，不要 `timeout /nobreak`（stdin 重定向会报错）。
11. 沙箱拦截 cmd.exe，AI 无法代跑 .bat；验证时用 bash 逐条执行等效命令（`./mvnw.cmd` 可直接调用）。
12. 日志：`springBootTest\log\xx.log`（Spring 日志）、`backend-run.log`（历史 MyBatis SQL 输出）。

## 七、常见操作速查

```bash
# 查注册记录
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 --default-character-set=utf8mb4 -D cangyuan -e "SELECT * FROM user_register_record ORDER BY id DESC LIMIT 10;"

# 测接口
curl -s --noproxy "*" --max-time 5 "http://localhost:8125/api/avatar?name=烛龙真君"
curl -s --noproxy "*" --max-time 5 -X POST http://localhost:8125/api/forge -H "Content-Type: application/json" -d '{"region":"东宁府","deity_name":"测试账号","myth_name":"测试","spell":"test123456","agreed":1}'

# 前端构建
cd D:\workSpace\ghost\ghost-main\cangyuantu-vue && npm run build
```

## 八、GitHub 同步（Ghost2423/ghost）

- 在**项目根** `ghost-main` 双击 `sync-to-github.bat`（自动 add / commit / push）；远程历史冲突时 `sync-to-github.bat force`。脚本 `cd /d "%~dp0"`，但 git 仓库根在上一级，`git add -A` 覆盖整个工作树，属正常行为。
- **直连 GitHub 是通的**（2026-11-11 实测 `git ls-remote` 返回 HEAD）。之前同步失败的根因是 Windows schannel 报 `CRYPT_E_NO_REVOCATION_CHECK`（证书吊销检查），脚本误判成网络不通而切到未监听的 54311 代理。脚本现已设 `GIT_SSL_NO_VERIFY=1` 并给 ls-remote 加 `-c http.schannelCheckRevoke=false`，**通常无需启动代理**；AI 手动验证也可用 `GIT_SSL_NO_VERIFY=1 git ls-remote origin HEAD`。
- token 放在 `ghost-main\github-token.txt`（单行，绝不入库）；**目前该文件不存在、系统凭据管理器也无缓存**，所以推送仍需用户先放 token。脚本在成功与失败路径都会把 token 从 remote URL 抹掉。
- 脚本另修两个静默坑：Git 身份未配置时 commit 会静默失败（现按历史作者 `Ghost2423 <Ghost2423@users.noreply.github.com>` 自动补本仓库 local 身份并显式报错）；push 失败时旧版会在 `.git/config` 残留带 token 的 URL（现统一清理）。
- **两层 `.gitignore`**：仓库根 `D:\workSpace\ghost\.gitignore`（IDEA / target / node_modules / dist / log 等）与项目根 `ghost-main\.gitignore`（token / 构建产物 / Vite 临时配置）。规则对**已跟踪**文件无效，所以 2026-11-11 用 `git rm -r --cached` 把 741 个历史跟踪的构建产物 + Vite 临时文件移出索引（本地文件保留），并随提交 **`98cd9b0`** 落地：765 files changed、约 57 万行删除。当前工作区干净，**远程仍是 `bc25a84`、尚未推送**。
- 索引里的路径都带 `ghost-main/` 前缀（`git ls-files --full-name` 可见）；在子目录里跑 `git status` / `git ls-files` 显示的是相对当前目录的路径，别被两种显示方式搞混。
- 安全提醒：bat 与指南含本地 MySQL 密码，仓库转公开前需先脱敏。

---
*本文件在 `springBootTest\` 与 `cangyuantu-vue\` 两处各存一份，内容保持一致。最后更新：2026-11-11（修正失效路径、绑定 0.0.0.0 / CORS、注册留痕表与接口、SQL 列表、厘清 git 仓库根、仓库瘦身、同步脚本证书与提交修复）。*
