package com.callapp.server.service

import org.mindrot.jbcrypt.BCrypt

class PasswordService {
    fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(12))

    fun verify(password: String, hash: String): Boolean = BCrypt.checkpw(password, hash)
}
