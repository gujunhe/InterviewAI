package com.google.ai.sample.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface InterviewQuestionRecordDao {
    @Insert
    fun insert(record: InterviewQuestionRecord)

    @Update
    fun update(record: InterviewQuestionRecord)

    @Query("SELECT * FROM interview_question_records WHERE interview_id = :interviewId")
    fun getQuestionsByInterviewId(interviewId: Int): List<InterviewQuestionRecord>

    // 其他查询方法...
}