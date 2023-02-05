package ru.yandex.disk.upload

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Test
import org.robolectric.annotation.Config
import retrofit2.Response
import ru.yandex.disk.commonactions.SingleWebdavClientPool
import ru.yandex.disk.remote.RemoteRepo
import ru.yandex.disk.remote.RestApiClient
import ru.yandex.disk.remote.UnlimApi
import ru.yandex.disk.settings.AutoUploadSettings
import ru.yandex.disk.settings.AutoUploadSettings.UnlimMode.Companion.DISABLED
import ru.yandex.disk.settings.AutoUploadSettings.UploadWhen.Companion.ALWAYS
import ru.yandex.disk.settings.AutoUploadSettings.UploadWhen.Companion.NEVER
import ru.yandex.disk.settings.UserSettings
import ru.yandex.disk.settings.config.CompositeStaticAutoUploadConfig
import ru.yandex.disk.settings.config.StaticAutoUploadConfig
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.toggle.LimitedPhotosToggle
import ru.yandex.disk.toggle.SeparatedAutouploadToggle
import ru.yandex.disk.util.UserAgentProvider
import rx.Single

@Config(manifest = Config.NONE)
class SendAutouploadSettingsCommandTest : AndroidTestCase2() {
    private lateinit var command: SendAutouploadSettingsCommand

    private lateinit var remoteRepo: RemoteRepo
    private lateinit var autoUploadSettings: AutoUploadSettings

    override fun setUp() {
        super.setUp()

        val api = mock<UnlimApi>()
        whenever(api.setUnlimState(any())).thenReturn(Single.just(Response.success(null)))
        whenever(api.setUnlimStates(any())).thenReturn(Single.just(Response.success(null)))
        val restApiClient = mock<RestApiClient>()
        whenever(restApiClient.unlimApi).thenReturn(api)
        remoteRepo = spy(RemoteRepo(mock(),
                SingleWebdavClientPool(mock()), restApiClient,
            mock(), SeparatedAutouploadToggle(false), mock()))

        val userSettings = mock<UserSettings>()
        autoUploadSettings = mock()
        whenever(userSettings.autoUploadSettings).thenReturn(autoUploadSettings)

        val userAgentProvider = mock<UserAgentProvider>()
        whenever(userAgentProvider.deviceId).thenReturn("deviceId")

        command = SendAutouploadSettingsCommand(userSettings, remoteRepo, userAgentProvider,
            SeparatedAutouploadToggle(false), mock(), mock(),
            mock(), LimitedPhotosToggle(enabled = true, disableOnboardingForNewUsers = false))
    }

    @Test
    fun `should photounlim be disabled if user disabled it`() {
        autoUploadSettings.let {
            whenever(it.uploadPhotoWhen).thenReturn(ALWAYS)
            whenever(it.photounlimMode).thenReturn(DISABLED)
            whenever(it.getAutoUploadConfig(any())).thenReturn(
                CompositeStaticAutoUploadConfig(
                    StaticAutoUploadConfig(ALWAYS, DISABLED),
                    StaticAutoUploadConfig(NEVER, DISABLED)
                )
            )
        }

        command.execute(SendAutouploadSettingsCommandRequest())

        verify(remoteRepo).setUnlimAutouploadActivated(false)
    }
}
