package com.yandex.mdm

import com.google.gson.Gson
import com.yandex.entity.Metadata
import org.junit.Assert.assertNull
import org.junit.Test

class GsonParserNullabilityTest {
    fun readJson(fileName: String): Metadata {
        val configText = javaClass.getResource(fileName)!!.readText()
        return Gson().fromJson(configText, Metadata::class.java)
    }

    @Test
    fun checkNullsJsonParsing() {
        with(readJson("hypercube_config_nulls.json")) {
            assertNull(bookedTo)
            assertNull(location)
            assertNull(deviceName)
            assertNull(previousOwner)
            assertNull(serialNumber)
            assertNull(owner)
        }
    }

    @Test
    fun checkEmptyJsonParsing() {
        with(readJson("hypercube_config_empty.json")) {
            assertNull(bookedTo)
            assertNull(location)
            assertNull(deviceName)
            assertNull(previousOwner)
            assertNull(serialNumber)
            assertNull(owner)
        }
    }
}
