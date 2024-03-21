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

import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.bytedance.speech.speechengine.SpeechResourceManager
import com.google.ai.sample.MyApplication
import com.google.ai.sample.R
import com.google.ai.sample.SettingsActivity
import com.google.ai.sample.model.ChatMessage
import com.google.ai.sample.model.ChatRequest
import com.google.ai.sample.model.ChatResponse
import com.google.ai.sample.model.Message
import com.google.ai.sample.model.RoleType
import com.google.ai.sample.network.BaiDuApiService
import com.google.ai.sample.network.BaiDuRetrofit
import com.google.ai.sample.settings.Settings
import com.google.ai.sample.util.SensitiveDefines
import com.google.ai.sample.util.SpeechDemoDefines
import com.google.ai.sample.util.SpeechStreamPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response


class ChatViewModel(
    context: Context
) : ViewModel(), SpeechEngine.SpeechListener, LifecycleObserver {
    private val historyChat = mutableListOf(
        ChatMessage(role = RoleType.USER, content = "你好"),
        ChatMessage(
            role = RoleType.ASSISTANT,
            content = "有什么能帮你的吗"
        )
    )
    private val mTtsTextTypeArray = arrayOf(
        SpeechEngineDefines.TTS_TEXT_TYPE_PLAIN,
        SpeechEngineDefines.TTS_TEXT_TYPE_SSML
    )
    private val mTtsWorkModeArray = intArrayOf(
        SpeechEngineDefines.TTS_WORK_MODE_ONLINE,
        SpeechEngineDefines.TTS_WORK_MODE_OFFLINE,
        SpeechEngineDefines.TTS_WORK_MODE_BOTH,
        SpeechEngineDefines.TTS_WORK_MODE_ALTERNATE,
        SpeechEngineDefines.TTS_WORK_MODE_FILE
    )
    private val mAuthenticationTypeArray = arrayOf(
        SpeechEngineDefines.AUTHENTICATE_TYPE_PRE_BIND,
        SpeechEngineDefines.AUTHENTICATE_TYPE_LATE_BIND
    )
    private var mTtsSynthesisMap= mutableMapOf<String?,Int?>()
    private var mTtsSynthesisIndex = 0
    private var mTtsSynthesisFromPlayer = false
    // Settings
    protected var mSettings: Settings? = null

    // Engine
    private  var mSpeechEngine: SpeechEngine ? = null
    private var mTtsPlayingIndex = -1
    // UI
    private var mReferText: EditText? = null
    private var mResult: TextView? = null
    private var mEngineStatus: TextView? = null
    private var mEngineSwitch: Button? = null
    private var mCreateConnectionBtn: Button? = null
    private var mStartBtn: Button? = null
    private var mStopBtn: Button? = null
    private var mPauseResumeBtn: Button? = null
    private var resultText:String?=null
    private var mTtsSynthesisText = mutableListOf<String>()
    // Engine State
    private var mEngineInited = false
    private var mConnectionCreated = false
    private var mEngineStarted = false
    private var mEngineErrorOccurred = false
    private var mPlayerPaused = false

    // Paths
    private var mDebugPath = ""

    // Offline Resource Manager
    private var mResourceManager: SpeechResourceManager? = null

    // Options Default Value
    private var mCurAppId: String = SensitiveDefines.APPID
    private var mCurTtsText = ""
    private var mCurVoiceOnline: String = SensitiveDefines.TTS_DEFAULT_ONLINE_VOICE
    private var mCurVoiceOffline: String = SensitiveDefines.TTS_DEFAULT_OFFLINE_VOICE
    private var mCurVoiceTypeOnline: String = SensitiveDefines.TTS_DEFAULT_ONLINE_VOICE_TYPE
    private var mCurVoiceTypeOffline: String = SensitiveDefines.TTS_DEFAULT_OFFLINE_VOICE_TYPE
    private var mCurTtsWorkMode = SpeechEngineDefines.TTS_WORK_MODE_ONLINE
    private var mTtsSilenceDuration = 0
    private val TTS_MAX_RETRY_COUNT = 3
    private var mRetryCount = TTS_MAX_RETRY_COUNT
    // Android AudioTrack Playing
    private var mStreamPlayer: SpeechStreamPlayer? = null

    // Android Audio Manager
    private var mAFChangeListener: AudioManager.OnAudioFocusChangeListener? = null
    private var mAudioManager: AudioManager? = null
    private var mResumeOnFocusGain = true
    private var mPlaybackNowAuthorized = false

    // State shared between Init Engine and Start Engine
    private var mDisablePlayerReuse = false


    init {
        val viewId: String = SpeechDemoDefines.TTS_VIEW
        mSettings = SettingsActivity.getSettings(viewId)
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        initEngine()
    }
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
                                    //合成语音
                                    resultText = modelResponse
                                    // 准备待合成的文本
                                    if (!prepareTextList()) {
                                        speechError("{err_code:3006, err_msg:\"Invalid input text.\"}")
                                        return
                                    }
                                    startEngineBtnClicked()
                                    triggerSynthesis()

                                }
                            }

                            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                                Log.d("TAG",t.message.toString())
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

    private fun prepareTextList(): Boolean {
        resetTtsContext()
        var ttsText = resultText ?:""
        if (ttsText.isEmpty()) {
            ttsText =
                "愿中国青年都摆脱冷气，只是向上走，不必听自暴自弃者流的话。能做事的做事，能发声的发声。有一分热，发一分光。就令萤火一般，也可以在黑暗里发一点光，不必等候炬火。此后如竟没有炬火：我便是唯一的光。"
        }
        //【必需配置】需合成的文本，不可超过 80 字
        if (mTtsSynthesisText == null || mTtsSynthesisText.isEmpty()) {
            // 使用下面几个标点符号来分句，会让通过 MESSAGE_TYPE_TTS_PLAYBACK_PROGRESS 返回的播放进度更加准确
            val tmp = ttsText.split("[;|!|?|。|！|？|；|…]".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            for (j in tmp.indices) {
                AddSentence(tmp[j])
            }
        }
        Log.d(SpeechDemoDefines.TAG, "Synthesis text item num: " + mTtsSynthesisText.size)
        return !mTtsSynthesisText.isEmpty()
    }

    private fun AddSentence(text: String) {
        val tmp = text.trim { it <= ' ' }
        if (!tmp.isEmpty()) {
            mTtsSynthesisText.add(tmp)
        }
    }

    private fun configInitParams() {
        //【必需配置】Engine Name
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ENGINE_NAME_STRING,
            SpeechEngineDefines.TTS_ENGINE
        )

        //【必需配置】Work Mode, 可选值如下
        // SpeechEngineDefines.TTS_WORK_MODE_ONLINE, 只进行在线合成，不需要配置离线合成相关参数；
        // SpeechEngineDefines.TTS_WORK_MODE_OFFLINE, 只进行离线合成，不需要配置在线合成相关参数；
        // SpeechEngineDefines.TTS_WORK_MODE_BOTH, 同时发起在线合成与离线合成，在线请求失败的情况下，使用离线合成数据，该模式会消耗更多系统性能；
        // SpeechEngineDefines.TTS_WORK_MODE_ALTERNATE, 先发起在线合成，失败后（网络超时），启动离线合成引擎开始合成；
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_WORK_MODE_INT,
            mCurTtsWorkMode
        )

        //【可选配置】Debug & Log
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_DEBUG_PATH_STRING,
            mDebugPath
        )
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_LOG_LEVEL_STRING,
            SpeechEngineDefines.LOG_LEVEL_DEBUG
        )

        //【可选配置】User ID（用以辅助定位线上用户问题）
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_UID_STRING,
            SensitiveDefines.UID
        )
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_DEVICE_ID_STRING,
            SensitiveDefines.DID
        )

        //【可选配置】是否将合成出的音频保存到设备上，为 true 时需要正确配置 PARAMS_KEY_TTS_AUDIO_PATH_STRING 才会生效
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_DUMP_BOOL,
            mSettings?.getBoolean(R.string.config_tts_dump) ?: false
        )
        // TTS 音频文件保存目录，必须在合成之前创建好且 APP 具有访问权限，保存的音频文件名格式为 tts_{reqid}.wav, {reqid} 是本次合成的请求 id
        // PARAMS_KEY_TTS_ENABLE_DUMP_BOOL 配置为 true 的音频时为【必需配置】，否则为【可选配置】
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_AUDIO_PATH_STRING,
            mDebugPath
        )
        mDisablePlayerReuse = mSettings?.getBoolean(R.string.config_disable_player_reuse) ?: false
        //【可选配置】是否禁止播放器对象的复用，如果禁用则每次 Start Engine 都会重新创建播放器对象
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_PLAYER_DISABLE_REUSE_BOOL,
            mDisablePlayerReuse
        )
        if (!mDisablePlayerReuse) {
            //【可选配置】用于控制 SDK 播放器所用的音源,默认为媒体音源
            mSpeechEngine!!.setOptionInt(
                SpeechEngineDefines.PARAMS_KEY_AUDIO_STREAM_TYPE_INT,
                mSettings?.getInt(R.string.config_player_stream_type) ?: 0
            )
        }

        //【可选配置】合成出的音频的采样率，默认为 24000
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_SAMPLE_RATE_INT,
            mSettings?.getInt(R.string.config_tts_sample_rate) ?: 0
        )
        //【可选配置】打断播放时使用多长时间淡出停止，单位：毫秒。默认值 0 表示不淡出
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_AUDIO_FADEOUT_DURATION_INT,
            mSettings?.getInt(R.string.config_audio_fadeout_duration) ?: 0
        )

        // ------------------------ 在线合成相关配置 -----------------------
        mCurAppId = mSettings?.getString(R.string.config_app_id) ?: ""
        if (mCurAppId.isEmpty()) {
            mCurAppId = SensitiveDefines.APPID
        }
        //【必需配置】在线合成鉴权相关：Appid
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, mCurAppId)
        var token: String = mSettings?.getString(R.string.config_token) ?: ""
        if (token.isEmpty()) {
            token = SensitiveDefines.TOKEN
        }
        //【必需配置】在线合成鉴权相关：Token
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING, token)
        var address: String = mSettings?.getString(R.string.config_address) ?: ""
        if (address.isEmpty()) {
            address = SensitiveDefines.DEFAULT_ADDRESS
        }
        Log.i(SpeechDemoDefines.TAG, "Current address: $address")
        //【必需配置】语音合成服务域名
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_TTS_ADDRESS_STRING, address)
        var uri: String = mSettings?.getString(R.string.config_uri) ?: ""
        if (uri.isEmpty()) {
            uri = SensitiveDefines.TTS_DEFAULT_URI
        }
        Log.i(SpeechDemoDefines.TAG, "Current uri: $uri")
        //【必需配置】语音合成服务Uri
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_TTS_URI_STRING, uri)
        var cluster: String = mSettings?.getString(R.string.config_cluster) ?: ""
        if (cluster.isEmpty()) {
            cluster = SensitiveDefines.TTS_DEFAULT_CLUSTER
        }
        Log.i(SpeechDemoDefines.TAG, "Current cluster: $cluster")
        //【必需配置】语音合成服务所用集群
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_TTS_CLUSTER_STRING, cluster)

        //【可选配置】在线合成下发的 opus-ogg 音频的压缩倍率
        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_TTS_COMPRESSION_RATE_INT, 10)

        // ------------------------ 离线合成相关配置 -----------------------

        //【必需配置】离线合成鉴权相关：证书文件存放路径
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_LICENSE_DIRECTORY_STRING,
            mDebugPath
        )
        val curAuthenticateType = mAuthenticationTypeArray[mSettings
            ?.getOptions(R.string.config_authenticate_type)?.chooseIdx!!]
        //【必需配置】Authenticate Type
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_AUTHENTICATE_TYPE_STRING,
            curAuthenticateType
        )
        if (curAuthenticateType == SpeechEngineDefines.AUTHENTICATE_TYPE_PRE_BIND) {
            // 按包名授权，获取到授权的 APP 可以不限次数、不限设备数的使用离线合成
            val ttsLicenseName: String = mSettings!!.getString(R.string.config_license_name)
            val ttsLicenseBusiId: String = mSettings!!.getString(R.string.config_license_busi_id)

            // 证书名和业务 ID, 离线合成鉴权相关，使用火山提供的证书下发服务时为【必需配置】, 否则为【无需配置】
            // 证书名，用于下载按报名授权的证书文件
            mSpeechEngine!!.setOptionString(
                SpeechEngineDefines.PARAMS_KEY_LICENSE_NAME_STRING,
                ttsLicenseName
            )
            // 业务 ID, 用于下载按报名授权的证书文件
            mSpeechEngine!!.setOptionString(
                SpeechEngineDefines.PARAMS_KEY_LICENSE_BUSI_ID_STRING,
                ttsLicenseBusiId
            )
        } else if (curAuthenticateType == SpeechEngineDefines.AUTHENTICATE_TYPE_LATE_BIND) {
            // 按装机量授权，不限制 APP 的包名和使用次数，但是限制使用离线合成的设备数量
            //【必需配置】离线合成鉴权相关：Authenticate Address
            mSpeechEngine!!.setOptionString(
                SpeechEngineDefines.PARAMS_KEY_AUTHENTICATE_ADDRESS_STRING,
                SensitiveDefines.AUTHENTICATE_ADDRESS
            )
            //【必需配置】离线合成鉴权相关：Authenticate Uri
            mSpeechEngine!!.setOptionString(
                SpeechEngineDefines.PARAMS_KEY_AUTHENTICATE_URI_STRING,
                SensitiveDefines.AUTHENTICATE_URI
            )
            val businessKey: String = mSettings!!.getString(R.string.config_business_key)
            val authenticateSecret: String =
                mSettings!!.getString(R.string.config_authenticate_secret)
            //【必需配置】离线合成鉴权相关：Business Key
            mSpeechEngine!!.setOptionString(
                SpeechEngineDefines.PARAMS_KEY_BUSINESS_KEY_STRING,
                businessKey
            )
            //【必需配置】离线合成鉴权相关：Authenticate Secret
            mSpeechEngine!!.setOptionString(
                SpeechEngineDefines.PARAMS_KEY_AUTHENTICATE_SECRET_STRING,
                authenticateSecret
            )
        }

        // ------------------------ 在离线切换相关配置 -----------------------
        if (mCurTtsWorkMode == SpeechEngineDefines.TTS_WORK_MODE_ALTERNATE) {
            // 断点续播功能在断点处会发生由在线合成音频切换到离线合成音频，为了提升用户体验，SDK 支持
            // 淡出地停止播放在线音频然后再淡入地开始播放离线音频，下面两个参数可以控制淡出淡入的长度

            //【可选配置】断点续播专用，切换到离线合成时淡入的音频长度，单位：毫秒
            mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_TTS_FADEIN_DURATION_INT, 30)
            //【可选配置】断点续播专用，在线合成停止播放时淡出的音频长度，单位：毫秒
            mSpeechEngine!!.setOptionInt(
                SpeechEngineDefines.PARAMS_KEY_TTS_FADEOUT_DURATION_INT,
                30
            )
        }
    }

    private fun synthesisNextSentence() {
        if (mEngineStarted) {
            mTtsSynthesisIndex = (1 + mTtsSynthesisIndex) % mTtsSynthesisText.size
            triggerSynthesis()
        }
    }
    private fun updateSynthesisMap(synthesisId: String?) {
        if (mTtsSynthesisIndex < (mTtsSynthesisText?.size ?: 0)) {
            mTtsSynthesisMap[synthesisId] = mTtsSynthesisIndex

        }
    }

    private fun resetTtsContext() {
        mTtsPlayingIndex = -1
        mTtsSynthesisIndex = 0
        mTtsSynthesisFromPlayer = false
        if (mTtsSynthesisText != null) {
            mTtsSynthesisText.clear()
        } else {
            mTtsSynthesisText = ArrayList<String>()
        }
        if (mTtsSynthesisMap != null) {
            mTtsSynthesisMap.clear()
        } else {
            mTtsSynthesisMap = mutableMapOf<String?,Int?>()
        }
    }

    fun speechStartSynthesis(data: String?) {
            updateSynthesisMap(data)
    }
    fun speechFinishSynthesis(data: String?) {
        if (mRetryCount < TTS_MAX_RETRY_COUNT) {
            mRetryCount = TTS_MAX_RETRY_COUNT
        }
            if (!mTtsSynthesisFromPlayer) {
                synthesisNextSentence()
            }

    }


//    fun speechStart(data: String?) {
//        mEngineStarted = true
//    }
//
//    fun speechStop(data: String?) {
//        mEngineStarted = false
//            mPlayerPaused = false
//        // Abandon audio focus when playback complete
//        mAudioManager!!.abandonAudioFocus(mAFChangeListener)
//        mPlaybackNowAuthorized = false
//    }
    private fun initEngine() {
        mCurTtsWorkMode =
            mTtsWorkModeArray[mSettings?.getOptions(R.string.tts_work_mode_title)?.chooseIdx!!]
        Log.i(
            SpeechDemoDefines.TAG,
            "调用初始化接口前的语音合成工作模式为 $mCurTtsWorkMode"
        )
        if (mCurTtsWorkMode == SpeechEngineDefines.TTS_WORK_MODE_ONLINE || mCurTtsWorkMode == SpeechEngineDefines.TTS_WORK_MODE_FILE) {
            // 当使用纯在线模式时，不需要下载离线合成所需资源
            initEngineInternal()
        }
    }
    private fun initEngineInternal() {
        var ret = SpeechEngineDefines.ERR_NO_ERROR
        if (mSpeechEngine == null) {
            Log.i(SpeechDemoDefines.TAG, "创建引擎.")
            mSpeechEngine = SpeechEngineGenerator.getInstance() !!
            mSpeechEngine!!.createEngine()
        }
        if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
            speechEngineInitFailed("Create engine failed: $ret")
            return
        }
        Log.d(SpeechDemoDefines.TAG, "SDK 版本号: " + mSpeechEngine!!.version)
        Log.i(SpeechDemoDefines.TAG, "配置初始化参数.")
        configInitParams()
        if (mSettings?.getBoolean(R.string.config_demo_player) == true && mStreamPlayer != null) {
            mStreamPlayer!!.SetPlayerSampleRate(mSettings!!.getInt(R.string.config_tts_sample_rate))
        }
        val startInitTimestamp = System.currentTimeMillis()
        Log.i(SpeechDemoDefines.TAG, "引擎初始化.")
        ret = mSpeechEngine!!.initEngine()
        if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
            val errMessage = "初始化失败，返回值: $ret"
            Log.e(SpeechDemoDefines.TAG, errMessage)
            speechEngineInitFailed(errMessage)
            return
        }
        Log.i(SpeechDemoDefines.TAG, "设置消息监听")
        mSpeechEngine!!.setListener(this)
        val cost = System.currentTimeMillis() - startInitTimestamp
        Log.d(SpeechDemoDefines.TAG, String.format("初始化耗时 %d 毫秒", cost))
        speechEnginInitSucceeded(cost)
    }


    private fun startEngineBtnClicked(text: String?) {
        Log.d(SpeechDemoDefines.TAG, "Start engine, current status: $mEngineStarted")
        if (!mEngineStarted) {
            AcquireAudioFocus()
            if (!mPlaybackNowAuthorized) {
                Log.w(SpeechDemoDefines.TAG, "Acquire audio focus failed, can't play audio")
                return
            }
            clearResultText()
            mEngineErrorOccurred = false

            // Directive：启动引擎前调用SYNC_STOP指令，保证前一次请求结束。
            Log.i(SpeechDemoDefines.TAG, "关闭引擎（同步）")
            Log.i(SpeechDemoDefines.TAG, "Directive: DIRECTIVE_SYNC_STOP_ENGINE")
            var ret =
                mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
            if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                Log.e(SpeechDemoDefines.TAG, "send directive syncstop failed, $ret")
            } else {
                configStartTtsParams()
                Log.i(SpeechDemoDefines.TAG, "启动引擎")
                Log.i(SpeechDemoDefines.TAG, "Directive: DIRECTIVE_START_ENGINE")
                ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_START_ENGINE, "")
                if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                    val message = "发送启动引擎指令失败, $ret"
                    sendStartEngineDirectiveFailed(message)
                }
            }
        }
    }

    private fun startEngineBtnClicked() {
        Log.d(SpeechDemoDefines.TAG, "Start engine, current status: $mEngineStarted")
        if (!mEngineStarted) {
            AcquireAudioFocus()
            if (!mPlaybackNowAuthorized) {
                Log.w(SpeechDemoDefines.TAG, "Acquire audio focus failed, can't play audio")
                return
            }
            clearResultText()
            mEngineErrorOccurred = false

            // Directive：启动引擎前调用SYNC_STOP指令，保证前一次请求结束。
            Log.i(SpeechDemoDefines.TAG, "关闭引擎（同步）")
            Log.i(SpeechDemoDefines.TAG, "Directive: DIRECTIVE_SYNC_STOP_ENGINE")
            var ret =
                mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
            if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                Log.e(SpeechDemoDefines.TAG, "send directive syncstop failed, $ret")
            } else {
                configStartTtsParams()
                Log.i(SpeechDemoDefines.TAG, "启动引擎")
                Log.i(SpeechDemoDefines.TAG, "Directive: DIRECTIVE_START_ENGINE")
                ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_START_ENGINE, "")
                if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                    val message = "发送启动引擎指令失败, $ret"
                    sendStartEngineDirectiveFailed(message)
                }
            }
        }
    }
    private fun configStartTtsParams() {
        //【必需配置】TTS 使用场景
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_SCENARIO_STRING,
            SpeechEngineDefines.TTS_SCENARIO_TYPE_NOVEL
        )
        if (mDisablePlayerReuse) {
            //【可选配置】用于控制 SDK 播放器所用的音源,默认为媒体音源
            mSpeechEngine!!.setOptionInt(
                SpeechEngineDefines.PARAMS_KEY_AUDIO_STREAM_TYPE_INT,
                mSettings!!.getInt(R.string.config_player_stream_type)
            )
        }
        //【可选配置】是否使用 SDK 内置播放器播放合成出的音频，默认为 true
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_PLAYER_BOOL,
            mSettings!!.getBoolean(R.string.config_sdk_player)
        )
        //【可选配置】是否令 SDK 通过回调返回合成的音频数据，默认不返回。
        // 开启后，SDK 会流式返回音频，收到 MESSAGE_TYPE_TTS_AUDIO_DATA_END 回调表示当次合成所有的音频已经全部返回
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_DATA_CALLBACK_MODE_INT,
            if (mSettings!!.getBoolean(R.string.config_tts_data_callback)) 2 else 0
        )
    }

    private fun triggerSynthesis() {
        configSynthesisParams()
        // DIRECTIVE_SYNTHESIS 是连续合成必需的一个指令，在成功调用 DIRECTIVE_START_ENGINE 之后，每次合成新的文本需要再调用 DIRECTIVE_SYNTHESIS 指令
        // DIRECTIVE_SYNTHESIS 需要在当前没有正在合成的文本时才可以成功调用，否则就会报错 -901，可以在收到 MESSAGE_TYPE_TTS_SYNTHESIS_END 之后调用
        // 当使用 SDK 内置的播放器时，为了避免缓存过多的音频导致内存占用过高，SDK 内部限制缓存的音频数量不超过 5 次合成的结果，
        // 如果 DIRECTIVE_SYNTHESIS 后返回 -902, 就需要在下一次收到 MESSAGE_TYPE_TTS_FINISH_PLAYING 再去调用 MESSAGE_TYPE_TTS_FINISH_PLAYING
        Log.i(SpeechDemoDefines.TAG, "触发合成")
        Log.i(SpeechDemoDefines.TAG, "Directive: DIRECTIVE_SYNTHESIS")
        val ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNTHESIS, "")
        if (ret != 0) {
            Log.e(SpeechDemoDefines.TAG, "Synthesis faile: $ret")
            if (ret == SpeechEngineDefines.ERR_SYNTHESIS_PLAYER_IS_BUSY) {
                mTtsSynthesisFromPlayer = true
            } else {
                val message = "发送合成指令失败, $ret"
                sendSynthesisDirectiveFailed(message)
            }
        }
    }

    private fun configSynthesisParams() {
        //【可选配置】需合成的文本的类型，支持直接传文本(TTS_TEXT_TYPE_PLAIN)和传 SSML 形式(TTS_TEXT_TYPE_SSML)的文本
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_TEXT_TYPE_STRING,
            mTtsTextTypeArray[mSettings!!.getOptions(R.string.tts_text_type_title).chooseIdx]
        )
        val text = mTtsSynthesisText[mTtsSynthesisIndex]
        Log.e(SpeechDemoDefines.TAG, "Synthesis Text: $text")
        //【必需配置】需合成的文本，不可超过 80 字
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_TTS_TEXT_STRING, text)
//        mTtsSpeakSpeed = mSettings!!.getDouble(R.string.config_tts_speak_speed)
//        //【可选配置】用于控制 TTS 音频的语速，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
//        mSpeechEngine!!.setOptionDouble(
//            SpeechEngineDefines.PARAMS_KEY_TTS_SPEED_RATIO_DOUBLE,
//            mTtsSpeakSpeed
//        )
//        mTtsAudioVolume = mSettings!!.getDouble(R.string.config_tts_audio_volume)
//        //【可选配置】用于控制 TTS 音频的音量，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
//        mSpeechEngine!!.setOptionDouble(
//            SpeechEngineDefines.PARAMS_KEY_TTS_VOLUME_RATIO_DOUBLE,
//            mTtsAudioVolume
//        )
//        mTtsAudioPitch = mSettings!!.getDouble(R.string.config_tts_audio_pitch)
//        //【可选配置】用于控制 TTS 音频的音高，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
//        mSpeechEngine!!.setOptionDouble(
//            SpeechEngineDefines.PARAMS_KEY_TTS_PITCH_RATIO_DOUBLE,
//            mTtsAudioPitch
//        )
        mTtsSilenceDuration = mSettings!!.getInt(R.string.config_tts_silence_duration)
        //【可选配置】是否在文本的每句结尾处添加静音段，单位：毫秒，默认为 0ms
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_SILENCE_DURATION_INT,
            mTtsSilenceDuration
        )

        // ------------------------ 在线合成相关配置 -----------------------
        var curVoiceOnline = mSettings!!.getString(R.string.config_voice_online)
        if (curVoiceOnline.isEmpty()) {
            curVoiceOnline = mSettings!!.getOptionsValue(R.string.config_voice_online)
        }
        mCurVoiceOnline = curVoiceOnline
        Log.d(SpeechDemoDefines.TAG, "Current online voice: $mCurVoiceOnline")
        //【必需配置】在线合成使用的发音人代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_ONLINE_STRING,
            mCurVoiceOnline
        )
        var curVoiceTypeOnline = mSettings!!.getString(R.string.config_voice_type_online)
        if (curVoiceTypeOnline.isEmpty()) {
            curVoiceTypeOnline = mSettings!!.getOptionsValue(R.string.config_voice_type_online)
        }
        mCurVoiceTypeOnline = curVoiceTypeOnline
        Log.d(SpeechDemoDefines.TAG, "Current online voice type: $mCurVoiceTypeOnline")
        //【必需配置】在线合成使用的音色代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_TYPE_ONLINE_STRING,
            mCurVoiceTypeOnline
        )

        //【可选配置】是否打开在线合成的服务端缓存，默认关闭
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_CACHE_BOOL,
            mSettings!!.getBoolean(R.string.enable_cache)
        )
        //【可选配置】指定在线合成的语种，默认为空，即不指定
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_LANGUAGE_ONLINE_STRING,
            mSettings!!.getString(R.string.config_tts_language_online)
        )
        //【可选配置】是否启用在线合成的情感预测功能
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_WITH_INTENT_BOOL,
            mSettings!!.getBoolean(R.string.config_tts_with_intent)
        )
        //【可选配置】指定在线合成的情感，例如 happy, sad 等
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_EMOTION_STRING,
            mSettings!!.getString(R.string.config_tts_emotion)
        )
        //【可选配置】需要返回详细的播放进度或需要启用断点续播功能时应配置为 1, 否则配置为 0 或不配置
        mSpeechEngine!!.setOptionInt(SpeechEngineDefines.PARAMS_KEY_TTS_WITH_FRONTEND_INT, 1)
        //【可选配置】需要返回字粒度的播放进度时应配置为 simple, 同时要求 PARAMS_KEY_TTS_WITH_FRONTEND_INT 也配置为 1; 默认为空
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_FRONTEND_TYPE_STRING,
            if (mSettings!!.getBoolean(R.string.config_tts_enable_word_level_progress_update)) "simple" else ""
        )
        //【可选配置】使用复刻音色
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_USE_VOICECLONE_BOOL,
            mSettings!!.getBoolean(R.string.config_tts_use_voiceclone)
        )
        //【可选配置】在开启前述使用复刻音色的开关后，制定复刻音色所用的后端集群
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_BACKEND_CLUSTER_STRING,
            mSettings!!.getString(R.string.config_backend_cluster)
        )

        // ------------------------ 离线合成相关配置 -----------------------
        var curVoiceOffline = mSettings!!.getString(R.string.config_voice_offline)
        if (curVoiceOffline.isEmpty()) {
            curVoiceOffline = mSettings!!.getOptionsValue(R.string.config_voice_offline)
        }
        mCurVoiceOffline = curVoiceOffline
        Log.d(SpeechDemoDefines.TAG, "Current offline voice: $mCurVoiceOffline")
        //【必需配置】离线合成使用的发音人代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_OFFLINE_STRING,
            mCurVoiceOffline
        )
        var curVoiceTypeOffline = mSettings!!.getString(R.string.config_voice_type_offline)
        if (curVoiceTypeOffline.isEmpty()) {
            curVoiceTypeOffline = mSettings!!.getOptionsValue(R.string.config_voice_type_offline)
        }
        mCurVoiceTypeOffline = curVoiceTypeOffline
        Log.d(SpeechDemoDefines.TAG, "Current offline voice type: $mCurVoiceTypeOffline")
        //【必需配置】离线合成使用的音色代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_TYPE_OFFLINE_STRING,
            mCurVoiceTypeOffline
        )

        //【可选配置】是否降低离线合成的 CPU 利用率，默认关闭
        // 打开该配置会使离线合成的实时率变大，仅当必要（例如为避免系统主动杀死CPU占用持续过高的进程）时才应开启
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_LIMIT_CPU_USAGE_BOOL,
            mSettings!!.getBoolean(R.string.tts_limit_cpu_usage)
        )
    }

    private fun sendSynthesisDirectiveFailed(tipText: String) {
        Log.e(SpeechDemoDefines.TAG, tipText)

            setResultText(tipText)
            mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_STOP_ENGINE, "")

    }

    override fun onSpeechMessage(type: Int, data: ByteArray, len: Int) {
        var stdData = ""
        stdData = String(data)
        when (type) {
            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> {
                // Callback: 引擎启动成功回调
                Log.i(SpeechDemoDefines.TAG, "Callback: 引擎启动成功: data: $stdData")
                speechStart(stdData)
            }

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP -> {
                // Callback: 引擎关闭回调
                Log.i(SpeechDemoDefines.TAG, "Callback: 引擎关闭: data: $stdData")
                speechStop(stdData)
            }

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR -> {
                // Callback: 错误信息回调
                Log.e(SpeechDemoDefines.TAG, "Callback: 错误信息: $stdData")
                speechError(stdData)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_SYNTHESIS_BEGIN -> {
                // Callback: 合成开始回调
                Log.e(SpeechDemoDefines.TAG, "Callback: 合成开始: $stdData")
                speechStartSynthesis(stdData)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_SYNTHESIS_END -> {
                // Callback: 合成结束回调
                Log.e(SpeechDemoDefines.TAG, "Callback: 合成结束: $stdData")
                speechFinishSynthesis(stdData)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_START_PLAYING -> {
                // Callback: 播放开始回调
                Log.e(SpeechDemoDefines.TAG, "Callback: 播放开始: $stdData")
                speechStartPlaying(stdData)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_PLAYBACK_PROGRESS -> {
                // Callback: 播放进度回调
                Log.e(SpeechDemoDefines.TAG, "Callback: 播放进度")
                speechPlayingProgress(stdData)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_FINISH_PLAYING -> {
                // Callback: 播放结束回调
                Log.e(SpeechDemoDefines.TAG, "Callback: 播放结束: $stdData")
                speechFinishPlaying(stdData)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_AUDIO_DATA -> {
                // Callback: 音频数据回调
                Log.e(
                    SpeechDemoDefines.TAG,
                    String.format("Callback: 音频数据，长度 %d 字节", stdData.length)
                )
                speechTtsAudioData(data, false)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_AUDIO_DATA_END -> {
                // Callback: 音频数据回调
                Log.e(
                    SpeechDemoDefines.TAG,
                    String.format("Callback: 音频数据，长度 %d 字节", stdData.length)
                )
                speechTtsAudioData(ByteArray(0), true)
            }

            else -> {}
        }
    }

    private fun speechEnginInitSucceeded(initCost: Long) {
        Log.i(SpeechDemoDefines.TAG, "引擎初始化成功!")
    }

    private fun speechEngineInitFailed(tipText: String?) {
        Log.e(SpeechDemoDefines.TAG, "引擎初始化失败: $tipText")
    }

    private fun createConnectionSucceeded(tipText: String) {
        Log.e(SpeechDemoDefines.TAG, "在线合成提前建连成功: $tipText")
    }

    private fun createConnectionFailed(tipText: String) {
        Log.e(SpeechDemoDefines.TAG, "在线合成提前建连失败: $tipText")
    }

    private fun sendStartEngineDirectiveFailed(tipText: String) {
        Log.e(SpeechDemoDefines.TAG, tipText)
    }

    private fun speechStart(data: String) {
        mEngineStarted = true
        if (mSettings?.getBoolean(R.string.config_demo_player) == true && mStreamPlayer != null) {
            mStreamPlayer!!.Start()
        }
    }

    private fun speechStop(data: String) {
        mEngineStarted = false
        if (mSettings?.getBoolean(R.string.config_demo_player) == true && mStreamPlayer != null) {
            mStreamPlayer!!.Feed(ByteArray(0), true)
            mStreamPlayer!!.WaitPlayerStop()
        }


        // Abandon audio focus when playback complete
        mAudioManager!!.abandonAudioFocus(mAFChangeListener)
        mPlaybackNowAuthorized = false
    }

    private fun speechError(data: String) {
        mEngineErrorOccurred = true
    }



    private fun speechStartPlaying(data: String) {}
    private fun speechPlayingProgress(data: String) {
        try {
            val reader = JSONObject(data)
            if (!reader.has("reqid") || !reader.has("progress")) {
                Log.w(SpeechDemoDefines.TAG, "Can't find necessary field in progress callback. ")
                return
            }
            val percentage = reader.getDouble("progress")
            val reqid = reader.getString("reqid")
            Log.d(
                SpeechDemoDefines.TAG,
                "当前播放的文本对应的 reqid: $reqid, 播放进度：$percentage"
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun speechFinishPlaying(data: String) {}
    private fun speechTtsAudioData(data: ByteArray, isFinal: Boolean) {
        if (mSettings?.getBoolean(R.string.config_demo_player) == true && mStreamPlayer != null) {
            mStreamPlayer!!.Feed(data, isFinal)
        }
    }

    private fun AcquireAudioFocus() {
        // 向系统请求 Audio Focus 并记录返回结果
        val res = mAudioManager!!.requestAudioFocus(
            mAFChangeListener, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            mPlaybackNowAuthorized = false
        } else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mPlaybackNowAuthorized = true
        }
    }

    private fun setResultText(text: String?) {
        mResult!!.append(text)
    }

    private fun clearResultText() {
    }
}
