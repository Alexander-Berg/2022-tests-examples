package ru.yandex.disk.provider

import org.mockito.kotlin.*
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.CredentialsManager
import ru.yandex.disk.autoupload.AutouploadCheckDebouncer
import ru.yandex.disk.autoupload.CheckAndStartAutouploadCommandRequest
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.TestObjectsFactory

private const val MOCK_USER_NAME = "testUser"
private const val MOCK_SCOPED_USER = "user@$MOCK_USER_NAME"

@Config(manifest = Config.NONE)
class AutouploadSettingListenerTest : AndroidTestCase2() {

    private val credentialsManager = mock<CredentialsManager> {
        on { activeAccountCredentials } doReturn TestObjectsFactory.createCredentials(MOCK_USER_NAME)
    }
    private val commandStarter = mock<AutouploadCheckDebouncer>()
    private val listener = AutouploadSettingListener(commandStarter, credentialsManager)

    @Test
    fun `should check autoupload if changed for active user`() {
        listener.onChange(DiskContract.Settings.UPLOAD_WHEN_LEGACY, "1", MOCK_SCOPED_USER)

        verify(commandStarter).requestCheck()
    }

    @Test
    fun `should not start autoupload check if changed for wrong user`() {
        listener.onChange(DiskContract.Settings.UPLOAD_WHEN_LEGACY, "1", "user@another_user")

        verifyNoMoreInteractions(commandStarter)
    }

    @Test
    fun `should not start autoupload check if changed another setting`() {
        listener.onChange(DiskContract.Settings.SETTINGS_SET_MANUALLY, "1", MOCK_SCOPED_USER)
        verifyNoMoreInteractions(commandStarter)
    }

    @Test
    fun `should not start crash on null input`() {
        whenever(credentialsManager.activeAccountCredentials).thenReturn(null)
        listener.onChange(null, null, null)
        verifyNoMoreInteractions(commandStarter)
    }
}
