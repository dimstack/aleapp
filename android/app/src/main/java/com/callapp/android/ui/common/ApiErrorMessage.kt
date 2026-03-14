package com.callapp.android.ui.common

import com.callapp.android.network.result.ApiError

fun apiErrorMessage(
    error: ApiError,
    fallback: String,
    notFound: String = "Данные не найдены",
    unauthorized: String = "Сессия истекла",
): String = when (error) {
    ApiError.NetworkError -> "Нет соединения с сервером"
    ApiError.NotFound -> notFound
    is ApiError.Unauthorized -> unauthorized
    is ApiError.ValidationError -> localizeBackendMessage(error.message) ?: fallback
    is ApiError.UsernameTaken -> "Username уже занят"
    is ApiError.LoginLocked -> "Слишком много попыток входа. Попробуйте через 15 минут."
    is ApiError.Forbidden -> localizeBackendMessage(error.message) ?: "Недостаточно прав для выполнения операции"
    is ApiError.DeprecatedEndpoint -> localizeBackendMessage(error.message) ?: "Эта операция недоступна в текущей версии"
    is ApiError.ServerError -> localizeBackendMessage(error.message) ?: fallback
}

fun localizeBackendMessage(message: String?): String? = when (message?.trim()) {
    null, "" -> null
    "Password must be at least 8 characters long" -> "Пароль должен содержать минимум 8 символов"
    "Username format is invalid" -> "Username может содержать только буквы, цифры и подчёркивание"
    "Username is required" -> "Username обязателен"
    "Name is required" -> "Имя обязательно"
    "Username is already taken" -> "Username уже занят"
    "Too many failed login attempts" -> "Слишком много попыток входа. Попробуйте через 15 минут."
    "Server name cannot be blank" -> "Название сервера обязательно"
    "Server username cannot be blank" -> "Username сервера обязателен"
    "Invalid status value" -> "Некорректный статус"
    "Label is required" -> "Название токена обязательно"
    "max_uses cannot be negative" -> "Количество использований не может быть отрицательным"
    "Invalid join request action" -> "Некорректное действие для заявки"
    "Invalid role value" -> "Некорректная роль"
    "Cannot add yourself to favorites" -> "Нельзя добавить себя в избранное"
    "Guest session is required" -> "Сессия подключения истекла. Подключитесь заново"
    "Server deletion is disabled in this build" -> "Удаление сервера отключено в этой сборке"
    "Use POST /api/users for registration and pending approval flow" -> "Используйте создание профиля для отправки заявки на вступление"
    "Invite token is invalid" -> "Токен приглашения недействителен"
    "Invite token has been revoked" -> "Токен приглашения отозван"
    "Invite token has expired" -> "Срок действия токена приглашения истек"
    "Invite token has reached its limit" -> "Лимит использования токена исчерпан"
    "Invalid username or password" -> "Неверный username или пароль"
    else -> message
}
