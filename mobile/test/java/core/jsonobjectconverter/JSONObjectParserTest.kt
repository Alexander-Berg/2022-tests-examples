package core.jsonobjectconverter

import android.os.Build
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class JSONObjectParserTest {

    private val rawJsonString = ResourceHelper.getResponse("/json/JsonTestString.json")
    private val jsonObjectFromRawString = Json.decodeFromString(JsonObject.serializer(), rawJsonString)

    @Test
    fun checkStringData() {
        val jsonObjectAfterParse = jsonObjectFromRawString.toJSONObject()
        val jsonStringAfterParse = jsonObjectAfterParse.toString()

        Assert.assertEquals(rawJsonString, jsonStringAfterParse)
    }
}
