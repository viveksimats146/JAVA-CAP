package com.example.myapplication.data.remote

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FallbackHostInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        val targetUrl = originalUrl.newBuilder()
            .host("127.0.0.1")
            .port(8080)
            .build()
        val targetRequest = originalRequest.newBuilder().url(targetUrl).build()

        return chain.proceed(targetRequest)
    }
}
