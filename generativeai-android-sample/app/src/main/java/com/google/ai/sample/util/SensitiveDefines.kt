package com.google.ai.sample.util


object SensitiveDefines {
    // User Info
    const val UID = "YOUR USER ID"

    // Device Info
    const val DID = "YOUR DEVICE ID"

    // Online & Resource Authorization
    const val APPID = "7513807436"
    const val TOKEN = "Bearer;PUGnByxyqE4DzcqewTM2TaClDlGOQmA-"
    const val APP_VERSION = "YOUR APP VERSION"

    // Offline Authorization
    const val AUTHENTICATE_ADDRESS = "AUTHENTICAT ADDRESS"
    const val AUTHENTICATE_URI = "AUTHENTICATE URI"
    const val LICENSE_NAME = "YOUR LICENSE NAME"
    const val LICENSE_BUSI_ID = "YOUR LICENSE BUSI_ID"
    const val SECRET = "wS28lv2QqlQJeI3MpIhOFI4xwtcPnKsC"
    const val BUSINESS_KEY = "YOUR BUSINESS KEY"

    // Address
    const val DEFAULT_ADDRESS = "wss://openspeech.bytedance.com"
    const val DEFAULT_HTTP_ADDRESS = "https://openspeech.bytedance.com"

    // ASR
    const val ASR_DEFAULT_URI = "/api/v2/asr"
    const val ASR_DEFAULT_CLUSTER = "YOUR ASR CLUSTER"

    // AU
    const val AU_DEFAULT_URI = "/api/v1/sauc"
    const val AU_DEFAULT_CLUSTER = "YOUR AU CLUSTER"

    // TTS
    const val TTS_DEFAULT_URI = "/api/v1/tts/ws_binary"
    const val TTS_DEFAULT_CLUSTER = "volcano_tts"
    const val TTS_DEFAULT_BACKEND_CLUSTER = "YOUR TTS BACKEND CLUSTER"
    const val TTS_DEFAULT_ONLINE_VOICE = "阳光男声"
    const val TTS_DEFAULT_ONLINE_VOICE_TYPE = "BV056_streaming"
    const val TTS_DEFAULT_OFFLINE_VOICE = "TTS OFFLINE VOICE"
    const val TTS_DEFAULT_OFFLINE_VOICE_TYPE = "TTS OFFLINE VOICE TYPE"
    const val TTS_DEFAULT_ONLINE_LANGUAGE = "TTS ONLINE LANGUAGE"
    const val TTS_DEFAULT_OFFLINE_LANGUAGE = "TTS OFFLINE LANGUAGE"
    val TTS_DEFAULT_DOWNLOAD_OFFLINE_VOICES = arrayOf<String>()

    // VoiceClone
    const val VOICECLONE_DEFAULT_UIDS = "uid_1;uid_2"
    const val VOICECLONE_DEFAULT_TASK_ID = -1

    // VoiceConv
    const val VOICECONV_DEFAULT_URI = "/api/v1/voice_conv/ws"
    const val VOICECONV_DEFAULT_CLUSTER = "YOUR VOICECONV CLUSTER"
    const val VOICECONV_DEFAULT_VOICE = "VOICECONV VOICE"
    const val VOICECONV_DEFAULT_VOICE_TYPE = "VOICECONV VOICE TYPE"

    // Fulllink
    const val FULLLINK_DEFAULT_URI = "FULLLINK URI"

    // Dialog
    const val DIALOG_DEFAULT_URI = "DIALOG URI"
    const val DIALOG_DEFAULT_APP_ID = "DIALOG APP ID"
    const val DIALOG_DEFAULT_ID = "DIALOG ID"
    const val DIALOG_DEFAULT_ROLE = "DIALOG ROLE"
    const val DIALOG_DEFAULT_CLOTHES_TYPE = "DIALOG CLOTHES TYPE"
    const val DIALOG_DEFAULT_TTA_VOICE_TYPE = "DIALOG TTA_VOICE_TYPE"

    // CAPT
    const val CAPT_DEFAULT_MDD_URI = "CAPT MDD URI"
    const val CAPT_DEFAULT_CLUSTER = "YOUR CAPT CLUSTER"
}

