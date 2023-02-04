package com.yandex.mobile.realty.migration

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @author rogovalex on 17/01/2019.
 */
class OpenPlanMigratorTest {

    private val migrator = OpenPlanMigrator()

    @Test
    fun testReplaceOpenPlan() {
        val json = JsonObject().apply {
            add(
                "roomsCount",
                JsonArray().apply {
                    add("OPEN_PLAN")
                }
            )
        }.toString()

        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject
        val migratedRoomsCount = migratedJsonObject.get("roomsCount").asJsonArray

        assertEquals(1, migratedRoomsCount.size())
        assertEquals("STUDIO", migratedRoomsCount.get(0).asString)
    }

    @Test
    fun testRemoveOpenPlan() {
        val json = JsonObject().apply {
            add(
                "roomsCount",
                JsonArray().apply {
                    add("OPEN_PLAN")
                    add("STUDIO")
                }
            )
        }.toString()

        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject
        val migratedRoomsCount = migratedJsonObject.get("roomsCount").asJsonArray

        assertEquals(1, migratedRoomsCount.size())
        assertEquals("STUDIO", migratedRoomsCount.get(0).asString)
    }

    @Test
    fun testNoOpenPlan() {
        val json = JsonObject().apply {
            add(
                "roomsCount",
                JsonArray().apply {
                    add("ONE")
                    add("STUDIO")
                }
            )
        }.toString()

        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject
        val migratedRoomsCount = migratedJsonObject.get("roomsCount").asJsonArray

        assertEquals(2, migratedRoomsCount.size())
        assertEquals("ONE", migratedRoomsCount.get(0).asString)
        assertEquals("STUDIO", migratedRoomsCount.get(1).asString)
    }

    @Test
    fun testEmptyRoomsCount() {
        val json = JsonObject().apply {
            add("roomsCount", JsonArray())
        }.toString()

        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject
        val migratedRoomsCount = migratedJsonObject.get("roomsCount").asJsonArray

        assertEquals(0, migratedRoomsCount.size())
    }

    @Test
    fun testNoRoomsCount() {
        val json = JsonObject().toString()

        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject
        val migratedRoomsCount = migratedJsonObject.get("roomsCount")?.asJsonArray

        assertNull(migratedRoomsCount)
    }
}
