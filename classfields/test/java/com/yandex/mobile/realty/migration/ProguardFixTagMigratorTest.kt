package com.yandex.mobile.realty.migration

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author shpigun on 01/04/2019
 */
class ProguardFixTagMigratorTest {

    private val migrator = ProguardFixTagMigrator()

    @Test
    fun testObfuscatedIncludeTag() {
        val obfuscatedTag = JsonObject().apply {
            addProperty("a", 42)
            addProperty("b", "Tag")
        }
        val jsonObject = JsonObject().apply {
            add("includeSearchTags", JsonArray().apply { add(obfuscatedTag) })
        }
        val json = jsonObject.toString()
        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject

        val migratedIncludeTags = migratedJsonObject.get("includeSearchTags").asJsonArray

        val expectedTag = JsonObject().apply {
            addProperty("id", 42)
            addProperty("title", "Tag")
        }

        assertEquals(1, migratedIncludeTags.size())
        assertEquals(expectedTag, migratedIncludeTags.get(0))
    }

    @Test
    fun testBrokenIncludeTag() {
        val brokenTag = JsonObject().apply { addProperty("id", 0) }
        val jsonObject = JsonObject().apply {
            add("includeSearchTags", JsonArray().apply { add(brokenTag) })
        }
        val json = jsonObject.toString()
        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject

        val migratedIncludeTags = migratedJsonObject.get("includeSearchTags").asJsonArray

        assertEquals(0, migratedIncludeTags.size())
    }

    @Test
    fun testValidIncludeTag() {
        val validTag = JsonObject().apply {
            addProperty("id", 42)
            addProperty("title", "Tag")
        }
        val jsonObject = JsonObject().apply {
            add("includeSearchTags", JsonArray().apply { add(validTag) })
        }
        val json = jsonObject.toString()
        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject

        val migratedIncludeTags = migratedJsonObject.get("includeSearchTags").asJsonArray

        assertEquals(1, migratedIncludeTags.size())
        assertEquals(validTag, migratedIncludeTags.get(0))
    }

    @Test
    fun testEmptyIncludeTag() {
        val jsonObject = JsonObject().apply {
            add("includeSearchTags", JsonArray())
        }
        val json = jsonObject.toString()
        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject

        val migratedIncludeTags = migratedJsonObject.get("includeSearchTags").asJsonArray

        assertEquals(0, migratedIncludeTags.size())
    }

    @Test
    fun testObfuscatedExcludeTag() {
        val obfuscatedTag = JsonObject().apply {
            addProperty("a", 42)
            addProperty("b", "Tag")
        }
        val jsonObject = JsonObject().apply {
            add("excludeSearchTags", JsonArray().apply { add(obfuscatedTag) })
        }
        val json = jsonObject.toString()
        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject

        val migratedExcludeTags = migratedJsonObject.get("excludeSearchTags").asJsonArray

        val expectedTag = JsonObject().apply {
            addProperty("id", 42)
            addProperty("title", "Tag")
        }

        assertEquals(1, migratedExcludeTags.size())
        assertEquals(expectedTag, migratedExcludeTags.get(0))
    }

    @Test
    fun testBrokenExcludeTag() {
        val brokenTag = JsonObject().apply { addProperty("id", 0) }
        val jsonObject = JsonObject().apply {
            add("excludeSearchTags", JsonArray().apply { add(brokenTag) })
        }
        val json = jsonObject.toString()
        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject

        val migratedExcludeTags = migratedJsonObject.get("excludeSearchTags").asJsonArray

        assertEquals(0, migratedExcludeTags.size())
    }

    @Test
    fun testValidExcludeTag() {
        val validTag = JsonObject().apply {
            addProperty("id", 42)
            addProperty("title", "Tag")
        }
        val jsonObject = JsonObject().apply {
            add("excludeSearchTags", JsonArray().apply { add(validTag) })
        }
        val json = jsonObject.toString()
        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject

        val migratedExcludeTags = migratedJsonObject.get("excludeSearchTags").asJsonArray

        assertEquals(1, migratedExcludeTags.size())
        assertEquals(validTag, migratedExcludeTags.get(0))
    }

    @Test
    fun testEmptyExcludeTag() {
        val jsonObject = JsonObject().apply {
            add("excludeSearchTags", JsonArray())
        }
        val json = jsonObject.toString()
        val migratedJson = migrator.migrate(json)
        val migratedJsonObject = JsonParser.parseString(migratedJson).asJsonObject

        val migratedExcludeTags = migratedJsonObject.get("excludeSearchTags").asJsonArray

        assertEquals(0, migratedExcludeTags.size())
    }
}
