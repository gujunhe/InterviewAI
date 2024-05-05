package com.google.ai.sample.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "interview_records")
data class InterviewRecord(
    @PrimaryKey()
    @ColumnInfo(name = "interview_id")
    val interviewId: String,
    @ColumnInfo(name = "interview_time") val interviewTime: String,
    @ColumnInfo(name = "job_description") val jobDescription: String,
    @ColumnInfo(name = "overall_score") val overallScore: String,
    @ColumnInfo(name = "overall_evaluation") val overallEvaluation: String
)