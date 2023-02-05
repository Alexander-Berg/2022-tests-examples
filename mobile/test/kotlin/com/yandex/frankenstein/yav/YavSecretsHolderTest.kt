package com.yandex.frankenstein.yav

import groovy.json.JsonOutput
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File
import java.nio.file.Files

class YavSecretsHolderTest {
    private val secretVersion = "secret_version"

    private val keyInFile = "key_in_file"
    private val valueInFile = "value_in_file"

    private val keyDownloaded = "key_downloaded"
    private val valueDownloaded = "value_downloaded"

    private val keyNotExists = "key_not_exists"

    private val storage = Files.createTempDirectory("tmp").toFile()
    private val yavClient = mock(YavClient::class.java)
    private val yavSecretsHolder = YavSecretsHolder(storage, yavClient)

    @Test
    fun testGetExistingSecretPropertyFromFile() {
        createSecretFile()
        val secret = yavSecretsHolder.getSecretProperty(secretVersion, keyInFile)

        assertThat(secret).isEqualTo(valueInFile)
    }

    @Test
    fun testGetNotExistingSecretPropertyFromFile() {
        createSecretFile()
        val secret = yavSecretsHolder.getSecretProperty(secretVersion, keyNotExists)

        assertThat(secret).isNull()
    }

    @Test
    fun testGetExistingSecretPropertyWithoutFile() {
        `when`(yavClient.getSecretFieldsByVersion(eq(secretVersion) ?: "")).thenReturn(
            listOf(
                mapOf(
                    "key" to keyDownloaded,
                    "value" to valueDownloaded,
                )
            )
        )
        val secret = yavSecretsHolder.getSecretProperty(secretVersion, keyDownloaded)

        assertThat(secret).isEqualTo(valueDownloaded)
    }

    @Test
    fun testGetNotExistingSecretPropertyWithoutFile() {
        `when`(yavClient.getSecretFieldsByVersion(eq(secretVersion) ?: "")).thenReturn(
            listOf(
                mapOf(
                    "key" to keyDownloaded,
                    "value" to valueDownloaded,
                )
            )
        )
        val secret = yavSecretsHolder.getSecretProperty(secretVersion, keyNotExists)

        assertThat(secret).isNull()
    }

    private fun createSecretFile() {
        val filename = "$secretVersion.json"
        val secretFile = File(storage, filename)

        val props = mapOf(
            keyInFile to valueInFile
        )

        secretFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(props)))
    }
}
