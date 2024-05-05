package com.google.ai.sample.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface InterviewRecordDao {
    @Insert
    fun insert(record: InterviewRecord)

    @Update
    fun update(record: InterviewRecord)

    @Query("SELECT * FROM interview_records")
    fun getAll(): List<InterviewRecord>

    // 其他查询方法...
}