# InkLauncher

Launcher Android ringan bergaya **Google Pixel** dengan tema **e-ink monokrom**: kertas (#F4F1EA), tinta (#111111), dan abu-abu (#5A5A5A). Tanpa warna lain, tanpa bayangan, tanpa blur.

Package: `com.taro.inklauncher` · minSdk 26 · targetSdk 35 · Kotlin + Jetpack Compose · Lisensi **GPL-3.0**

> Dirancang untuk Redmi 10 2022 (MIUI 14, Android 13) dan HP dengan RAM terbatas. APK debug ± 9 MB.

---

## Fitur

- **Tata letak ala Pixel**
  - *At a Glance* versi e-ink: tanggal besar ("Rabu, 4 Nov") plus satu baris acara kalender terdekat atau cuaca (Open-Meteo, tanpa API key). Kalender dan cuaca **default mati**, jadi tidak perlu izin atau internet.
  - Grid ikon favorit (default 4×5, bisa diatur), **tanpa label**.
  - Dock berisi 4–5 ikon dan bilah pencarian kapsul "G" dengan ikon mic dan Lens.
  - Bisa beberapa halaman yang digeser kiri/kanan (default 1).
- **Ikon Material monokrom** tanpa label. Sumber ikon dicoba berurutan:
  1. Themed icon resmi Android 13+ (`AdaptiveIconDrawable.getMonochrome()`).
  2. Peta package → ikon **Material Symbols Outlined** (telepon, pesan, kamera, galeri, dll.). Tidak memakai logo merek.
  3. Fallback grayscale: kontras dinaikkan, di-threshold, lalu diambil tepinya supaya terlihat seperti ikon garis.

  Bentuk latar ikon bisa dipilih: tanpa latar, lingkaran outline, atau squircle outline.
- **Laci aplikasi**: swipe ke atas untuk membuka. Ada bilah cari di atas, nama aplikasi hanya muncul saat mencari, dan daftar diperbarui otomatis saat aplikasi di-install/uninstall.
- **Long-press ikon**:
  - Shortcut aplikasi
  - Info aplikasi
  - Uninstall
  - Hapus dari layar utama
  - Pindahkan (tahan lalu geser, atau pilih *Pindahkan* lalu ketuk tujuan, termasuk dock)
- **Long-press area kosong**: Wallpaper, Widget, Widget kustom, Pengaturan launcher.
- **Widget sistem** (termasuk InkWidgets):
  - Pemilih widget dikelompokkan per aplikasi, lengkap dengan preview.
  - Alur bind dan konfigurasi.
  - Resize dengan pegangan (snap ke grid) dan hapus.
- **Editor widget kustom** (mirip KWGT, tapi ringan): lihat bagian di bawah.
- **Gestur launcher** (masing-masing bisa dimatikan): swipe ke atas atau tombol laci di dock membuka laci aplikasi, swipe ke bawah membuka notifikasi, ketuk dua kali mengunci layar (opsional, lewat layanan aksesibilitas).
- **Cadangkan & pulihkan** tata letak, widget kustom, dan pengaturan ke file JSON.

## Screenshot

| Layar utama | Laci | Editor widget |
|---|---|---|
| _(placeholder)_ | _(placeholder)_ | _(placeholder)_ |

## Download APK

APK dibuat otomatis oleh GitHub Actions setiap kali ada push ke `main`.

1. Buka tab **[Actions](https://github.com/Clarity69/ink-launcher/actions)** di repo ini.
2. Pilih run **Build APK** teratas yang berstatus hijau (✓).
3. Di bagian **Artifacts**, unduh **InkLauncher-apk**. Anda perlu login GitHub.
4. Ekstrak file zip; isinya `app-debug.apk`.

Build manual: tab Actions → **Build APK** → **Run workflow**.

## Cara install

1. Pindahkan `app-debug.apk` ke HP, lalu buka dari File Manager.
2. Izinkan *Instal aplikasi tidak dikenal* untuk File Manager bila diminta.
3. Di MIUI, jika muncul peringatan keamanan, pilih **Tetap instal**.

## Menjadikan launcher default di MIUI

**Setelan → Aplikasi → Kelola aplikasi → (⋮) Aplikasi default → Layar utama → InkLauncher**

Cara lain: buka ikon **Pengaturan InkLauncher** di launcher lama → **Jadikan launcher default**.

## Widget kustom

Widget kustom dirender langsung oleh InkLauncher dengan Compose, bukan sebagai AppWidget, sehingga ringan dan selalu monokrom.

**Membuat widget:**

1. Tahan area kosong → **Widget kustom** → **+**.
2. Atur ukuran widget dalam sel grid (misalnya 4×2). Pratinjau langsung tampil di atas.
3. Tambahkan layer:
   - **Teks**
   - **Kotak**: persegi/kapsul/lingkaran, isi atau outline
   - **Ikon** Material Symbols
   - **Jam dot-matrix** (5×7, 24/12 jam)
   - **Garis pemisah**
   - **Progres titik** (baterai/hari/tahun)
4. Atur setiap layer:
   - Posisi dan ukuran (% atau dp), perataan
   - Font (sans/serif/mono), ukuran font
   - Warna: hanya Ink/Paper/Muted/transparan
   - Aksi tap: buka aplikasi, Google Search, Google Lens, jam, atau kalender
5. Widget tersimpan otomatis. Kembali ke daftar, ketuk **⋮ → Tambah ke layar utama**.

**Token teks** (diganti otomatis; format tanggal mengikuti locale):

`{time}` `{time12}` `{hour}` `{minute}` `{date}` `{day}` `{month}` `{year}` `{battery}` `{charging}` `{next_alarm}`

Token waktu diperbarui setiap menit hanya saat launcher terlihat, lalu berhenti saat launcher tidak tampil agar hemat baterai.

**Preset bawaan:**

1. Jam dot-matrix + tanggal
2. Bilah pencarian G + Lens
3. Jam serif besar + baterai

**Bagikan widget:**

- **Ekspor**: ⋮ → **Ekspor** menyimpan file `.inkw` (JSON).
- **Impor**: ikon impor di kanan atas layar Widget kustom → pilih file `.inkw`.

## Struktur proyek

```
app/src/main/java/com/taro/inklauncher/
├── MainActivity.kt, LauncherViewModel.kt, InkApp.kt
├── ui/   theme, home, drawer, glance, search, settings, widgeteditor, common
├── data/ AppRepository, IconProcessor, MaterialIconMap, Prefs, LayoutStore, GlanceProvider, Backup
├── widget/ WidgetHostManager, custom/ (model JSON, renderer, token, font dot-matrix, preset)
└── system/ LaunchHelper, StatusBarHelper
```

Build hanya lewat GitHub Actions (`.github/workflows/build.yml`). Jika `gradle-wrapper.jar` belum ada, workflow membuatnya terlebih dahulu dengan `gradle wrapper`.

## Lisensi & atribusi

- **InkLauncher** © Taro, dirilis di bawah **GNU GPL v3.0** (lihat [LICENSE](LICENSE)).
- **[Lawnchair Launcher](https://github.com/LawnchairLauncher/lawnchair)** (GPL-3.0): beberapa pendekatan diadaptasi dan ditulis ulang secara ringan dengan Compose, yaitu themed/monochrome icon, intent Google Search/voice/Lens, gestur buka notifikasi, dan alur widget host. File yang mengadaptasi kode Lawnchair diberi komentar sumber di bagian atasnya. Modul Launcher3/AOSP dan Quickstep tidak dipakai.
- **[Material Symbols](https://github.com/google/material-design-icons)** oleh Google, Apache License 2.0 ([licenses/Apache-2.0-MaterialSymbols.txt](licenses/Apache-2.0-MaterialSymbols.txt)).
- **Font** [Figtree](https://github.com/erikdkennedy/figtree) dan [Instrument Serif](https://github.com/Instrument/instrument-serif), SIL Open Font License 1.1 ([licenses/](licenses/)).
- Data cuaca dari **[Open-Meteo](https://open-meteo.com/)** (CC BY 4.0).
