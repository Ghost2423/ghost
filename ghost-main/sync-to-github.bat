@echo off
chcp 936 >nul
title 苍元图-GitHub同步
setlocal
cd /d "%~dp0"

set "REPO_URL=https://github.com/Ghost2423/ghost.git"
set "TOKENFILE=%~dp0github-token.txt"

rem Windows schannel 常报 CRYPT_E_NO_REVOCATION_CHECK（证书吊销检查失败），
rem 不放宽会被误判成「网络不通」而错误切换到未启动的代理。直连 GitHub 本身可用。
rem setlocal 内的环境变量只影响本脚本进程，不会污染用户全局 Git 配置。
set "GIT_SSL_NO_VERIFY=1"

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
git -c http.schannelCheckRevoke=false ls-remote "https://%TOKEN%@github.com/Ghost2423/ghost.git" HEAD >nul 2>nul
if not errorlevel 1 goto :net_ok
echo [提示] 直连失败，尝试本地代理 127.0.0.1:54311 ...
set "HTTP_PROXY=http://127.0.0.1:54311"
set "HTTPS_PROXY=http://127.0.0.1:54311"
git -c http.sslVerify=false -c http.schannelCheckRevoke=false ls-remote "https://%TOKEN%@github.com/Ghost2423/ghost.git" HEAD >nul 2>nul
if errorlevel 1 (
  echo [错误] 直连与代理均未连通，请检查：
  echo   1. 网络是否能访问 github.com（已放宽证书校验，通常无需代理）
  echo   2. 若必须走代理，确认代理软件监听 127.0.0.1:54311
  echo   3. github-token.txt 里的 token 是否有效且具备 repo 权限
  goto :end
)

:net_ok
echo [OK] GitHub 连接正常

rem ---- 3. 提交本地改动 ----
rem 未配置 Git 身份时 commit 会静默失败，造成「以为同步了、其实推的是旧内容」
set "GITNAME="
for /f "delims=" %%n in ('git config user.name') do set "GITNAME=%%n"
if not defined GITNAME (
  git config user.name "Ghost2423"
  git config user.email "Ghost2423@users.noreply.github.com"
  echo [提示] 已按历史提交补全本仓库 Git 身份
)
git add -A
git diff --cached --quiet
if errorlevel 1 (
  git commit -m "sync update %date% %time%" >nul 2>nul
  if errorlevel 1 (
    echo [错误] 本地提交失败，已终止同步（避免推送旧内容）
    goto :end
  )
  echo [OK] 本地改动已提交
) else (
  echo [OK] 无待提交改动，直接推送
)

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
  git remote set-url origin "%REPO_URL%" >nul 2>nul
  echo.
  echo [失败] 推送未成功（远程历史冲突 / 网络 / token 权限问题）。
  echo 已从远程地址中移除 token。
  echo 如确认是历史冲突且要用本地最新版覆盖远程，请运行：
  echo     sync-to-github.bat force
  goto :end
)

rem ---- 6. 从远程地址中移除 token（安全处理）----
git remote set-url origin "%REPO_URL%"
echo.
echo [成功] 已同步到 GitHub：https://github.com/Ghost2423/ghost

:end
rem 任何退出路径都把 token 从远程地址中抹掉，避免失败后残留在 .git/config
git remote set-url origin "%REPO_URL%" >nul 2>nul
endlocal
echo.
pause
