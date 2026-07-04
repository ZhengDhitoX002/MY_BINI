# Desain Spesifikasi: Ekspor-Impor Cadangan Terenkripsi "MY BINI"

Spesifikasi ini mendokumentasikan desain arsitektur untuk sistem pencadangan dan pemulihan data luring terenkripsi (Backup & Restore) dengan ekstensi kustom `.mybini` pada Android 14+.

---

## 1. 💾 Arsitektur Ekspor-Impor Cadangan (Backup Architecture)

Sistem pencadangan mengompresi database SQLCipher (riwayat & berkas bilik rahasia) serta seluruh file media terunduh menjadi satu berkas arsip terpadu.

```
[Database & File Media] ──► [java.util.zip.ZipOutputStream] ──► [Enkripsi AES File ZIP] ──► [Berkas Cadangan (.mybini)]
                                                                                                    │
                                                                                                    ▼
                                                                                            [Google Drive / SD]
```

### A. Komponen Manajer Cadangan (`BackupManager.kt`)
*   **Arsip ZIP Dinamis:** Menggunakan pustaka standar Java `ZipOutputStream` untuk mengemas berkas database SQLite/SQLCipher (`mybini_db`, `mybini_secure.db`, `mybini_decoy.db`) beserta seluruh file video/audio fisik hasil unduhan secara rekursif luring.
*   **Enkripsi Kunci PIN:** Konten arsip disandi secara aman menggunakan kunci turunan dari PIN utama pengguna agar data tetap privat bahkan jika berkas `.mybini` bocor ke publik.

### B. Antarmuka UI (`SettingsScreen.kt`)
*   Menambahkan opsi kartu aksi di bagian bawah halaman pengaturan:
    1.  **Ekspor Cadangan (.mybini):** Menyimpan berkas cadangan ke folder publik `Documents/MY_BINI_BACKUPS/`.
    2.  **Impor Pemulihan:** Membaca berkas `.mybini`, memasukkan PIN pemulihan, mendekripsi kontainer ZIP, dan menulis ulang database serta file media luring secara aman.
