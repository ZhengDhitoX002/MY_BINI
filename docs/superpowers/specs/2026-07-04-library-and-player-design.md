# Desain Spesifikasi: Perpustakaan Offline & Pemutar Media (ExoPlayer) "MY BINI"

Spesifikasi ini mendokumentasikan arsitektur untuk **Perpustakaan Offline (Offline Library)** dan integrasi **Pemutar Media (ExoPlayer)** luring yang aman di Android 14+.

---

## 1. 📂 Arsitektur Perpustakaan Offline (Library)

Perpustakaan offline membaca rekaman riwayat unduhan dari Room database dan menampilkannya kepada pengguna dalam bentuk list kartu yang rapi.

```
[MyBiniDatabase.downloadDao()] ──► [Flow<List<DownloadItem>>] ──► [LibraryScreen UI]
                                                                        │
                                                                        ▼
                                                             [Klik Item / Putar Media]
                                                                        │
                                                                        ▼
                                                          [VideoPlayerScreen / ExoPlayer]
```

### A. Tampilan Perpustakaan (`LibraryScreen.kt`)
*   Membaca data riwayat unduhan secara asinkron menggunakan Coroutines Flow.
*   Menampilkan daftar berkas berdasarkan tanggal unduhan terbaru.
*   Menyediakan tombol "Hapus" (yang akan menghapus data di database Room sekaligus menghapus berkas fisik di penyimpanan lokal).

---

## 2. 🎬 Integrasi Pemutar Media (ExoPlayer & Secure Playback)

Pemutar video menggunakan pustaka **Jetpack Media3 ExoPlayer** dengan kustomisasi penyuplai data (DataSource):

### A. Alur Kerja Deteksi Sumber Berkas
*   **Berkas Biasa (Terbuka):** Diputar menggunakan `DefaultDataSource.Factory` standar untuk efisiensi pemutaran berkas mentah.
*   **Berkas Enkripsi (Bilik Rahasia):** Menggunakan **`AesCtrDataSource`** kustom yang didekripsi secara langsung di memori RAM tanpa menyimpan teks biasa di disk.
    ```kotlin
    val dataSourceFactory = DataSource.Factory {
        if (isEncrypted) {
            AesCtrDataSource(secretKey, baseIv)
        } else {
            DefaultDataSource.Factory(context).createDataSource()
        }
    }
    ```

### B. Antarmuka Pemutar Video (`VideoPlayerScreen.kt`)
*   Menggunakan `AndroidView` untuk menempelkan `PlayerView` Media3 Compose.
*   Mendukung rotasi layar otomatis luring.
*   Mendukung kontrol gerakan (pinch-to-zoom, ketuk ganda untuk mempercepat/mundur).
*   Manajemen daur hidup (*lifecycle handling*) yang tepat untuk melepaskan resource ExoPlayer (`player.release()`) saat pengguna menutup video untuk mencegah kebocoran memori RAM.
