package com.google.ai.sample

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngine.SpeechListener
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.bytedance.speech.speechengine.SpeechResourceManager
import com.bytedance.speech.speechengine.SpeechResourceManager.FetchResourceListener
import com.bytedance.speech.speechengine.SpeechResourceManagerGenerator
import com.google.ai.sample.settings.Settings
import com.google.ai.sample.util.SensitiveDefines
import com.google.ai.sample.util.SpeechDemoDefines
import com.google.ai.sample.util.SpeechStreamPlayer
import org.json.JSONException
import org.json.JSONObject

class TtsNormalActivity : BaseActivity(), SpeechListener, LifecycleObserver {
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

    // Settings
    protected var mSettings: Settings? = null

    // Engine
    private  var mSpeechEngine: SpeechEngine ? = null

    // UI
    private var mReferText: EditText? = null
    private var mResult: TextView? = null
    private var mEngineStatus: TextView? = null
    private var mEngineSwitch: Button? = null
    private var mCreateConnectionBtn: Button? = null
    private var mStartBtn: Button? = null
    private var mStopBtn: Button? = null
    private var mPauseResumeBtn: Button? = null

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

    // Android AudioTrack Playing
    private var mStreamPlayer: SpeechStreamPlayer? = null

    // Android Audio Manager
    private var mAFChangeListener: OnAudioFocusChangeListener? = null
    private var mAudioManager: AudioManager? = null
    private var mResumeOnFocusGain = true
    private var mPlaybackNowAuthorized = false

    // State shared between Init Engine and Start Engine
    private var mDisablePlayerReuse = false
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        // App in background
        Log.i(SpeechDemoDefines.TAG, "Application becomming background.")
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams", "HardwareIds")
     override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(SpeechDemoDefines.TAG, "Tts onCreate")
        super.onCreate(savedInstanceState)
        SpeechEngineGenerator.PrepareEnvironment(applicationContext, application)
        setContentView(R.layout.activity_tts_normal)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        setTitleBar(R.string.tts_one_name)
        val viewId: String = SpeechDemoDefines.TTS_VIEW
        mSettings = SettingsActivity.getSettings(viewId)
        mReferText = findViewById(R.id.refer_text)
        mReferText!!.isEnabled = true
        mResult = findViewById(R.id.result_text)
        mResult!!.movementMethod = ScrollingMovementMethod()
        mEngineStatus = findViewById(R.id.engine_status)
        val mConfig: Button = findViewById(R.id.engine_config)
        mConfig.isEnabled = true
        mConfig.setOnClickListener { v: View? ->
            goToSettingsActivity(
                viewId
            )
        }
        mEngineSwitch = findViewById(R.id.engine_switch)
        setButton(mEngineSwitch, true)
        mEngineSwitch!!.setOnClickListener { v: View? -> switchEngine() }
        mCreateConnectionBtn = findViewById(R.id.create_connection_button)
        setButton(mCreateConnectionBtn, false)
        mCreateConnectionBtn!!.setOnClickListener { v: View? -> createConnection() }
        mStartBtn = findViewById(R.id.start_engine_button)
        setButton(mStartBtn, false)
        mStartBtn!!.setOnClickListener { v: View? -> startEngineBtnClicked() }
        mStopBtn = findViewById(R.id.stop_engine_button)
        setButton(mStopBtn, false)
        mStopBtn!!.setOnClickListener { v: View? -> stopEngineBtnClicked() }
        mPauseResumeBtn = findViewById(R.id.pause_resume_button)
        setButton(mPauseResumeBtn, false)
        mPauseResumeBtn!!.setOnClickListener { v: View? -> controlPlayingStatus() }
        val serviceIntent = Intent(this, ForegroundService::class.java)
        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android")
        ContextCompat.startForegroundService(this, serviceIntent)
        mStreamPlayer = MainActivity.getStreamPlayer()
        if (mDebugPath.isEmpty()) {
            mDebugPath = getDebugPath()
        }
        Log.i(SpeechDemoDefines.TAG, "当前调试路径：$mDebugPath")
        mAFChangeListener = OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Log.d(
                        SpeechDemoDefines.TAG,
                        "onAudioFocusChange: AUDIOFOCUS_GAIN, $mResumeOnFocusGain"
                    )
                    if (mResumeOnFocusGain) {
                        mResumeOnFocusGain = false
                        resumePlayback()
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    Log.d(SpeechDemoDefines.TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS")
                    mResumeOnFocusGain = false
                    pausePlayback()
                    mPlaybackNowAuthorized = false
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    Log.d(SpeechDemoDefines.TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT")
                    mResumeOnFocusGain = mEngineStarted
                    pausePlayback()
                }
            }
        }
        mAudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    }

    override fun onDestroy() {
        Log.i(SpeechDemoDefines.TAG, "Tts onDestroy")
        uninitEngine()
        val serviceIntent = Intent(this, ForegroundService::class.java)
        stopService(serviceIntent)
        super.onDestroy()
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
        if (mResourceManager != null && mCurTtsWorkMode != SpeechEngineDefines.TTS_WORK_MODE_ONLINE && mCurTtsWorkMode != SpeechEngineDefines.TTS_WORK_MODE_FILE) {
            var ttsResourcePath = ""
            if (mSettings?.getOptionsValue(R.string.tts_offline_resource_format_title, this)
                    .equals("MultipleVoice")
            ) {
                ttsResourcePath =
                    mResourceManager!!.getResourcePath(mSettings?.getString(R.string.config_tts_model_name)
                        ?: "")
            } else if (mSettings?.getOptionsValue(R.string.tts_offline_resource_format_title, this)
                    .equals("SingleVoice")
            ) {
                ttsResourcePath = mResourceManager!!.resourcePath
            }
            Log.d(SpeechDemoDefines.TAG, "tts resource root path:$ttsResourcePath")
            //【必需配置】离线合成所需资源存放路径
            mSpeechEngine!!.setOptionString(
                SpeechEngineDefines.PARAMS_KEY_TTS_OFFLINE_RESOURCE_PATH_STRING,
                ttsResourcePath
            )
        }

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

    private fun configStartTtsParams() {
        //【必需配置】TTS 使用场景
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_SCENARIO_STRING,
            SpeechEngineDefines.TTS_SCENARIO_TYPE_NORMAL
        )
        val ttsText = mReferText!!.text.toString()
        mCurTtsText = if (!ttsText.isEmpty()) {
            ttsText
        } else {
            "愿中国青年都摆脱冷气，只是向上走，不必听自暴自弃者流的话。能做事的做事，能发声的发声。有一分热，发一分光。就令萤火一般，也可以在黑暗里发一点光，不必等候炬火。此后如竟没有炬火：我便是唯一的光。"
        }
        //【必需配置】需合成的文本，不可超过 80 字
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_TTS_TEXT_STRING, mCurTtsText)
        //【可选配置】需合成的文本的类型，支持直接传文本(TTS_TEXT_TYPE_PLAIN)和传 SSML 形式(TTS_TEXT_TYPE_SSML)的文本
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_TEXT_TYPE_STRING,
            mTtsTextTypeArray[mSettings?.getOptions(R.string.tts_text_type_title)!!.chooseIdx]
        )
        //【可选配置】用于控制 TTS 音频的语速，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
        mSettings?.let {
            mSpeechEngine!!.setOptionDouble(
                SpeechEngineDefines.PARAMS_KEY_TTS_SPEED_RATIO_DOUBLE,
                it.getDouble(R.string.config_tts_speak_speed)
            )
        }
        //【可选配置】用于控制 TTS 音频的音量，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
        mSettings?.let {
            mSpeechEngine!!.setOptionDouble(
                SpeechEngineDefines.PARAMS_KEY_TTS_VOLUME_RATIO_DOUBLE,
                it.getDouble(R.string.config_tts_audio_volume)
            )
        }
        //【可选配置】用于控制 TTS 音频的音高，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
        mSettings?.let {
            mSpeechEngine!!.setOptionDouble(
                SpeechEngineDefines.PARAMS_KEY_TTS_PITCH_RATIO_DOUBLE,
                it.getDouble(R.string.config_tts_audio_pitch)
            )
        }
        mTtsSilenceDuration = (mSettings?.getInt(R.string.config_tts_silence_duration) ?:
        //【可选配置】是否在文本的每句结尾处添加静音段，单位：毫秒，默认为 0ms
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_SILENCE_DURATION_INT,
            mTtsSilenceDuration
        )) as Int
        if (mDisablePlayerReuse) {
            //【可选配置】用于控制 SDK 播放器所用的音源,默认为媒体音源
            mSettings?.let {
                mSpeechEngine!!.setOptionInt(
                    SpeechEngineDefines.PARAMS_KEY_AUDIO_STREAM_TYPE_INT,
                    it.getInt(R.string.config_player_stream_type)
                )
            }
        }
        //【可选配置】是否使用 SDK 内置播放器播放合成出的音频，默认为 true
        mSettings?.let {
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_PLAYER_BOOL,
                it.getBoolean(R.string.config_sdk_player)
            )
        }
        //【可选配置】是否令 SDK 通过回调返回合成的音频数据，默认不返回。
        // 开启后，SDK 会流式返回音频，收到 MESSAGE_TYPE_TTS_AUDIO_DATA_END 回调表示当次合成所有的音频已经全部返回
        mSettings?.getInt(R.string.config_tts_data_callback)?.let {
            mSpeechEngine!!.setOptionInt(
                SpeechEngineDefines.PARAMS_KEY_TTS_DATA_CALLBACK_MODE_INT,
                it
            )
        }

        // ------------------------ 在线合成相关配置 -----------------------
        var curVoiceOnline: String = mSettings?.getString(R.string.config_voice_online) ?: ""
        if (curVoiceOnline.isEmpty()) {
            curVoiceOnline = mSettings?.getOptionsValue(R.string.config_voice_online) ?: ""
        }
        mCurVoiceOnline = curVoiceOnline
        Log.d(SpeechDemoDefines.TAG, "Current online voice: $mCurVoiceOnline")
        //【必需配置】在线合成使用的发音人代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_ONLINE_STRING,
            mCurVoiceOnline
        )
        var curVoiceTypeOnline: String = mSettings?.getString(R.string.config_voice_type_online) ?: ""
        if (curVoiceTypeOnline.isEmpty()) {
            curVoiceTypeOnline = mSettings?.getOptionsValue(R.string.config_voice_type_online) ?: ""
        }
        mCurVoiceTypeOnline = curVoiceTypeOnline
        Log.d(SpeechDemoDefines.TAG, "Current online voice type: $mCurVoiceTypeOnline")
        //【必需配置】在线合成使用的音色代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_TYPE_ONLINE_STRING,
            mCurVoiceTypeOnline
        )

        //【可选配置】是否打开在线合成的服务端缓存，默认关闭
        mSettings?.let {
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_CACHE_BOOL,
                it.getBoolean(R.string.enable_cache)
            )
        }
        //【可选配置】指定在线合成的语种，默认为空，即不指定
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_LANGUAGE_ONLINE_STRING,
            ""
        )
        //【可选配置】是否启用在线合成的情感预测功能
        mSettings?.let {
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_TTS_WITH_INTENT_BOOL,
                it.getBoolean(R.string.config_tts_with_intent)
            )
        }
        //【可选配置】指定在线合成的情感，例如 happy, sad 等
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_EMOTION_STRING,
            mSettings?.getString(R.string.config_tts_emotion) ?: ""
        )
        //【可选配置】需要返回详细的播放进度或需要启用断点续播功能时应配置为 1, 否则配置为 0 或不配置
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_WITH_FRONTEND_INT,
            if (mSettings?.getBoolean(R.string.config_tts_enable_resume_from_breakpoint) == true) 1 else 0
        )
        //【可选配置】使用复刻音色
        mSettings?.let {
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_TTS_USE_VOICECLONE_BOOL,
                it.getBoolean(R.string.config_tts_use_voiceclone)
            )
        }
        //【可选配置】在开启前述使用复刻音色的开关后，制定复刻音色所用的后端集群
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_BACKEND_CLUSTER_STRING,
            mSettings?.getString(R.string.config_backend_cluster) ?: ""
        )

        // ------------------------ 离线合成相关配置 -----------------------
        var curVoiceOffline: String = mSettings?.getString(R.string.config_voice_offline) ?: ""
        if (curVoiceOffline.isEmpty()) {
            curVoiceOffline = mSettings?.getOptionsValue(R.string.config_voice_offline) ?: ""
        }
        mCurVoiceOffline = curVoiceOffline
        Log.d(SpeechDemoDefines.TAG, "Current offline voice: $mCurVoiceOffline")
        //【必需配置】离线合成使用的发音人代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_OFFLINE_STRING,
            mCurVoiceOffline
        )
        var curVoiceTypeOffline: String =
            mSettings?.getString(R.string.config_voice_type_offline) ?:""
        if (curVoiceTypeOffline.isEmpty()) {
            curVoiceTypeOffline = mSettings?.getOptionsValue(R.string.config_voice_type_offline) ?: ""
        }
        mCurVoiceTypeOffline = curVoiceTypeOffline
        Log.d(
            SpeechDemoDefines.TAG,
            "Current offline voice type: $mCurVoiceTypeOffline"
        )
        //【必需配置】离线合成使用的音色代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_TYPE_OFFLINE_STRING,
            mCurVoiceTypeOffline
        )

        //【可选配置】是否降低离线合成的 CPU 利用率，默认关闭
        // 打开该配置会使离线合成的实时率变大，仅当必要（例如为避免系统主动杀死CPU占用持续过高的进程）时才应开启
        mSettings?.let {
            mSpeechEngine!!.setOptionBoolean(
                SpeechEngineDefines.PARAMS_KEY_TTS_LIMIT_CPU_USAGE_BOOL,
                it.getBoolean(R.string.tts_limit_cpu_usage)
            )
        }
    }

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
        } else {
            try {
                if (mResourceManager == null) {
                    mResourceManager = SpeechResourceManagerGenerator.getInstance()
                }
            } catch (e: RuntimeException) {
                speechEngineInitFailed(e.message)
                return
            }
            // 下载离线合成所需资源需要区分多音色资源和单音色资源，下载这两种资源所调用的方法略有不同
            if (mSettings!!.getOptionsValue(R.string.tts_offline_resource_format_title, this)
                    .equals("MultipleVoice")
            ) {
                // 多音色资源是指一个资源文件中包含了多个离线音色，这种资源一般是旧版(V2)离线合成所用资源
                Log.i(SpeechDemoDefines.TAG, "当前所用资源类别为多音色资源，开始准备多音色资源")
                prepareMultipleVoiceResource()
            } else if (mSettings!!.getOptionsValue(R.string.tts_offline_resource_format_title, this)
                    .equals("SingleVoice")
            ) {
                // 单音色资源是指一个资源文件仅包含一个离线音色，新版(V4 及以上)离线合成用的就是单音色资源
                Log.i(SpeechDemoDefines.TAG, "当前所用资源类别为单音色资源，开始准备单音色资源")
                prepareSingleVoiceResource()
            }
        }
    }

    private fun prepareMultipleVoiceResource() {
        Log.i(SpeechDemoDefines.TAG, "初始化模型资源管理器")
        mResourceManager!!.initResourceManager(
            getApplicationContext(),
            "0",
            mCurAppId,
            SensitiveDefines.APP_VERSION,
            true,
            mDebugPath
        )
        // 因为多音色资源的一个文件包含了多个音色，导致资源的名字和音色的名字无法一一对应
        // 所以下载资源需要显式指定资源名字
        val resourceName: String = mSettings?.getString(R.string.config_tts_model_name) ?: ""
        Log.i(SpeechDemoDefines.TAG, "检查本地是否存在可用资源")
        if (!mResourceManager!!.checkResourceDownload(resourceName)) {
            Log.i(SpeechDemoDefines.TAG, "本地没有资源，开始下载")
            fetchMultipleVoiceResource(resourceName)
        } else {
            Log.i(SpeechDemoDefines.TAG, "资源存在，检查资源是否需要升级")
            mResourceManager!!.checkResourceUpdate(
                resourceName
            ) { needUpdate ->
                if (needUpdate) {
                    Log.i(SpeechDemoDefines.TAG, "存在可用升级，开始升级")
                    fetchMultipleVoiceResource(resourceName)
                } else {
                    Log.i(SpeechDemoDefines.TAG, "不存在可用升级，使用本地已有模型")
                    initEngineInternal()
                }
            }
        }
    }

    private fun fetchMultipleVoiceResource(resourceName: String) {
        Log.i(SpeechDemoDefines.TAG, "需要下载的资源名为: $resourceName")
        mResourceManager!!.fetchResourceByName(resourceName,
            object : FetchResourceListener {
                override fun onSuccess() {
                    Log.i(SpeechDemoDefines.TAG, "资源下载成功")
                    initEngineInternal()
                }

                override fun onFailed(errorMsg: String) {
                    Log.i(SpeechDemoDefines.TAG, "资源下载失败，错误：$errorMsg")
                    speechEngineInitFailed("Download tts resource failed.")
                }
            })
    }

    private fun prepareSingleVoiceResource() {
        mResourceManager!!.setAppVersion(SensitiveDefines.APP_VERSION)
        mResourceManager!!.setAppId(mCurAppId)
        val androidId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID)
        Log.i(SpeechDemoDefines.TAG, "Current device android id: $androidId")
        mResourceManager!!.setDeviceId(androidId)
        mResourceManager!!.setUseOnlineModel(true)
        mResourceManager!!.setEngineName(SpeechEngineDefines.TTS_ENGINE)
        Log.i(SpeechDemoDefines.TAG, "初始化模型资源管理器")
        mResourceManager!!.initResourceManager(getApplicationContext(), mDebugPath)
        var needDownloadVoiceType: Array<String> =
            SensitiveDefines.TTS_DEFAULT_DOWNLOAD_OFFLINE_VOICES
        val voiceTypeArray: List<String> =
            mSettings?.getOptions(R.string.config_voice_type_offline)!!.arrayObj
        if (voiceTypeArray != null && !voiceTypeArray.isEmpty()) {
            needDownloadVoiceType = voiceTypeArray.toTypedArray<String>()
        }
        Log.d(
            SpeechDemoDefines.TAG,
            "离线合成将会使用的音色有： " + needDownloadVoiceType.contentToString()
        )
        mResourceManager!!.setTtsVoiceType(needDownloadVoiceType)
        var offlineLanguage: String = mSettings?.getString(R.string.config_tts_language_offline) ?: ""
        if (offlineLanguage.isEmpty()) {
            offlineLanguage = SensitiveDefines.TTS_DEFAULT_OFFLINE_LANGUAGE
        }
        val needDownloadLanauges = arrayOf(offlineLanguage)
        Log.d(SpeechDemoDefines.TAG, "需要下载的离线合成语种资源有： $offlineLanguage")
        mResourceManager!!.setTtsLanguage(arrayOf(offlineLanguage))
        Log.i(SpeechDemoDefines.TAG, "检查本地是否存在可用资源")
        if (!mResourceManager!!.checkResourceDownload()) {
            Log.i(SpeechDemoDefines.TAG, "本地没有资源，开始下载")
            fetchSingleVoiceResource()
        } else {
            Log.i(SpeechDemoDefines.TAG, "资源存在，检查资源是否需要升级")
            mResourceManager!!.checkResourceUpdate { needUpdate ->
                if (needUpdate) {
                    Log.i(SpeechDemoDefines.TAG, "存在可用升级，开始升级")
                    fetchSingleVoiceResource()
                } else {
                    Log.i(SpeechDemoDefines.TAG, "不存在可用升级，使用本地已有模型")
                    initEngineInternal()
                }
            }
        }
    }

    private fun fetchSingleVoiceResource() {
        mResourceManager!!.fetchResource(object : FetchResourceListener {
            override fun onSuccess() {
                Log.i(SpeechDemoDefines.TAG, "资源下载成功")
                initEngineInternal()
            }

            override fun onFailed(errorMsg: String) {
                Log.i(SpeechDemoDefines.TAG, "资源下载失败，错误：$errorMsg")
                speechEngineInitFailed("Download tts resource failed.")
            }
        })
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

    private fun uninitEngine() {
        if (mSpeechEngine != null) {
            Log.i(SpeechDemoDefines.TAG, "引擎析构.")
            mSpeechEngine!!.destroyEngine()
            //mSpeechEngine = null
            Log.i(SpeechDemoDefines.TAG, "引擎析构完成!")
        }
    }

    private fun switchEngine() {
        if (mEngineStarted) {
            mEngineStatus?.setText(R.string.hint_engine_busy)
            return
        }
        clearResultText()
        setButton(mStartBtn, false)
        setButton(mPauseResumeBtn, false)
        if (mEngineInited) {
            setButton(mEngineSwitch, true)
            uninitEngine()
            mEngineInited = false
            mEngineStatus?.setText(R.string.hint_waiting_init)
            mEngineSwitch?.setText(R.string.init_engine_title)
            setButton(mStopBtn, false)
            setButton(mCreateConnectionBtn, false)
            mConnectionCreated = false
            mReferText!!.isEnabled = false
        } else {
            mReferText!!.isEnabled = false
            initEngine()
        }
    }

    private fun createConnection() {
        if (mConnectionCreated) {
            Log.i(SpeechDemoDefines.TAG, "Connection is created.")
            return
        }

        // DIRECTIVE_CREATE_CONNECTION 指令，可减小在线合成的端到端播放延时，主要应用在能够提前预知要使用语音合成的情况下，例如语音交互场景
        // DIRECTIVE_CREATE_CONNECTION 指令是一个同步指令，调用返回之后可以根据返回值判断连接是否建立成功
        // 如果不使用 DIRECTIVE_CREATE_CONNECTION 指令，建连实际发生在调用 DIRECTIVE_START_ENGINE 后
        Log.i(SpeechDemoDefines.TAG, "触发提前建连")
        Log.i(SpeechDemoDefines.TAG, "Directive: DIRECTIVE_CREATE_CONNECTION")
        val ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_CREATE_CONNECTION, "")
        if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
            val errorMessage = "在线合成提前建连失败: $ret"
            Log.e(SpeechDemoDefines.TAG, errorMessage)
            createConnectionFailed(errorMessage)
        } else {
            val message = "在线合成提前建连成功 $ret"
            Log.e(SpeechDemoDefines.TAG, message)
            createConnectionSucceeded(message)
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

    private fun stopEngineBtnClicked() {
        Log.i(SpeechDemoDefines.TAG, "关闭引擎（异步）")
        Log.i(SpeechDemoDefines.TAG, "Directive: DIRECTIVE_STOP_ENGINE")
        if (mEngineStarted) {
            mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_STOP_ENGINE, "")
        }
        if (mSettings?.getBoolean(R.string.config_demo_player) == true && mStreamPlayer != null) {
            mStreamPlayer!!.Stop()
        }
    }

    private fun pausePlayback() {
        Log.i(SpeechDemoDefines.TAG, "暂停播放")
        var ret = SpeechEngineDefines.ERR_NO_ERROR
        if (mSettings?.getBoolean(R.string.config_sdk_player) == true) {
            Log.i(SpeechDemoDefines.TAG, "Directive: DIRECTIVE_PAUSE_PLAYER")
            ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_PAUSE_PLAYER, "")
        }
        if (mSettings?.getBoolean(R.string.config_demo_player) == true && mStreamPlayer != null) {
            mStreamPlayer!!.Pause()
        }
        if (ret == SpeechEngineDefines.ERR_NO_ERROR) {
            mPlayerPaused = true
            mPauseResumeBtn!!.text = "Resume"
        }
        Log.d(SpeechDemoDefines.TAG, "Pause playback status:$ret")
    }

    private fun resumePlayback() {
        Log.i(SpeechDemoDefines.TAG, "继续播放")
        var ret = SpeechEngineDefines.ERR_NO_ERROR
        if (mSettings?.getBoolean(R.string.config_sdk_player) == true) {
            Log.i(SpeechDemoDefines.TAG, "Directive: DIRECTIVE_RESUME_PLAYER")
            ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_RESUME_PLAYER, "")
        }
        if (mSettings?.getBoolean(R.string.config_demo_player) == true && mStreamPlayer != null) {
            mStreamPlayer!!.Resume()
        }
        if (ret == SpeechEngineDefines.ERR_NO_ERROR) {
            mPlayerPaused = false
            mPauseResumeBtn!!.text = "Pause"
        }
        Log.d(SpeechDemoDefines.TAG, "Resume playback status:$ret")
    }

    private fun controlPlayingStatus() {
        Log.d(
            SpeechDemoDefines.TAG,
            "Pause or resume player, current player status: $mPlayerPaused"
        )
        if (mPlayerPaused) {
            if (!mPlaybackNowAuthorized) { // AudioFocus 被其他 APP 占用，需要再次获取
                AcquireAudioFocus()
            }
            resumePlayback()
        } else {
            pausePlayback()
        }
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
        this.runOnUiThread {
            mEngineStatus?.setText(R.string.hint_ready)
            mReferText!!.isEnabled = true
            setResultText("Initialize cost: " + initCost + "ms.")
            mEngineSwitch?.setText(R.string.uninit_engine_title)
            setButton(mEngineSwitch, true)
            setButton(
                mCreateConnectionBtn,
                mCurTtsWorkMode != SpeechEngineDefines.TTS_WORK_MODE_OFFLINE
            )
            setButton(mStartBtn, true)
            mEngineInited = true
        }
    }

    private fun speechEngineInitFailed(tipText: String?) {
        Log.e(SpeechDemoDefines.TAG, "引擎初始化失败: $tipText")
        this.runOnUiThread {
            setResultText(tipText)
            mEngineStatus?.setText(R.string.hint_setup_failure)
            setButton(mEngineSwitch, true)
            mEngineInited = false
        }
    }

    private fun createConnectionSucceeded(tipText: String) {
        Log.e(SpeechDemoDefines.TAG, "在线合成提前建连成功: $tipText")
        this.runOnUiThread {
            setResultText(tipText)
            setButton(mCreateConnectionBtn, false)
            mConnectionCreated = true
        }
    }

    private fun createConnectionFailed(tipText: String) {
        Log.e(SpeechDemoDefines.TAG, "在线合成提前建连失败: $tipText")
        this.runOnUiThread {
            setResultText(tipText)
            mConnectionCreated = false
        }
    }

    private fun sendStartEngineDirectiveFailed(tipText: String) {
        Log.e(SpeechDemoDefines.TAG, tipText)
        this.runOnUiThread {
            setResultText(tipText)
            mEngineStarted = false
        }
    }

    private fun speechStart(data: String) {
        mEngineStarted = true
        if (mSettings?.getBoolean(R.string.config_demo_player) == true && mStreamPlayer != null) {
            mStreamPlayer!!.Start()
        }
        this.runOnUiThread {
            mEngineStatus?.setText(R.string.hint_start_cb)
            mReferText!!.isEnabled = false
            setResultText(mCurTtsText)
            setButton(mStartBtn, false)
            setButton(mStopBtn, true)
            setButton(mCreateConnectionBtn, false)
            setButton(
                mPauseResumeBtn,
                mSettings?.getBoolean(R.string.config_sdk_player) ?: false || mSettings?.getBoolean(R.string.config_demo_player) ?: false
            )
        }
    }

    private fun speechStop(data: String) {
        mEngineStarted = false
        if (mSettings?.getBoolean(R.string.config_demo_player) == true && mStreamPlayer != null) {
            mStreamPlayer!!.Feed(ByteArray(0), true)
            mStreamPlayer!!.WaitPlayerStop()
        }
        this.runOnUiThread {
            mEngineStatus?.setText(R.string.hint_stop_cb)
            mPauseResumeBtn!!.text = "Pause"
            mReferText!!.isEnabled = true
            setButton(mStopBtn, false)
            setButton(mStartBtn, true)
            setButton(
                mCreateConnectionBtn,
                mCurTtsWorkMode != SpeechEngineDefines.TTS_WORK_MODE_OFFLINE
            )
            setButton(mPauseResumeBtn, false)
            mConnectionCreated = false
            mPlayerPaused = false
        }

        // Abandon audio focus when playback complete
        mAudioManager!!.abandonAudioFocus(mAFChangeListener)
        mPlaybackNowAuthorized = false
    }

    private fun speechError(data: String) {
        mEngineErrorOccurred = true
        this.runOnUiThread {
            try {
                val reader = JSONObject(data)
                if (!reader.has("err_code") || !reader.has("err_msg")) {
                    return@runOnUiThread
                }
                setResultText(data)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun speechStartSynthesis(data: String) {}
    private fun speechFinishSynthesis(data: String) {}
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
        this.runOnUiThread { mResult!!.text = "" }
    }
}
