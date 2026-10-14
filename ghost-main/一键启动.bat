@echo off
chcp 936 >nul
title 沧元图 - 一键启动
setlocal EnableExtensions
cd /d "%~dp0"

set "ROOT=%~dp0"
set "BE=%ROOT%springBootTest"
set "FE=%ROOT%cangyuantu-vue"
set "JAR=%BE%\target\springBootTest-0.0.1-SNAPSHOT.jar"
set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

echo ============================================================
echo    沧元图 · 一键启动
echo    后端 Spring Boot :8125    前端 Vue3+Vite :5173
echo    项目根目录: %ROOT%
echo ============================================================
echo.

rem ================= 1. 环境检查 =================
echo [1/6] 检查运行环境 ...
where java >nul 2>nul
if errorlevel 1 (
  echo   [错误] 未找到 java，请安装 JDK 17+ 并加入 PATH
  goto :fail
)
where node >nul 2>nul
if errorlevel 1 (
  echo   [错误] 未找到 node，请安装 Node.js 并加入 PATH
  goto :fail
)
where npm >nul 2>nul
if errorlevel 1 (
  echo   [错误] 未找到 npm，请安装 Node.js 并加入 PATH
  goto :fail
)
echo   [OK] Java / Node / npm 已就绪

rem ================= 2. MySQL =================
echo [2/6] 检查 MySQL 3306 ...
netstat -ano | findstr "LISTENING" | findstr /C:":3306 " >nul 2>nul
if not errorlevel 1 goto :mysqlok
echo   未检测到 MySQL，尝试启动服务 ...
for %%S in (MySQL80 MySQL57 MySQL MySQL8 MySQL84) do (
  net start %%S >nul 2>nul
)
netstat -ano | findstr "LISTENING" | findstr /C:":3306 " >nul 2>nul
if errorlevel 1 (
  echo   [错误] MySQL 未启动，请手动启动 MySQL 后重试
  goto :fail
)
:mysqlok
echo   [OK] MySQL 运行中 127.0.0.1:3306

rem ================= 3. 初始化数据库 =================
echo [3/6] 初始化数据库 cangyuan（幂等，可重复执行）...
if not exist "%MYSQL_BIN%" set "MYSQL_BIN=mysql.exe"
"%MYSQL_BIN%" -uroot -p123456 --default-character-set=utf8mb4 < "%BE%\sql\cangyuan.sql" >nul 2>nul
if errorlevel 1 (
  echo   [警告] 数据库脚本未执行成功（若库已存在可忽略）
) else (
  echo   [OK] 数据库 cangyuan 已就绪
)

rem ================= 4. 启动后端 =================
echo [4/6] 启动后端 ...
netstat -ano | findstr "LISTENING" | findstr /C:":8125 " >nul 2>nul
if not errorlevel 1 (
  echo   [OK] 后端已在运行，跳过
  goto :frontend
)
if exist "%JAR%" goto :runbe
echo   未找到构建产物，正在编译打包（阿里云镜像，首次约 1 分钟）...
pushd "%BE%"
call "%BE%\mvnw.cmd" -s "%BE%\maven-settings-aliyun.xml" package -DskipTests
popd
if errorlevel 1 (
  echo   [错误] 后端构建失败，请检查上方 Maven 输出
  goto :fail
)
if not exist "%JAR%" (
  echo   [错误] 构建未产出 jar 包
  goto :fail
)
:runbe
start "沧元图-后端" /d "%BE%" cmd /k java -jar "%JAR%" --server.port=8125
set /a t=0
:waitbe
netstat -ano | findstr "LISTENING" | findstr /C:":8125 " >nul 2>nul
if not errorlevel 1 goto :beok
set /a t+=1
if %t% geq 30 goto :betimeout
ping -n 3 127.0.0.1 >nul
goto :waitbe
:betimeout
echo   [错误] 端口 8125 在 60 秒内未就绪，请查看「沧元图-后端」窗口日志
goto :fail
:beok
echo   [OK] 后端已启动 http://127.0.0.1:8125

rem ================= 5. 启动前端 =================
:frontend
echo [5/6] 启动前端 ...
netstat -ano | findstr "LISTENING" | findstr /C:":5173" >nul 2>nul
if not errorlevel 1 (
  echo   [OK] 前端已在运行，跳过
  goto :open
)
if exist "%FE%\node_modules\.bin\vite.cmd" goto :runfe
echo   依赖缺失，正在 npm install（npmmirror 镜像，约 30 秒）...
pushd "%FE%"
call npm install --no-audit --no-fund
popd
if errorlevel 1 (
  echo   [错误] npm install 失败，请检查网络
  goto :fail
)
:runfe
start "沧元图-前端" /d "%FE%" cmd /k npm run dev
set /a t=0
:waitfe
netstat -ano | findstr "LISTENING" | findstr /C:":5173" >nul 2>nul
if not errorlevel 1 goto :feok
set /a t+=1
if %t% geq 20 goto :fetimeout
ping -n 3 127.0.0.1 >nul
goto :waitfe
:fetimeout
echo   [错误] 端口 5173 在 40 秒内未就绪，请查看「沧元图-前端」窗口日志
goto :fail
:feok
echo   [OK] 前端已启动 http://localhost:5173

rem ================= 6. 打开浏览器 =================
:open
echo [6/6] 打开浏览器 ...
start "" http://localhost:5173

echo.
echo ============================================================
echo   启动完成！
echo     访问地址 : http://localhost:5173
echo     后端接口 : http://127.0.0.1:8125
echo     停止服务 : 双击运行 一键停止.bat
echo.
echo   测试账号: 九天应元雷声普化天尊 / 诸般雷霆，遵吾号令
echo ============================================================
echo.
echo 本窗口可以关闭；后端和前端在各自窗口中运行。
pause
goto :eof

:fail
echo.
echo ============================================================
echo   启动失败，请根据上方提示处理后重新双击运行。
echo ============================================================
echo.
pause
endlocal
