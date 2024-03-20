package com.google.ai.sample.model

import com.google.gson.annotations.SerializedName


data class TokenResponse(
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("session_key")
    val sessionKey: String,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("scope")
    val scope: String,
    @SerializedName("session_secret")
    val sessionSecret: String
)