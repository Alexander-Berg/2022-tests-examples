package ru.yandex.yandexmaps.tools.testpalm.impl

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Config(
    val testPalmHost: String = "https://testpalm-api.yandex-team.ru",
    val projectId: String,
    val testCasesDir: String,
)

object ConfigLoader {
    private val name = "TESTPALM_CONFIG"

    @Throws(IllegalArgumentException::class)
    fun loadConfig(): Config {
        val path = System.getenv(name) ?: throw IllegalArgumentException("Specify path to testpalm config file in environmental variable $name")
        val file = File(path)
        return Json { encodeDefaults = false; ignoreUnknownKeys = true }.decodeFromString<Config>(Config.serializer(), file.readText())
    }
}
