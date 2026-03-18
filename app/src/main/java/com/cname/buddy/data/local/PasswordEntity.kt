package com.cname.buddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val platform: String,  // e.g., "Netflix", "Gmail", "Bank"
    val username: String,  // e.g., "user@email.com"
    val password: String   // The actual password
)