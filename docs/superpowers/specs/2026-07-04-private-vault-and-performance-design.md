# Desain Spesifikasi: Bilik Rahasia (Private Vault) & Optimasi Performa Ringan "MY BINI"

Dokumen ini mendokumentasikan spesifikasi teknis untuk fitur keamanan **Bilik Rahasia (Private Vault)** dengan enkripsi tingkat militer dan strategi **Optimasi Performa** agar aplikasi tetap berukuran kecil (<20MB APK) dan hemat RAM/baterai.

---

## 1. 🔒 Arsitektur Keamanan & Enkripsi Bilik Rahasia

Bilik Rahasia menggunakan pendekatan **Hardware-Backed Authenticated Encryption** dengan alur kerja sebagai berikut:

```
[User PIN / Biometrik]
       │
       ▼
[PBKDF2 Key Derivation] + [Salt] ──► [AES-256-GCM Master Key] ──► [Android Keystore (TEE/StrongBox)]
                                                                           │
                                                                           ▼
                                                                  [Deksripsi File di RAM] (On-The-Fly)
                                                                           │
                                                                           ▼
                                                                   [ExoPlayer / Visual]
```

### A. Mekanisme Enkripsi File
*   **Algoritma:** AES-256 dalam mode GCM (Galois/Counter Mode). GCM dipilih karena menyediakan *Authenticated Encryption* (menolak file jika dirusak di luar aplikasi) dan memiliki akselerasi hardware pada prosesor ARM modern.
*   **Key Derivation:** Kunci AES tidak disimpan secara langsung. Saat pengguna membuat PIN, kunci diturunkan menggunakan fungsi **PBKDF2 dengan SHA-256**, salt acak sepanjang 16-byte, dan 10.000 iterasi.
*   **Android Keystore:** Kunci master ditaruh di dalam Android Keystore yang di-back oleh hardware aman (**TEE** atau **StrongBox** pada perangkat yang mendukung). Akses ke kunci ini dapat dijaga oleh otorisasi sidik jari/wajah via `BiometricPrompt`.

### B. In-Memory Decryption & Playback (Streaming Tanpa Cache Disk)
Untuk mencegah kebocoran data di memori penyimpanan (*storage*), video dan audio di dalam Bilik Rahasia tidak pernah didekripsi menjadi file biasa di disk saat diputar.
*   **Teknologi:** Kustom `AesGcmDataSource` yang mengimplementasikan antarmuka `androidx.media3.datasource.DataSource` ExoPlayer.
*   **Cara Kerja:** ExoPlayer meminta rentang byte tertentu dari video -> `AesGcmDataSource` membaca potongan byte terenkripsi dari file -> melakukan dekripsi AES-GCM secara langsung di memori RAM -> menyerahkan byte bersih ke ExoPlayer.

### C. Sistem PIN Umpan (Decoy Vault)
Untuk melindungi pengguna dari pemaksaan pihak luar:
*   Aplikasi menyimpan dua salt dan hash PIN di database SQLCipher terpisah.
*   **PIN Utama:** Membuka database file asli.
*   **PIN Umpan (Decoy):** Membuka database tiruan yang kosong atau hanya berisi file dekoratif biasa yang tidak sensitif.

---

## 2. ⚡ Strategi Aplikasi Ringan (Lightweight Optimization)

Meskipun memiliki fitur kompleks (AI, Torrent, FFmpeg, WebView), aplikasi "MY BINI" dirancang untuk tetap hemat sumber daya menggunakan metode berikut:

### A. Dynamic Asset Delivery (Unduh Dinamis)
Untuk menjaga ukuran awal APK di bawah **20 MB**:
*   **Whisper AI Model:** File model `.tflite` (sekitar ~40MB) tidak dimasukkan ke dalam paket APK. Saat pengguna pertama kali menekan tombol "Transkripsi AI", aplikasi menampilkan unduhan sekali pakai yang mengambil model dari Firebase Storage dan menyimpannya di folder internal aplikasi.
*   **Ad-Blocker EasyList:** File teks filter iklan (~5MB) diunduh secara dinamis saat browser pertama kali dibuka dan diperbarui otomatis seminggu sekali di background.
*   **Gemini Nano:** Nol megabyte footprint karena aplikasi memanggil **Google AICore** bawaan OS Android secara lokal tanpa membundel model LLM ke dalam aplikasi.

### B. Konfigurasi Gradle ABI Splits (Android App Bundle)
Untuk memangkas ukuran pustaka C++ bawaan FFmpeg dan jLibtorrent:
```kotlin
android {
    bundle {
        abi {
            enableSplit = true
        }
    }
}
```
Saat dipublikasikan sebagai `.aab` ke Google Play Store, Google akan membagi aplikasi menjadi potongan arsitektur CPU (arm64-v8a, armeabi-v7a, x86_64). Pengguna hanya mengunduh potongan yang sesuai dengan HP mereka, menghemat ukuran download hingga 70%.

### C. Manajemen RAM WebView & Threading
*   **WebView Lifecycle:** WebView browser akan dihancurkan secara total dari memori RAM (`webView.destroy()`) seketika setelah pengguna keluar dari tab Browser. Tidak ada proses background browser yang dibiarkan hidup.
*   **Non-Blocking IO:** Database Room (SQLCipher) dan pemrosesan file enkripsi dipaksa berjalan pada `Dispatchers.IO` menggunakan Kotlin Coroutines agar tidak membebani UI Thread utama, mencegah aplikasi lag atau membeku (ANR).
