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
        val df = DecimalFormat("0.00########")

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

    private val devAdress: String = PropertyService.instance.getProperty("semuxDevAddress")

    override fun send(event: MessageCreateEvent): String {
        val contents = event.message.content.split(" ")
        if (contents.size == 4 || contents.size == 5) {

            when (contents[1].toUpperCase()) {
                "SEM" -> {
                    val available = semuxService.checkBalance(event.message.author.idAsString).available.multiply(SemuxServiceImpl.semMultiplicator)
                    val fee = BigDecimal(SemuxServiceImpl.fee)
                    val devFee = BigDecimal(SemuxServiceImpl.devFee)
                    val amount = BigDecimal(contents[2]).multiply(SemuxServiceImpl.semMultiplicator)
                    val possible = available.subtract(fee.multiply(BigDecimal("2")).add(devFee).add(amount))
                    var dataToSend = ""
                    if (contents.size == 5) {
                        dataToSend = contents[4]
                    }
                    println("available $available - fee $fee - devFee $devFee - amount - $amount + possible $possible")
                    if (possible.compareTo(BigDecimal.ZERO) > 0) {

                        var resp = semuxService.send(event.message.author.idAsString, contents[2], contents[3], dataToSend)
                        semuxService.send(event.message.author.idAsString, devFee.divide(SemuxServiceImpl.semMultiplicator).toPlainString(), devAdress, "dev fee")
                        resp += " plus devFee: ${devFee.divide(SemuxServiceImpl.semMultiplicator).toPlainString()}"
                        return resp
                    } else {
                        return "not enough funds!"
                    }
                }
            }
        } else
            return "Use: !send CUR amount address [data]"


        return "not supported!"
    }

    override fun tip(event: MessageCreateEvent): String {
        val contents = event.message.content.split(" ")
        var resp = ""

        val users = HashSet<User>(event.message.mentionedUsers)
        val roles = event.message.mentionedRoles

        roles.forEach { users.addAll(it.users) }


        var amountToSend = BigDecimal(contents[2])
        if (users.size > 0) {
            amountToSend = amountToSend.divide(BigDecimal(users.size)).subtract(BigDecimal(SemuxServiceImpl.fee).divide(SemuxServiceImpl.semMultiplicator))
        }
        users.forEach {

            if (contents.size.equals(4) || contents.size.equals(5)) {
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

    override fun listVotes(event: MessageCreateEvent) {
        val contents = event.message.content.split(" ")
        if (contents.size != 2) {
            event.channel.sendMessage("use !listvotes CUR")
        } else {
            when (contents[1].toUpperCase()) {
                "SEM" ->
                    semuxService.votes(event)
            }
        }
    }

    override fun unvote(event: MessageCreateEvent): String {
        val contents = event.message.content.split(" ")
        if (contents.size != 4) return "use !vote cur amount address"
        val amountToSend = BigDecimal(contents[2])
        var resp = ""
        when (contents[1].toUpperCase()) {
            "SEM" ->
                resp = "Unvoted: ${semuxService.unvote(event.message.author.idAsString, amountToSend.toPlainString(), contents[3])}"
        }
        return resp
    }

    override fun vote(event: MessageCreateEvent): String {

        val contents = event.message.content.split(" ")
        if (contents.size != 4) return "use !vote cur amount address"
        val amountToSend = BigDecimal(contents[2])
        var resp = ""
        when (contents[1].toUpperCase()) {
            "SEM" ->
                resp = "Voted: ${semuxService.vote(event.message.author.idAsString, amountToSend.toPlainString(), contents[3])}"
        }
        return resp
    }
}