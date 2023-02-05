package com.yandex.frankenstein.yav

import com.yandex.frankenstein.yav.info.YavInfo
import com.yandex.frankenstein.yav.info.YavKeysInfo
import com.yandex.frankenstein.yav.plugin.FrankensteinYavPluginExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.provider.Property
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class YavSecretsHolderFacadeTest {

    private val password = "password"
    private val yavInfo = YavInfo()
    private val yavKeysInfo = YavKeysInfo()
    private val yavSecretsHolder = mock(YavSecretsHolder::class.java)

    private val enableYavProperty = mock(Property::class.java)
    private val yavInfoProperty = mock(Property::class.java)
    private val yavKeysInfoProperty = mock(Property::class.java)
    private val frankensteinYavPluginExtension = mock(FrankensteinYavPluginExtension::class.java)

    @Before
    fun setUp() {
        `when`(yavInfoProperty.get()).thenReturn(yavInfo)
        `when`(yavKeysInfoProperty.get()).thenReturn(yavKeysInfo)
        `when`(frankensteinYavPluginExtension.enableYav).thenReturn(enableYavProperty as Property<Boolean>)
        `when`(frankensteinYavPluginExtension.yavInfo).thenReturn(yavInfoProperty as Property<YavInfo>)
        `when`(frankensteinYavPluginExtension.yavKeysInfo).thenReturn(yavKeysInfoProperty as Property<YavKeysInfo>)
    }

    @Test
    fun testGetPropertyIfEnabledIfEnabled() {
        val property = "some_property"
        `when`(yavSecretsHolder.getSecretProperty(yavInfo.secretVersion, property)).thenReturn(password)
        `when`(enableYavProperty.get()).thenReturn(true)
        val yavSecretsHolderFacade = YavSecretsHolderFacade(
            yavExtension = frankensteinYavPluginExtension,
            yavSecretsHolder = yavSecretsHolder
        )

        assertThat(yavSecretsHolderFacade.getPropertyIfEnabled(property)).isEqualTo(password)
    }

    @Test
    fun testGetPropertyIfEnabledIfDisabled() {
        val property = "some_property"
        `when`(yavSecretsHolder.getSecretProperty(yavInfo.secretVersion, property)).thenReturn(password)
        `when`(enableYavProperty.get()).thenReturn(false)
        val yavSecretsHolderFacade = YavSecretsHolderFacade(
            yavExtension = frankensteinYavPluginExtension,
            yavSecretsHolder = yavSecretsHolder
        )

        assertThat(yavSecretsHolderFacade.getPropertyIfEnabled(property)).isEmpty()
    }
}
