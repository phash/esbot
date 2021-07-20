package de.phash

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class OrderServiceTest {

    @Test
    fun orderContent() {
        val os = OrderService()
        assertEquals("abc", os.orderWord("abc") )
    }

    @Test
    fun orderContentDifferent_one() {
        val os = OrderService()
        assertEquals("abc", os.orderWord("acb") )
    }
}