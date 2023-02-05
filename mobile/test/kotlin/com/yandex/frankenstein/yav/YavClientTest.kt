package com.yandex.frankenstein.yav

import com.yandex.frankenstein.utils.NetworkHelper
import com.yandex.frankenstein.utils.authorization.RsaSigner
import com.yandex.frankenstein.yav.info.YavInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class YavClientTest {

    private val yavInfo = YavInfo(
        baseUrl = "https://base/url",
        apiVersion = "1",
        oauthToken = "token",
        username = "username",
        secretVersion = "version"
    )
    val version = "version_1"
    private val secretsResponce = """
{
  "secrets": [
    {
      "last_secret_version": {
        "version": "version 1"
      },
      "name": "test secret 1",
    },
    {
      "last_secret_version": {
        "version": "version 2"
      },
      "name": "test secret 2"
    }
  ],
  "status": "ok"
}
"""
    private val versionResponce = """
{
  "status": "ok",
  "version": {
    "value": [
      {
        "key": "key1",
        "value": "value1"
      },
      {
        "key": "key2",
        "value": "value2"
      }
    ],
    "version": "$version"
  }
}
"""

    private val networkHelper = mock(NetworkHelper::class.java)
    private val rsaSigner = mock(RsaSigner::class.java)

    @Test
    fun testGetSecrets() {
        `when`(
            networkHelper.get(
                eq("${yavInfo.baseUrl}/${yavInfo.apiVersion}/secrets/?") ?: "",
                any() ?: emptyMap(),
                any() ?: emptyMap()
            )
        ).thenReturn(secretsResponce)
        val yavClient = YavClient(networkHelper, yavInfo, rsaSigner)

        assertThat(yavClient.getSecrets())
            .extracting("name")
            .containsExactly("test secret 1", "test secret 2")
    }

    @Test
    fun testGetSecretFieldsByVersion() {
        `when`(
            networkHelper.get(
                eq("${yavInfo.baseUrl}/${yavInfo.apiVersion}/versions/$version/?") ?: "",
                any() ?: emptyMap(),
                any() ?: emptyMap()
            )
        ).thenReturn(versionResponce)
        val yavClient = YavClient(networkHelper, yavInfo, rsaSigner)

        assertThat(yavClient.getSecretFieldsByVersion(version))
            .containsExactly(
                mapOf(
                    "key" to "key1",
                    "value" to "value1"
                ),
                mapOf(
                    "key" to "key2",
                    "value" to "value2"
                )
            )
    }
}
