@echo off
setlocal

echo ========================================
echo  J2ME Launcher - Build EXE + JRE
echo ========================================
echo.

set APP_NAME=J2MELauncher
set APP_VERSION=1.0.2
set MAIN_CLASS=me.kitakeyos.j2me.application.MainApplication
set MAIN_JAR=%APP_NAME%-%APP_VERSION%-jar-with-dependencies.jar
set DEST_DIR=dist

:: Step 1: Build fat JAR
echo [1/4] Building fat JAR with Maven...
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo ERROR: Maven build failed!
    exit /b 1
)
echo       Done: target\%MAIN_JAR%

:: Step 2: Prepare staging directory (only fat JAR)
echo [2/4] Preparing staging directory...
if exist "target\staging" rmdir /s /q "target\staging"
mkdir "target\staging"
copy "target\%MAIN_JAR%" "target\staging\" >nul

:: Step 3: Clean previous build
echo [3/4] Cleaning previous build...
if exist "%DEST_DIR%\%APP_NAME%" (
    rmdir /s /q "%DEST_DIR%\%APP_NAME%"
)

:: Step 4: Run jpackage
echo [4/4] Running jpackage...
jpackage ^
    --type app-image ^
    --name %APP_NAME% ^
    --app-version %APP_VERSION% ^
    --input target\staging ^
    --main-jar %MAIN_JAR% ^
    --main-class %MAIN_CLASS% ^
    --dest %DEST_DIR% ^
    --icon src\main\resources\icons\app.ico ^
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" ^
    --java-options "--add-opens java.base/java.lang.reflect=ALL-UNNAMED" ^
    --java-options "--add-opens java.base/java.io=ALL-UNNAMED"

if errorlevel 1 (
    echo ERROR: jpackage failed!
    exit /b 1
)

:: Cleanup staging
rmdir /s /q "target\staging"

echo.
echo ========================================
echo  Build complete!
echo  Output: %DEST_DIR%\%APP_NAME%\%APP_NAME%.exe
echo ========================================
