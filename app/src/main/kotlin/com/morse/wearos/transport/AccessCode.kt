package com.morse.wearos.transport

import com.morse.wearos.domain.GroupConfig
import kotlin.random.Random

object AccessCode {
    fun generate(): String =
        (1..GroupConfig.CODE_LENGTH).joinToString("") { Random.nextInt(10).toString() }

    fun isValid(code: String): Boolean =
        code.length == GroupConfig.CODE_LENGTH && code.all { it.isDigit() }
}
