package com.google.ai.sample.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users ORDER BY userId ASC LIMIT 1")
     fun getFirstUsers(): LiveData<User>

    @Delete()
    suspend fun deleteUser(user: User)

    // ... 其他必要的方法，如更新、删除等
}