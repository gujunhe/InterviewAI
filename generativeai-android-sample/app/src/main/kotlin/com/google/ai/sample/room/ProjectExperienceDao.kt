package com.google.ai.sample.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectExperienceDao {
    @Insert
    suspend fun insert(projectExperience: ProjectExperience)

    @Delete
    suspend fun delete(projectExperience: ProjectExperience)

    @Insert
    suspend fun insertAll(projectExperiences: List<ProjectExperience>)

    @Query("SELECT * FROM project_experiences")
    fun getAll(): LiveData<List<ProjectExperience>>

    // 根据需要添加其他查询方法，如根据ID查询、按公司名查询等
}