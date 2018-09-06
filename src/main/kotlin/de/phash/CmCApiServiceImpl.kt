package de.phash

import okhttp3.*
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.event.message.MessageCreateEvent
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.util.*
import kotlin.collections.HashMap

class CmCApiServiceImpl private constructor() : CmCApiService {
    init {
        println("This ($this) is a singleton")
    }

    private object Holder {
        val INSTANCE = CmCApiServiceImpl()
    }

    companion object {
        val instance: CmCApiServiceImpl by lazy { Holder.INSTANCE }
    }

    var timestamp: Date = Calendar.getInstance().time
    var cachedCurrencies: HashMap<String, JSONObject> = HashMap(5000)


    override fun calculateCached(event: MessageCreateEvent) {

        val content = event.message.content
        val contents = content.split(" ")
        if (contents.size == 2 || contents.size == 3) {
            val now = Calendar.getInstance().time

            if (timestamp.time + (60 * 1000) < now.time || cachedCurrencies.isEmpty()) {
                refreshCache()
                println("cache refreshed, old timestamp= $timestamp, new: ${Calendar.getInstance().time} ")
                timestamp = Calendar.getInstance().time

                Thread.sleep(1000)
            }

            val currency = cachedCurrencies.get(contents[1].toUpperCase())

            val quote = currency?.getJSONObject("quote")
            val myErg = quote?.getJSONObject("BTC")?.getBigDecimal("price")


            val embed = EmbedBuilder()
                    .setTitle("calculating  ${contents[1].toUpperCase()} in BTC")
                    .addField("Searched Currency", contents[1].toUpperCase(), true)
                    .addField("Conversion Currency", "BTC", true)
            if (contents.size == 3) {
                val calculated = myErg?.multiply(BigDecimal(contents[2]))
                embed.addField("Quantity", contents[2], true)
                embed.addField("Value", "$calculated", true)
            }
            embed.addField("last updated", "$timestamp", true)
            event.channel.sendMessage(embed)
        }

    }

    private fun refreshCache() {
        val client = OkHttpClient()

        val request = Request.Builder()
                .addHeader("X-CMC_PRO_API_KEY", PropertyService.prop.getProperty("cmcAPIKey"))
                .url("https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest?start=1&limit=5000&convert=BTC")
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println(call.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                val res = response.body()?.string()
                val json = JSONObject(res)
                val currencies = json.getJSONArray("data")
                for (i in 0 until currencies.length()) {
                    val currency = currencies.getJSONObject(i)
                    cachedCurrencies.put(currency.getString("symbol"), currency)
                }
            }
        })
    }

    override fun calculateSingle(event: MessageCreateEvent) {

        if (event.message.author.isServerAdmin) {

            val content = event.message.content
            val contents = content.split(" ")
            if (contents.size == 4 || contents.size == 3) {

                val client = OkHttpClient()

                val request = Request.Builder()
                        .addHeader("X-CMC_PRO_API_KEY", PropertyService.prop.getProperty("cmcAPIKey"))
                        // .url("https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest?start=1&limit=500&convert=USD")
                        .url("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest?symbol=${contents[1].toUpperCase()}&convert=${contents[2].toUpperCase()}")
                        .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        println(call.toString())

                    }

                    override fun onResponse(call: Call, response: Response) {
                        val res = response.body()?.string()
                        println(res)

                        val json = JSONObject(res)
                        val data = json.getJSONObject("data")
                        println(data.toString())
                        val currency = data.getJSONObject(contents[1].toUpperCase())
                        val quote = currency.getJSONObject("quote")
                        val myErg = quote.getJSONObject(contents[2].toUpperCase()).getBigDecimal("price")
                        val timestamp = quote.getJSONObject(contents[2].toUpperCase()).getString("last_updated")
                        var calculated = myErg
                        var anzahl = ""
                        if (contents.size == 4) {
                            calculated = myErg.multiply(BigDecimal(contents[3]))
                            anzahl = contents[3]
                        }

                        val embed = EmbedBuilder()
                                .setTitle("calculating ${anzahl} ${contents[1].toUpperCase()} in ${contents[2]}")
                                .addField("Searched Currency", contents[1].toUpperCase(), true)
                                .addField("Conversion Currency", contents[2].toUpperCase(), true)
                        if (contents.size == 4) {
                            embed.addField("Quantity", contents[3], true)
                        }
                        embed.addField("Value", "$calculated", true)
                        embed.addField("last updated", "${stringtoDate(timestamp)}", true)
                        event.channel.sendMessage(embed)

                    }
                })
            } else {
                event.channel.sendMessage("use: !calculate XLQ BTC 3")
            }
        } else {
            event.channel.sendMessage("Only admins may use this command")

        }
    }
}