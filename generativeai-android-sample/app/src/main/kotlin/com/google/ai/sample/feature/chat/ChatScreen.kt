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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.google.ai.sample.feature.chat

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytedance.speech.speechengine.SpeechEngine
import com.google.ai.sample.MyViewModelFactory
import com.google.ai.sample.R
import com.google.ai.sample.model.ChatMessage
import com.google.ai.sample.model.RoleType
import com.google.ai.sample.realtime.RealtimeHelper
import kotlinx.coroutines.launch

@SuppressLint("SuspiciousIndentation")
@Preview
@Composable
internal fun ChatRoute(
    chatViewModel: ChatViewModel = viewModel(factory = MyViewModelFactory(LocalContext.current))
) {
    // 弹窗状态
    var showDialog by remember { mutableStateOf(true) }
    // 输入框内容
    var jobDescription by remember { mutableStateOf("") }
    val chatUiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snapshotFlow = snapshotFlow { chatUiState.messages }
    var mSpeechEngine: SpeechEngine? = null
    val realtimeHelper = RealtimeHelper(context = LocalContext.current)
    // 定义一个状态变量来控制异步操作的执行
    var isFirstTime by remember { mutableStateOf(true) }
    fun handleNewMessage(newMessage: ChatMessage) {
        // 在这里处理新添加的消息
        // ...

    }



    LaunchedEffect(key1 = showDialog) {
        if(!showDialog)
        realtimeHelper.setInterface(object : RealtimeHelper.Listener {
            override fun onSegmentSuccess(text: String) {
//                chatViewModel.sendMessage(text)
            }

            override fun onSuccess(text: String) {
                chatViewModel.sendMessage(text)
            }

        })
    }
    LaunchedEffect(snapshotFlow) {
        snapshotFlow.collect { messages ->
            val newMessage = messages.lastOrNull()
            if (newMessage != null) {
                handleNewMessage(newMessage)
            }
        }
    }

    // 显示弹窗
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.Transparent)
        ) {
                Column(
                    modifier = Modifier
                        .size(400.dp).wrapContentHeight()
                        .background(Color.White).clip(RoundedCornerShape(16.dp))
                ) {
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        label = { Text("职位、职位描述(默认为java后端开发工程师)") },
//                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions.Default,
                        modifier = Modifier.fillMaxWidth().padding(20.dp,20.dp,20.dp,0.dp).fillMaxWidth().height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showDialog = false
                                      chatViewModel.setJobDescription("")
                                      },
                            modifier = Modifier.weight(1f).padding(20.dp,0.dp,20.dp,20.dp)
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                if (jobDescription.isNotEmpty()) {
                                    // 执行页面跳转逻辑
                                    // ...
                                    showDialog = false
                                    chatViewModel.setJobDescription(jobDescription)

                                }
                            },
                            enabled = jobDescription.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (jobDescription.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f).padding(20.dp,0.dp,20.dp,20.dp)
                        ) {
                            Text("确认")
                        }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            MessageInput(
                onSendMessage = { inputText ->
                    chatViewModel.sendMessage(inputText)
                },
                onRecordingEnd = {
                    if(!it)
                    realtimeHelper.stopRecord()
                    else
                    realtimeHelper.cancelRecord()
                },
                onRecordingStart = {
                    realtimeHelper.start()
                },
                resetScroll = {
                    coroutineScope.launch {
                        listState.scrollToItem(0)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Messages List
            ChatList(chatUiState.messages.drop(1), listState)
        }
    }
}

@Composable
fun ChatList(
    chatMessages: List<ChatMessage>,
    listState: LazyListState
) {
    LazyColumn(
        reverseLayout = true,
        state = listState
    ) {
        items(chatMessages.reversed()) { message ->
            ChatBubbleItem(message)
        }
    }
}

@Composable
fun ChatBubbleItem(
    chatMessage: ChatMessage
) {
    val isModelMessage = chatMessage.role == RoleType.ASSISTANT ||
            chatMessage.role == RoleType.ERROR

    val backgroundColor = when (chatMessage.role) {
        RoleType.ASSISTANT -> MaterialTheme.colorScheme.primaryContainer
        RoleType.USER -> MaterialTheme.colorScheme.tertiaryContainer
        RoleType.ERROR -> MaterialTheme.colorScheme.errorContainer
    }

    val bubbleShape = if (isModelMessage) {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    }

    val horizontalAlignment = if (isModelMessage) {
        Alignment.Start
    } else {
        Alignment.End
    }

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = chatMessage.role.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row {
            if (chatMessage.isPending) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(all = 8.dp)
                )
            }
            BoxWithConstraints {
                Card(
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = bubbleShape,
                    modifier = Modifier.widthIn(0.dp, maxWidth * 0.9f)
                ) {
                    Text(
                        text = chatMessage.content,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    onRecordingStart: () -> Unit,
    onRecordingEnd: (Boolean) -> Unit, // 传入 true 表示发送录音,false 表示取消录音
    resetScroll: () -> Unit = {}
) {
    var userMessage by rememberSaveable { mutableStateOf("") }
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var showHint by rememberSaveable { mutableStateOf(false) }
    var isRecordingMode by rememberSaveable { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = if (isRecordingMode) "" else userMessage,
                onValueChange = { userMessage = it },
                //label = { Text(if (isRecordingMode) "按住说话" else "发消息...") },
                placeholder = { Text(if (isRecordingMode) "按住说话" else "发消息...") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                enabled = !isRecordingMode,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(0.85f)
                    .pointerInteropFilter {
                        when {
                            isRecordingMode && it.action == MotionEvent.ACTION_DOWN -> {
                                isRecording = true
                                onRecordingStart()
                                showHint = true
                            }

                            isRecording && it.action == MotionEvent.ACTION_UP -> {
                                showHint = false
                                isRecording = false
                                onRecordingEnd(it.y < 0)
                            }
                        }
                        isRecordingMode
                    }
            )
            IconButton(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        onSendMessage(userMessage)
                        userMessage = ""
                        resetScroll()
                    } else {
                        isRecordingMode = !isRecordingMode
                    }

                },
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(0.15f)
            ) {
                Icon(
                    if (userMessage.isNotBlank()) painterResource(id = R.mipmap.send) else if (!isRecordingMode) painterResource(
                        id = R.mipmap.audio
                    ) else painterResource(id = R.mipmap.keyboard),
                    contentDescription = if (isRecording) "键盘" else "语音",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }

    if (showHint) {
        Text(
            text = "松手发送,上移取消",
            modifier = Modifier
                .padding(16.dp),
//                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )
    }
}
