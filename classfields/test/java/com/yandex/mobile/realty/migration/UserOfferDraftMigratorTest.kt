package com.yandex.mobile.realty.migration

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @author andrey-bgm on 07/07/2021.
 */
class UserOfferDraftMigratorTest {

    private val migrator = UserOfferDraftMigrator()

    @Test
    fun migrateDraft() {
        val json = JsonObject().apply {
            addProperty("_TYPE_", "SellApartment")
            add(
                "deal",
                JsonObject().apply {
                    addProperty("dealStatus", "REASSIGNMENT")
                    addProperty("mortgage", true)
                    addProperty("currency", "RUR")
                }
            )
            addProperty("security", true)
        }.toString()

        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject

        assertEquals("SellApartment", migratedJsonObject.get("_TYPE_")?.asString)

        assertEquals(true, migratedJsonObject.get("concierge")?.asBoolean)
        assertNull(migratedJsonObject.get("security"))

        val migratedDeal = migratedJsonObject.get("deal")?.asJsonObject
        assertNotNull(migratedDeal)
        assertEquals("RUR", migratedDeal?.get("currency")?.asString)

        assertEquals("REASSIGNMENT", migratedJsonObject.get("dealStatus")?.asString)
        assertNull(migratedDeal?.get("dealStatus"))

        assertEquals(true, migratedJsonObject.get("mortgage")?.asBoolean)
        assertNull(migratedDeal?.get("mortgage"))
    }

    @Test
    fun migrateDraftWhenNoTargetFields() {
        val json = JsonObject().apply {
            addProperty("_TYPE_", "SellApartment")
            add(
                "deal",
                JsonObject().apply {
                    addProperty("currency", "RUR")
                }
            )
        }.toString()

        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject
        val migratedDeal = migratedJsonObject.get("deal")?.asJsonObject

        assertEquals("SellApartment", migratedJsonObject.get("_TYPE_")?.asString)
        assertNotNull(migratedDeal)
        assertEquals("RUR", migratedDeal?.get("currency")?.asString)
    }
}
