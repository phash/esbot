package de.phash.semux

data class VotesList(
        val success: Boolean,
        val message: String,
        val result: List<Result>
)