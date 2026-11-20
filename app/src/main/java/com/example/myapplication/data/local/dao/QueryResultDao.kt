package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.QueryResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueryResultDao {
    @Query("SELECT * FROM query_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<QueryResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<QueryResultEntity>)

    @Query("DELETE FROM query_results")
    suspend fun clearResults()
}
