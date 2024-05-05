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

package com.google.ai.sample.feature.record

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import com.google.ai.sample.GenerativeViewModelFactory
import com.google.ai.sample.R
import com.google.ai.sample.util.UriSaver
import kotlinx.coroutines.launch

@Composable
internal fun PhotoReasoningRoute(
    viewModel: PhotoReasoningViewModel = viewModel(factory = GenerativeViewModelFactory)
) {
    val photoReasoningUiState by viewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val imageRequestBuilder = ImageRequest.Builder(LocalContext.current)
    val imageLoader = ImageLoader.Builder(LocalContext.current).build()

    PhotoReasoningScreen(
        uiState = photoReasoningUiState,
        onReasonClicked = { inputText, selectedItems ->
            coroutineScope.launch {
                val bitmaps = selectedItems.mapNotNull {
                    val imageRequest = imageRequestBuilder
                        .data(it)
                        // Scale the image down to 768px for faster uploads
                        .size(size = 768)
                        .precision(Precision.EXACT)
                        .build()
                    try {
                        val result = imageLoader.execute(imageRequest)
                        if (result is SuccessResult) {
                            return@mapNotNull (result.drawable as BitmapDrawable).bitmap
                        } else {
                            return@mapNotNull null
                        }
                    } catch (e: Exception) {
                        return@mapNotNull null
                    }
                }
                viewModel.reason(inputText, bitmaps)
            }
        }
    )
}

@Composable
fun PhotoReasoningScreen(
    uiState: PhotoReasoningUiState = PhotoReasoningUiState.Loading,
    onReasonClicked: (String, List<Uri>) -> Unit = { _, _ -> }
) {
    var userQuestion by rememberSaveable { mutableStateOf("") }
    val imageUris = rememberSaveable(saver = UriSaver()) { mutableStateListOf() }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { imageUri ->
        imageUri?.let {
            imageUris.add(it)
        }
    }

    Column(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {


                Text(text = "面试对话:",
                    Modifier
                        .padding(start = 10.dp, top = 10.dp)
                        .weight(1f))

            Divider(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp), color = Color.Gray.copy(alpha = 0.5f))
                Text(text = "AI建议:",
                    Modifier
                        .padding(start = 10.dp, top = 5.dp)
                        .weight(1f))



        }
//        IconButton(onClick = { /*TODO*/ }, modifier = Modifier.background(Color.Transparent)) {
//            Icon(painter = painterResource(id = R.mipmap.record), null)
//        }
//        FloatingActionButton(onClick = { /*do something*/ }) {
//            Icon(painter = painterResource(id = R.mipmap.record), contentDescription = "Localized description")
//        }
//        Box(
//            modifier = Modifier
//                .align(Alignment.CenterHorizontally) // 底部居中
//                .padding(bottom = 16.dp) // 与底部有一定间距
//                .background(
//                    shape = RoundedCornerShape(50.dp), // 圆角
//                    color = Color.Transparent // 背景色
//                )
//                .clip(CircleShape)
//                .shadow(elevation = 8.dp) // 添加阴影效果，类似于 elevation
//        ) {
        Row (modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxSize()
                ,horizontalArrangement = Arrangement.Absolute.SpaceEvenly){


            IconButton(onClick = { /*TODO*/ },
                Modifier
                    .background(Color.Transparent)
                    .size(70.dp)
                    .padding(top = 10.dp)) {
                Image(
                    painter = painterResource(id = R.mipmap.record),
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                )
            }

                IconButton(
                    onClick = { /*TODO*/ },
                    Modifier
                        .background(Color.Transparent)
                        .size(67.dp)
                        .padding(top = 10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.llm),
                        contentDescription = null,
                        modifier = Modifier
                            .size(67.dp)
                    )
                }

            }
//
//        }

    }
}

@Composable
@Preview(showSystemUi = true)
fun PhotoReasoningScreenPreview() {
    PhotoReasoningScreen()
}
