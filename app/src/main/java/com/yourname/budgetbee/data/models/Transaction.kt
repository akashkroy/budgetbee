package com.yourname.budgetbee.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String,        // "debit" or "credit"
    val category: String,    // e.g., "Bank", "UPI", etc.
    val merchant: String,    // e.g., "Zomato", "Uber"
    val date: String,         // "yyyy-MM-dd"
    val note: String? = null
)
