package com.mybini.app.media

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FFmpegMerger {

    interface MergeListener {
        fun onSuccess(outputFile: File)
        fun onError(message: String)
    }

    suspend fun mergeAudioVideo(
        videoFile: File,
        audioFile: File,
        outputFile: File,
        listener: MergeListener
    ) = withContext(Dispatchers.IO) {
        // Hapus file output jika sudah ada
        if (outputFile.exists()) {
            outputFile.delete()
        }

        // Perintah FFmpeg untuk menggabungkan video & audio tanpa re-encoding video (c:v copy)
        // dan re-encoding audio ke AAC demi kompatibilitas tinggi.
        val cmd = "-i \"${videoFile.absolutePath}\" -i \"${audioFile.absolutePath}\" -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 \"${outputFile.absolutePath}\""

        FFmpegKit.executeAsync(cmd) { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                // Hapus file video & audio terpisah yang lama demi menghemat penyimpanan
                videoFile.delete()
                audioFile.delete()
                
                listener.onSuccess(outputFile)
            } else {
                val failMessage = session.failStackTrace ?: "Unknown FFmpeg error"
                listener.onError("Gagal menggabungkan media: $failMessage")
            }
        }
    }
}
