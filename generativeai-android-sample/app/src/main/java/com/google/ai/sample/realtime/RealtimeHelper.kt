package com.google.ai.sample.realtime

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper.getMainLooper
import android.text.TextUtils
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tencent.aai.AAIClient
import com.tencent.aai.audio.utils.WavCache
import com.tencent.aai.auth.LocalCredentialProvider
import com.tencent.aai.config.ClientConfiguration
import com.tencent.aai.exception.ClientException
import com.tencent.aai.exception.ServerException
import com.tencent.aai.listener.AudioRecognizeResultListener
import com.tencent.aai.listener.AudioRecognizeStateListener
import com.tencent.aai.log.AAILogger
import com.tencent.aai.model.AudioRecognizeConfiguration
import com.tencent.aai.model.AudioRecognizeRequest
import com.tencent.aai.model.AudioRecognizeResult
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("DEPRECATED_IDENTITY_EQUALS")
class RealtimeHelper(var context: Context) {

//    var start: Button? = null
//    var cancel: Button? = null
//
//    var recognizeState: TextView? = null
//    var volume: TextView? = null
//
//    var voiceDb: TextView? = null
//
//    var recognizeResult: TextView? = null
//    var mScrollView: ScrollView? = null
//    var mIsCompressSW: Switch? = null
//    var mEnableAEC: Switch? = null
//    var mEnablePlayer: Switch? = null
//
//    var mSilentSwitch: Switch? = null
//    var mFileName: EditText? = null
    var mStream: FileOutputStream? = null

    var isRecording = false

    var isCompress = true //音频压缩，默认true


    var isOpenSilentCheck = false

    var handler: Handler? = null

    private val TAG: String = "RealtimeHelper"

    val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1

    var aaiClient: AAIClient? = null


    var isSaveAudioRecordFiles = false

    var mp: MediaPlayer? = null
    /**
     * 识别状态监听器
     */
    /**
     * 识别状态监听器
     */
    val audioRecognizeStateListener: AudioRecognizeStateListener =
        object : AudioRecognizeStateListener {
            var dataOutputStream: DataOutputStream? = null
            var fileName: String? = null
            var filePath: String? = null
            var mExecutorService: ExecutorService? = null
            var minVoiceDb = Float.MAX_VALUE
            var maxVoiceDb = Float.MIN_VALUE

            /**
             * 开始录音
             * @param request
             */
            override fun onStartRecord(request: AudioRecognizeRequest) {
                ShowMsg("")
                isRecording = true
                minVoiceDb = Float.MAX_VALUE
                maxVoiceDb = Float.MIN_VALUE
                AAILogger.info(TAG, "onStartRecord..")
//                handler!!.post {
//                    recognizeState?.setText(R.string.start_record)
//                    start!!.isEnabled = true
//                    start!!.text = "STOP"
//                }
                //为本次录音创建缓存一个文件
                if (isSaveAudioRecordFiles) {
                    if (mExecutorService == null) {
                        mExecutorService = Executors.newSingleThreadExecutor()
                    }
                    filePath ="${context.filesDir}/tencent_audio_sdk_cache"
//                    fileName = mFileName!!.text.toString() + ".pcm"
                    fileName = "test" + ".pcm"
                    if (fileName!!.isEmpty()) {
                        fileName = System.currentTimeMillis().toString() + ".pcm"
                    }
                    dataOutputStream = WavCache.creatPmcFileByPath(filePath, fileName)
                }
            }

            /**
             * 结束录音
             * @param request
             */
            override fun onStopRecord(request: AudioRecognizeRequest) {
                AAILogger.info(TAG, "onStopRecord..")
                isRecording = false
//                handler!!.post {
//                    recognizeState?.setText(R.string.end_record)
//                    //                    start.setEnabled(true);
//                    start!!.text = "START"
//                }

                //如果设置了保存音频
                if (isSaveAudioRecordFiles) {
                    mExecutorService!!.execute {
                        WavCache.closeDataOutputStream(dataOutputStream)
                        WavCache.makePCMFileToWAVFile(filePath, fileName) //sdk内提供了一套pcm转wav工具类
                    }
                }
            }

            /**
             * 返回音频流，
             * 用于返回宿主层做录音缓存业务。
             * 由于方法跑在sdk线程上，这里多用于文件操作，宿主需要新开一条线程专门用于实现业务逻辑
             * @param audioDatas
             */
            override fun onNextAudioData(audioDatas: ShortArray, readBufferLength: Int) {
                if (isSaveAudioRecordFiles) {
                    mExecutorService!!.execute {
                        WavCache.savePcmData(
                            dataOutputStream,
                            audioDatas,
                            readBufferLength
                        )
                    }
                }
            }

            /**
             * 静音检测回调
             * 当设置AudioRecognizeConfiguration  setSilentDetectTimeOut为true时，如触发静音超时，将触发此回调
             * 当setSilentDetectTimeOutAutoStop 为true时，触发此回调的同时会停止本次识别，相当于手动调用了 aaiClient.stopAudioRecognize()
             */
            override fun onSilentDetectTimeOut() {
                Log.d(TAG, "onSilentDetectTimeOut: ")
                //您的业务逻辑
            }

            /**
             * 音量变化时回调。该方法已废弃
             *
             * 建议使用 [.onVoiceDb]
             *
             */
            @Deprecated("建议使用 {@link #onVoiceDb(float db)}.")
            override fun onVoiceVolume(request: AudioRecognizeRequest, volume: Int) {
                AAILogger.info(TAG, "onVoiceVolume..")
//                handler!!.post { this@RealtimeHelper.volume?.setText(R.string.volume+ volume) }
            }

            override fun onVoiceDb(volumeDb: Float) {
                AAILogger.info(TAG, "onVoiceDb: $volumeDb")
                handler!!.post {
                    if (volumeDb > maxVoiceDb) {
                        maxVoiceDb = volumeDb
                    }
                    if (volumeDb < minVoiceDb) {
                        minVoiceDb = volumeDb
                    }
                    if (minVoiceDb != Float.MAX_VALUE && maxVoiceDb != Float.MIN_VALUE) {
//                        voiceDb?.text = (R.string.voice_db + volumeDb
//                                ).toString() + "(" + minVoiceDb + " ~ " + maxVoiceDb + ")"
                    }
                }
            }
        }

    // 识别结果回调监听器
    val audioRecognizeResultlistener: AudioRecognizeResultListener =
        object : AudioRecognizeResultListener {
            /**
             * 返回分片的识别结果
             * @param request 相应的请求
             * @param result 识别结果
             * @param seq 该分片所在句子的序号 (0, 1, 2...)
             * 此为中间态结果，会被持续修正
             */
            override fun onSliceSuccess(
                request: AudioRecognizeRequest,
                result: AudioRecognizeResult,
                seq: Int
            ) {
                AAILogger.info(TAG, "分片on slice success..")
                AAILogger.info(
                    TAG,
                    "分片slice seq =" + seq + "voiceid =" + result.voiceId + "result = " + result.text + "startTime =" + result.startTime + "endTime = " + result.endTime
                )
                AAILogger.info(
                    TAG,
                    "分片on slice success..   ResultJson =" + result.resultJson
                ) //后端返回的未解析的json文本，您可以自行解析获取更多信息
                resMap[seq.toString()] = result.text
                val msg = buildMessage(resMap)
                AAILogger.info(TAG, "分片slice msg=$msg")
                ShowMsg(msg)
            }

            /**
             * 返回语音流的识别结果
             * @param request 相应的请求
             * @param result 识别结果
             * @param seq 该句子的序号 (1, 2, 3...)
             * 此为稳定态结果，可做为识别结果用与业务
             */
            override fun onSegmentSuccess(
                request: AudioRecognizeRequest,
                result: AudioRecognizeResult,
                seq: Int
            ) {
                AAILogger.info(TAG, "语音流on segment success")
                AAILogger.info(
                    TAG,
                    "语音流segment seq =" + seq + "voiceid =" + result.voiceId + "result = " + result.text + "startTime =" + result.startTime + "endTime = " + result.endTime
                )
                AAILogger.info(
                    TAG,
                    "语音流on segment success..   ResultJson =" + result.resultJson
                ) //后端返回的未解析的json文本，您可以自行解析获取更多信息
                resMap[seq.toString()] = result.text
                val msg = buildMessage(resMap)
                AAILogger.info(TAG, "语音流segment msg=$msg")
                Log.d(TAG,"segment + $msg")
                ShowMsg(msg)
                listener.onSegmentSuccess(msg)
            }

            /**
             * 识别结束回调，返回所有的识别结果
             * @param request 相应的请求
             * @param result 识别结果,sdk内会把所有的onSegmentSuccess结果合并返回，如果业务不需要，可以只使用onSegmentSuccess返回的结果
             * 注意：仅收到onStopRecord回调时，表明本次语音流录音任务已经停止，但识别任务还未停止，需要等待后端返回最终识别结果，
             * 如果此时立即启动下一次录音，结果上一次结果仍会返回，可以调用cancelAudioRecognize取消上一次识别任务
             * 当收到 onSuccess 或者  onFailure时，表明本次语音流识别完毕，可以进行下一次识别；
             */
            override fun onSuccess(request: AudioRecognizeRequest, result: String) {
//                handler!!.post { start!!.isEnabled = true }
                AAILogger.info(TAG, "识别结束, onSuccess..")
                AAILogger.info(TAG, "识别结束, result = $result")
                Log.d(TAG,"识别结束, result = $result")
                listener.onSuccess(result)

            }

            /**
             * 识别失败
             * @param request 相应的请求
             * @param clientException 客户端异常
             * @param serverException 服务端异常
             * @param response   服务端返回的json字符串（如果有）
             * 注意：仅收到onStopRecord回调时，表明本次语音流录音任务已经停止，但识别任务还未停止，需要等待后端返回最终识别结果，
             * 如果此时立即启动下一次录音，结果上一次结果仍会返回，可以调用cancelAudioRecognize取消上一次识别任务
             * 当收到 onSuccess 或者  onFailure时，表明本次语音流识别完毕，可以进行下一次识别；
             */
            override fun onFailure(
                request: AudioRecognizeRequest,
                clientException: ClientException,
                serverException: ServerException,
                response: String
            ) {
                if (response != null) {
                    AAILogger.info(TAG, "onFailure response.. :$response")
                }
                if (clientException != null) {
                    AAILogger.info(TAG, "onFailure..:$clientException")
                }
                if (serverException != null) {
                    AAILogger.info(TAG, "onFailure..:$serverException")
                }
//                handler!!.post {
//                    start!!.isEnabled = true
//                    if (clientException != null) {
//                        recognizeState!!.text = "识别状态：失败,  $clientException"
//                        ShowMsg("识别状态：失败,  $clientException")
//                        AAILogger.info(TAG, "识别状态：失败,  $clientException")
//                    } else if (serverException != null) {
//                        recognizeState!!.text = "识别状态：失败,  $serverException"
//                        ShowMsg("识别状态：失败,  $serverException")
//                    }
//                }
            }
        }
    private fun checkPermissions() {
        val permissions: MutableList<String> = LinkedList()
        addPermission(permissions, Manifest.permission.RECORD_AUDIO)
        addPermission(permissions, Manifest.permission.INTERNET)
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                context as Activity, permissions.toTypedArray(),
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun addPermission(permissionList: MutableList<String>, permission: String) {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            permissionList.add(permission)
        }
    }

    var resMap = LinkedHashMap<String, String>()
    private fun buildMessage(msg: Map<String, String>): String {
        val stringBuffer = StringBuffer()
        val iter = msg.entries.iterator()
        while (iter.hasNext()) {
            val value = iter.next().value
            stringBuffer.append(value + "\r\n")
        }
        return stringBuffer.toString()
    }
    init {

        handler = Handler(getMainLooper())
        // 检查sdk运行的必要条件权限


        // 检查sdk运行的必要条件权限
        checkPermissions()

        // 用户配置：需要在控制台申请相关的账号;

        // 用户配置：需要在控制台申请相关的账号;
        val appid: Int
        if (!TextUtils.isEmpty(RealtimeConfig.apppId)) {
            appid = Integer.valueOf(RealtimeConfig.apppId)
        } else {
            appid = 0
        }
        //设置ProjectId 不设置默认使用0，说明：项目功能用于按项目管理云资源，可以对云资源进行分项目管理，详情见 https://console.cloud.tencent.com/project
        //设置ProjectId 不设置默认使用0，说明：项目功能用于按项目管理云资源，可以对云资源进行分项目管理，详情见 https://console.cloud.tencent.com/project
        val projectId = 0
        val secretId: String = RealtimeConfig.secretId
        val secretKey: String = RealtimeConfig.secretKey

        // okhttp全局配置

        // okhttp全局配置
        ClientConfiguration.setAudioRecognizeConnectTimeout(3000)
        ClientConfiguration.setAudioRecognizeWriteTimeout(5000)

        // 识别结果回调监听器

        if (aaiClient == null) {
            /**直接鉴权 */
            aaiClient = AAIClient(
                context,
                appid,
                projectId,
                secretId,
                LocalCredentialProvider(secretKey)
            )
            /**使用临时密钥鉴权
             * * 1.通过sts 获取到临时证书 （secretId secretKey  token） ,此步骤应在您的服务器端实现，见https://cloud.tencent.com/document/product/598/33416
             * 2.通过临时密钥调用接口
             */
//            aaiClient = new AAIClient(MainActivity.this, appid, projectId,"临时secretId", "临时secretKey","对应的token");
        }


//启动&停止 识别按钮


//启动&停止 识别按钮
//        start!!.setOnClickListener { v: View? ->
//            AAILogger.info(TAG, "the start button has clicked..")
//            handler!!.post { start!!.isEnabled = false }
//            if (isRecording) {
//                AAILogger.info(TAG, "stop button is clicked..")
//                Thread {
//                    if (aaiClient != null) {
//                        aaiClient!!.stopAudioRecognize()
//                    }
//                    if (mp != null) {
//                        mp!!.stop()
//                        mp = null
//                    }
//                }.start()
//            } else {
//                resMap.clear()
//                if (aaiClient != null) { //丢弃上一次结果
//                    val taskExist = aaiClient!!.cancelAudioRecognize()
//                    AAILogger.info(TAG, "taskExist=$taskExist")
//                }
//                isSaveAudioRecordFiles = false
//                if (!mFileName!!.text.toString().isEmpty()) {
//                    isSaveAudioRecordFiles = true
//                }
//                val dataSource =
//                    DemoAudioRecordDataSource(isSaveAudioRecordFiles, context)
//                if (mEnablePlayer!!.isChecked) {
//                    mp = MediaPlayer()
//                    var fd: AssetFileDescriptor? = null
//                    try {
//                        fd = context.assets.openFd("test2.mp3")
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                            mp!!.setDataSource(fd)
//                        } else {
//                            mp!!.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
//                        }
//                        mp!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
//                        mp!!.prepare()
//                        mp!!.start()
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                    }
//                }
//                dataSource.enableAEC(mEnableAEC!!.isChecked)
//                val builder = AudioRecognizeRequest.Builder()
//                // 初始化识别请求
//                val audioRecognizeRequest =
//                    builder //设置数据源，数据源要求实现PcmAudioDataSource接口，您可以自己实现此接口来定制您的自定义数据源，例如从第三方推流中获取音频数据
//                        //注意：音频数据必须为16k采样率的PCM音频，否则将导致语音识别输出错误的识别结果！！！！
//                        .pcmAudioDataSource(dataSource) //使用Demo提供的录音器源码作为数据源，源码与SDK内置录音器一致，您可以参考此源代码自由定制修改，详情查阅DemoAudioRecordDataSource.java内注释
//                        //                        .pcmAudioDataSource(new AudioRecordDataSource(isSaveAudioRecordFiles)) // 使用SDK内置录音器作为数据源
//                        .setEngineModelType("16k_zh") // 设置引擎(16k_zh--通用引擎，支持中文普通话+英文),更多引擎请关注官网文档https://cloud.tencent.com/document/product/1093/48982 ，引擎种类持续增加中
//                        .setFilterDirty(0) // 0 ：默认状态 不过滤脏话 1：过滤脏话
//                        .setFilterModal(0) // 0 ：默认状态 不过滤语气词  1：过滤部分语气词 2:严格过滤
//                        .setFilterPunc(0) // 0 ：默认状态 不过滤句末的句号 1：滤句末的句号
//                        .setConvert_num_mode(1) //1：默认状态 根据场景智能转换为阿拉伯数字；0：全部转为中文数字。
//                        //                        .setVadSilenceTime(1000) // 语音断句检测阈值，静音时长超过该阈值会被认为断句（多用在智能客服场景，需配合 needvad = 1 使用） 默认不传递该参数，不建议更改
//                        .setNeedvad(1) //0：关闭 vad，1：默认状态 开启 vad。语音时长超过一分钟需要开启,如果对实时性要求较高,并且时间较短
//                        // 的输入,建议关闭,可以显著降低onSliceSuccess结果返回的时延以及stop后onSegmentSuccess和onSuccess返回的时延
//                        //                        .setHotWordId("")//热词 id。用于调用对应的热词表，如果在调用语音识别服务时，不进行单独的热词 id 设置，自动生效默认热词；如果进行了单独的热词 id 设置，那么将生效单独设置的热词 id。
//                        .setWordInfo(1) //                        .setCustomizationId("")//自学习模型 id。如果设置了该参数，那么将生效对应的自学习模型,如果您不了解此参数，请不要设置
//                        //                        .setReinforceHotword(1)
//                        //                        .setNoiseThreshold(0)
//                        //                        .setMaxSpeakTime(5000) // 强制断句功能，取值范围 5000-90000(单位:毫秒），默认值0(不开启)。 在连续说话不间断情况下，该参数将实现强制断句（此时结果变成稳态，slice_type=2）。如：游戏解说场景，解说员持续不间断解说，无法断句的情况下，将此参数设置为10000，则将在每10秒收到 slice_type=2的回调。
//                        .build()
//
//                // 自定义识别配置
//                val audioRecognizeConfiguration =
//                    AudioRecognizeConfiguration.Builder() //分片默认40ms，可设置40-5000,必须为20的整倍数，如果不是，sdk内将自动调整为20的整倍数，例如77将被调整为60，如果您不了解此参数不建议更改
//                        //.sliceTime(40)
//                        // 是否使能静音检测，
//                        .setSilentDetectTimeOut(isOpenSilentCheck) //触发静音超时后是否停止识别，默认为true:停止，setSilentDetectTimeOut为true时参数有效
//                        .setSilentDetectTimeOutAutoStop(true) // 静音检测超时可设置>2000ms，setSilentDetectTimeOut为true有效，超过指定时间没有说话超过指定时间没有说话收到onSilentDetectTimeOut回调；需要大于等于sliceTime，实际时间为sliceTime的倍数，如果小于sliceTime，则按sliceTime的时间为准
//                        .audioFlowSilenceTimeOut(5000) // 音量回调时间，需要大于等于sliceTime，实际时间为sliceTime的倍数，如果小于sliceTime，则按sliceTime的时间为准
//                        .minVolumeCallbackTime(80) //是否压缩音频。默认压缩，压缩音频有助于优化弱网或网络不稳定时的识别速度及稳定性
//                        //SDK历史版本均默认压缩且未提供配置开关，如无特殊需求，建议使用默认值
//                        .isCompress(isCompress)
//                        .build()
//                //启动识别器
//                Thread {
//                    aaiClient!!.startAudioRecognize(
//                        audioRecognizeRequest,
//                        audioRecognizeResultlistener,
//                        audioRecognizeStateListener,
//                        audioRecognizeConfiguration
//                    )
//                }.start()
//            }
//        }

//取消识别，丢弃识别结Z果，不等待最终识别结果返回

//取消识别，丢弃识别结Z果，不等待最终识别结果返回
//        cancel!!.setOnClickListener { v: View? ->
//            AAILogger.info(TAG, "cancel button is clicked..")
//            Thread {
//                var taskExist = false
//                if (aaiClient != null) {
//                    taskExist = aaiClient!!.cancelAudioRecognize()
//                    handler!!.post { start!!.isEnabled = true }
//                }
//                if (!taskExist) {
//                    handler!!.post { recognizeState?.setText(R.string.cant_cancel) }
//                }
//            }.start()
//        }

        //音频压缩开关

        //音频压缩开关
//        mIsCompressSW!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
//            isCompress = isChecked
//            Toast.makeText(
//                context,
//                "音频压缩修改将在下次识别生效:$isChecked",
//                Toast.LENGTH_LONG
//            ).show()
//        }
//
//        mSilentSwitch!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
//            isOpenSilentCheck = isChecked
//        }
    }

    public fun start(){
        AAILogger.info(TAG, "the start button has clicked..")
//        handler!!.post { start!!.isEnabled = false }
        if (isRecording) {
            AAILogger.info(TAG, "stop button is clicked..")
            Thread {
                if (aaiClient != null) {
                    aaiClient!!.stopAudioRecognize()
                }
                if (mp != null) {
                    mp!!.stop()
                    mp = null
                }
            }.start()
        } else {
            resMap.clear()
            if (aaiClient != null) { //丢弃上一次结果
                val taskExist = aaiClient!!.cancelAudioRecognize()
                AAILogger.info(TAG, "taskExist=$taskExist")
            }
            isSaveAudioRecordFiles = false
//            if (!mFileName!!.text.toString().isEmpty()) {
//                isSaveAudioRecordFiles = true
//            }
            val dataSource =
                DemoAudioRecordDataSource(isSaveAudioRecordFiles, context)
//            if (mEnablePlayer!!.isChecked) {
//                mp = MediaPlayer()
//                var fd: AssetFileDescriptor? = null
//                try {
//                    fd = context.assets.openFd("test2.mp3")
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        mp!!.setDataSource(fd)
//                    } else {
//                        mp!!.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
//                    }
//                    mp!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
//                    mp!!.prepare()
//                    mp!!.start()
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//            dataSource.enableAEC(mEnableAEC!!.isChecked)
            val builder = AudioRecognizeRequest.Builder()
            // 初始化识别请求
            val audioRecognizeRequest =
                builder //设置数据源，数据源要求实现PcmAudioDataSource接口，您可以自己实现此接口来定制您的自定义数据源，例如从第三方推流中获取音频数据
                    //注意：音频数据必须为16k采样率的PCM音频，否则将导致语音识别输出错误的识别结果！！！！
                    .pcmAudioDataSource(dataSource) //使用Demo提供的录音器源码作为数据源，源码与SDK内置录音器一致，您可以参考此源代码自由定制修改，详情查阅DemoAudioRecordDataSource.java内注释
                    //                        .pcmAudioDataSource(new AudioRecordDataSource(isSaveAudioRecordFiles)) // 使用SDK内置录音器作为数据源
                    .setEngineModelType("16k_zh") // 设置引擎(16k_zh--通用引擎，支持中文普通话+英文),更多引擎请关注官网文档https://cloud.tencent.com/document/product/1093/48982 ，引擎种类持续增加中
                    .setFilterDirty(0) // 0 ：默认状态 不过滤脏话 1：过滤脏话
                    .setFilterModal(0) // 0 ：默认状态 不过滤语气词  1：过滤部分语气词 2:严格过滤
                    .setFilterPunc(0) // 0 ：默认状态 不过滤句末的句号 1：滤句末的句号
                    .setConvert_num_mode(1) //1：默认状态 根据场景智能转换为阿拉伯数字；0：全部转为中文数字。
                    //                        .setVadSilenceTime(1000) // 语音断句检测阈值，静音时长超过该阈值会被认为断句（多用在智能客服场景，需配合 needvad = 1 使用） 默认不传递该参数，不建议更改
                    .setNeedvad(1) //0：关闭 vad，1：默认状态 开启 vad。语音时长超过一分钟需要开启,如果对实时性要求较高,并且时间较短
                    // 的输入,建议关闭,可以显著降低onSliceSuccess结果返回的时延以及stop后onSegmentSuccess和onSuccess返回的时延
                    //                        .setHotWordId("")//热词 id。用于调用对应的热词表，如果在调用语音识别服务时，不进行单独的热词 id 设置，自动生效默认热词；如果进行了单独的热词 id 设置，那么将生效单独设置的热词 id。
                    .setWordInfo(1) //                        .setCustomizationId("")//自学习模型 id。如果设置了该参数，那么将生效对应的自学习模型,如果您不了解此参数，请不要设置
                    //                        .setReinforceHotword(1)
                    //                        .setNoiseThreshold(0)
                    //                        .setMaxSpeakTime(5000) // 强制断句功能，取值范围 5000-90000(单位:毫秒），默认值0(不开启)。 在连续说话不间断情况下，该参数将实现强制断句（此时结果变成稳态，slice_type=2）。如：游戏解说场景，解说员持续不间断解说，无法断句的情况下，将此参数设置为10000，则将在每10秒收到 slice_type=2的回调。
                    .build()

            // 自定义识别配置
            val audioRecognizeConfiguration =
                AudioRecognizeConfiguration.Builder() //分片默认40ms，可设置40-5000,必须为20的整倍数，如果不是，sdk内将自动调整为20的整倍数，例如77将被调整为60，如果您不了解此参数不建议更改
                    //.sliceTime(40)
                    // 是否使能静音检测，
                    .setSilentDetectTimeOut(isOpenSilentCheck) //触发静音超时后是否停止识别，默认为true:停止，setSilentDetectTimeOut为true时参数有效
                    .setSilentDetectTimeOutAutoStop(true) // 静音检测超时可设置>2000ms，setSilentDetectTimeOut为true有效，超过指定时间没有说话超过指定时间没有说话收到onSilentDetectTimeOut回调；需要大于等于sliceTime，实际时间为sliceTime的倍数，如果小于sliceTime，则按sliceTime的时间为准
                    .audioFlowSilenceTimeOut(5000) // 音量回调时间，需要大于等于sliceTime，实际时间为sliceTime的倍数，如果小于sliceTime，则按sliceTime的时间为准
                    .minVolumeCallbackTime(80) //是否压缩音频。默认压缩，压缩音频有助于优化弱网或网络不稳定时的识别速度及稳定性
                    //SDK历史版本均默认压缩且未提供配置开关，如无特殊需求，建议使用默认值
                    .isCompress(isCompress)
                    .build()
            //启动识别器
            Thread {
                aaiClient!!.startAudioRecognize(
                    audioRecognizeRequest,
                    audioRecognizeResultlistener,
                    audioRecognizeStateListener,
                    audioRecognizeConfiguration
                )
            }.start()
        }
    }

    //取消识别，丢弃识别结Z果，不等待最终识别结果返回
    public fun cancelRecord(){
        AAILogger.info(TAG, "cancel button is clicked..")
        Thread {
            var taskExist = false
            if (aaiClient != null) {
                taskExist = aaiClient!!.cancelAudioRecognize()
//                handler!!.post { start!!.isEnabled = true }
            }
            if (!taskExist) {
//                handler!!.post { recognizeState?.setText(R.string.cant_cancel) }
            }
        }.start()
    }



    private fun ShowMsg(message: String) {
//        Log.i(TAG, message)
//        object : Thread() {
//            override fun run() {
////                runOnUiThread {
////                    recognizeResult!!.text = message
////                    mScrollView!!.fullScroll(ScrollView.FOCUS_DOWN)
////                }
//            }
//        }.start()
    }

    public fun stopRecord(){
        aaiClient?.stopAudioRecognize()
    }

    interface Listener {

        fun onSegmentSuccess(text:String)

        fun onSuccess(text: String)
    }
    fun setInterface(listener: Listener) {
        this.listener = listener
    }

    private lateinit var listener: Listener
}