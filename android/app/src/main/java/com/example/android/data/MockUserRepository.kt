package com.example.android.data

import com.example.android.domain.model.User

class MockUserRepository {

    private val allUsers = SampleData.techMembers + SampleData.creativeMembers

    fun getMembersByServerId(serverId: String): List<User> =
        allUsers.filter { it.serverId == serverId }

    fun getFavorites(): List<User> = SampleData.favorites

    fun getUserById(id: String): User? = allUsers.find { it.id == id }

    fun isAdmin(serverId: String): Boolean = serverId == "s1" || serverId == "s3"
}
