package de.phash.semux

import de.phash.AccountServiceImpl
import org.javacord.api.event.message.MessageCreateEvent

interface SemuxService {

    fun register(username: String, discordId: String): String

    fun getAddress(event: MessageCreateEvent)
    fun checkBalance(username: String): AccountServiceImpl.Balance
    fun tip(from: String, amount: String, user: String, data: String?): String
    fun send(from: String, amount: String, address: String, data: String?): String
}