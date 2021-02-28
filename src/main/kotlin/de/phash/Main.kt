package de.phash


import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.event.message.MessageCreateEvent
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger

val cmCApiService = CmCApiServiceImpl.instance as CmCApiService

val accountService = AccountServiceImpl.instance as AccountService

val log = Logger.getLogger("BotLog")
fun main(args: Array<String>) {

    val token = PropertyService.instance.getProperty("discordToken")
    val api = DiscordApiBuilder().setToken(token).login().join()
    println("You can invite the bot by using the following url: " + api.createBotInvite())

    api.addMessageCreateListener { event ->

        if (event.message.content.startsWith("!calculate", ignoreCase = true))
            calculate(event)
        else if (event.message.content.startsWith("!help", ignoreCase = true))
            help(event)
        else if (event.message.content.startsWith("!bc", ignoreCase = true))
            calculateCached(event)
        else if (event.message.content.startsWith("!register", ignoreCase = true))
            regist(event)
        else if (event.message.content.startsWith("!account", ignoreCase = true))
            account(event)
        else if (event.message.content.startsWith("!send", ignoreCase = true))
            send(event)
        else if (event.message.content.startsWith("!tip", ignoreCase = true))
            tip(event)
        else if (event.message.content.startsWith("!vote", ignoreCase = true))
            vote(event)
        else if (event.message.content.startsWith("!unvote", ignoreCase = true))
            unvote(event)
        else if (event.message.content.startsWith("!listvotes", ignoreCase = true))
            listvotes(event)
        else if (event.message.content.startsWith("!cookie", ignoreCase = true))
            cookie(event)
        else if (event.message.content.startsWith("!balance", ignoreCase = true))
            checkBalance(event)
    }
}

private fun cookie(event: MessageCreateEvent) {
    log.info("Event received ${event.messageContent}")
    event.channel.sendMessage("serving ${event.message.author.displayName} a ${PropertyService.instance.getProperty("cookie")}")
}

fun listvotes(event: MessageCreateEvent) {
    accountService.listVotes(event)
}

fun vote(event: MessageCreateEvent) {
    event.message.channel.sendMessage(accountService.vote(event))

}

fun unvote(event: MessageCreateEvent) {
    event.message.channel.sendMessage(accountService.unvote(event))
}

fun tip(event: MessageCreateEvent) {
    event.message.channel.sendMessage(accountService.tip(event))
}

fun send(event: MessageCreateEvent) {
    event.message.channel.sendMessage(accountService.send(event))
}

fun checkBalance(event: MessageCreateEvent) {
    accountService.checkBalance(event)
}

fun account(event: MessageCreateEvent) {
    accountService.getAccount(event)
}

fun regist(event: MessageCreateEvent) {
    accountService.register(event)
}

fun calculateCached(event: MessageCreateEvent) {
    cmCApiService.calculateCached(event)
}

fun help(event: MessageCreateEvent) {
    val embed = EmbedBuilder()
            .setTitle("Help")
            .addField("View market price", "!bc CUR [amount]", true)
            .addField("Get a cookie!", "!cookie", true)
            .addField("Calculate Prices (Admin only)", "!calculate CUR1 CUR2 [amount]", true)
            .addField("check Balance", "!balance", true)
            .addField("register new acc", "!register", true)
            .addField("Get Keys", "!account CUR", true)
            .addField("Send Money to address", "!send CUR amount receiver", true)
            .addField("Tip Money", "!tip CUR amount @user", true)
            .addField("Vote", "!vote CUR amount delegateAddress", true)
            .addField("Unvote", "!unvote CUR amount delegateAddress", true)
            .addField("List Votes", "!listvotes CUR", true)

    event.channel.sendMessage(embed)

}


fun calculate(event: MessageCreateEvent) {
    log.info("Event received ${event.messageContent}")
    cmCApiService.calculateSingle(event)
}

fun stringtoDate(date: String): Date {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.ENGLISH)
    return sdf.parse(date)!!
}

