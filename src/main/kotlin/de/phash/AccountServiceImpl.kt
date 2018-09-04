package de.phash

import de.phash.semux.SemuxService
import de.phash.semux.SemuxServiceImpl
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.event.message.MessageCreateEvent
import java.math.BigDecimal
import java.text.DecimalFormat

class AccountServiceImpl : AccountService {
    init {
        println("This ($this) is a singleton")
    }

    private object Holder {
        val INSTANCE = AccountServiceImpl()
    }

    data class Balance(val available: BigDecimal, val currency: String, val address: String)

    companion object {
        val instance: AccountServiceImpl by lazy { Holder.INSTANCE }
    }

    val semuxService = SemuxServiceImpl.instance as SemuxService

    override fun getAccount(event: MessageCreateEvent) {
        val contents = event.message.content.split(" ")
        if (contents.size != 2) {
            event.message.channel.sendMessage("Use !account CUR")
            return
        }
        when (contents[1].toUpperCase()) {
            "SEM" -> semuxService.getAddress(event)
        }
    }

    override fun register(event: MessageCreateEvent) {
        val contents = event.message.content.split(" ")
        val author = event.message.author
        //  println("id: ${author.id}, idAsString: ${author.idAsString}, discName: ${author.discriminatedName}, name: ${author.name}, display: ${author.displayName}")
        val semuxMessage = semuxService.register(author.idAsString)

        event.channel.sendMessage(semuxMessage)
    }

    override fun checkBalance(event: MessageCreateEvent) {

        val balances = ArrayList<Balance>()
        var df = DecimalFormat("0.00########")

        balances.add(semuxService.checkBalance(event.message.author.idAsString))

        val embed = EmbedBuilder()
                .setTitle("Account Details  ")
        balances.forEach {

            embed.addField("Currency: ", it.currency, true)
            embed.addField("Address", it.address, false)
            embed.addField("Balance", df.format(it.available), false)

        }

        event.channel.sendMessage(embed)

    }

    override fun send(event: MessageCreateEvent): String {
        val contents = event.message.content.split(" ")
        if (contents.size != 4) return "Use: !send CUR amount address"
        when (contents[1].toUpperCase()) {
            "SEM" -> semuxService.send(contents[2], contents[3])
        }


        return ""
    }

    override fun tip(event: MessageCreateEvent): String {
        val contents = event.message.content.split(" ")
        if (contents.size != 4) return "Use: !tip CUR amount @User"
        when (contents[1].toUpperCase()) {
            "SEM" -> semuxService.tip(contents[2], contents[3])
        }


        return ""
    }
}