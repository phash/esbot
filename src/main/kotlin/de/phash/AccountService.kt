package de.phash

import org.javacord.api.event.message.MessageCreateEvent

interface AccountService {

    fun checkBalance(event: MessageCreateEvent)
    fun getAccount(event: MessageCreateEvent)
    fun register(event: MessageCreateEvent)
    fun send(event: MessageCreateEvent): String
    fun tip(event: MessageCreateEvent): String
}