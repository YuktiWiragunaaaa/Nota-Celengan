# Pasang & update Cukup tanpa kabel

Setiap kali ada perubahan kode, GitHub otomatis membangun APK dan menaruhnya di halaman **Releases**.
Kamu tinggal mengunduhnya dari HP. Tidak perlu laptop, kabel, atau Android Studio.

## Sekali saja: pasang kunci tanda tangan

Android hanya mau menimpa (update) aplikasi kalau APK baru ditandatangani **kunci yang sama**.
Kunci ini rahasia, jadi disimpan di GitHub Secrets, bukan di dalam kode.

1. Buka file `kunci-github-secrets.txt` yang dikirim Claude.
2. Di browser, buka **github.com/YuktiWiragunaaaa/Nota-Celengan** → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**.
3. Buat secret pertama:
   - Name: `CUKUP_KEYSTORE_PASSWORD`
   - Secret: baris di bawah judul *SECRET 1*
4. Buat secret kedua:
   - Name: `CUKUP_KEYSTORE_B64`
   - Secret: seluruh baris panjang di bawah judul *SECRET 2*
5. Buka tab **Actions** → **APK** → **Run workflow** supaya APK dibangun ulang dengan kunci itu.
6. Simpan `kunci-github-secrets.txt` di tempat aman (mis. Google Drive pribadi). Kalau kuncinya hilang, update berikutnya harus uninstall dulu dan datanya ikut hilang.

## Memasang / memperbarui di HP

1. Di HP, buka **github.com/YuktiWiragunaaaa/Nota-Celengan/releases** (login GitHub dulu karena repo-nya private).
2. Buka rilis teratas (label *Latest*), ketuk file **Cukup-0.x.x-bNN.apk**.
3. Buka file yang terunduh. Kalau diminta, izinkan browser **memasang aplikasi tidak dikenal**.
4. Pertama kali: **Install**. Selanjutnya: **Update**. Data tetap aman.

> Cukup versi rilis (`id.cukup`) terpisah dari versi lama yang dipasang lewat kabel (`id.cukup.debug`).
> Setelah yang baru jalan, versi lama boleh dihapus.

## Opsional: update otomatis dengan Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) mengecek Releases dan memberi tahu kalau ada versi baru.

1. Pasang Obtainium (dari F-Droid atau halaman GitHub-nya).
2. Karena repo private, buat token dulu: GitHub → Settings → Developer settings → **Fine-grained tokens** → akses *Read-only* ke repo Nota-Celengan, izin **Contents: Read**.
3. Di Obtainium: **Add App** → URL `https://github.com/YuktiWiragunaaaa/Nota-Celengan` → isi token di *GitHub access token*.
4. Obtainium akan memasang dan memperbarui Cukup untukmu.

## Kalau gagal

- **"Aplikasi tidak terpasang" / "konflik paket"**: APK ditandatangani kunci lain (biasanya karena secret belum diisi). Isi secret, jalankan ulang workflow, lalu uninstall-install sekali lagi.
- **Build merah di tab Actions**: kirim tautannya ke Claude.
