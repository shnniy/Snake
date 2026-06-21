@echo off
REM ========================================
REM 贪吃蛇游戏 — EXE 打包脚本
REM 使用 jpackage 生成 Windows 原生可执行文件
REM 需要 JDK 17+ 环境
REM ========================================
echo ========================================
echo   贪吃蛇游戏 — EXE 打包工具
echo ========================================
echo.

cd /d "%~dp0\.."

REM Step 1: 确认 jpackage 可用
where jpackage >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未找到 jpackage，请确保 JDK 17+ 已安装并添加到 PATH
    pause
    exit /b 1
)

REM Step 2: 编译打包 Fat JAR
echo [1/3] 正在构建 Fat JAR...
call mvn clean package -DskipTests -Dexec.skip=true -q
if %ERRORLEVEL% NEQ 0 (
    echo [错误] Maven 构建失败
    pause
    exit /b 1
)
echo [1/3] Fat JAR 构建完成

REM Step 3: 使用 jpackage 创建 EXE
echo [2/3] 正在使用 jpackage 创建 EXE...

REM 先获取主 JAR 文件名
for /f "delims=" %%i in ('dir /b target\*.jar 2^>nul ^| findstr /v "sources" ^| findstr /v "javadoc"') do set JAR_FILE=%%i

if "%JAR_FILE%"=="" (
    echo [错误] 未找到 JAR 文件
    pause
    exit /b 1
)

echo 主 JAR: target\%JAR_FILE%

jpackage ^
    --type exe ^
    --name SnakeGame ^
    --app-version 1.0.0 ^
    --vendor "SnakeGame" ^
    --input target ^
    --main-jar %JAR_FILE% ^
    --main-class com.snakegame.SnakeGameApplication ^
    --win-console ^
    --dest target\installer ^
    --description "经典贪吃蛇游戏 - Web Edition" ^
    --about-url http://localhost:8080

if %ERRORLEVEL% NEQ 0 (
    echo [警告] jpackage 打包失败。
    echo 如果提示需要 WiX Toolset，请安装 WiX 或使用 --type app-image 代替
    echo.
    echo 尝试使用 app-image 模式（无需 WiX）...
    jpackage ^
        --type app-image ^
        --name SnakeGame ^
        --app-version 1.0.0 ^
        --input target ^
        --main-jar %JAR_FILE% ^
        --main-class com.snakegame.SnakeGameApplication ^
        --dest target\installer
    if %ERRORLEVEL% EQU 0 (
        echo [2/3] app-image 创建成功 (target\installer\SnakeGame\)
    )
) else (
    echo [2/3] EXE 创建成功
)

REM Step 4: 完成
echo [3/3] 打包完成!
echo.
echo 输出位置: target\installer\
dir /b target\installer\ 2>nul
echo.
echo 直接运行 target\installer\SnakeGame\SnakeGame.exe 启动游戏
echo 或运行 target\%JAR_FILE% (需要 java -jar)

pause
