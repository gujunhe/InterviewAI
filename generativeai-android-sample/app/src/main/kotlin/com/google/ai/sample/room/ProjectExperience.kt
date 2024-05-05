package com.google.ai.sample.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_experiences")
data class ProjectExperience(
    @PrimaryKey() val projectId: String, // 唯一标识符，自动生成
    @ColumnInfo(name = "company") val company: String, // 公司名称
    @ColumnInfo(name = "project_name") val projectName: String, // 项目名称
    @ColumnInfo(name = "position") val position: String, // 职位
    @ColumnInfo(name = "start_date") val startDate: String, // 开始时间
    @ColumnInfo(name = "end_date") val endDate: String, // 结束时间
    @ColumnInfo(name = "project_content") val projectContent: String // 项目内容描述
    // 可以根据需要添加其他字段，如项目类型、项目结果等
)