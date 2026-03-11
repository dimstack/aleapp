package com.example.android.data

import com.example.android.domain.model.JoinRequest
import com.example.android.domain.model.Server
import com.example.android.domain.model.User
import com.example.android.domain.model.UserStatus

object SampleData {

    // ── Servers ──────────────────────────────────────────────────────────────

    val serverTech = Server(
        id = "s1",
        name = "Tech Community",
        username = "@tech_community",
        description = "Сообщество разработчиков и технических специалистов. Обсуждаем последние технологии и делимся опытом.",
        address = "http://192.168.1.10:3000",
    )

    val serverCreative = Server(
        id = "s2",
        name = "Creative Studio",
        username = "@creative_studio",
        description = "Творческая студия для дизайнеров, художников и креативных профессионалов.",
        address = "http://192.168.1.20:3000",
    )

    val serverMusic = Server(
        id = "s3",
        name = "Music Production",
        username = "@music_prod",
        description = "Студия музыкального продакшена.",
        address = "http://192.168.1.30:3000",
    )

    val serverGameDev = Server(
        id = "s4",
        name = "Game Dev Hub",
        username = "@gamedev_hub",
        description = "Сообщество разработчиков игр.",
        address = "http://192.168.1.40:3000",
    )

    val servers = listOf(serverTech, serverCreative, serverMusic, serverGameDev)

    // ── Users — Tech Community ───────────────────────────────────────────────

    val userAnna = User(
        id = "u1", name = "Анна Смирнова", username = "@anna_s",
        status = UserStatus.ONLINE, serverId = "s1",
    )

    val userAlexey = User(
        id = "u2", name = "Алексей Козлов", username = "@alexey_k",
        status = UserStatus.ONLINE, serverId = "s1",
    )

    val userSergey = User(
        id = "u3", name = "Сергей Новиков", username = "@sergey_n",
        status = UserStatus.ONLINE, serverId = "s1",
    )

    // ── Users — Creative Studio ──────────────────────────────────────────────

    val userDmitry = User(
        id = "u4", name = "Дмитрий Петров", username = "@dmitry_p",
        status = UserStatus.ONLINE, serverId = "s2",
    )

    val userMaria = User(
        id = "u5", name = "Мария Волкова", username = "@maria_v",
        status = UserStatus.INVISIBLE, serverId = "s2",
    )

    val userNatasha = User(
        id = "u6", name = "Наталья Попова", username = "@natasha_p",
        status = UserStatus.ONLINE, serverId = "s2",
    )

    val techMembers = listOf(userAnna, userAlexey, userSergey)
    val creativeMembers = listOf(userDmitry, userMaria, userNatasha)

    // ── Favorites ────────────────────────────────────────────────────────────

    val favorites = listOf(userAnna, userDmitry, userSergey)

    // ── Join Requests ────────────────────────────────────────────────────────

    val joinRequests = listOf(
        JoinRequest(
            id = "r1", userName = "Иван Петров", username = "@ivan_petrov",
            serverId = "s1", createdAt = "2 марта в 10:30",
        ),
        JoinRequest(
            id = "r2", userName = "Мария Сидорова", username = "@maria_sidorova",
            serverId = "s1", createdAt = "3 марта в 09:15",
        ),
    )
}
