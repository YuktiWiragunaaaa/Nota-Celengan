@echo off
REM ==========================================================
REM  Cukup - bangun & pasang aplikasi Android
REM  Pilih salah satu menu di bawah.
REM ==========================================================
setlocal
cd /d "%~dp0"

for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%J"
set "ANDROID_HOME=D:\Android\Sdk"
set "JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=D:/Android/tmp"
if not exist "D:\Android\tmp" mkdir "D:\Android\tmp"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"

echo.
echo   CUKUP
echo   -----
echo   1. Pasang ke HP (kabel USB)
echo   2. Buat APK saja
echo   3. Jalankan tes
echo   4. Buka di emulator
echo.
set /p PILIH="Pilih 1-4: "

if "%PILIH%"=="1" goto pasang
if "%PILIH%"=="2" goto apk
if "%PILIH%"=="3" goto tes
if "%PILIH%"=="4" goto emulator
echo Pilihan tidak dikenal.
goto selesai

:pasang
"%ADB%" devices | findstr /r /c:"device$" >nul
if errorlevel 1 (
  echo.
  echo HP belum terdeteksi. Sambungkan kabel USB, aktifkan USB debugging,
  echo lalu izinkan komputer ini di layar HP. Setelah itu jalankan lagi.
  goto selesai
)
call gradlew.bat :app:installDebug
if errorlevel 1 goto gagal
"%ADB%" shell am start -n id.cukup.debug/id.cukup.MainActivity >nul
echo.
echo Selesai. Cukup sudah terbuka di HP.
goto selesai

:apk
call gradlew.bat :app:assembleDebug
if errorlevel 1 goto gagal
copy /y "app\build\outputs\apk\debug\app-debug.apk" "Cukup-debug.apk" >nul
echo.
echo APK siap: %~dp0Cukup-debug.apk
echo Kirim ke HP lalu buka untuk memasang (izinkan "sumber tidak dikenal").
goto selesai

:tes
call gradlew.bat :domain:test
if errorlevel 1 goto gagal
echo.
echo Semua tes lulus.
goto selesai

:emulator
start "" "%ANDROID_HOME%\emulator\emulator.exe" -avd Cukup_Pixel
echo Menunggu emulator menyala...
"%ADB%" wait-for-device
call gradlew.bat :app:installDebug
if errorlevel 1 goto gagal
"%ADB%" shell am start -n id.cukup.debug/id.cukup.MainActivity >nul
goto selesai

:gagal
echo.
echo Ada yang gagal. Screenshot pesan error di atas lalu kirim ke Claude.

:selesai
echo.
pause
