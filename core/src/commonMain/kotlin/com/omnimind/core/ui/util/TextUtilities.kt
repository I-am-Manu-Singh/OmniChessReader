package com.creativeblisslabs.omnimind.core.ui.util

fun unescapeHtml(text: String): String {
    return text.replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
}
