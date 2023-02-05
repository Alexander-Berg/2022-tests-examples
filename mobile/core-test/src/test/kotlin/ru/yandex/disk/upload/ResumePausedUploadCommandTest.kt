package ru.yandex.disk.upload

import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ResumePausedUploadCommandTest {

    private val uploadQueue = mock<UploadQueue>()
    private val diskUploader = mock<DiskUploader>()

    private val command = ResumePausedUploadsCommand(uploadQueue, diskUploader)

    @Test
    fun `should resume all paused uploads`() {
        command.execute(ResumePausedUploadsCommandRequest())

        verify(uploadQueue).resumeAllPausedUploads()
    }

    @Test
    fun `should restart upload if something resumed`() {
        whenever(uploadQueue.resumeAllPausedUploads()).thenReturn(true)

        command.execute(ResumePausedUploadsCommandRequest())

        verify(diskUploader).markQueueChanged()
        verify(diskUploader).startUpload()
    }

    @Test
    fun `should not restart upload if nothing resumed `() {
        whenever(uploadQueue.resumeAllPausedUploads()).thenReturn(false)

        command.execute(ResumePausedUploadsCommandRequest())

        verifyNoMoreInteractions(diskUploader)
    }
}
