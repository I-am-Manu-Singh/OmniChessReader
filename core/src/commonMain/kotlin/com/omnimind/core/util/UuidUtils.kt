package com.creativeblisslabs.omnimind.core.util

import kotlin.random.Random

object UuidUtils {
    /**
     * Generates a simple UUID-like string for common use in KMP.
     * format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
     */
    fun generateUUID(): String {
        val chars = "0123456789abcdef"
        
        fun randomHex(length: Int): String {
            return (1..length).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        }

        val variantChars = "89ab"
        val variant = variantChars[Random.nextInt(variantChars.length)]

        return "${randomHex(8)}-${randomHex(4)}-4${randomHex(3)}-$variant${randomHex(3)}-${randomHex(12)}"
    }
}
