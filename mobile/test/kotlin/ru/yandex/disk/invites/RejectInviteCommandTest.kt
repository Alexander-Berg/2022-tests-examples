package ru.yandex.disk.invites

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import org.mockito.kotlin.*
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventSender
import ru.yandex.disk.remote.RemoteRepo
import ru.yandex.disk.remote.webdav.WebdavClient
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.test.AndroidTestCase2

@Config(manifest = Config.NONE)
class RejectInviteCommandTest : AndroidTestCase2() {
    private val invite = mock<Uri>()
    private val cursor = mock<Cursor> {
        on { moveToPosition(0) } doReturn true
        on { moveToFirst() } doReturn true
    }

    private val contentResolver = mock<ContentResolver> {
        on { query(invite, null, null, null, null) } doReturn cursor
    }
    private val commandStarter = mock<CommandStarter>()
    private val eventSender = mock<EventSender>()
    private val remoteRepo = mock<RemoteRepo>()
    private val command = RejectInviteCommand(contentResolver, commandStarter, eventSender, remoteRepo)

    @Test
    fun `should reject invites`() {
        command.execute(RejectInviteCommandRequest(invite))

        verify(remoteRepo).rejectInvitation(anyOrNull())
        verify(commandStarter).start(any<RefreshInvitesListCommandRequest>())
        verify(eventSender).send(any<DiskEvents.InviteRejectingSucceeded>())
        verify(eventSender).send(any<DiskEvents.InviteRejectingFinished>())
    }
}
