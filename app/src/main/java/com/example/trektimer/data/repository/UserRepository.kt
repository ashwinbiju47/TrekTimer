package com.example.trektimer.data.repository

import com.example.trektimer.data.local.User
import com.example.trektimer.data.local.UserDao


class UserRepository(private val userDao: UserDao) {

    suspend fun saveFirebaseUser(uid: String, email: String) {
        userDao.insertUser(User(firebaseUid = uid, email = email))
    }

    suspend fun getLocalUser(uid: String): User? {
        return userDao.getUserByUid(uid)
    }
}
