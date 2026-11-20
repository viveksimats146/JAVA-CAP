package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.NodeHealthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeHealthDao {
    @Query("SELECT * FROM node_health")
    fun getAllNodeHealth(): Flow<List<NodeHealthEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodeHealth(nodeHealth: NodeHealthEntity)

    @Query("DELETE FROM node_health")
    suspend fun clearAll()
}
