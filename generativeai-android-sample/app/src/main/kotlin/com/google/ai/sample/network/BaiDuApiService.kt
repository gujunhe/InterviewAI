package com.google.ai.sample.network


import com.google.ai.sample.model.ChatRequest
import com.google.ai.sample.model.ChatResponse
import com.google.ai.sample.model.TokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Query


public interface BaiDuApiService {
    @FormUrlEncoded
    @POST("oauth/2.0/token")
    fun getToken(@Field("grant_type") grant_type:String, @Field("client_id") client_id:String,@Field("client_secret")client_secret:String): Call<TokenResponse>



    @POST("rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions_pro")
    fun sendMessage(@Query("access_token") access_token: String,@Body chatRequest : ChatRequest) : Call<ChatResponse>
}