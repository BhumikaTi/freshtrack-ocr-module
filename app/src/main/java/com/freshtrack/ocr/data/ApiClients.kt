package com.freshtrack.ocr.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "FreshTrack/1.0 (Android capstone project; contact: your-email@example.com)")
            .build()
        return chain.proceed(request)
    }
}

object ApiClients {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // turn down to NONE before your demo
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(loggingInterceptor)
        .followRedirects(true)     // default is already true — explicit so it's never accidentally disabled
        .followSslRedirects(true)  // this is the setting that matters for product_type=all to work
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // One Retrofit instance only — the universal endpoint means you never need
    // to know or call a different host for beauty/petfood/other products.
    val openFoodFacts: OpenFoodFactsService by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsService::class.java)
    }
}