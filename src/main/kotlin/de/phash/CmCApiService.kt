package de.phash

import org.javacord.api.event.message.MessageCreateEvent

interface CmCApiService {
    fun calculateCached(event: MessageCreateEvent)
    fun calculateSingle(event: MessageCreateEvent)


}