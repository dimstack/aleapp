package com.callapp.android.data

import com.callapp.android.domain.model.Server

class MockServerRepository {

    fun getServers(): List<Server> = SampleData.servers

    fun getServerById(id: String): Server? = SampleData.servers.find { it.id == id }
}
