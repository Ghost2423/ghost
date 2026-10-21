@echo off
chcp 936 >nul
title 沧元界-后端启动
setlocal
cd /d "%~dp0"

echo ==================================================
echo   沧元界 后端启动 (Spring Boot + MyBatis-Plus + MySQL)
echo   项目目录: %~dp0
echo ==================================================
echo.

rem ---- 1. 检查 Java ----
where java >nul 2>nul
if errorlevel 1 (
  echo [错误] 未找到 java，请先安装 JDK17+ 并配置 PATH
  goto :end
)
echo [OK] Java 已就绪

rem ---- 2. 检查 MySQL (3306) ----
netstat -ano | findstr "LISTENING" | findstr /C:":3306 " >nul 2>nul
if errorlevel 1 (
  echo [提示] MySQL 未运行，尝试启动服务 ...
  net start MySQL80 >nul 2>nul
  netstat -ano | findstr "LISTENING" | findstr /C:":3306 " >nul 2>nul
  if errorlevel 1 (
    echo [错误] MySQL 未启动。请手动启动 MySQL 服务后重试
    goto :end
  )
)
echo [OK] MySQL 运行中 (127.0.0.1:3306)

rem ---- 3. 初始化数据库（幂等，可重复执行）----
set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not exist "%MYSQL_BIN%" set "MYSQL_BIN=mysql.exe"
"%MYSQL_BIN%" -uroot -p123456 --default-character-set=utf8mb4 < "%~dp0sql\cangyuan.sql" >nul 2>nul
if errorlevel 1 (
  echo [警告] 数据库初始化未成功（若库已存在可忽略）
) else (
  echo [OK] 数据库 cangyuan 已就绪
)

rem ---- 4. 若后端已在运行则直接退出 ----
netstat -ano | findstr "LISTENING" | findstr /C:":8125 " >nul 2>nul
if not errorlevel 1 (
  echo [OK] 后端已在运行，无需重复启动
  goto :done
)

rem ---- 5. 构建（target 每次开机会被删除，缺失则重建）----
if exist "target\springBootTest-0.0.1-SNAPSHOT.jar" goto :run
echo [构建] 正在编译打包（阿里云镜像，首次约 1 分钟）...
call mvnw.cmd -s "%~dp0maven-settings-aliyun.xml" package -DskipTests
if errorlevel 1 (
  echo [错误] 构建失败，请检查上方 Maven 日志
  goto :end
)

:run
rem ---- 6. 启动后端（显式指定端口，避免环境变量干扰）----
echo [启动] 正在启动沧元界后端 ...
start "沧元界-后端" cmd /k java -jar "%~dp0target\springBootTest-0.0.1-SNAPSHOT.jar" --server.port=8125

rem ---- 7. 等待端口就绪 ----
set /a tries=0
:waitloop
netstat -ano | findstr "LISTENING" | findstr /C:":8125 " >nul 2>nul
if not errorlevel 1 goto :up
set /a tries+=1
if %tries% geq 15 goto :timeout
ping -n 3 127.0.0.1 >nul
goto :waitloop

:up
echo [OK] 后端已启动

:done
echo.
echo ==================================================
echo   后端地址:  http://127.0.0.1:8125
echo   接口自检:  http://127.0.0.1:8125/api/avatar?name=九天应元雷声普化天尊
echo   前端请运行: cangyuantu-vue\start-frontend.bat
echo ==================================================
goto :end

:timeout
echo [警告] 端口 8125 未在 30 秒内就绪，请查看后端窗口日志

:end
endlocal
echo.
pause
