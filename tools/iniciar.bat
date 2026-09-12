@echo off
setlocal
title AutoClicker-Pro - Iniciar

echo ========================================
echo   AutoClicker-Pro - Iniciar
echo ========================================
echo.

cd /d "%~dp0.."

set "JNATIVEHOOK_JAR=lib\jnativehook-2.2.2.jar"
set "JNATIVEHOOK_URL=https://repo1.maven.org/maven2/com/github/kwhat/jnativehook/2.2.2/jnativehook-2.2.2.jar"
set "JNATIVEHOOK_SHA256=2C7904423BC680AF02D9EA9557AE233C35199E302D072773A9D0304B568ACD41"
set "FLATLAF_JAR=lib\flatlaf-3.2.5.jar"
set "FLATLAF_URL=https://repo1.maven.org/maven2/com/formdev/flatlaf/3.2.5/flatlaf-3.2.5.jar"
set "FLATLAF_SHA256=BFB14CEF748B364761115FA6756409C19FA668AA8C3647FA5590ED206ECF698F"
set "JAVA_NATIVE_ACCESS="
for /f "tokens=3" %%V in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /C:"java.specification.version ="') do call :CONFIGURE_NATIVE_ACCESS %%V

set "NEED_BUILD=0"
if not exist "build\autoclicker-pro.jar" set "NEED_BUILD=1"
if exist "build\autoclicker-pro.jar" powershell -NoProfile -Command "$jar=(Get-Item 'build\autoclicker-pro.jar').LastWriteTimeUtc; if (Get-ChildItem 'src\main\java' -Recurse -Filter '*.java' | Where-Object { $_.LastWriteTimeUtc -gt $jar } | Select-Object -First 1) { exit 1 }"
if errorlevel 1 set "NEED_BUILD=1"

if "%NEED_BUILD%"=="1" (
    echo Compilando...
    call tools\compilar.bat
    if errorlevel 1 (
        echo.
        echo [ERRO] Build falhou.
        pause
        exit /b 1
    )
)

if not exist "%JNATIVEHOOK_JAR%" (
    echo Baixando JNativeHook...
    if not exist "lib" mkdir "lib"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%JNATIVEHOOK_URL%' -OutFile '%JNATIVEHOOK_JAR%'"
    if errorlevel 1 goto DEP_FAIL
)

if not exist "%FLATLAF_JAR%" (
    echo Baixando FlatLaf...
    if not exist "lib" mkdir "lib"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%FLATLAF_URL%' -OutFile '%FLATLAF_JAR%'"
    if errorlevel 1 goto DEP_FAIL
)

call :VERIFY_SHA256 "%JNATIVEHOOK_JAR%" "%JNATIVEHOOK_SHA256%"
if errorlevel 1 goto HASH_FAIL
call :VERIFY_SHA256 "%FLATLAF_JAR%" "%FLATLAF_SHA256%"
if errorlevel 1 goto HASH_FAIL

echo Iniciando AutoClicker-Pro...
echo Hotkey: F6 (funciona minimizado)
echo.

java %JAVA_NATIVE_ACCESS% -cp "build\autoclicker-pro.jar;%JNATIVEHOOK_JAR%;%FLATLAF_JAR%" com.autoclicker.ui.AutoClickerUI
exit /b %ERRORLEVEL%

:DEP_FAIL
echo.
echo [ERRO] Falha ao baixar dependencias.
pause
exit /b 1

:HASH_FAIL
echo.
echo [ERRO] Uma dependencia falhou na verificacao SHA-256.
echo Exclua o arquivo indicado e execute este script novamente.
pause
exit /b 1

:VERIFY_SHA256
set "VERIFY_FILE=%~1"
set "VERIFY_HASH=%~2"
powershell -NoProfile -Command "$actual=(Get-FileHash -Algorithm SHA256 -LiteralPath $env:VERIFY_FILE).Hash; if ($actual -ne $env:VERIFY_HASH) { Write-Error ('SHA-256 invalido: ' + $env:VERIFY_FILE); exit 1 }"
exit /b %ERRORLEVEL%

:CONFIGURE_NATIVE_ACCESS
if %~1 GEQ 24 set "JAVA_NATIVE_ACCESS=--enable-native-access=ALL-UNNAMED"
exit /b 0

