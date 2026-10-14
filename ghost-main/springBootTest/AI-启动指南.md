# AI 快速启动指南（沧元图项目）

> **给 AI 的说明**：电脑每次开机都会重置（node_modules / target 会被删除）。
> 用户说「启动项目」时，直接照本文件执行，**不要重新探索项目结构、不要重新设计方案**。

## 一、启动（一键，首选）

```
双击运行  C:\Users\Administrator\Desktop\ghost-main\一键启动.bat
```

脚本按顺序自动完成：环境检查 → MySQL 检测/启动 → 数据库 cangyuan 初始化（幂等）→ 后端（缺 jar 时自动 mvn 打包）启动 :8125 → 前端（缺 node_modules 时自动 npm install）启动 :5173 → 自动打开浏览器。
停止服务：双击 `C:\Users\Administrator\Desktop\ghost-main\一键停止.bat`（结束 5173 / 8125 端口进程）。

备用（分步手动，仅在一键脚本异常时用）：
```
1. springBootTest\start-backend.bat
2. cangyuantu-vue\start-frontend.bat
```

浏览器访问：**http://localhost:5173**（脚本会自动打开）
- 登录页输入神尊相 + 召唤语；注册页锻造新神尊（数据进 MySQL）

## 二、项目全景

| 部分 | 位置 | 说明 |
|---|---|---|
| **前端** | `C:\Users\Administrator\Desktop\ghost-main\cangyuantu-vue` | Vue3 + Vite，端口 **5173**，`/api` 代理到 8125 |
| **后端** | `D:\workSpace\springBootTest` | Spring Boot 3.4.5 (Java 17 编译/Java 21 运行) + MyBatis-Plus 3.5.7，端口 **8125** |
| **数据库** | MySQL 8.0.39 | 库 `cangyuan`，表 `avatar_foundation`，root/123456，3306 |
| **SQL 文件** | `D:\workSpace\springBootTest\sql\cangyuan.sql` | 幂等（可重复执行），含种子数据 |
| 旧版静态页 | `Desktop\ghost-main\ghost-main\` | 历史 Python server.py 方案，**已废弃**，勿动 |

后端包结构：`com.fenglin.springboottest` 下 entity / mapper / service / controller / dto / common / config

## 三、API 契约（返回 JSON 统一 `{ok, msg}` 结构，字段为 snake_case）

| 接口 | 说明 |
|---|---|
| `POST /api/login` `{name, spell}` | 登录校验（服务端比对召唤语），成功 `{ok, avatar:{...}}` |
| `POST /api/forge` `{region, deity_name, myth_name, spell, myth_story, agreed}` | 注册锻造，成功 `{ok, id}`；重复 409 |
| `GET /api/avatar?name=神尊相` | 查询神尊根基，404 表示未寻得 |

测试账号：`九天应元雷声普化天尊` / `诸般雷霆，遵吾号令`（还有：烛龙真君、西王母元君、句芒星君）

## 四、本机环境事实（已验证，勿重复探测）

- **Java 21**（Zulu，PATH 里有）；Maven 用项目自带 wrapper `mvnw.cmd`（3.9.9，dist 已缓存于 `~/.m2/wrapper`）
- **MySQL 客户端**：`C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`（不在 PATH）
- **Node** 在 PATH（沙箱里是 22，用户环境 20，均兼容 Vite 5）
- **网络**：阿里云 Maven 镜像 / npmmirror 均可直连（不需要代理）。项目已内置：`maven-settings-aliyun.xml`、`.npmrc`、wrapper 下载源已改为阿里云
- **构建产物**：`target\springBootTest-0.0.1-SNAPSHOT.jar`

## 五、重要陷阱（踩过的坑，务必绕开）

1. **环境变量 `SERVER__PORT` 污染**：沙箱会注入该变量（值 53945），Spring Boot 会把它当 `server.port`！
   → 启动 jar 必须显式 `--server.port=8125`（start-backend.bat 已内置）。
2. **沙箱 HTTP_PROXY=127.0.0.1:54000 会劫持 curl**：测试本地端口要加 `--noproxy "*"`，否则返回 502 假象。
3. **bat 必须用 GBK 编码**（Write 工具写的是 UTF-8，中文会乱码）→ 用 Python `open(path,'w',encoding='gbk')` 生成；已有 4 个脚本，改中文时同样处理。
4. **脚本只用相对路径**（`%~dp0` 锚定自身目录），不能引用 node_modules / target 里的内容（运行前会被删除）。
5. **批处理里不要用 `timeout /nobreak`**（stdin 重定向时报错），用 `ping -n 3 127.0.0.1 >nul` 延迟。
6. **端口匹配**用 `findstr ":8125 "`（带尾随空格），避免误配 81250 之类。
7. **杀进程别用 Git Bash 的 taskkill**：`export MSYS_NO_PATHCONV=1` + `taskkill /F /PID xxx` 实测仍被 MSYS 转义破坏（`/F` 变 `//F`，报"无效参数/选项"）→ 直接用 PowerShell：`Stop-Process -Id <pid> -Force`（先 `Get-NetTCPConnection -LocalPort 8125 -State Listen` 拿 PID）。
8. **application-dev.properties 会覆盖主配置的 server.port** —— dev 已改为 8125，别改回 8081。
9. **沙箱跨任务网络隔离**：AI 验证代理时，java 和 vite 必须在**同一个 bash 任务**里启动；任务结束后其孤儿进程会被断网（vite 代理报 10061）。这是沙箱行为，用户双击 bat 运行完全无此问题。
10. 沙箱拦截 cmd.exe，AI 无法代跑 .bat —— 验证时用 bash 逐条执行等效命令（mvnw.cmd 可直接 `./mvnw.cmd` 调用）。
11. **Vite 只监听 `[::1]:5173`（IPv6 回环）**：浏览器/curl 一律用 `http://localhost:5173`，写 `127.0.0.1:5173` 会连不上。
12. **AI 同任务启动实操（陷阱 9 的落地写法）**：先按第 7 条 kill 掉单独起的 java，再在同一条 bash 里
    `(cd <后端目录> && nohup java -jar target/springBootTest-0.0.1-SNAPSHOT.jar --server.port=8125 > backend-run.log 2>&1 &)` → `sleep 12` → `npm install && npm run dev`。

## 六、常见操作速查

```bash
# 停止（也可直接双击 stop-*.bat）
D:\workSpace\springBootTest\stop-backend.bat        # 杀 8125
...cangyuantu-vue\stop-frontend.bat                # 杀 5173

# 手动测试后端接口（注意 --noproxy）
curl --noproxy "*" "http://127.0.0.1:8125/api/avatar?name=烛龙真君"

# 查数据库
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 --default-character-set=utf8mb4 -e "USE cangyuan; SELECT * FROM avatar_foundation;"

# 前端构建产物（可选，部署用）
cd C:\Users\Administrator\Desktop\ghost-main\cangyuantu-vue && npm run build
```

## 七、GitHub 同步备份（Ghost2423/ghost）

- **仓库根目录**：`C:\Users\Administrator\Desktop\ghost-main`（含 cangyuantu-vue + springBootTest 副本，已 git init，分支 main，首次提交 0ee045c）
- **首次使用**：在仓库根目录创建 `github-token.txt`，粘贴 GitHub admin token（只占一行，前后无空格）。该文件已被 .gitignore 排除，**绝不会被提交**
- **同步**：双击 `sync-to-github.bat`（自动 add/commit/push；直连失败自动改走本地代理 127.0.0.1:54311）
- **远程历史冲突**（旧版内容导致 push 被拒）：运行 `sync-to-github.bat force`，用本地最新版覆盖远程
- **后端双副本注意**：运行和开发用 `D:\workSpace\springBootTest`（工作副本）；同步 GitHub 前，把 D: 的 springBootTest 整个复制到 Desktop\ghost-main\ 下覆盖（或叫 AI 处理）
- **已排除入库**（.gitignore）：node_modules / target / dist / log / *.rar *.zip / 旧版嵌套目录 ghost-main\ / vite 临时文件 / github-token.txt
- **安全**：推送成功后脚本自动从 .git/config 移除 token
- 提醒：bat 脚本与指南含本地 MySQL 密码，若仓库转公开需先脱敏

## 八、表结构（avatar_foundation）

`id`(PK,AI) / `region`(地域) / `deity_name`(神尊相,唯一) / `myth_name`(神话原型) / `spell`(召唤语) / `myth_story`(简介) / `agreed`(协议) / `created_at`

---
*本文件存在于 Desktop\ghost-main\cangyuantu-vue、Desktop\ghost-main\springBootTest 两处，内容保持一致（`D:\workSpace\springBootTest` 停留在 9 月旧版、无本指南与 sql 目录，勿当作工作副本）。最后更新：2026-10-10（修正第 7 条杀进程方法，新增第 11/12 条）*
