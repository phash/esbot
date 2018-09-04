package de.phash


import okhttp3.*
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.event.message.MessageCreateEvent
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*


fun main(args: Array<String>) {
    if (args.size <1 ) {
      System.exit(0)
    }
    val token = args.get(0)
    val api = DiscordApiBuilder().setToken(token).login().join()
    println("You can invite the bot by using the following url: " + api.createBotInvite())

    api.addMessageCreateListener { event ->

        val message = event.message.content.toLowerCase()

        if (event.message.content.equals("!ping", ignoreCase = true)) {
            event.channel.sendMessage("pong!")
        } else if (event.message.content.startsWith("!calculate", ignoreCase = true)) {
            println("calculate")
            calculate(event)
        } /*else if (event.message.content.startsWith("!galculate", ignoreCase = true)) {
            println("calculate")
            calculateGson(event)
        }*/ else if (event.message.content.startsWith("!help", ignoreCase = true)) {

            help(event)
        }


    }


}

fun help(event: MessageCreateEvent) {
    val content = event.message.content
    val contents = content.split(" ")


    val embed = EmbedBuilder()
            .setTitle("Help")

            .addField("Calculate Prices", "!calculate CUR1 CUR2 [amount]", true)
    event.getChannel().sendMessage(embed)


}

/*fun calculateGson(event: MessageCreateEvent?) {
    val client = OkHttpClient()

    val request = Request.Builder()
            .addHeader("X-CMC_PRO_API_KEY", "5d7e7efc-49a6-4381-8d50-240c10977a06")
            // .url("https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest?start=1&limit=500&convert=USD")
            .url("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest?symbol=XLQ&convert=BTC")
            .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println(call.toString())

        }

        override fun onResponse(call: Call, response: Response) {
             val gson =Gson().fromJson( response.body()?.string(), CmCResponse::class.java)

            gson.data[0].id
        }
    })
}*/

fun calculate(event: MessageCreateEvent) {
    val content = event.message.content
    val contents = content.split(" ")
    if (contents.size == 4 || contents.size == 3) {

        val client = OkHttpClient()

        val request = Request.Builder()
                .addHeader("X-CMC_PRO_API_KEY", "5d7e7efc-49a6-4381-8d50-240c10977a06")
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

                var json = JSONObject(res)
                val data = json.getJSONObject("data")
                println(data.toString())
                val currency = data.getJSONObject(contents[1].toUpperCase())
                val quote = currency.getJSONObject("quote")
                var myErg = quote.getJSONObject(contents[2].toUpperCase()).getBigDecimal("price")
                var timestamp = quote.getJSONObject(contents[2].toUpperCase()).getString("last_updated")
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
                event.getChannel().sendMessage(embed)

            }
        })
    } else {
        event.channel.sendMessage("use: !calculate XLQ BTC 3")
    }
}

fun stringtoDate(dates: String): Date {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.ENGLISH)
    var date = sdf.parse(dates)
    println(date)

    return date!!
}

/*
data class CmCResponse(val status: Status, val data: Array<CmCCurrency>)
data class Status(val timestamp: String, val error_code: String, val error_message: String, val elapsed: String, val credit_count: String)
//data class Data(val data: Array<CmCCurrency> )//, val name: String, val symbol: String, val quote: Quote)
data class CmCCurrency(val id: Integer, val name: String, val symbol: String, val quote: Array<Quote>)

data class Quote(val price: BigDecimal, val volume24: BigDecimal, val percent_change_1h: BigDecimal, val percent_change_24h: BigDecimal, val percent_change_7d: BigDecimal, val market_cap: BigDecimal, val last_updated: String)
*/

