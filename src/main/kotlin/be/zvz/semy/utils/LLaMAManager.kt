package be.zvz.semy.utils

import be.zvz.semy.dto.LLaMAPrompt
import be.zvz.semy.dto.LLaMAResponse
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object LLaMAManager {
    private val serverUrl = System.getProperty("serverUrl", "http://localhost:8884")
    private val authKey = System.getProperty("authKey", "test")
    private val inferenceUrl = "$serverUrl/"
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)

    fun inference(prompt: String): LLaMAResponse {
        val request = Request.Builder()
            .url(inferenceUrl)
            .post(
                JacksonManager.jacksonObjectMapper.writeValueAsString(
                    LLaMAPrompt(prompt, authKey),
                ).toRequestBody("application/json".toMediaType()),
            )
            .build()
        return okHttpClient.build().newCall(request).execute().body.byteStream().use(
            JacksonManager.jacksonObjectMapper::readValue
        )
    }
}
