# Desain Spesifikasi: Offline AI Engine (Whisper & Gemini Nano) "MY BINI"

Spesifikasi ini mendokumentasikan desain integrasi model kecerdasan buatan lokal (100% luring) untuk transkripsi suara (Whisper TFLite) dan peringkasan teks (Google Play Services AICore / Gemini Nano) di Android 14+.

---

## 1. 🧠 Arsitektur Alur Kerja AI Lokal (Offline AI Workflow)

Fitur AI pada MY BINI berjalan sepenuhnya secara lokal di perangkat tanpa mengirim data suara pengguna ke server luar, menjaga privasi mutlak.

```
[Berkas Audio/Video] ──► [Ekstrak PCM Audio 16kHz]
                                │
                                ▼
                       [WhisperTranscriber] (TFLite Inference)
                                │
                                ▼
                         [Teks Transkrip]
                                │
                                ▼
                       [GeminiNanoWrapper] (AICore / Local LLM)
                                │
                                ▼
                         [Ringkasan Teks]
```

### A. Transkriptor Suara Lokal (`WhisperTranscriber.kt`)
*   **Model:** Whisper-Tiny terenkapsulasi dalam format `.tflite` (~40MB) yang diunduh dinamis via `DynamicAssetManager`.
*   **Pra-pemrosesan:** Mengonversi berkas audio hasil unduhan menjadi format PCM linear 16-bit dengan frekuensi sampel 16 kHz (format wajib untuk Whisper).
*   **Inferensi:** Memuat model dengan `org.tensorflow.lite.Interpreter` dan menjalankan model untuk memprediksi token teks.

### B. Peringkas Teks Lokal (`GeminiNanoWrapper.kt`)
*   **Teknologi:** Memanfaatkan Google Play Services **AICore** API bawaan OS Android 14+ untuk memanggil model **Gemini Nano** secara lokal di HP.
*   **Keuntungan:** Bebas biaya API, tidak membebani memori instalasi APK, dan berjalan dengan akselerasi perangkat keras NPU (Neural Processing Unit) telepon.

---

## 2. 📁 Rencana Berkas & Perubahan Kode

1.  **`WhisperTranscriber.kt`**: Logika pemrosesan berkas audio WAV, kalkulasi spektrogram Mel sederhana, pemanggilan interpreter TensorFlow Lite, dan dekode token menjadi teks Bahasa Indonesia/Inggris.
2.  **`GeminiNanoWrapper.kt`**: Logika pendeteksian ketersediaan AICore sistem Android 14+, inisialisasi model lokal, dan pengiriman perintah (*prompt*) peringkasan secara offline.
3.  **Integrasi UI (`LibraryScreen.kt` & `MainActivity.kt`)**: Menambahkan tombol "Transkripsi AI" pada setiap item di Library. Hasil transkripsi dan ringkasannya ditampilkan dalam lembar dialog melayang (BottomSheet/Dialog).
