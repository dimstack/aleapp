package com.callapp.server.models

enum class Role {
    ADMIN,
    MEMBER,
}

enum class UserStatus {
    ONLINE,
    DO_NOT_DISTURB,
    INVISIBLE,
}

enum class JoinRequestStatus {
    PENDING,
    APPROVED,
    DECLINED,
}

enum class NotificationType {
    REQUEST_SENT,
    REQUEST_APPROVED,
    REQUEST_DECLINED,
    INCOMING_CALL,
    MISSED_CALL,
}

enum class SessionType {
    GUEST,
    USER,
    ADMIN,
}
