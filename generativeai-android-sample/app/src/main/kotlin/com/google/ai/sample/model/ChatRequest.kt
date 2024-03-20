package com.google.ai.sample.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("messages")
    val messages: List<Message>,

    @SerializedName("temperature")
    val temperature: Float = 0.8f,

    @SerializedName("top_p")
    val top_p: Float = 0.8f,

    @SerializedName("penalty_score")
    val penalty_score: Float = 1.0f,

    @SerializedName("stream")
    val stream: Boolean = false,

    @SerializedName("system")
    val system: String? = null,

    @SerializedName("stop")
    val stop: List<String>? = null,

    @SerializedName("disable_search")
    val disable_search: Boolean = false,

    @SerializedName("enable_citation")
    val enable_citation: Boolean = false,

    @SerializedName("max_output_tokens")
    val max_output_tokens: Int? = null,

    @SerializedName("response_format")
    val response_format: String? = null,

    @SerializedName("user_id")
    val user_id: String? = null
) {
}


enum class RoleType(val value: String) {
    @SerializedName("user")
    USER("user"),

    @SerializedName("assistant")
    ASSISTANT("assistant"),

    @SerializedName("error")
    ERROR("error");

    companion object {
        fun fromValue(value: String): RoleType = values().firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Invalid role value: $value")
    }
}
data class Message(
    @SerializedName("role")
    val role: String = RoleType.USER.value,

    @SerializedName("content")
    val content: String,

    @SerializedName("name")
    val name: String? = null
)