package com.cname.buddy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Insert
    suspend fun insertPassword(password: PasswordEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllPasswords(passwords: List<PasswordEntity>)

    @Update
    suspend fun updatePassword(password: PasswordEntity)

    @Delete
    suspend fun deletePassword(password: PasswordEntity)

    // Automatically emits the list of passwords alphabetically by platform
    @Query("SELECT * FROM passwords ORDER BY platform ASC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>
}