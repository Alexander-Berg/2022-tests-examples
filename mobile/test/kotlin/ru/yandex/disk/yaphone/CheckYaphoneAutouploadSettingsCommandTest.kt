package ru.yandex.disk.yaphone

import org.mockito.kotlin.*
import org.junit.Test
import ru.yandex.disk.settings.markers.YandexPhoneAutouploadSettings
import rx.Single

class CheckYaphoneAutouploadSettingsCommandTest {

    private val yaphoneAutouploadMarkers = mock<YandexPhoneAutouploadSettings>()
    private val yandexPhoneSettings = mock<YandexPhoneSettings>()

    private val command = CheckYandexPhoneAutouploadSettingsCommand(yaphoneAutouploadMarkers, yandexPhoneSettings)
    private val request = CheckYandexPhoneAutouploadSettingsCommandRequest()

    @Test
    fun `should mark autoupload setting fetched`() {
        whenever(yaphoneAutouploadMarkers.shouldFetchYandexPhoneAutouploadSetting()).thenReturn(true)
        whenever(yandexPhoneSettings.apply()).thenReturn(Single.just(true))

        command.execute(request)

        verify(yaphoneAutouploadMarkers).markYandexPhoneAutouploadSettingFetched()
    }

    @Test
    fun `should not mark autoupload setting if already fetched`() {
        whenever(yaphoneAutouploadMarkers.shouldFetchYandexPhoneAutouploadSetting()).thenReturn(false)

        command.execute(request)

        verifyNoMoreInteractions(yandexPhoneSettings)
        verify(yaphoneAutouploadMarkers, never()).markYandexPhoneAutouploadSettingFetched()
    }
}
