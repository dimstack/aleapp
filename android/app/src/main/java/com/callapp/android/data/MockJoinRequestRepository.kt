package com.callapp.android.data

import com.callapp.android.domain.model.JoinRequest

class MockJoinRequestRepository {

    fun getRequestsByServerId(serverId: String): List<JoinRequest> =
        SampleData.joinRequests.filter { it.serverId == serverId }
}
