package de.phash

import org.javacord.api.event.message.MessageCreateEvent

class OrderService {
    fun orderContent(event: MessageCreateEvent): String {
        val contents = event.message.content.split(" ")
        if (contents.size != 2) return "wrong arguments - only 1 allowed"
        val wordToOrder = contents[1]

        return orderWord(wordToOrder)
    }

    fun orderWord(wordToOrder: String): String {
        var inOrder =""
        var listed = ArrayList<String>()
        for (i in wordToOrder.indices) {
            listed.add(wordToOrder[i].toString())
        }
        listed.sort();
        inOrder= listed.joinToString ("")
        return inOrder;
    }


}