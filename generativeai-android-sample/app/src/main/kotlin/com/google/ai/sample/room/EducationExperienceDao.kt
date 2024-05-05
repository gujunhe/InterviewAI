package com.google.ai.sample.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EducationExperienceDao {
    @Insert
    suspend fun insertEducationExperience(educationExperience: EducationExperience)
    @Delete
    suspend fun deleteEducationExperience(educationExperience: EducationExperience)

    @Query("SELECT * FROM education_experiences")
    fun getAllEducationExperiences(): LiveData<List<EducationExperience>>
}