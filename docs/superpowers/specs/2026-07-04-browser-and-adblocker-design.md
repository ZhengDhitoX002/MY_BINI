# Desain Spesifikasi: Built-in Browser, Link Sniffer & Ad-Blocker "MY BINI"

Spesifikasi ini mendokumentasikan desain arsitektur untuk penjelajah web internal (Built-in Browser) yang dilengkapi pendeteksi tautan video/audio otomatis (Link Sniffer) serta mesin pemblokir iklan (Ad-Blocker) berkinerja tinggi untuk Android 14+.

---

## 1. 🌐 Arsitektur Built-in Browser

Browser menggunakan **Android WebView** yang diintegrasikan secara mulus ke dalam tata letak Jetpack Compose.

```
[UI Browser (Address Bar, Nav)] ──► [Android WebView] ──► [WebViewClient / WebChromeClient]
                                                                  │
                                                                  ├──► [AdBlocker.shouldBlock(url)]
                                                                  └──► [LinkSniffer.sniff(url)] ──► [Trigger Floating Download FAB]
```

### A. Komponen UI (`BrowserScreen.kt`)
*   **Bilah Alamat (Address Bar):** Input teks dinamis untuk mengetik URL atau kata kunci pencarian (otomatis dialihkan ke Google Search jika bukan URL valid).
*   **Tombol Navigasi:** Back, Forward, Reload, dan Home.
*   **Indikator Pemuatan (Linear Progress Bar):** Menampilkan persentase loading halaman situs web secara real-time.
*   **Floating Action Button (FAB) Unduhan:** Tombol melayang semi-transparan dengan efek kaca buram (Glassmorphism). Tombol ini akan tersembunyi secara default, dan **hanya akan muncul/memantul (bouncing animation)** saat Link Sniffer mendeteksi adanya tautan media siap unduh di halaman yang sedang aktif.

---

## 2. 🛡️ Mesin Pemblokir Iklan (Ad-Blocker Engine)

Untuk menjaga performa browser tetap ringan dan menghemat pemakaian RAM/CPU:
*   **Komponen:** `AdBlocker.kt`
*   **Daftar Blokir:** Memuat daftar domain iklan terkompilasi (versi ringkas dari EasyList) yang disimpan dalam folder assets aplikasi (`ads-blocklist.txt`).
*   **Intersepsi:** Memanfaatkan metode `shouldInterceptRequest` pada `WebViewClient`:
    ```kotlin
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()
        if (AdBlocker.shouldBlock(url)) {
            // Kembalikan respon kosong (blokir request iklan)
            return WebResourceResponse("text/javascript", "UTF-8", ByteArrayInputStream(ByteArray(0)))
        }
        return super.shouldInterceptRequest(view, request)
    }
    ```
*   **Pencocokan Cepat:** Menggunakan struktur data **HashSet** untuk pencocokan nama domain secara instan ($O(1)$) sebelum memproses request, sehingga tidak ada lag saat memuat halaman situs web.

---

## 3. 🔍 Pendeteksi Tautan Media (Link Sniffer)

Sniffer bertugas menangkap tautan langsung media yang dialirkan oleh situs web:
*   **Komponen:** `LinkSniffer.kt`
*   **Kriteria Deteksi:**
    *   **Pola Ekstensi:** Mencocokkan URL request dengan ekspresi reguler (Regex) untuk tipe berkas video/audio: `.*\\.(mp4|m3u8|mp3|m4a|aac|ogg|webm|mov)(\\?.*)?$`
    *   **MIME-Type Interception:** Memeriksa header `Content-Type` yang dikembalikan oleh request jaringan (misal: `video/mp4`, `application/x-mpegURL` untuk HLS stream).
*   **Pemberitahuan UI:** Ketika tautan terdeteksi, `LinkSniffer` memicu callback yang mengirimkan URL, Judul Halaman Web, dan estimasi tipe media ke `BrowserScreen` untuk menghidupkan tombol download melayang.
