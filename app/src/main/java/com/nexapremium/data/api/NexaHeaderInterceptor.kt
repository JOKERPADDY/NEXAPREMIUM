package com.nexapremium.data.api

import okhttp3.Interceptor
import okhttp3.Response

class NexaHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("User-Agent", "okhttp/4.11.0")
            .header("Accept", "application/json")

        return chain.proceed(requestBuilder.build())
    }
}
