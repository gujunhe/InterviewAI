package com.google.ai.sample.model

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("object")
    val objectType: String,
    @SerializedName("created")
    val created: Int,
    @SerializedName("sentence_id")
    val sentenceId: Int? = null,
    @SerializedName("is_end")
    val isEnd: Boolean? = null,
    @SerializedName("is_truncated")
    val isTruncated: Boolean,
    @SerializedName("finish_reason")
    val finishReason: String,
    @SerializedName("search_info")
    val searchInfo: SearchInfo? = null,
    @SerializedName("result")
    val result: String,
    @SerializedName("need_clear_history")
    val needClearHistory: Boolean,
    @SerializedName("flag")
    val flag: Int,
    @SerializedName("ban_round")
    val banRound: Int? = null,
    @SerializedName("usage")
    val usage: Usage
)

data class SearchInfo(
    @SerializedName("search_results")
    val searchResults: List<SearchResult>
)

data class SearchResult(
    @SerializedName("index")
    val index: Int,
    @SerializedName("url")
    val url: String,
    @SerializedName("title")
    val title: String
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)