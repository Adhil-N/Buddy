package com.cname.buddy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    @Insert
    suspend fun insertFinance(finance: FinanceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllFinances(finances: List<FinanceEntity>)

    @Update
    suspend fun updateFinance(finance: FinanceEntity)

    @Delete
    suspend fun deleteFinance(finance: FinanceEntity)

    // The magic Flow! This automatically emits a new list to your UI
    // anytime the database changes.
    @Query("SELECT * FROM finances ORDER BY id DESC")
    fun getAllFinances(): Flow<List<FinanceEntity>>

    @Query("SELECT * FROM finances")
    suspend fun getAllFinancesSync(): List<FinanceEntity>
}