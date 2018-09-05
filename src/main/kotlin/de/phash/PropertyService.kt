package de.phash

import java.io.FileInputStream
import java.util.*

class PropertyService {
    init {
        println("This ($this) is a singleton")
    }

    private object Holder {
        val INSTANCE = PropertyService()
    }

    companion object {
        val instance: PropertyService by lazy { Holder.INSTANCE }
        val prop = Properties()
    }


    fun getProperty(key: String): String {
        return prop.getProperty(key)
    }

    fun readProperties(get: String) {
        FileInputStream(get).use {
            prop.load(it)
        }
    }


    private constructor() {

        FileInputStream("config/config.properties").use {
            prop.load(it)
        }
    }
}
