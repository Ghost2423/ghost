# ghost
用于学习记录Java


## 一键启动

双击运行根目录的 **`一键启动.bat`**，即可自动拉起后端（Spring Boot :8125）与前端（Vue3 + Vite :5173），并自动打开 http://localhost:5173 。

- 首次运行会自动完成 Maven 打包与 npm 依赖安装（约 1-2 分钟）
- 停止服务：双击 **`一键停止.bat`**
- 环境要求：JDK 17+、Node.js 16+、MySQL 8（root/123456，库 `cangyuan`）

目录：`springBootTest/`（后端）、`cangyuantu-vue/`（前端）、`springBootTest/sql/cangyuan.sql`（建表脚本，幂等）
