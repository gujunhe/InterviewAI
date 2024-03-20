package com.google.ai.sample

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.bytedance.speech.speechengine.SpeechEngineGenerator

class MyApplication : Application() {
    companion object {
        private lateinit var instance: MyApplication
        private lateinit var sharedPreferences: SharedPreferences

        fun getSharedPreferences(): SharedPreferences {
            return sharedPreferences
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        sharedPreferences = getSharedPreferences("MY_PREFS", Context.MODE_PRIVATE)
    }
}