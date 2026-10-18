package com.creativeblisslabs.omnimind.core.util

import kotlinx.serialization.json.Json

object JsonUtils {
    val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Attempts to extract a JSON object from a string that might contain other text or Markdown blocks.
     */
    fun extractJsonObject(input: String): String {
        val start = input.indexOf('{')
        val end = input.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return input.substring(start, end + 1)
        }
        return input
    }

    /**
     * Attempts to extract a JSON array from a string.
     */
    fun extractJsonArray(input: String): String {
        val start = input.indexOf('[')
        val end = input.lastIndexOf(']')
        if (start != -1 && end != -1 && end > start) {
            return input.substring(start, end + 1)
        }
        return input
    }
}
