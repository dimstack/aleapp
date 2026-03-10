package com.example.android.data

import com.example.android.domain.model.JoinRequest

class MockJoinRequestRepository {

    fun getRequestsByServerId(serverId: String): List<JoinRequest> =
        SampleData.joinRequests.filter { it.serverId == serverId }
}
