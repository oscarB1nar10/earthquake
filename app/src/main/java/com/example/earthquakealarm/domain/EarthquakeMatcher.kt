package com.example.earthquakealarm.domain

/**
 * Pure, framework-free keyword matcher — the heart of the detection logic,
 * deliberately isolated so it can be unit-tested without Android.
 */
class EarthquakeMatcher {

    /** Returns the first keyword contained in [text], or null if none match. */
    fun firstMatch(text: String, keywords: List<String>): String? {
        if (text.isBlank() || keywords.isEmpty()) return null
        val haystack = text.lowercase()
        return keywords.firstOrNull { haystack.contains(it) }
    }
}
