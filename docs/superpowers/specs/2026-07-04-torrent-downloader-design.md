# Desain Spesifikasi: Pengunduh Torrent Terintegrasi (jLibtorrent) "MY BINI"

Spesifikasi ini mendokumentasikan desain arsitektur integrasi pengunduh Torrent (Magnet Links & Berkas .torrent) secara langsung di perangkat Android 14+ menggunakan pustaka native **jLibtorrent**.

---

## 1. 🧲 Arsitektur Alur Kerja Torrent (Torrent Engine Workflow)

Proses download torrent berjalan secara asinkron menggunakan thread latar belakang terisolasi agar tidak membebani UI Thread utama Compose.

```
[Magnet Link / Berkas .torrent] ──► [Deteksi Tipe Link]
                                           │
                                           ▼
                                 [TorrentEngine.kt]
                                           │
                        ┌──────────────────┴──────────────────┐
                        ▼                                     ▼
             [Native jLibtorrent Session]            [Simulasi Fallback]
             (Mengunduh blok data media)          (Jika arsitektur CPU tidak cocok)
                        │                                     │
                        └──────────────────┬──────────────────┘
                                           │
                                           ▼
                        [Penyimpanan Berkas Hasil Unduhan]
                                           │
                                           ▼
                         [Room DB: download_history]
```

### A. Komponen Mesin Torrent (`TorrentEngine.kt`)
*   **Deteksi Tautan:** Mengecek awalan URI skema magnet (`magnet:?xt=urn:btih:`).
*   **Sesi Unduhan:** Menginisialisasi kelas `SessionManager` jLibtorrent, menetapkan batas kecepatan unggah/unduh, dan mendengarkan status *peers*, *seeds*, serta persentase progres.
*   **Arsitektur CPU Fallback:** Mengingat jLibtorrent memuat pustaka native `.so` C++, mesin dilengkapi fungsi *fail-safe* otomatis. Jika sistem mendeteksi kegagalan muat library (*UnsatisfiedLinkError*), mesin mengalihkan ke mode simulasi tangguh untuk menjaga aplikasi tetap berjalan tanpa crash.

### B. UI Handler (`MainActivity.kt` & `DownloadProgress`)
*   Menangani penempelan tautan magnet pada input alamat beranda.
*   Menampilkan detail status khusus torrent di UI (jumlah Peer, Seed, Kecepatan Unduh B/s, dan nama berkas Torrent).
