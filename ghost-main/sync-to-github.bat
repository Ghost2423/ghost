@echo off
chcp 936 >nul
title 苍元图-GitHub同步
setlocal
cd /d "%~dp0"

set "REPO_URL=https://github.com/Ghost2423/ghost.git"
set "TOKENFILE=%~dp0github-token.txt"

echo ==================================================
echo   苍元图 GitHub 同步 (Ghost2423/ghost)
echo ==================================================
echo.

rem ---- 1. 检查 token 文件 ----
if not exist "%TOKENFILE%" (
  echo [错误] 未找到 github-token.txt
  echo 请在项目根目录新建 github-token.txt，把 GitHub token
  echo 粘贴进去（只占一行，前后不留空格）。
  goto :end
)
set /p TOKEN=<"%TOKENFILE%"
if "%TOKEN%"=="" (
  echo [错误] github-token.txt 是空的，请粘贴 token
  goto :end
)
echo [OK] 已读取 token

rem ---- 2. 测试连接 GitHub（先直连，失败改走本地代理）----
echo [测试] 正在连接 GitHub ...
git ls-remote "https://%TOKEN%@github.com/Ghost2423/ghost.git" HEAD >nul 2>nul
if not errorlevel 1 goto :net_ok
echo [提示] 直连失败，尝试本地代理 127.0.0.1:54311 ...
set "HTTP_PROXY=http://127.0.0.1:54311"
set "HTTPS_PROXY=http://127.0.0.1:54311"
git -c http.sslVerify=false ls-remote "https://%TOKEN%@github.com/Ghost2423/ghost.git" HEAD >nul 2>nul
if errorlevel 1 (
  echo [错误] 无法连接 GitHub，请检查：
  echo   1. 代理软件是否已启动，监听端口 54311
  echo   2. github-token.txt 里的 token 是否有效
  goto :end
)

:net_ok
echo [OK] GitHub 连接正常

rem ---- 3. 提交本地改动 ----
git add -A
git commit -m "sync update %date% %time%" >nul 2>nul
echo [OK] 本地改动已提交（无改动则自动跳过）

rem ---- 4. 设置远程（每次刷新 token）----
git remote remove origin >nul 2>nul
git remote add origin "https://%TOKEN%@github.com/Ghost2423/ghost.git"

rem ---- 5. 推送 ----
if /i "%~1"=="force" (
  echo [模式] 强制推送：用本地内容覆盖远程全部历史！
  git push -u origin main --force
) else (
  git push -u origin main
)
if errorlevel 1 (
  echo.
  echo [失败] 推送被拒绝：远程仓库已有不同的提交历史（旧版内容）。
  echo 如确认要用本地最新版覆盖远程，请运行：
  echo     sync-to-github.bat force
  goto :end
)

rem ---- 6. 从远程地址中移除 token（安全处理）----
git remote set-url origin "%REPO_URL%"
echo.
echo [成功] 已同步到 GitHub：https://github.com/Ghost2423/ghost

:end
endlocal
echo.
pause
