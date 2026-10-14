@echo off
chcp 936 >nul
title 沧元界-前端启动
setlocal
cd /d "%~dp0"

echo ==================================================
echo   沧元界 前端启动 (Vue3 + Vite，代理到后端 8125)
echo   项目目录: %~dp0
echo ==================================================
echo.

rem ---- 1. 检查 Node ----
where node >nul 2>nul
if errorlevel 1 (
  echo [错误] 未找到 node，请先安装 Node.js 并配置 PATH
  goto :end
)
echo [OK] Node 已就绪

rem ---- 2. 若前端已在运行则直接退出 ----
netstat -ano | findstr "LISTENING" | findstr /C:":5173 " >nul 2>nul
if not errorlevel 1 (
  echo [OK] 前端已在运行，无需重复启动
  goto :done
)

rem ---- 3. node_modules 每次开机会被删除，缺失则重装 ----
if not exist "node_modules" (
  echo [安装] 正在安装前端依赖（npmmirror 镜像，约 20-40 秒）...
  call npm install --no-audit --no-fund
  if errorlevel 1 (
    echo [错误] npm install 失败，请检查网络
    goto :end
  )
)

rem ---- 4. 启动 Vite 开发服务器 ----
echo [启动] 正在启动沧元界前端 ...
start "沧元界-前端" cmd /k npm run dev

rem ---- 5. 等待端口就绪 ----
set /a tries=0
:waitloop
netstat -ano | findstr "LISTENING" | findstr /C:":5173 " >nul 2>nul
if not errorlevel 1 goto :up
set /a tries+=1
if %tries% geq 10 goto :timeout
ping -n 3 127.0.0.1 >nul
goto :waitloop

:up
echo [OK] 前端已启动

:done
echo.
echo ==================================================
echo   前端地址:  http://localhost:5173
echo   依赖后端:  请先运行 springBootTest\start-backend.bat
echo   API 代理:  /api -> http://127.0.0.1:8125
echo ==================================================
goto :end

:timeout
echo [警告] 端口 5173 未在 20 秒内就绪，请查看前端窗口日志

:end
endlocal
echo.
pause
