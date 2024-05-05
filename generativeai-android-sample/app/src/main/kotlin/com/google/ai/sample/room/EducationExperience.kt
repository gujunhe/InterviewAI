package com.google.ai.sample.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "education_experiences")
data class EducationExperience(
    @PrimaryKey val educationId: String,
    @ColumnInfo(name = "school") val school: String,
    @ColumnInfo(name = "education_level") val educationLevel: String,
    @ColumnInfo(name = "major") val major: String,
    @ColumnInfo(name = "start_date") val startDate: String, // 在校时间的开始日期
    @ColumnInfo(name = "end_date") val endDate: String, // 在校时间的结束日期，可以为空
    @ColumnInfo(name = "experience") val experience: String // 在校经历
)