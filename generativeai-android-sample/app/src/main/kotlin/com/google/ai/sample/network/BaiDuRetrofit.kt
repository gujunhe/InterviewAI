package com.google.ai.sample.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object BaiDuRetrofit {

    private const val BASE_URL = "https://aip.baidubce.com" // 替换为您的API基础URL

    private val okHttpClient = OkHttpClient.Builder()
//        .addInterceptor { chain ->
//            // 在这里可以添加请求头或者其他拦截器
//            val original = chain.request()
//            val requestBuilder = original.newBuilder()
//                .header("Accept", "application/json")
//                .method(original.method, original.body)
//
//            val request = requestBuilder.build()
//            chain.proceed(request)
//        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}