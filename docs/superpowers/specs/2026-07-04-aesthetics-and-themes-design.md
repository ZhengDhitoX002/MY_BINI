# Desain Spesifikasi: Kustomisasi Tema Premium & Latar Belakang "MY BINI"

Spesifikasi ini mendokumentasikan desain visual untuk sistem tema kustom dinamis (Premium Themes) dan efek visual transisi premium pada aplikasi Android MY BINI.

---

## 1. 🎨 Sistem Tema Kustom Dinamis (Dynamic Theme Engine)

Kami membuat sistem tema modular yang mengubah skema warna latar belakang gradien dan warna utama (*Primary Accent*) secara real-time di seluruh aplikasi.

```
[SettingsScreen: Pilih Tema] ──► [Kirim Perubahan State] ──► [MainScreen / MainActivity]
                                                                  │
                                                                  ├──► [Update Gradien Latar Belakang]
                                                                  └──► [Update Warna Aksen Tombol & Nav]
```

### A. Tipe Tema yang Didukung (`AppTheme.kt`)
1.  **Dark Knight (Default):** Gradien abu-abu gelap/hitam karbon berkelas dengan aksen biru/putih bersih.
2.  **Cyberpunk Neon:** Gradien ungu tua ke ungu neon gelap dengan aksen pink neon menyala yang futuristik.
3.  **Pastel Gold:** Gradien merah muda lembut (*rose-gold*) ke warna kuning persik (*peach/amber*) hangat yang mewah.

---

## 2. ⚙️ Halaman Pengaturan & Tema (`SettingsScreen.kt`)

*   **Pilihan Tema:** Menu pilihan radio-button berdesain kartu M3 modern dengan pratinjau warna visual mini.
*   **Pengaturan Lainnya:** Sakelar (*toggle*) batas kecepatan unduh dan opsi hapus instan seluruh berkas tersimpan untuk kenyamanan privasi.
*   **State Management:** State disimpan menggunakan status `remember` di tingkat tertinggi `MainScreen` dan ditransfer ke layar anak untuk re-render tampilan instan saat tema berganti.
