package ru.auto.data.network.scala

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author dumchev on 11.10.17.
 */
@RunWith(AllureRunner::class) class ScalaStringTypeAdapterTest {

    private val gson: Gson = GsonBuilder()
            .registerTypeAdapter(String::class.java, ScalaStringTypeAdapter())
            .create()

    @Test
    fun `can parse normal strings, empty string and null`() {
        val nullResult = gson.fromJson(NULL_STRING, String::class.java)
        check(nullResult == null) { "we should parse 'null' as null, instead we have '$nullResult'" }

        val emptyResult = gson.fromJson("\"$EMPTY_STRING\"", String::class.java)
        check(emptyResult == EMPTY_STRING) { "we should parse \"\" as \"\", instead we have '$emptyResult'" }

        val textResult = gson.fromJson("\"$TEXT\"", String::class.java)
        check(textResult == TEXT) { "we should parse $TEXT as $TEXT, bu we have '$textResult'" }
    }

    @Test
    fun `convert empty string to null`() {
        val nullJson: String = gson.toJson(EMPTY_STRING)
        check(nullJson == NULL_STRING) { "we should convert empty string to null, bu we have $nullJson" }
    }

    @Test
    fun `can write non-null texts`() {
        val textJson: String = gson.toJson(TEXT)
        check("\"$TEXT\"" == textJson) { "expected '\"text\"' but was $textJson" }
    }

    companion object {
        private const val NULL_STRING = "null"
        private const val EMPTY_STRING = ""
        private const val TEXT = "text"
    }
}
