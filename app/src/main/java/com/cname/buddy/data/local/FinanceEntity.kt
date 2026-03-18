package com.cname.buddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finances")
data class FinanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Room will automatically number these 1, 2, 3...

    val title: String,
    val totalAmount: Float,
    val paidAmount: Float,
    val dueAmount: Float,
    val dueDate: String, // e.g., "15" for the 15th of the month
    val isExpense: Boolean // True for EMI/Loans, False for Income
)