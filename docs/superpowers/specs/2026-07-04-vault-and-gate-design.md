# Desain Spesifikasi: UI Bilik Rahasia & Gerbang Autentikasi "MY BINI"

Spesifikasi ini mendokumentasikan desain antarmuka dan logika gerbang keamanan untuk **Bilik Rahasia (Private Vault)** yang mendukung PIN ganda (PIN Utama vs PIN Umpan/Decoy) di Android 14+.

---

## 1. 🔒 Alur Kerja Gerbang Keamanan (Gate Logic)

Gerbang Bilik Rahasia memproteksi akses database Room terenkripsi SQLCipher dan kunci AES-256 berkas.

```
[Buka Tab Vault] ──► [Apakah PIN Sudah Dibuat?]
                             │
                             ├──► [BELUM] ──► [Setup PIN Screen (Input PIN Baru)]
                             │
                             └──► [SUDAH] ──► [Otentikasi PIN Screen / Biometrik]
                                                     │
                                                     ├──► [PIN Utama] ──► Muat DB Utama (File Asli)
                                                     └──► [PIN Umpan] ──► Muat DB Umpan (File Palsu)
```

### A. Komponen Layar Otentikasi (`VaultGateScreen.kt`)
*   **Keypad Angka Kustom:** Tata letak grid Compose (1-9, hapus, 0, konfirmasi) agar tidak memicu keyboard sistem, menjaga keamanan dari keylogger perangkat lunak pihak ketiga.
*   **Integrasi Biometrik:** Tombol pintas sidik jari/wajah yang terikat dengan `BiometricPrompt` dan Android Keystore untuk mengambil kunci enkripsi utama.

### B. Komponen Layar Bilik Rahasia (`VaultScreen.kt`)
*   Menampilkan galeri berkas yang terenkripsi.
*   Menyediakan tombol "Hapus Dekripsi" untuk mengembalikan berkas dari bilik rahasia menjadi berkas biasa di penyimpanan telepon umum.
*   Memutar video/audio privat secara instan menggunakan `VideoPlayerScreen` dengan parameter `isEncrypted = true` (mengalirkan dekripsi byte data di RAM).

---

## 2. 🗄️ Manajemen Database Terpisah (Decoy Database Handling)

Untuk merealisasikan fitur PIN Umpan (*Decoy PIN*):
*   Aplikasi memelihara dua nama file database terpisah di penyimpanan internal:
    1.  `mybini_secure.db` (Database Utama)
    2.  `mybini_decoy.db` (Database Umpan/Tiruan)
*   Ketika otentikasi berhasil:
    *   Jika PIN Utama dimasukkan -> Panggil `MyBiniDatabase.getDatabase(context, key, "mybini_secure.db")`.
    *   Jika PIN Umpan dimasukkan -> Panggil `MyBiniDatabase.getDatabase(context, key, "mybini_decoy.db")`.
*   Sistem ini menjamin bahwa orang luar yang memaksa pengguna membuka bilik rahasia tidak akan pernah bisa melihat metadata maupun fisik berkas asli karena database yang terbuka benar-benar kosong/palsu di tingkat sistem penyimpanan.
