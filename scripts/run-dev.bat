@echo off
REM ========================================
REM 贪吃蛇游戏 — 开发启动脚本
REM ========================================
echo 正在启动贪吃蛇游戏开发服务器...
echo.
cd /d "%~dp0\.."
call mvn spring-boot:run -Dexec.skip=true
pause
