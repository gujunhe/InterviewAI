package com.google.ai.sample.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [InterviewRecord::class, InterviewQuestionRecord::class,EducationExperience::class,ProjectExperience::class,User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun interviewRecordDao(): InterviewRecordDao
    abstract fun interviewQuestionRecordDao():InterviewQuestionRecordDao
    abstract fun EducationExperienceDao():EducationExperienceDao
    abstract fun ProjectExperienceDao():ProjectExperienceDao

    abstract fun UserDao():UserDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "kot.db").allowMainThreadQueries()
                .build()
    }
}