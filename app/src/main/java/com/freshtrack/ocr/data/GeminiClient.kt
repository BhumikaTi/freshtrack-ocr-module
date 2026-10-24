package com.freshtrack.ocr.data

import com.freshtrack.ocr.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface GeminiService {

    // POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generate(
        @Path("model") model: String,
        @Body request: GeminiRequest
    ): retrofit2.Response<GeminiResponse>
}

/** Adds the key as a header (not in the URL), so it never ends up in a logged web address. */
private class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("x-goog-api-key", apiKey)
            .build()
        return chain.proceed(request)
    }
}

object GeminiClient {


    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(BuildConfig.GEMINI_API_KEY))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // a Google Search lookup can take a while
            .build()
    }

    val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiService::class.java)
    }
}