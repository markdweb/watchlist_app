package com.cheese.watchlist.server

import kotlin.random.Random

/** Generates a random 6-digit temporary access code, regenerated fresh whenever requested. */
object PasscodeGenerator {
    fun generate(): String {
        return Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
    }

    /** Longer random token used for session cookies (not shown to the user). */
    fun generateSessionToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32).map { chars.random() }.joinToString("")
    }
}
