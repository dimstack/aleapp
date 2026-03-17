package com.callapp.android.ui.preview

import com.callapp.android.domain.model.JoinRequest
import com.callapp.android.domain.model.Server
import com.callapp.android.domain.model.ServerAvailabilityStatus
import com.callapp.android.domain.model.User
import com.callapp.android.domain.model.UserStatus
import com.callapp.android.ui.screens.home.toFavoriteContactItem

object PreviewData {

    val serverTech = Server(
        id = "s1",
        name = "Tech Community",
        username = "@tech_community",
        description = "Сообщество разработчиков и технических специалистов.",
        address = "https://tech-community.example",
    )

    val serverCreative = Server(
        id = "s2",
        name = "Creative Studio",
        username = "@creative_studio",
        description = "Творческая студия для дизайнеров и художников.",
        address = "https://creative-studio.example",
    )

    val serverMusic = Server(
        id = "s3",
        name = "Music Production",
        username = "@music_prod",
        description = "Комьюнити для музыкантов и продюсеров.",
        address = "https://music-production.example",
    )

    val serverGameDev = Server(
        id = "s4",
        name = "Game Dev Hub",
        username = "@gamedev_hub",
        description = "Сообщество разработчиков игр.",
        address = "https://gamedev-hub.example",
    )

    val serverCreativeUnavailable = serverCreative.copy(
        availabilityStatus = ServerAvailabilityStatus.UNAVAILABLE,
        availabilityMessage = "Сервер временно недоступен",
    )

    val servers = listOf(serverTech, serverCreative, serverMusic, serverGameDev)

    val userAnna = User(
        id = "u1",
        name = "Анна Смирнова",
        username = "@anna_s",
        status = UserStatus.ONLINE,
        serverId = serverTech.id,
    )

    val userAlexey = User(
        id = "u2",
        name = "Алексей Козлов",
        username = "@alexey_k",
        status = UserStatus.ONLINE,
        serverId = serverTech.id,
    )

    val userSergey = User(
        id = "u3",
        name = "Сергей Новиков",
        username = "@sergey_n",
        status = UserStatus.ONLINE,
        serverId = serverTech.id,
    )

    val userDmitry = User(
        id = "u4",
        name = "Дмитрий Петров",
        username = "@dmitry_p",
        status = UserStatus.ONLINE,
        serverId = serverCreative.id,
    )

    val userMaria = User(
        id = "u5",
        name = "Мария Волкова",
        username = "@maria_v",
        status = UserStatus.INVISIBLE,
        serverId = serverCreative.id,
    )

    val userNatasha = User(
        id = "u6",
        name = "Наталья Попова",
        username = "@natasha_p",
        status = UserStatus.ONLINE,
        serverId = serverCreative.id,
    )

    val techMembers = listOf(userAnna, userAlexey, userSergey)
    val creativeMembers = listOf(userDmitry, userMaria, userNatasha)
    val favorites = listOf(userAnna, userDmitry, userSergey)
    val favoriteItems = listOf(
        userAnna.toFavoriteContactItem(serverTech),
        userDmitry.toFavoriteContactItem(serverCreativeUnavailable),
        userSergey.toFavoriteContactItem(serverTech),
    )

    val joinRequests = listOf(
        JoinRequest(
            id = "r1",
            userName = "Иван Петров",
            username = "@ivan_petrov",
            serverId = serverTech.id,
            createdAt = "2 марта в 10:30",
        ),
        JoinRequest(
            id = "r2",
            userName = "Мария Сидорова",
            username = "@maria_sidorova",
            serverId = serverTech.id,
            createdAt = "3 марта в 09:15",
        ),
    )
}
