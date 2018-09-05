package de.phash.semux

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.semuxpool.client.ISemuxClient
import com.semuxpool.client.SemuxClient
import de.phash.AccountServiceImpl
import de.phash.PropertyService
import org.bson.types.ObjectId
import org.javacord.api.event.message.MessageCreateEvent
import org.litote.kmongo.KMongo
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import java.math.BigDecimal
import java.nio.charset.Charset

class SemuxServiceImpl : SemuxService {


    data class Benutzer(val _id: ObjectId, val name: String, val discordId: String, val accounts: HashMap<String, Account>)
    data class Account(val _id: ObjectId, val currencyName: String, val address: String)

    private val sex: ISemuxClient
    //sex.createAccount()
    private val client: MongoClient = KMongo.createClient("localhost", 27017)
    private val database: MongoDatabase = client.getDatabase("accounts")
    private val col: MongoCollection<Benutzer>

    init {
        col = database.getCollection<Benutzer>()
        println(" ($this) is initialized")
    }

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


        var res: Benutzer? = col.findOne(Benutzer::name eq name)
        if (res == null) {

            col.insertOne(Benutzer(ObjectId(), name, discordId, hashMapOf(Pair("SEM", creaeteSemuxAccount()))))

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

            col.findOne(Benutzer::discordId eq name).let { benutzer ->
                val account = benutzer?.accounts?.get(contents[1].toUpperCase()) as Account
                event.message.channel.sendMessage("SEM address: ${account.address}")

                return@let benutzer
            }

        }
    }

    override fun checkBalance(name: String): AccountServiceImpl.Balance {

        println("checking balance for $name")
        col.findOne(Benutzer::discordId eq name).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Account
            val semuxAccount = sex.getAccount(account.address)
            //       event.channel.sendMessage("currently available: ${semuxAccount.available}, Address: ${key.toAddressString()}")

            return AccountServiceImpl.Balance(BigDecimal(semuxAccount.available).divide(BigDecimal("1000000000")), "SEM", semuxAccount.address)
        }


    }

    override fun tip(from: String, amount: String, userTo: String, data: String?): String {
        var resp = ""
        col.findOne(Benutzer::discordId eq userTo).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Account
            resp = this.send(from, amount, account.address, data)
        }
        return resp
    }

    override fun send(from: String, amount: String, address: String, data: String?): String {
        var response: String = ""
        println("sending $amount from $from (userId) to $address, with data: $data")
        val let = col.findOne(Benutzer::discordId eq from).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Account
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

        val let = col.findOne(Benutzer::discordId eq from).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Account
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

        val let = col.findOne(Benutzer::discordId eq from).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Account
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

    private fun creaeteSemuxAccount(): Account {

        val addr = sex.createAccount()
        val acc = sex.getAccount(addr)
        //val key = createSemuxAccount()
        return Account(ObjectId(), "SEM", addr)
    }

}