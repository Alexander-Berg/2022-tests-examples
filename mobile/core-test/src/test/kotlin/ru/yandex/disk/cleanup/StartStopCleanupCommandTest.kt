package ru.yandex.disk.cleanup

import org.mockito.kotlin.whenever
import org.junit.Test
import org.mockito.AdditionalMatchers.not
import org.mockito.kotlin.any
import org.mockito.Mockito.*
import ru.yandex.disk.cleanup.command.*
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventSender
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.settings.ApplicationSettings
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.disk.util.SystemClock

class StartStopCleanupCommandTest : AndroidTestCase2() {

    private val commandStarter: CommandStarter = mock(CommandStarter::class.java)
    private val uploadQueue: UploadQueue = mock(UploadQueue::class.java)
    private val cleanupPolicy = CleanupPolicy(SystemClock.REAL)
    private val appSettings = mock(ApplicationSettings::class.java)


    @Test
    fun `should start all cleanup`() {
        val startCommand = StartCleanupCommand(uploadQueue, cleanupPolicy, commandStarter)
        startCommand.execute(StartCleanupCommandRequest(false))
        verify(uploadQueue).markCheckingCleanupAsDefault()
        verify(uploadQueue).markDefaultCleanupAsChecking(eq(cleanupPolicy.all()))
        verify(commandStarter).start(any<CleanupLocalFilesCommandRequest>())
    }

    @Test
    fun `should start old cleanup`() {
        val startCommand = StartCleanupCommand(uploadQueue, cleanupPolicy, commandStarter)
        startCommand.execute(StartCleanupCommandRequest(true))
        verify(uploadQueue).markCheckingCleanupAsDefault()
        verify(uploadQueue).markDefaultCleanupAsChecking(not(eq(cleanupPolicy.all())))
        verify(commandStarter).start(any<CleanupLocalFilesCommandRequest>())
    }

    @Test
    fun `should stop cleanup`() {
        val eventSender = mock(EventSender::class.java)
        val stopCommand = StopCleanupCommand(uploadQueue, eventSender, appSettings)
        stopCommand.execute(StopCleanupCommandRequest(true))
        verify(uploadQueue).markCheckingCleanupAsDefault()
        verify(appSettings).isCleanupInProgress = eq(false)
        verify(eventSender).send(any<DiskEvents.CleanupForceStoppedEvent>())
    }

    @Test
    fun `should not resume cleanup`() {
        val resumeCommand = ResumeCleanupCommand(appSettings, commandStarter)
        whenever(appSettings.isCleanupInProgress).thenReturn(false)
        resumeCommand.execute(ResumeCleanupCommandRequest())
        verify(commandStarter, never()).start(any<CleanupLocalFilesCommandRequest>())
    }

    @Test
    fun `should resume cleanup`() {
        val resumeCommand = ResumeCleanupCommand(appSettings, commandStarter)
        whenever(appSettings.isCleanupInProgress).thenReturn(true)
        whenever(uploadQueue.queryCleanupFilesCount()).thenReturn(10)
        resumeCommand.execute(ResumeCleanupCommandRequest())
        verify(commandStarter).start(any<CleanupLocalFilesCommandRequest>())
    }
}
