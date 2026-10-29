@echo off
setlocal
cd /d "%~dp0"
title 금기 룰렛 모드 빌드

echo ==================================================
echo   금기 룰렛 모드 빌드  [Minecraft 26.3 / Fabric]
echo ==================================================
echo.

set "JAVACMD=java"
if defined JAVA_HOME set "JAVACMD=%JAVA_HOME%\bin\java.exe"

call "%JAVACMD%" -version >nul 2>nul
if errorlevel 1 goto nojava

set "JVER="
set "JMAJOR="
for /f "tokens=3" %%v in ('call "%JAVACMD%" -version 2^>^&1 ^| findstr /i "version"') do set "JVER=%%~v"
for /f "delims=." %%m in ("%JVER%") do set "JMAJOR=%%m"
if not defined JMAJOR goto nojava
if %JMAJOR% LSS 25 goto oldjava

echo [1/2] 빌드 중입니다.
echo       처음 한 번은 마인크래프트 파일을 내려받느라 5~10분 정도 걸립니다.
echo.
call gradlew.bat build --no-daemon
if errorlevel 1 goto failed

if not exist "output" mkdir "output"
del /q "output\*.jar" >nul 2>nul
copy /y "build\libs\taboo-roulette-*.jar" "output\" >nul

echo.
echo ==================================================
echo [2/2] 완료!
echo   output 폴더의 taboo-roulette-1.0.0.jar 를
echo   .minecraft\mods 폴더에 넣으세요. Fabric API 도 함께 필요합니다.
echo ==================================================
start "" "output"
pause
exit /b 0

:nojava
echo [오류] Java 를 찾을 수 없습니다.
echo   https://adoptium.net/temurin/releases/ 에서 JDK 25 를 설치하세요.
echo   설치 화면에서 "Set JAVA_HOME variable" 항목을 꼭 켜 주세요.
echo   설치가 끝나면 이 창을 닫고 build-mod.bat 를 다시 실행하세요.
echo.
pause
exit /b 1

:oldjava
echo [오류] 지금 쓰이는 Java 버전은 %JMAJOR% 입니다. 26.3 모드는 JDK 25 이상이 필요합니다.
echo   https://adoptium.net/temurin/releases/ 에서 JDK 25 를 설치하세요.
echo   설치 화면에서 "Set JAVA_HOME variable" 항목을 꼭 켜 주세요.
echo.
pause
exit /b 1

:failed
echo.
echo [빌드 실패] 위쪽 검은 창의 오류 메시지를 복사해서 Claude 에게 보여주세요.
echo   특히 "error:" 또는 "FAILURE" 로 시작하는 줄 주변이 필요합니다.
echo.
pause
exit /b 1
