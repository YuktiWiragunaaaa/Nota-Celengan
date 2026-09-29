@echo off
REM ==========================================================
REM  Cukup - bangun & pasang aplikasi Android
REM  Pembaruan dipasang menimpa versi lama: data di HP tetap aman.
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
echo   1. Pasang / perbarui di HP  (data tetap aman)
echo   2. Buat APK saja
echo   3. Jalankan tes
echo   4. Buka di emulator
echo   5. Sambungkan HP tanpa kabel (Wi-Fi)
echo   6. Ambil log error dari HP (kalau aplikasi tertutup sendiri)
echo.
set /p PILIH="Pilih 1-6: "

if "%PILIH%"=="1" goto pasang
if "%PILIH%"=="2" goto apk
if "%PILIH%"=="3" goto tes
if "%PILIH%"=="4" goto emulator
if "%PILIH%"=="5" goto wifi
if "%PILIH%"=="6" goto log
echo Pilihan tidak dikenal.
goto selesai

:pasang
"%ADB%" devices | findstr /r /c:"device$" >nul
if errorlevel 1 (
  echo.
  echo HP belum terdeteksi. Sambungkan kabel USB dan izinkan di layar HP,
  echo atau pakai menu 5 untuk sambungan Wi-Fi. Lalu jalankan lagi.
  goto selesai
)
call gradlew.bat :app:installDebug
if errorlevel 1 goto gagal
"%ADB%" shell am start -n id.cukup.debug/id.cukup.MainActivity >nul
echo.
echo Selesai. Cukup versi terbaru sudah terbuka di HP.
goto selesai

:apk
call gradlew.bat :app:assembleDebug
if errorlevel 1 goto gagal
copy /y "app\build\outputs\apk\debug\app-debug.apk" "Cukup-debug.apk" >nul
echo.
echo APK siap: %~dp0Cukup-debug.apk
echo Kirim ke HP lalu buka. Kalau Cukup sudah terpasang, pilih "Update" - data tidak hilang.
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
"%ADB%" -e wait-for-device
call gradlew.bat :app:installDebug
if errorlevel 1 goto gagal
"%ADB%" -e shell am start -n id.cukup.debug/id.cukup.MainActivity >nul
goto selesai

:wifi
echo.
echo Di HP: Setelan tambahan ^> Opsi pengembang ^> Debugging nirkabel ^> aktifkan.
echo HP dan laptop harus di Wi-Fi yang sama.
echo.
echo LANGKAH A (cukup sekali): ketuk "Sambungkan perangkat dengan kode penyambungan".
set "PAIR="
set /p PAIR="Alamat penyambungan (mis. 192.168.1.5:37123) - kosongkan jika sudah pernah: "
if "%PAIR%"=="" goto sambung
set /p KODE="Kode 6 digit: "
"%ADB%" pair %PAIR% %KODE%

:sambung
echo.
echo LANGKAH B: lihat "Alamat IP & port" di layar Debugging nirkabel.
set /p CONN="Alamat IP dan port (mis. 192.168.1.5:41234): "
"%ADB%" connect %CONN%
"%ADB%" devices
echo.
echo Kalau tertulis "device", pilih menu 1 untuk memasang.
goto selesai

:log
"%ADB%" logcat -d -b crash > "%~dp0log-crash.txt"
echo Log tersimpan di %~dp0log-crash.txt
echo Bilang ke Claude: "ada crash, cek log-crash.txt".
goto selesai

:gagal
echo.
echo Ada yang gagal. Screenshot pesan error di atas lalu kirim ke Claude.

:selesai
echo.
pause
