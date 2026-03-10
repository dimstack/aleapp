package com.example.android.data

import com.example.android.domain.model.Server

class MockServerRepository {

    fun getServers(): List<Server> = SampleData.servers

    fun getServerById(id: String): Server? = SampleData.servers.find { it.id == id }
}
