package com.example.trektimer.data.repository

import com.example.trektimer.data.local.Trek
import com.example.trektimer.data.local.TrekDao
import kotlinx.coroutines.flow.Flow

class TrekRepository(private val trekDao: TrekDao) {

    suspend fun saveTrek(trek: Trek): Long {
        return trekDao.insert(trek)
    }

    fun getTreksForUser(userUid: String): Flow<List<Trek>> {
        return trekDao.getTreksByUser(userUid)
    }

    suspend fun getTrekById(id: Long): Trek? {
        return trekDao.getTrekById(id)
    }

    suspend fun deleteTrek(id: Long) {
        trekDao.deleteById(id)
    }
}
