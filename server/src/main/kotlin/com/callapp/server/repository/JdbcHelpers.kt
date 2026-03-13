package com.callapp.server.repository

import java.sql.ResultSet
import java.time.Instant

internal fun ResultSet.getNullableString(column: String): String? =
    getString(column)?.takeIf { !wasNull() }

internal fun ResultSet.getInstant(column: String): Instant =
    Instant.parse(getString(column))

internal fun ResultSet.getNullableInstant(column: String): Instant? =
    getString(column)?.let(Instant::parse)
