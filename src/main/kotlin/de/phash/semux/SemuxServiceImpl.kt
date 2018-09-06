package de.phash.semux

import com.google.gson.Gson
import com.semuxpool.client.ISemuxClient
import com.semuxpool.client.SemuxClient
import de.phash.AccountServiceImpl
import de.phash.PropertyService
import de.phash.Repository
import okhttp3.*
import org.apache.commons.codec.binary.Base64
import org.bson.types.ObjectId
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.event.message.MessageCreateEvent
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import java.io.IOException
import java.math.BigDecimal
import java.nio.charset.Charset
import java.text.DecimalFormat

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

    init {
        sex = SemuxClient(
                PropertyService.instance.getProperty("semuxServiceUrl"),
                PropertyService.instance.getProperty("semuxServicePort").toInt(),
                PropertyService.instance.getProperty("semuxServiceUser"),
                PropertyService.instance.getProperty("semuxServicePassword"))
    }

    override fun register(username: String, discordId: String): String {
        val res: Repository.Benutzer? = Repository.instance.col.findOne(Repository.Benutzer::name eq username)
        return if (res == null) {
            Repository.instance.col.insertOne(Repository.Benutzer(ObjectId(), username, discordId, hashMapOf(Pair("SEM", creaeteSemuxAccount()))))
            ("User ${username} created")
        } else {
            ("User ${res.name} already registered")
        }

    }

    override fun getAddress(event: MessageCreateEvent) {

        val name = event.message.author.idAsString
        val contents = event.message.content.split(" ")
        if (contents.size == 2) {

            Repository.instance.col.findOne(Repository.Benutzer::discordId eq name).let { benutzer ->
                val account = benutzer?.accounts?.get(contents[1].toUpperCase()) as Repository.Account
                val file = AccountServiceImpl.instance.createQRCode(account.address)
                val embed = EmbedBuilder()
                        .setTitle("SEMUX - www.semux.org")
                        .addField("Address", account.address)
                        .setImage(file?.toFile())

                event.message.channel.sendMessage(embed)
                return@let benutzer
            }
        }
    }

    override fun checkBalance(username: String): AccountServiceImpl.Balance {

        println("checking balance for $username")
        Repository.instance.col.findOne(Repository.Benutzer::discordId eq username).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            val semuxAccount = sex.getAccount(account.address)
            //       event.channel.sendMessage("currently available: ${semuxAccount.available}, Address: ${key.toAddressString()}")

            return AccountServiceImpl.Balance(BigDecimal(semuxAccount.available).divide(semMultiplicator), "SEM", semuxAccount.address)
        }
    }

    override fun tip(from: String, amount: String, user: String, data: String?): String {
        var resp = ""
        Repository.instance.col.findOne(Repository.Benutzer::discordId eq user).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            resp = this.send(from, amount, account.address, data)
        }
        return resp
    }

    override fun send(from: String, amount: String, address: String, data: String?): String {
        var response = ""
        println("sending $amount from $from (userId) to $address, with data: $data")
        Repository.instance.col.findOne(Repository.Benutzer::discordId eq from).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            try {
                println("User found: account.address ${account.address}")
                val amountToSend = BigDecimal(amount).multiply(semMultiplicator)
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
        var response = ""

        Repository.instance.col.findOne(Repository.Benutzer::discordId eq from).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            try {
                println("User found: account.address ${account.address}")
                val amountToSend = BigDecimal(amount).multiply(semMultiplicator)
                println("amountToSend: ${amountToSend.toPlainString()}")

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
        var response = ""

        Repository.instance.col.findOne(Repository.Benutzer::discordId eq from).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            try {
                println("User found: account.address ${account.address}")
                val amountToSend = BigDecimal(amount).multiply(semMultiplicator)
                println("amountToSend: ${amountToSend.toPlainString()}")

                response = try {
                    val result = sex.unvote(account.address, address, amountToSend.toLong(), fee)
                    "unvoted $result"
                } catch (e: Exception) {
                    e.localizedMessage
                }

                return@let benutzer
            } catch (e: Exception) {
                response = e.localizedMessage
            }
        }
        return response
    }

    override fun votes(event: MessageCreateEvent) {

        Repository.instance.col.findOne(Repository.Benutzer::discordId eq event.message.author.idAsString).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Repository.Account
            try {
                val df = DecimalFormat("0.00########")
                val client = OkHttpClient()

                //   val mediaType = MediaType.parse("application/json")

                val authString = """${PropertyService.instance.getProperty("semuxServiceUser")}:${PropertyService.instance.getProperty("semuxServicePassword")}"""
                val authStringEnc = String(Base64.encodeBase64(authString.toByteArray()))

                println("http://phash:testnet@127.0.0.1:5171/v2.1.0/account/votes?address=${account.address}")
                val request = Request.Builder()
                        .url("http://phash:testnet@127.0.0.1:5171/v2.1.0/account/votes?address=${account.address}")
                        .addHeader("content-type", "application/json")
                        .addHeader("cache-control", "no-cache")
                        .addHeader("Authorization", "Basic $authStringEnc")

                        .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        println(call.toString())
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val res = response.body()?.string()
                        println("add-> ${account.address}")
                        println("res-> $res")
                        val votes = Gson().fromJson(res, VotesList::class.java)

                        if (votes.result.isNotEmpty()) {
                            val embed = EmbedBuilder()
                                    .setTitle("Votes for SEMUX - delegates for ${event.message.author.displayName}")

                            votes.result.forEach { it -> embed.addField("${it.delegate.address} (${it.delegate.name})", df.format(BigDecimal(it.votes).divide(semMultiplicator)), true) }
                            event.channel.sendMessage(embed)
                        } else event.channel.sendMessage("no votes found")
                    }
                })
                return@let benutzer
            } catch (e: Exception) {
                event.channel.sendMessage(e.localizedMessage)
            }
        }
    }

    private fun creaeteSemuxAccount(): Repository.Account {

        return Repository.Account(ObjectId(), "SEM", sex.createAccount())
    }

}