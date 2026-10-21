@echo off
chcp 936 >nul
title 沧元图 - 一键停止
setlocal EnableExtensions

echo 正在停止沧元图服务（前端 5173 / 后端 8125）...
set found=0

for /f "tokens=5" %%p in ('netstat -ano ^| findstr "LISTENING" ^| findstr /C:":5173"') do (
  taskkill /F /PID %%p >nul 2>nul
  echo   [OK] 已结束前端进程 PID %%p
  set found=1
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr "LISTENING" ^| findstr /C:":8125 "') do (
  taskkill /F /PID %%p >nul 2>nul
  echo   [OK] 已结束后端进程 PID %%p
  set found=1
)

if "%found%"=="0" echo   [提示] 两个服务均未在运行
echo 完成。
pause
endlocal
