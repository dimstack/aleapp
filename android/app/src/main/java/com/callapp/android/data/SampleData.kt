package com.callapp.android.data

import com.callapp.android.ui.preview.PreviewData

@Deprecated(
    message = "Use PreviewData directly for previews. This shim exists only to avoid touching large preview-only UI files.",
    replaceWith = ReplaceWith("PreviewData"),
)
object SampleData {
    val serverTech = PreviewData.serverTech
    val serverCreative = PreviewData.serverCreative
    val serverMusic = PreviewData.serverMusic
    val serverGameDev = PreviewData.serverGameDev
    val servers = PreviewData.servers

    val userAnna = PreviewData.userAnna
    val userAlexey = PreviewData.userAlexey
    val userSergey = PreviewData.userSergey
    val userDmitry = PreviewData.userDmitry
    val userMaria = PreviewData.userMaria
    val userNatasha = PreviewData.userNatasha

    val techMembers = PreviewData.techMembers
    val creativeMembers = PreviewData.creativeMembers
    val favorites = PreviewData.favorites
    val joinRequests = PreviewData.joinRequests
}
