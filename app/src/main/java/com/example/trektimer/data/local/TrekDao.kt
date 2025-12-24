package com.example.trektimer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrekDao {
    @Insert
    suspend fun insert(trek: Trek): Long

    @Query("SELECT * FROM treks WHERE userUid = :uid ORDER BY startTime DESC")
    fun getTreksByUser(uid: String): Flow<List<Trek>>

    @Query("SELECT * FROM treks WHERE id = :id")
    suspend fun getTrekById(id: Long): Trek?

    @Query("DELETE FROM treks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
