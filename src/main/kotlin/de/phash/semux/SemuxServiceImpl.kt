package de.phash.semux

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.semuxpool.client.ISemuxClient
import com.semuxpool.client.SemuxClient
import de.phash.AccountServiceImpl
import org.bson.types.ObjectId
import org.javacord.api.event.message.MessageCreateEvent
import org.litote.kmongo.*
import java.math.BigDecimal

class SemuxServiceImpl : SemuxService {

    data class Benutzer(val _id: ObjectId, val name: String, val accounts: HashMap<String, Account>)
    data class Account(val _id: ObjectId, val currencyName: String, val privateKey: ByteArray, val publicKey: ByteArray, val address: String)

    val sex: ISemuxClient = SemuxClient("localhost", 5171, "phash", "testnet")
    //sex.createAccount()
    val client: MongoClient = KMongo.createClient("localhost", 27017)
    val database: MongoDatabase = client.getDatabase("accounts")
    val col: MongoCollection<Benutzer>

    init {
        col = database.getCollection<Benutzer>()
        println(" ($this) is initialized")
    }

    private object Holder {
        val INSTANCE = SemuxServiceImpl()
    }

    companion object {
        val instance: SemuxServiceImpl by lazy { Holder.INSTANCE }
    }

    override fun register(name: String): String {


        var res: Benutzer? = col.findOne(Benutzer::name eq name)
        if (res == null) {

            col.insertOne(Benutzer(ObjectId(), name, hashMapOf(Pair("SEM", getSemuxAccount()))))
            return ("User ${name} created")
        } else {
            res.accounts.put("SEM", getSemuxAccount())
            col.updateOne(res) // Benutzer(name, mapOf(Pair("a", "NEU"), Pair("b", "x"))))
            return ("User ${res.name} already registered")
        }


    }

    override fun getAddress(event: MessageCreateEvent) {

        val name = event.message.author.idAsString
        val contents = event.message.content.split(" ")
        if (contents.size == 2) {


            col.findOne(Benutzer::name eq name).let { benutzer ->
                val account = benutzer?.accounts?.get(contents[1].toUpperCase()) as Account

                val key = Key(account.privateKey, account.publicKey)
                if (event.message.userAuthor.isPresent) {
                    val userAuthor = event.message.userAuthor.get()
                    userAuthor.sendMessage("your credentials for ${contents[1].toUpperCase()} - keep them save! Loss of this can hurt you!")
                    userAuthor.sendMessage("your address for ${contents[1].toUpperCase()} is ${key.toAddressString()}")
                    userAuthor.sendMessage("your public key for ${contents[1].toUpperCase()} is ${Hex.encode0x(key.getPublicKey())}")
                    userAuthor.sendMessage("your private key for ${contents[1].toUpperCase()} is ${Hex.encode0x(key.getPrivateKey())}")
                    event.channel.sendMessage("User Account: ${benutzer.name}, Address: ${key.toAddressString()}")
                }
                return@let benutzer
            }

        }
    }

    override fun checkBalance(name: String): AccountServiceImpl.Balance {

        println("checking balance for $name")
        col.findOne(Benutzer::name eq name).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Account
            val semuxAccount = sex.getAccount(account.address)
            //       event.channel.sendMessage("currently available: ${semuxAccount.available}, Address: ${key.toAddressString()}")

            return AccountServiceImpl.Balance(BigDecimal(semuxAccount.available).divide(BigDecimal("1000000000")), "SEM", semuxAccount.address)
        }


    }

    override fun tip(from: String, amount: String, user: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun send(from: String, amount: String, address: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        /*
        col.findOne(Benutzer::name eq from).let { benutzer ->
            val account = benutzer?.accounts?.get("SEM") as Account

            sex.sendTransaction()

            return@let benutzer
        }*/
    }

    private fun createSemuxAccount(): Key {
        return Key()
    }

    private fun getSemuxAccount(): Account {
        val key = createSemuxAccount()
        return Account(ObjectId(), "SEM", key.getPrivateKey(), key.getPublicKey(), key.toAddressString())
    }

}