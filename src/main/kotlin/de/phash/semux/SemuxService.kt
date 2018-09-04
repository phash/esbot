package de.phash.semux

import de.phash.AccountServiceImpl
import org.javacord.api.event.message.MessageCreateEvent

interface SemuxService {

    fun register(username: String): String

    fun getAddress(event: MessageCreateEvent)
    fun checkBalance(username: String): AccountServiceImpl.Balance
    fun tip(from: String, amount: String, user: String)
    fun send(from: String, amount: String, address: String)
}