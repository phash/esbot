package de.phash

import org.javacord.api.event.message.MessageCreateEvent
import java.nio.file.Path

interface AccountService {

    fun checkBalance(event: MessageCreateEvent)
    fun getAccount(event: MessageCreateEvent)
    fun register(event: MessageCreateEvent)
    fun send(event: MessageCreateEvent): String
    fun tip(event: MessageCreateEvent): String
    fun unvote(event: MessageCreateEvent): String

    fun vote(event: MessageCreateEvent): String
    fun listVotes(event: MessageCreateEvent)

    fun createQRCode(toCode: String): Path?
}