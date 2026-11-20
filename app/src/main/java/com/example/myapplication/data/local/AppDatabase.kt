package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.dao.NodeHealthDao
import com.example.myapplication.data.local.dao.QueryResultDao
import com.example.myapplication.data.local.entity.NodeHealthEntity
import com.example.myapplication.data.local.entity.QueryResultEntity

@Database(
    entities = [QueryResultEntity::class, NodeHealthEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun queryResultDao(): QueryResultDao
    abstract fun nodeHealthDao(): NodeHealthDao
}
