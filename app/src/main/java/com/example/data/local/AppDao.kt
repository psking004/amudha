package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    @Query("SELECT * FROM data_brokers ORDER BY name ASC")
    fun getAllBrokers(): Flow<List<DataBrokerItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrokers(brokers: List<DataBrokerItem>)

    @Query("UPDATE data_brokers SET isListed = :isListed, lastChecked = :timestamp WHERE id = :id")
    suspend fun updateBrokerStatus(id: String, isListed: Boolean, timestamp: Long = System.currentTimeMillis())
}
