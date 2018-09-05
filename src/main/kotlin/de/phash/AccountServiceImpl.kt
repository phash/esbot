package de.phash

import de.phash.semux.SemuxService
import de.phash.semux.SemuxServiceImpl
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.user.User
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
        val author = event.message.author
        //  println("id: ${author.id}, idAsString: ${author.idAsString}, discName: ${author.discriminatedName}, name: ${author.name}, display: ${author.displayName}")
        val semuxMessage = semuxService.register(author.displayName, author.idAsString)

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
        if (contents.size == 4 || contents.size == 5) {
            when (contents[1].toUpperCase()) {
                "SEM" -> return semuxService.send(event.message.author.idAsString, contents[2], contents[3], contents[4])
            }
        } else
            return "Use: !send CUR amount address [data]"


        return "not supported!"
    }

    override fun tip(event: MessageCreateEvent): String {
        val contents = event.message.content.split(" ")
        var resp = ""

        var users = HashSet<User>(event.message.mentionedUsers)
        var roles = event.message.mentionedRoles

        roles.forEach { users.addAll(it.users) }


        var amountToSend = BigDecimal(contents[2])
        if (users.size > 0) {
            amountToSend = amountToSend.divide(BigDecimal(users.size))
        }
        users.forEach {

            val contents = event.message.content.split(" ")
            if (contents.size == 4 || contents.size == 5) {
                var dataToSend = ""
                if (contents.size == 5) {
                    dataToSend = contents[4]
                }
                when (contents[1].toUpperCase()) {

                    "SEM" -> resp = semuxService.tip(event.message.author.idAsString, amountToSend.toPlainString(), it.idAsString, dataToSend)
                }
            } else {

                resp = "Use: !tip CUR amount @User"
            }
        }


        return resp
    }
}