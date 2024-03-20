package com.google.ai.sample.model

import com.google.gson.annotations.SerializedName
import java.util.UUID


data class ChatMessage(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("role")
    val role: RoleType = RoleType.USER,

    @SerializedName("content")
    val content: String,

    @SerializedName("isPending")
    var isPending: Boolean = false
)
//enum class Participant {
//    USER, MODEL, ERROR
//}
//
//data class ChatMessage(
//    val id: String = UUID.randomUUID().toString(),
//    var text: String = "",
//    val participant: Participant = Participant.USER,
//    var isPending: Boolean = false
//)


