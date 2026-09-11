package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_brokers")
data class DataBrokerItem(
    @PrimaryKey val id: String,
    val name: String,
    val optOutUrl: String,
    val isListed: Boolean = false,
    val notes: String = "",
    val lastChecked: Long = System.currentTimeMillis()
)
