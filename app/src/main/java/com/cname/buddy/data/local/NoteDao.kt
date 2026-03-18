package com.cname.buddy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert suspend fun insertNote(note: NoteEntity)
    @Update suspend fun updateNote(note: NoteEntity)
    @Delete suspend fun deleteNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllNotes(notes: List<NoteEntity>)

    @Query("SELECT * FROM notes ORDER BY id DESC") // Newest first!
    fun getAllNotes(): Flow<List<NoteEntity>>
}