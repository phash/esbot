package de.phash.semux

import com.semuxpool.client.ISemuxClient
import com.semuxpool.client.SemuxClient
import de.phash.AccountServiceImpl
import de.phash.PropertyService
import de.phash.Repository
import org.bson.types.ObjectId
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.event.message.MessageCreateEvent
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import java.math.BigDecimal
import java.nio.charset.Charset

class SemuxServiceImpl : SemuxService {

    private val sex: ISemuxClient
    //sex.createAccount()

    private object Holder {
        val INSTANCE = SemuxServiceImpl()
    }

    companion object {
        val instance: SemuxServiceImpl by lazy { Holder.INSTANCE }
        val fee = PropertyService.instance.getProperty("semuxFee").toLong()
        val devFee = PropertyService.instance.getProperty("semuxDevFee").toLong()

        val semMultiplicator = BigDecimal("1000000000")
    }

    constructor() {
        sex = SemuxClient(
                PropertyService.instance.getProperty("semuxServiceUrl"),
                PropertyService.instance.getProperty("semuxServicePort").toInt(),
                PropertyService.instance.getProperty("semuxServiceUser"),
                PropertyService.instance.getProperty("semuxServicePassword"))
    }

    override fun register(name: String, discordId: String): String {


        var res: Repository.Benutzer? = Repository.instance.col.findOne(Repository.Benutzer::name eq name)
        if (res == null) {

            Repository.instance.col.insertOne(Repository.Benutzer(ObjectId(), name, discordId, hashMapOf(Pair("SEM", creaeteSemuxAccount()))))

            return ("User ${name} created")
        } else {
            //  res.accounts.put("SEM", creaeteSemuxAccount())
            //  col.updateOne(res) // Benutzer(name, mapOf(Pair("a", "NEU"), Pair("b", "x"))))
            return ("User ${res.name} already registered")
        }

    }

    override fun getAddress(event: MessageCreateEvent) {

        val name = event.message.author.idAsString
        val contents = event.message.content.split(" ")
        if (contents.size == 2) {

            Repository.instance.col.findOne(Repository.Benutzer::discordId eq name).let { benutzer ->
                val account = benutzer?.accounts?.get(contents[1].toUpperCase()) as Repository.Account
                event.message.channel.sendMessage("SEM address: ${account.address}")

                return@let benutzer
            }

        }
    }

    override fun checkBalance(name: String): AccountServiceImpl.Balance {

        println("checking balance for $name")
        Repository.instance.col.findOne(Repository.Benutzer::discordId eq name).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            val semuxAccount = sex.getAccount(account.address)
            //       event.channel.sendMessage("currently available: ${semuxAccount.available}, Address: ${key.toAddressString()}")

            return AccountServiceImpl.Balance(BigDecimal(semuxAccount.available).divide(BigDecimal("1000000000")), "SEM", semuxAccount.address)
        }


    }

    override fun tip(from: String, amount: String, userTo: String, data: String?): String {
        var resp = ""
        Repository.instance.col.findOne(Repository.Benutzer::discordId eq userTo).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            resp = this.send(from, amount, account.address, data)
        }
        return resp
    }

    override fun send(from: String, amount: String, address: String, data: String?): String {
        var response: String = ""
        println("sending $amount from $from (userId) to $address, with data: $data")
        val let = Repository.instance.col.findOne(Repository.Benutzer::discordId eq from).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            try {
                println("User found: account.address ${account.address}")
                val amountToSend = BigDecimal(amount).multiply(BigDecimal("1000000000"))
                println("amountToSend: ${amountToSend.toPlainString()}")
                var dataToSend: ByteArray? = null
                if (data != null) {

                    dataToSend = data.toByteArray(Charset.forName(Charsets.UTF_8.name()))
                }
                try {
                    val result = sex.transfer(amountToSend.toLong(), account.address, address, fee, dataToSend)
                    response = "TX Hash: " + result +
                            "\nhttps://semux.info/explorer/transaction/" + result
                } catch (e: Exception) {
                    response = e.localizedMessage
                }

                return@let benutzer
            } catch (e: Exception) {
                response = e.localizedMessage
            }

        }
        return response
    }

    override fun vote(from: String, amount: String, address: String): String {
        var response: String = ""

        val let = Repository.instance.col.findOne(Repository.Benutzer::discordId eq from).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            try {
                println("User found: account.address ${account.address}")
                val amountToSend = BigDecimal(amount).multiply(BigDecimal("1000000000"))
                println("amountToSend: ${amountToSend.toPlainString()}")
                var dataToSend: ByteArray? = null

                try {
                    val result = sex.vote(account.address, address, amountToSend.toLong(), fee)
                    response = "voted $result"
                } catch (e: Exception) {
                    response = e.localizedMessage
                }

                return@let benutzer
            } catch (e: Exception) {
                response = e.localizedMessage
            }

        }
        return response
    }

    override fun unvote(from: String, amount: String, address: String): String {
        var response: String = ""

        val let = Repository.instance.col.findOne(Repository.Benutzer::discordId eq from).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            try {
                println("User found: account.address ${account.address}")
                val amountToSend = BigDecimal(amount).multiply(BigDecimal("1000000000"))
                println("amountToSend: ${amountToSend.toPlainString()}")
                var dataToSend: ByteArray? = null

                try {
                    val result = sex.unvote(account.address, address, amountToSend.toLong(), fee)
                    response = "unvoted $result"
                } catch (e: Exception) {
                    response = e.localizedMessage
                }

                return@let benutzer
            } catch (e: Exception) {
                response = e.localizedMessage
            }

        }
        return response
    }

    override fun votes(event: MessageCreateEvent) {

        val let = Repository.instance.col.findOne(Repository.Benutzer::discordId eq event.message.author.idAsString).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            try {
                println("User found: account.address ${account.address}")

                try {
                    val result = sex.getVotes(account.address)
                    if (result.isNotEmpty()) {

                        val embed = EmbedBuilder()
                                .setTitle("Votes for SEMUX - delegates for ${event.message.author.displayName}")
                        result.forEach { (key, value) -> embed.addField(key, "$value", true) }
                        event.channel.sendMessage(embed)

                    } else event.channel.sendMessage("no votes found")

                } catch (e: Exception) {
                    event.channel.sendMessage(e.localizedMessage)
                }

                return@let benutzer
            } catch (e: Exception) {
                event.channel.sendMessage(e.localizedMessage)
            }

        }
    }

    private fun creaeteSemuxAccount(): Repository.Account {

        val addr = sex.createAccount()
        val acc = sex.getAccount(addr)
        //val key = createSemuxAccount()
        return Repository.Account(ObjectId(), "SEM", addr)
    }

}