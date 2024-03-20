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

package com.google.ai.sample

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.google.ai.sample.feature.chat.ChatRoute
import com.google.ai.sample.feature.multimodal.PhotoReasoningRoute
import com.google.ai.sample.feature.text.SummarizeRoute
import com.google.ai.sample.model.TokenResponse
import com.google.ai.sample.network.BaiDuApiService
import com.google.ai.sample.network.BaiDuRetrofit
import com.google.ai.sample.ui.theme.GenerativeAISample
import com.google.ai.sample.util.SpeechStreamPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //window.setDecorFitsSystemWindows(false)
        SpeechEngineGenerator.PrepareEnvironment(applicationContext, application);
        if (mStreamPlayer == null) {
            mStreamPlayer = SpeechStreamPlayer()
        }
        CoroutineScope(Dispatchers.IO).launch {
            val apiService = BaiDuRetrofit.createService(BaiDuApiService::class.java)
            apiService.getToken("client_credentials","NHBMZ3uZzmjSm0t4HwX1gGYN","lVdx0Qi1keZ68p40nspdhBdUYBzlhGVt").enqueue(object : retrofit2.Callback<TokenResponse>{
                override fun onResponse(
                    call: Call<TokenResponse>,
                    response: Response<TokenResponse>
                ) {
                    Log.d("NetWorkLog",response.body().toString())
                    response.body()?.accessToken?.let { saveToken(it) }
                }

                override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                    TODO("Not yet implemented")
                }

            })
            withContext(Dispatchers.Main) {
            }
        }
        setContent {
            GenerativeAISample {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "menu") {
                        composable("menu") {
                            MenuScreen(onItemClicked = { routeId ->
                                navController.navigate(routeId)
                            })
                        }
                        composable("summarize") {
                            SummarizeRoute()
                        }
                        composable("photo_reasoning") {
                            PhotoReasoningRoute()
                        }
                        composable("chat") {
                            ChatRoute()
                        }
                    }
                }
            }
        }
    }
    fun saveToken(token: String) {
        val editor = MyApplication.getSharedPreferences().edit()
        editor.putString("TOKEN_KEY", token)
        editor.apply()
    }

    public fun getStreamPlayer(): SpeechStreamPlayer? {
        return mStreamPlayer
    }

    companion object {
        //private val mStreamRecorder: SpeechStreamRecorder? = null
        private var mStreamPlayer: SpeechStreamPlayer? = null
        fun getStreamPlayer(): SpeechStreamPlayer? {
            return this.mStreamPlayer
        }
    }
}
