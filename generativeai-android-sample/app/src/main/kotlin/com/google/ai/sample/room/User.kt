package com.google.ai.sample.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull
import java.util.Date

@Entity(tableName = "users")
data class User(
    @NotNull
    @PrimaryKey(autoGenerate = true) val userId: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "avatar") val avatar: ByteArray,
    @ColumnInfo(name = "age") val age: Int,
    @ColumnInfo(name = "gender") val gender: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "job_preference") val jobPreference: String,
    @ColumnInfo(name = "personal_strengths") val personalStrengths: String,
    @ColumnInfo(name = "honor_award") val honorAward: String,
    @ColumnInfo(name = "certificate") val certificates: String
)