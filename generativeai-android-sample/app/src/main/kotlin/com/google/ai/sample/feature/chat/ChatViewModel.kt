/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.sample.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.sample.MyApplication
import com.google.ai.sample.model.ChatMessage
import com.google.ai.sample.model.ChatRequest
import com.google.ai.sample.model.ChatResponse
import com.google.ai.sample.model.Message
import com.google.ai.sample.model.RoleType
import com.google.ai.sample.network.BaiDuApiService
import com.google.ai.sample.network.BaiDuRetrofit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response


class ChatViewModel(
    generativeModel: GenerativeModel
) : ViewModel() {
    private val historyChat = mutableListOf(
        ChatMessage(role = RoleType.USER, content = "你好"),
        ChatMessage(
            role = RoleType.ASSISTANT,
            content = "有什么能帮你的吗"
        )
    )


    private val _uiState: MutableStateFlow<ChatUiState> =
        MutableStateFlow(ChatUiState(historyChat.map { content ->
            // Map the initial messages
            ChatMessage(
                content = content.content ?: "",
                role = if (content.role == RoleType.USER) RoleType.USER else RoleType.ASSISTANT,
                isPending = false
            )
        }))
    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()


    private val accessToken: String? = getToken()
    fun sendMessage(userMessage: String) {
        // Add a pending message
        _uiState.value.addMessage(
            ChatMessage(
                content = userMessage,
                role = RoleType.USER,
                isPending = true
            )
        )
        historyChat.add(ChatMessage(
            content = userMessage,
            role = RoleType.USER,
        ))
        viewModelScope.launch {
            try {
                val token = accessToken ?: getToken()
                val apiService = BaiDuRetrofit.createService(BaiDuApiService::class.java)
                val messageList = historyChat.map { it -> Message(it.role.value, it.content) }
                val chatRequest = ChatRequest(messageList)
                token?.let {
                    apiService.sendMessage(it, chatRequest)
                        .enqueue(object : retrofit2.Callback<ChatResponse> {
                            override fun onResponse(
                                call: Call<ChatResponse>,
                                response: Response<ChatResponse>
                            ) {
                                Log.d("NetWorkLog", response.body().toString())
                                _uiState.value.replaceLastPendingMessage()
                                response.body()?.result?.let { modelResponse ->
                                    _uiState.value.addMessage(
                                        ChatMessage(
                                            content = modelResponse,
                                            role = RoleType.ASSISTANT,
                                            isPending = false
                                        )
                                    )
                                    historyChat.add(ChatMessage(
                                        content = modelResponse,
                                        role = RoleType.ASSISTANT,
                                    ))
                                }
                            }

                            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                                TODO("Not yet implemented")
                            }

                        })
                }

            } catch (e: Exception) {
                _uiState.value.replaceLastPendingMessage()
                _uiState.value.addMessage(
                    ChatMessage(
                        content = e.localizedMessage,
                        role = RoleType.ERROR
                    )
                )
            }
        }
    }

    private fun getToken(): String? {
        return MyApplication.getSharedPreferences().getString("TOKEN_KEY", null)
    }
}
