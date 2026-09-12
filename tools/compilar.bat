@echo off
setlocal EnableExtensions

title AutoClicker-Pro - Compilar

set "ROOT=%~dp0.."
set "SRC=%ROOT%\src\main\java"
set "TARGET=%ROOT%\target"
set "BUILD=%ROOT%\build"
set "LIB=%ROOT%\lib"
set "JNATIVEHOOK_JAR=%LIB%\jnativehook-2.2.2.jar"
set "JNATIVEHOOK_URL=https://repo1.maven.org/maven2/com/github/kwhat/jnativehook/2.2.2/jnativehook-2.2.2.jar"
set "JNATIVEHOOK_SHA256=2C7904423BC680AF02D9EA9557AE233C35199E302D072773A9D0304B568ACD41"
set "FLATLAF_JAR=%LIB%\flatlaf-3.2.5.jar"
set "FLATLAF_URL=https://repo1.maven.org/maven2/com/formdev/flatlaf/3.2.5/flatlaf-3.2.5.jar"
set "FLATLAF_SHA256=BFB14CEF748B364761115FA6756409C19FA668AA8C3647FA5590ED206ECF698F"
set "OUT_JAR=%BUILD%\autoclicker-pro.jar"

echo ========================================
echo   AutoClicker-Pro - Compilar
echo ========================================
echo.

if not exist "%BUILD%" mkdir "%BUILD%"
if not exist "%LIB%" mkdir "%LIB%"

if not exist "%JNATIVEHOOK_JAR%" (
    echo [1/4] Baixando JNativeHook 2.2.2...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%JNATIVEHOOK_URL%' -OutFile '%JNATIVEHOOK_JAR%'"
    if errorlevel 1 goto DL_FAIL
)

if not exist "%FLATLAF_JAR%" (
    echo [2/4] Baixando FlatLaf 3.2.5...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%FLATLAF_URL%' -OutFile '%FLATLAF_JAR%'"
    if errorlevel 1 goto DL_FAIL
)

call :VERIFY_SHA256 "%JNATIVEHOOK_JAR%" "%JNATIVEHOOK_SHA256%"
if errorlevel 1 goto HASH_FAIL
call :VERIFY_SHA256 "%FLATLAF_JAR%" "%FLATLAF_SHA256%"
if errorlevel 1 goto HASH_FAIL

goto HAVE_DEP

:DL_FAIL
echo.
echo [ERRO] Falha ao baixar dependencias.
echo.
pause
exit /b 1

:HASH_FAIL
echo.
echo [ERRO] Uma dependencia falhou na verificacao SHA-256.
echo Exclua o arquivo indicado e execute este script novamente.
echo.
pause
exit /b 1

:HAVE_DEP
echo [1/4] Dependencias OK.

echo [2/4] Compilando...
if exist "%TARGET%\classes" rmdir /S /Q "%TARGET%\classes"
mkdir "%TARGET%\classes"
pushd "%SRC%" >nul
javac --release 11 -encoding UTF-8 -d "%TARGET%\classes" -cp "%JNATIVEHOOK_JAR%;%FLATLAF_JAR%" com\autoclicker\config\*.java com\autoclicker\engine\*.java com\autoclicker\macro\*.java com\autoclicker\presenter\*.java com\autoclicker\listener\*.java com\autoclicker\ui\*.java
if errorlevel 1 goto COMPILE_FAIL
popd >nul

goto MAKE_JAR

:COMPILE_FAIL
popd >nul
echo.
echo [ERRO] Falha na compilacao.
echo.
pause
exit /b 1

:VERIFY_SHA256
set "VERIFY_FILE=%~1"
set "VERIFY_HASH=%~2"
powershell -NoProfile -Command "$actual=(Get-FileHash -Algorithm SHA256 -LiteralPath $env:VERIFY_FILE).Hash; if ($actual -ne $env:VERIFY_HASH) { Write-Error ('SHA-256 invalido: ' + $env:VERIFY_FILE); exit 1 }"
exit /b %ERRORLEVEL%

:MAKE_JAR
echo [3/4] Gerando JAR...
if exist "%OUT_JAR%" del /Q "%OUT_JAR%" >nul 2>&1
jar cfe "%OUT_JAR%" com.autoclicker.ui.AutoClickerUI -C "%TARGET%\classes" .
if errorlevel 1 goto JAR_FAIL

echo.
echo OK! JAR criado:
echo   %OUT_JAR%
echo.
exit /b 0

:JAR_FAIL
echo.
echo [ERRO] Falha ao criar o JAR.
echo.
pause
exit /b 1

