package de.phash.semux

data class Delegate(
        val address: String,
        val name: String,
        val registeredAt: String,
        val votes: String,
        val blocksForged: String,
        val turnsHit: String,
        val turnsMissed: String,
        val validator: Boolean
)