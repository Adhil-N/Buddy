package com.cname.buddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FinanceEntity::class, PasswordEntity::class, NoteEntity::class], version = 3, exportSchema = false)
abstract class BuddyDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao
    abstract fun passwordDao(): PasswordDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var Instance: BuddyDatabase? = null

        fun getDatabase(context: Context): BuddyDatabase {
            // If the database already exists, return it. Otherwise, build it.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    BuddyDatabase::class.java,
                    "buddy_database" // This is the actual file name saved on the phone!
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}