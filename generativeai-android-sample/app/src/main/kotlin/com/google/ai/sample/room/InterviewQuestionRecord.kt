package com.google.ai.sample.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "interview_question_records",
//    foreignKeys = [
//        ForeignKey(
//            entity = InterviewRecord::class,
//            parentColumns = ["interview_id"],
//            childColumns = ["question_record_id"],
//            onDelete = ForeignKey.CASCADE
//        )
//    ]
)
data class InterviewQuestionRecord(
    @PrimaryKey()
    @ColumnInfo(name = "question_id")
    val questionRecordId: String,
    @ColumnInfo(name = "interview_id") val interviewId: String,
//    @ColumnInfo(name = "question_time") val questionTime: String,
    @ColumnInfo(name = "question") val question: String,
    @ColumnInfo(name = "user_answer") val userAnswer: String,
    @ColumnInfo(name = "answer_score") val answerScore: String,
    @ColumnInfo(name = "answer_evaluation") val answerEvaluation: String
)