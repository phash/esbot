package de.phash

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.types.ObjectId
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection

class Repository {
    fun insertOne(benutzer: Repository.Benutzer) {
        col.insertOne(benutzer)
    }


    data class Benutzer(val _id: ObjectId, val name: String, val discordId: String, val accounts: HashMap<String, Account>)
    data class Account(val _id: ObjectId, val currencyName: String, val address: String)

    private object Holder {
        val INSTANCE = Repository()
    }

    companion object {
        val instance: Repository by lazy { Holder.INSTANCE }
    }

    private val client: MongoClient = KMongo.createClient()
   // ("localhost", 27017)
    private val database: MongoDatabase = client.getDatabase(PropertyService.instance.getProperty("dbName"))
    val col: MongoCollection<Benutzer>

    init {
        col = database.getCollection<Benutzer>()
        println(" ($this) is initialized")
    }
}