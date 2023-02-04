package com.yandex.mobile.realty.migration

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @author rogovalex on 28.02.18.
 */
class LastFloorMigratorTest {
    private val migrator = LastFloorMigrator()

    @Test
    fun exceptLastFloorTrue() {
        val json = JsonObject().apply {
            addProperty("dealType", "BUY")
            addProperty("propertyType", "APARTMENT")
            addProperty("exceptLastFloor", true)
        }.toString()

        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject
        val exceptLastFloorMigrated = migratedJsonObject.get("exceptLastFloor")?.asBoolean
        val lastFloorMigrated = migratedJsonObject.get("lastFloor")?.asBoolean

        assertNull(exceptLastFloorMigrated)
        assertNotNull(lastFloorMigrated)
        assertEquals(false, lastFloorMigrated)
    }

    @Test
    fun exceptLastFloorFalse() {
        val json = JsonObject().apply {
            addProperty("dealType", "BUY")
            addProperty("propertyType", "APARTMENT")
        }.toString()

        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject
        val exceptLastFloorMigrated = migratedJsonObject.get("exceptLastFloor")?.asBoolean
        val lastFloorMigrated = migratedJsonObject.get("lastFloor")?.asBoolean

        assertNull(exceptLastFloorMigrated)
        assertNull(lastFloorMigrated)
    }
}
