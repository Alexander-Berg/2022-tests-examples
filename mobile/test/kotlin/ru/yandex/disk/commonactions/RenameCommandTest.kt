package ru.yandex.disk.commonactions

import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.gallery.data.command.CheckGalleryItemsChangedCommandRequest
import ru.yandex.disk.provider.DiskDatabaseMethodTest
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.disk.util.MediaTypes
import ru.yandex.util.Path.asPath

@Config(manifest = Config.NONE)
class RenameCommandTest : DiskDatabaseMethodTest() {

    private val uploadQueue = mock<UploadQueue>()

    private val commandStarter = mock<CommandStarter>()
    private val renameDirRequest = RenameCommandRequest(
            DiskItemBuilder().setPath("/disk/A").setIsDir(true).build(), "B")

    private lateinit var command: RenameCommand

    override fun setUp() {
        super.setUp()
        command = RenameCommand(mock(), mock(), diskDb, mock(), mock(), mock(), uploadQueue,
                commandStarter)
    }

    @Test
    fun shouldCleanUpUploadQueue() {
        command.execute(renameDirRequest)

        verify(uploadQueue).deleteUploadsByDestDir(asPath("/disk/A")!!)
    }

    @Test
    fun `should not start gallery check for dir item`() {
        command.execute(renameDirRequest)

        verify(commandStarter, times(0)).start(argThat {
            this is CheckGalleryItemsChangedCommandRequest
        })
    }

    @Test
    fun `should not start gallery check for not media item`() {
        val renameDocumentCommand = RenameCommandRequest(
                DiskItemBuilder().setPath("/disk/A").setMediaType(MediaTypes.DOCUMENT).build(), "B")

        command.execute(renameDocumentCommand)

        verify(commandStarter, times(0)).start(argThat {
            this is CheckGalleryItemsChangedCommandRequest
        })
    }

    @Test
    fun `should start gallery check for media item with etag`() {
        val renameDocumentCommand = RenameCommandRequest(
                DiskItemBuilder().setPath("/disk/A").setMediaType(MediaTypes.IMAGE)
                        .setEtag("testEtag").build(), "B")

        command.execute(renameDocumentCommand)

        verify(commandStarter).start(argThat {
            this is CheckGalleryItemsChangedCommandRequest
        })
    }
}
