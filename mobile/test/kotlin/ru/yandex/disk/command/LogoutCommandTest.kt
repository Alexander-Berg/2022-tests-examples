package ru.yandex.disk.command

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import com.bumptech.glide.Glide
import org.mockito.kotlin.*
import com.yandex.mail360.purchase.InApp360Controller
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.*
import ru.yandex.disk.asyncbitmap.LegacyPreviewsDatabase
import ru.yandex.disk.audio.DownloadProgressBus
import ru.yandex.disk.audio.PlayerProgressBus
import ru.yandex.disk.cleanup.command.CheckForCleanupCommandRequest
import ru.yandex.disk.download.DownloadQueue
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.feed.FeedDatabase
import ru.yandex.disk.feedback.form.FeedbackFormRepository
import ru.yandex.disk.gallery.data.database.PreviewsDaoInternal
import ru.yandex.disk.gallery.data.database.PreviewsDatabase
import ru.yandex.disk.imports.ImportingFilesStorage
import ru.yandex.disk.monitoring.MediaMonitor
import ru.yandex.disk.notifications.PushRegistrator
import ru.yandex.disk.offline.IndexDatabase
import ru.yandex.disk.offline.OfflineProgressNotificator
import ru.yandex.disk.operation.OperationLists
import ru.yandex.disk.photoslice.MomentsDatabase
import ru.yandex.disk.plugin.SessionClearable
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.remote.webdav.WebdavClient
import ru.yandex.disk.service.CommandScheduler
import ru.yandex.disk.settings.ApplicationSettings
import ru.yandex.disk.sync.SyncManagersRegistry
import ru.yandex.disk.telemost.Telemost
import ru.yandex.disk.test.DiskMatchers.argOfClass
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.trash.TrashDatabase
import ru.yandex.disk.ui.SortOrderPolicy
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.disk.util.assertHasEvent
import rx.observers.TestSubscriber
import kotlin.time.ExperimentalTime

private const val user = "User"

@ExperimentalTime
@Config(manifest = Config.NONE)
class LogoutCommandTest : TestCase2() {

    private val context = mock<Context>()
    private val application = MockedApplication()
    private val credentials = mock<Credentials> {
        on { user } doReturn (user)
    }
    private val syncManagersRegistry = mock<SyncManagersRegistry>()
    private val operationLists = mock<OperationLists>()
    private val offlineProgressNotificator = mock<OfflineProgressNotificator>()
    private val storage = mock<Storage>()
    private val importingFilesStorage = mock<ImportingFilesStorage>()
    private val downloadQueue = mock<DownloadQueue>()
    private val indexDatabase = mock<IndexDatabase>()
    private val sortOrderPolicy = mock<SortOrderPolicy>()
    private val trashDatabase = mock<TrashDatabase>()
    private val momentsDatabase = mock<MomentsDatabase>()
    private val loginPreferencesEditor = mock<SharedPreferences.Editor> {
        on { clear() }.doReturn(it)
    }
    private val loginPreferences = mock<SharedPreferences> {
        on { edit() }.doReturn(loginPreferencesEditor)
    }
    private val legacyPreviewsDatabase = mock<LegacyPreviewsDatabase>()
    private val diskDatabase = mock<DiskDatabase>()
    private val credentialsManager = mock<CredentialsManager>()
    private val eventLogger = EventLogger()
    private val notificationManager = mock<NotificationManager>()
    private val pushRegistrator = mock<PushRegistrator>()
    private val feedDatabase = mock<FeedDatabase>()
    private val applicationSettings = mock<ApplicationSettings>()
    private val uploadQueue = mock<UploadQueue>()
    private val webdavClient = mock<WebdavClient.Pool>()
    private val clearable = mock<SessionClearable>()
    private val clearableSet = mutableListOf(clearable)
    private val commandScheduler = mock<CommandScheduler>()
    private val glide = mock<Glide>()
    private val mediaMonitor = mock<MediaMonitor>()
    private val internalPreviewsDao = mock<PreviewsDaoInternal>()
    private val previewsDatabase = PreviewsDatabase(mock(), mock(), internalPreviewsDao)
    private val feedbackFormRepository = mock<FeedbackFormRepository>()
    private val purchaseController = mock<InApp360Controller>()
    private val telemost = mock<Telemost>()
    private val command = LogoutCommand(context, application, credentials, syncManagersRegistry,
            credentialsManager, eventLogger, operationLists, storage, importingFilesStorage,
            downloadQueue, offlineProgressNotificator, indexDatabase, sortOrderPolicy,
            trashDatabase, momentsDatabase, loginPreferences, legacyPreviewsDatabase,
            diskDatabase, notificationManager, pushRegistrator, feedDatabase,
            applicationSettings, uploadQueue, webdavClient, clearableSet,
            commandScheduler, glide, mock(), mediaMonitor, previewsDatabase, feedbackFormRepository,
            mock(), mock(), mock(), purchaseController, mock(), mock(), telemost)

    @Test
    fun shouldLogout() {
        val downloadMonitor = TestSubscriber<Int>()
        val playerMonitor = TestSubscriber<Int>()
        DownloadProgressBus.get().observable().subscribe(downloadMonitor)
        PlayerProgressBus.get().observable().subscribe(playerMonitor)

        command.execute(LogoutCommandRequest())

        verify(mediaMonitor).stop()
        verify(syncManagersRegistry).updateAccountsSyncableState(eq(false))
        verify(pushRegistrator).requestUnregistration()
        verify(pushRegistrator).refreshToken()
        verify(purchaseController).terminate()
        verify(credentialsManager).resetActiveAccountCredentials()
        eventLogger.assertHasEvent<DiskEvents.UserLoggedOut>()
        verify(glide).clearMemory()
        verify(notificationManager).cancelAll()
        verify(commandScheduler).cancel(argOfClass(CheckForCleanupCommandRequest::class.java))
        verify(storage).dropUserCachedFiles()
        verify(importingFilesStorage).deleteAllFiles()
        verify(downloadQueue).clearUnfinishedItems()
        verify(offlineProgressNotificator).clearSessionData()
        verify(operationLists).clear()
        verify(loginPreferencesEditor).clear()
        verify(loginPreferencesEditor).apply()
        verify(webdavClient).reset()

        verify(uploadQueue).removeNonAutouploads()
        verify(uploadQueue).markCheckingCleanupAsDefault()
        verify(indexDatabase).deleteAll()
        verify(diskDatabase).clear()
        verify(trashDatabase).clear()
        verify(momentsDatabase).clear()
        verify(legacyPreviewsDatabase).clear()
        verify(feedDatabase).clear()
        verify(internalPreviewsDao).clear()

        assertThat(downloadMonitor.completions, equalTo(1))
        assertThat(playerMonitor.completions, equalTo(1))
        verify(sortOrderPolicy).resetSortToDefault()
        verify(applicationSettings).resetUserSettings()
        assertThat(application.userCompotentReseted, equalTo(true))
        verify(clearable).clear()
    }
}

private class MockedApplication : DiskApplication() {
    var userCompotentReseted = false

    override fun resetUserComponent() {
        userCompotentReseted = true
    }
}
