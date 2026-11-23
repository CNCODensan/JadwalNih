# JadwalNih v1.0

**JadwalNih** adalah aplikasi pengatur jadwal harian, prioritas Kuadran Eisenhower (K1–K4), timer fokus bebas, rekap foto harian, serta pengingat ibadah & tema Hari Raya otomatis untuk Android berbasis Kotlin & Jetpack Compose.

---

## Fitur Utama v1.0

### 1. Jadwal Fleksibel & Kuadran Prioritas (K1–K4)
- **Kelompok prioritas K1–K4**: Penting & Mendesak (K1), Penting & Kapan Aja (K2), Biasa & Mendesak (K3), hingga Biasa & Santai (K4).
- **Tampilan Harian, Mingguan, dan Bulanan**: Dilengkapi ringkasan beban waktu harian dan indikator bentrok jam otomatis.
- **Bayangan Jadwal Berulang (4 Minggu Ke Depan)**: Jadwal berulang mingguan otomatis menampilkan kartu bayangan samar hingga 4 minggu mendatang, bisa diaktifkan & diedit lebih awal atau dilewati untuk minggu tertentu, serta otomatis terwujud saat minggu tiba.
- **Konfirmasi Hapus Aman + Urungkan**: Setiap penghapusan (jadwal satuan, seluruh seri berulang, sub-langkah, draf, foto rekap, hingga riwayat notifikasi) meminta konfirmasi terlebih dahulu dan menyediakan tombol **Urungkan** singkat.

### 2. Impor Multi-Jadwal Lewat AI + 136 Ikon Otomatis
- **Impor banyak jadwal sekaligus**: Tempel array JSON hasil AI untuk banyak kegiatan lintas hari/minggu.
- **Pratinjau Edit Langsung**: Ubah judul, jam mulai/selesai, kuadran K1–K4, atau ikon sebelum menyimpan massal.
- **Deteksi & Pilihan Bentrok**: Saat ada jam yang bertabrakan dengan jadwal lama, pilih **Timpa jadwal lama**, **Simpan yang lama saja**, atau **Biarkan bentrok**.
- **136 Ikon Jadwal**: Pemilihan ikon otomatis berdasarkan kata kunci judul Bahasa Indonesia/Inggris, lengkap dengan pencari ikon di editor dan dukungan ikon pada Widget & layar Fokus.

#### Template Prompt & Format JSON Impor AI
Salin prompt siap pakai langsung dari tab **Otomatis (AI)** di dalam aplikasi, atau gunakan format JSON berikut:

```json
[
  {
    "title": "Rapat Koordinasi Proyek",
    "date": "2026-11-23",
    "startTime": "09:00",
    "endTime": "10:30",
    "priority": "K1",
    "icon": "meeting_room",
    "location": "Ruang Rapat Lantai 2",
    "reminderMinutes": 15,
    "notes": "Siapkan slide progres mingguan",
    "checklist": ["Cek laporan tim", "Kirim agenda"]
  }
]
```

### 3. Pusat Notifikasi & Pemulihan 5 Draf
- Ikon lonceng di sebelah ikon Pengaturan menyimpan riwayat pengingat tugas, waktu ibadah, Hari Raya (otomatis dibersihkan setelah 30 hari), dan hingga **5 draf edit terakhir** agar tidak hilang jika editor tertutup tanpa sengaja.

### 4. Pengingat Sholat 5 Waktu & Ibadah Multi-Agama
- Mendukung pilihan **Islam** (5 waktu sholat harian via API Aladhan dengan cadangan perhitungan astronomi matahari lokal/offline), serta jadwal doa harian & ibadah mingguan untuk **Kristen**, **Katolik**, **Hindu**, **Buddha**, dan **Konghucu**.
- Dapat dicentang selesai lengkap dengan hitung mundur waktu berikutnya, suara/mode senyap di Pengaturan, kartu ringkasan mingguan di tab **Analitik**, pengingat lembut yang tidak memutus timer di tab **Fokus**, serta tombol ganda langsung di **Widget** layar utama.

### 5. Hari Raya Nasional & Tema Dinamis
- Mengambil daftar hari libur nasional & hari raya dari API publik (dengan cadangan kalender lokal).
- Tema warna, ornamen header, spanduk sapaan, dan latar samar daftar jadwal otomatis menyesuaikan tema **Kemerdekaan RI (Merah Putih)**, **Idul Fitri**, **Imlek**, **Natal**, maupun **Nyepi / Waisak** saat hari H — atau bisa dicoba kapan saja lewat opsi Pratinjau di Pengaturan.

### 6. Lokasi GPS Otomatis, Timer Fokus & Rekap Foto
- **Lokasi Perkiraan (GPS) & Manual**: Meminta izin `ACCESS_COARSE_LOCATION` satu kali setelah animasi pembuka selesai, disertai fallback kota terakhir / pengisian manual di Pengaturan. Dipakai untuk nama lokasi jadwal, metadata lokasi di Rekap Foto (galeri & lightbox), dan akurasi waktu sholat.
- **Rekap Foto Harian**: Kamera bawaan CameraX + Photo Picker (tanpa izin baca galeri luas), cap waktu & gaya teks, kolase JPEG, dan metadata lokasi pengambilan foto.

---

## Ringkasan Privasi & Izin
- **Semua data utama tersimpan lokal** di perangkat pengguna (Room Database & DataStore).
- **Kamera (`CAMERA`)**: Hanya aktif saat mengambil foto di tab Rekap; memilih foto dari galeri memakai Android Photo Picker tanpa izin penyimpanan luas.
- **Lokasi Perkiraan (`ACCESS_COARSE_LOCATION`)**: Opsional; hanya untuk menamai tempat jadwal/rekap dan akurasi jam sholat.
- **Notifikasi (`POST_NOTIFICATIONS`)**: Opsional; untuk pengingat jadwal, konfirmasi kehadiran, dan pengingat ibadah.
- Tersedia fitur **Ekspor & Impor Cadangan JSON** lengkap di Pengaturan (mencakup jadwal, sesi fokus, rekap foto, pengaturan agama/lokasi, serta riwayat notifikasi).

---

## Teknologi
- **Bahasa**: Kotlin
- **UI**: Jetpack Compose + Material Design 3 (`JadwalNihTheme`)
- **Database & Preferensi**: Room (`AppDatabase` v5), Jetpack DataStore
- **Kamera & Media**: CameraX, Coil, Android Photo Picker
- **Widget**: Jetpack Glance (`PriorityQuadrantWidget`)
- **Pengujian**: JUnit 4 + Robolectric + Compose UI Test + Glance AppWidget Unit Test

---

## Build & Pengujian

Jalankan pengujian unit & Robolectric:
```bash
gradle :app:testDebugUnitTest
```

Bangun APK debug:
```bash
gradle assembleDebug
```
