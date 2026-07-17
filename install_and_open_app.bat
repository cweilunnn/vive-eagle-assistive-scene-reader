@echo off
setlocal

set "JAVA_HOME=D:\Android Studio\jbr"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"
set "GRADLE=%USERPROFILE%\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat"
set "APK=app\build\outputs\apk\debug\app-debug.apk"
set "PACKAGE=tw.edu.nchu.viveeagle.assistivereader"
set "ACTIVITY=.MainActivity"

cd /d "%~dp0"

echo Building app...
call "%GRADLE%" assembleDebug --offline
if errorlevel 1 goto failed

echo Checking Android device...
adb devices

echo Installing app...
adb install -r "%APK%"
if errorlevel 1 goto failed

echo Opening app...
adb shell am start -n "%PACKAGE%/%ACTIVITY%"
if errorlevel 1 goto failed

echo Done. The app is open on your phone.
pause
exit /b 0

:failed
echo.
echo Failed. Please check that the phone is connected, unlocked, and USB debugging is allowed.
pause
exit /b 1
