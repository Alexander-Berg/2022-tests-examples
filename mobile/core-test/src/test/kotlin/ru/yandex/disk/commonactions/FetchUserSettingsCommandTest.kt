package ru.yandex.disk.commonactions

import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.hamcrest.Matchers.`is`
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import ru.yandex.disk.DeveloperSettings
import ru.yandex.disk.SortOrder
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.overdraft.OverdraftStateProxy
import ru.yandex.disk.remote.RecentListApi
import ru.yandex.disk.remote.RemoteRepo
import ru.yandex.disk.remote.SettingsFromServer
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.settings.AutoUploadSettings
import ru.yandex.disk.settings.Postponer
import ru.yandex.disk.settings.SessionSettings
import ru.yandex.disk.settings.UserSettings
import ru.yandex.disk.settings.overdraft.OverdraftState
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.toggle.LimitedPhotosToggle
import ru.yandex.disk.toggle.SeparatedAutouploadToggle
import ru.yandex.disk.upload.LimitedPhotosHandler
import ru.yandex.disk.util.UserAgentProvider
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers

class FetchUserSettingsCommandTest : AndroidTestCase2() {

    private lateinit var mockRemoteRepo: RemoteRepo
    private lateinit var userAgentProvider: UserAgentProvider
    private lateinit var userSettings: UserSettings
    private var testScheduler = Schedulers.immediate()

    private lateinit var command: FetchUserSettingsCommand

    override fun setUp() {
        super.setUp()
        mockRemoteRepo = mock(RemoteRepo::class.java)
        val user = TestObjectsFactory.createCredentials()
        `when`(mockRemoteRepo.getSettingsFromServer())
                .thenReturn(Single.just(createSettingsFromServer()))
        `when`(mockRemoteRepo.getLastEvents(anyOrNull(), anyOrNull(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Observable.just(mock(RecentListApi.GroupsResponse::class.java)))

        userAgentProvider = mock(UserAgentProvider::class.java)
        `when`(userAgentProvider.deviceId).thenReturn("deviceId")

        userSettings = TestObjectsFactory.createUserSettings(user.user, TestObjectsFactory.createSettings(mContext))

        val autoUploadSettings = mock(AutoUploadSettings::class.java)
        `when`(autoUploadSettings.shouldStartPushing).thenReturn(false)
        `when`(autoUploadSettings.pushingInProgress).thenReturn(false)

        command = FetchUserSettingsCommand(
            mockRemoteRepo, userAgentProvider, userSettings, mock(DeveloperSettings::class.java),
            OverdraftStateProxy.None,
            EventLogger(), testScheduler,
            SeparatedAutouploadToggle(false),
            mock(CommandStarter::class.java), autoUploadSettings, SessionSettings(),
            mock(LimitedPhotosHandler::class.java), LimitedPhotosToggle(true, false)
        )
    }

    @Test
    fun `should request Camera uploads state`() {
        command.execute(FetchUserSettingsCommandRequest())

        verify(mockRemoteRepo)
                .getFileList(eq("/disk/Camera uploads"),
                        eq(1), eq(null), any())
    }

    @Test
    fun `should set flag after success getting init state for user`() {
        command.execute(FetchUserSettingsCommandRequest())

        assertThat(userSettings.shouldCheckPhotostreamFolder, `is`(false))
    }

    @Test
    fun `should not request Camera uploads if success got it before`() {
        userSettings.shouldCheckPhotostreamFolder = false

        command.execute(FetchUserSettingsCommandRequest())

        verify(mockRemoteRepo, never())
                .getFileList(eq("/disk/Camera uploads"),
                        eq(1), eq(SortOrder.NAME_ASC), any())
    }

    fun `should skip settings requesting if deviceId is not set`() {
        `when`(userAgentProvider.deviceId).thenReturn(null)

        command.execute(FetchUserSettingsCommandRequest())

        verify(mockRemoteRepo, never()).getSettingsFromServer()
    }

    private fun createSettingsFromServer(): SettingsFromServer {
        return SettingsFromServer(
                "/disk/Camera uploads",
                "/disk/Screenshots",
                "/disk/Downloads",
                "/disk/Social networks"
        ).apply {
            overdraftState = OverdraftState.none()
        }
    }
}
