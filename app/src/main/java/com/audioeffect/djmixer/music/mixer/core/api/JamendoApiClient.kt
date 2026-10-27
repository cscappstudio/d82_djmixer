package com.audioeffect.djmixer.music.mixer.core.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

data class JamendoTrack(
    val id: String,
    val name: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Long,
    val audioUrl: String,
    val imageUrl: String
)

object JamendoApiClient {

    private const val TAG = "JamendoApiClient"
    private const val CLIENT_ID = "40c9a81c"
    private const val BASE_URL = "https://api.jamendo.com/v3.0/tracks/"

    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun executeRequest(urlString: String): List<JamendoTrack> {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("User-Agent", "DJMixerApp/1.0")
        }

        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val results = json.optJSONArray("results")
            val list = mutableListOf<JamendoTrack>()

            if (results != null) {
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val id = item.optString("id", "")
                    val name = item.optString("name", "Untitled")
                    val artist = item.optString("artist_name", "Unknown Artist")
                    val album = item.optString("album_name", "")
                    val duration = item.optLong("duration", 0L)
                    val audio = item.optString("audio", "")
                    val image = item.optString("album_image", item.optString("image", ""))

                    if (id.isNotEmpty() && audio.isNotEmpty()) {
                        list.add(
                            JamendoTrack(
                                id = id,
                                name = name,
                                artistName = artist,
                                albumName = album,
                                durationSeconds = duration,
                                audioUrl = audio,
                                imageUrl = image
                            )
                        )
                    }
                }
            }
            return list
        } else {
            throw Exception("HTTP error $responseCode")
        }
    }

    fun fetchTracks(
        query: String? = null,
        genreTag: String? = null,
        offset: Int = 0,
        limit: Int = 30,
        callback: (Result<List<JamendoTrack>>) -> Unit
    ) {
        executor.execute {
            try {
                if (!query.isNullOrBlank()) {
                    val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                    val urlString = "$BASE_URL?client_id=$CLIENT_ID&format=json&limit=$limit&offset=$offset&namesearch=$encoded&audioformat=mp32"
                    val tracks = executeRequest(urlString)
                    mainHandler.post {
                        callback(Result.success(tracks))
                    }
                    return@execute
                }

                val tag = if (!genreTag.isNullOrBlank() && genreTag != "electronic+dance") {
                    genreTag
                } else {
                    "electronic"
                }

                // 1. Try fuzzytags
                val urlFuzzy = "$BASE_URL?client_id=$CLIENT_ID&format=json&limit=$limit&offset=$offset&fuzzytags=$tag&audioformat=mp32"
                var tracks = try {
                    executeRequest(urlFuzzy)
                } catch (e: Exception) {
                    emptyList()
                }

                // 2. If empty, fallback to namesearch
                if (tracks.isEmpty()) {
                    val urlName = "$BASE_URL?client_id=$CLIENT_ID&format=json&limit=$limit&offset=$offset&namesearch=$tag&audioformat=mp32"
                    tracks = try {
                        executeRequest(urlName)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                // 3. If still empty, fallback to tags
                if (tracks.isEmpty()) {
                    val urlTag = "$BASE_URL?client_id=$CLIENT_ID&format=json&limit=$limit&offset=$offset&tags=$tag&audioformat=mp32"
                    tracks = try {
                        executeRequest(urlTag)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                mainHandler.post {
                    callback(Result.success(tracks))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Jamendo tracks", e)
                mainHandler.post {
                    callback(Result.failure(e))
                }
            }
        }
    }

    fun downloadTrack(
        context: Context,
        track: JamendoTrack,
        onProgress: ((Int) -> Unit)? = null,
        onComplete: (File?) -> Unit
    ) {
        executor.execute {
            try {
                val cacheDir = File(context.cacheDir, "jamendo_cache")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                val targetFile = File(cacheDir, "jamendo_${track.id}.mp3")
                if (targetFile.exists() && targetFile.length() > 50000) {
                    // Already cached and valid
                    mainHandler.post {
                        onComplete(targetFile)
                    }
                    return@execute
                }

                val tempFile = File(cacheDir, "jamendo_${track.id}.tmp")
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                val url = URL(track.audioUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 25000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "DJMixerApp/1.0")
                }

                val totalLength = conn.contentLength
                val inputStream: InputStream = conn.inputStream
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloaded: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalLength > 0 && onProgress != null) {
                        val percent = ((downloaded * 100) / totalLength).toInt()
                        mainHandler.post { onProgress(percent) }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (tempFile.exists() && tempFile.length() > 10000) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    tempFile.renameTo(targetFile)
                    mainHandler.post {
                        onComplete(targetFile)
                    }
                } else {
                    tempFile.delete()
                    mainHandler.post {
                        onComplete(null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading track ${track.id}", e)
                mainHandler.post {
                    onComplete(null)
                }
            }
        }
    }
}
