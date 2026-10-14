@echo off
chcp 936 >nul
title 沧元界-后端停止
echo 正在停止沧元界后端（端口 8125）...
set found=0
for /f "tokens=5" %%p in ('netstat -ano ^| findstr "LISTENING" ^| findstr /C:":8125 "') do (
  taskkill /F /PID %%p >nul 2>nul
  echo [OK] 已结束进程 PID %%p
  set found=1
)
if "%found%"=="0" echo [提示] 后端未在运行
echo 完成。
pause
