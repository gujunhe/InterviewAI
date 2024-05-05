package com.google.ai.sample.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class InterviewMessage(
    @SerializedName("accuracy")
    val accuracy: String ,

    @SerializedName("comment")
    val comment: String,

    @SerializedName("question")
    val question: String,

    @SerializedName("totalAccuracy")
    val totalAccuracy:String,

    @SerializedName("totalComment")
    val totalComment:String,

)