package com.callapp.server.service

import com.callapp.server.routes.ApiException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID

private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
private val validFileName = Regex("^[A-Za-z0-9._-]+$")

enum class MediaCategory(val folderName: String) {
    PROFILE("profile"),
    SERVER("server"),
}

data class StoredImage(
    val category: MediaCategory,
    val fileName: String,
) {
    val relativePath: String = "/uploads/${category.folderName}/$fileName"
}

class MediaStorageService(
    uploadRoot: Path,
) {
    private val root = uploadRoot.toAbsolutePath().normalize()

    init {
        MediaCategory.entries.forEach { category ->
            Files.createDirectories(root.resolve(category.folderName))
        }
    }

    fun storeImage(
        bytes: ByteArray,
        originalFileName: String?,
        mimeType: String?,
        category: MediaCategory,
    ): StoredImage {
        if (bytes.isEmpty()) {
            throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Image body is empty")
        }
        if (bytes.size > MAX_IMAGE_SIZE_BYTES) {
            throw ApiException(HttpStatusCode.PayloadTooLarge, "validation_error", "Image is too large")
        }
        if (mimeType.isNullOrBlank() || !mimeType.startsWith("image/")) {
            throw ApiException(HttpStatusCode.BadRequest, "validation_error", "Only image uploads are supported")
        }

        val extension = extensionFor(mimeType, originalFileName)
        val fileName = "${UUID.randomUUID()}.$extension"
        val target = categoryDirectory(category).resolve(fileName)
        Files.write(
            target,
            bytes,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE,
        )
        return StoredImage(category = category, fileName = fileName)
    }

    fun resolve(category: MediaCategory, fileName: String): Path? {
        if (!validFileName.matches(fileName)) {
            return null
        }
        val path = categoryDirectory(category).resolve(fileName).normalize()
        return path.takeIf { it.startsWith(categoryDirectory(category)) && Files.isRegularFile(it) }
    }

    fun contentType(fileName: String): ContentType = when (fileName.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> ContentType.Image.JPEG
        "png" -> ContentType.Image.PNG
        "gif" -> ContentType.Image.GIF
        "webp" -> ContentType.parse("image/webp")
        "heic" -> ContentType.parse("image/heic")
        "heif" -> ContentType.parse("image/heif")
        else -> ContentType.Image.Any
    }

    private fun categoryDirectory(category: MediaCategory): Path = root.resolve(category.folderName)

    private fun extensionFor(mimeType: String, originalFileName: String?): String {
        val explicit = originalFileName
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() && it.matches(Regex("^[a-z0-9]+$")) }
        if (explicit != null) {
            return explicit
        }

        return when (mimeType.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> "img"
        }
    }
}
