package ru.yandex.disk.invites

import android.net.Uri
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.Matchers
import org.junit.Test
import org.mockito.Mockito
import org.robolectric.RuntimeEnvironment
import ru.yandex.disk.Mocks
import ru.yandex.disk.commonactions.SingleWebdavClientPool
import ru.yandex.disk.event.DiskEvents.InvitesRefreshFailed
import ru.yandex.disk.event.DiskEvents.InvitesRefreshSuccess
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.provider.DH
import ru.yandex.disk.provider.DiskContentProvider
import ru.yandex.disk.provider.DiskContract.Invites
import ru.yandex.disk.provider.DiskContract.InvitesCursor
import ru.yandex.disk.provider.InvitesSchemeCreator
import ru.yandex.disk.provider.ProviderTestCase3
import ru.yandex.disk.remote.RemoteRepo
import ru.yandex.disk.remote.exceptions.RemoteExecutionException
import ru.yandex.disk.remote.webdav.WebdavClient
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.CursorTracker
import ru.yandex.disk.test.CursorTrackers
import ru.yandex.disk.test.SeclusiveContext
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.toggle.SeparatedAutouploadToggle
import ru.yandex.disk.util.Diagnostics

class RefreshInvitesListCommandTest : AndroidTestCase2() {

    private val context = SeclusiveContext(RuntimeEnvironment.application).apply {
        setActivityManager(Mocks.mockActivityManager())
    }
    private val db = DH(context).apply {
        addDatabaseOpenListener(InvitesSchemeCreator())
    }
    private val dcp = Mocks.createDiskContentProvider(context, db).apply {
        attachInfo(context, null)
    }
    private val cursorTracker = CursorTracker()
    private val webdav = mock<WebdavClient>()
    private var eventLogger = EventLogger()
    private val diagnostics = mock<Diagnostics>()
    private val remoteRepo = RemoteRepo(
        mock(),
        SingleWebdavClientPool(webdav),
        mock(),
        mock(),
        SeparatedAutouploadToggle(false),
        mock()
    )
    private val client = TestObjectsFactory.createSelfContentProviderClient(context, db)
    private val command = RefreshInvitesListCommand(remoteRepo, eventLogger, diagnostics, client)
    private val baseUri = DiskContentProvider.getUri(context)
    private val invitesUri = Uri.withAppendedPath(baseUri, Invites.INVITES_AUTHORITY)

    override fun setUp() {
        super.setUp()
        CursorTrackers.registerProvider(DiskContentProvider.getAuthority(context), dcp, cursorTracker)
    }

    @Test
    fun testRefreshInvitesNotInvites() {
        whenever(webdav.invites).thenReturn(emptyList())
        command.execute(RefreshInvitesListCommandRequest())
        Mockito.verify(webdav).invites
        assertNotBroadcasts()
        assertInvitesContentChanged()
        assertThat(eventLogger[0], Matchers.instanceOf(InvitesRefreshSuccess::class.java))
    }

    private fun assertInvitesContentChanged() {
        ProviderTestCase3.assertUriNotified(invitesUri, context.contentResolver)
    }

    private fun assertNotBroadcasts() {
        val broadcasts = context.andClearBroadcastIntents
        assertTrue(broadcasts.isEmpty())
    }

    @Test
    fun testRefreshInvitesOnWebdavException() {
        prepareToGetInvitesThrowException()
        command.execute(RefreshInvitesListCommandRequest())
        assertNotBroadcasts()
        assertInvitesContentChanged()
        assertThat(eventLogger[0], Matchers.instanceOf(InvitesRefreshFailed::class.java))
    }

    private fun prepareToGetInvitesThrowException() {
        Mockito.doThrow(RemoteExecutionException("test")).`when`(webdav).invites
    }

    @Test
    fun testRefreshInvites() {
        prepareToReturnOneFileItem()
        command.execute(RefreshInvitesListCommandRequest())
        assertInvitesContentChanged()
        assertOneInviteInContentProvider()
        assertThat(eventLogger[0], Matchers.instanceOf(InvitesRefreshSuccess::class.java))
    }

    @Test
    fun shouldIgnoreIncorrectInvites() {
        val baseInvite = Invite("n", "dn", 1024, true, "on")
        val incorrectInvites = listOf(
            baseInvite.copy(path = null),
            baseInvite.copy(displayName = null),
            baseInvite.copy(ownerName = null),
            baseInvite
        )
        whenever(webdav.invites).thenReturn(incorrectInvites)
        command.execute(RefreshInvitesListCommandRequest())
        Mockito.verify(webdav).invites
        assertInvitesContentChanged()
        assertOneInviteInContentProvider()
        assertThat(eventLogger[0], Matchers.instanceOf(InvitesRefreshSuccess::class.java))
    }

    private fun assertOneInviteInContentProvider() {
        val cr = context.contentResolver
        val invite = InvitesCursor(cr.query(invitesUri, null, null, null, null))
        assertEquals(1, invite.count)
        invite.moveToFirst()
        assertEquals("dn", invite.displayName)
        assertEquals("n", invite.path)
        assertEquals(1024, invite.length)
        assertEquals("on", invite.owner)
        assertEquals(true, invite.isReadonly)
        invite.close()
    }

    private fun prepareToReturnOneFileItem() {
        val fileItem = Invite("n", "dn", 1024, true, "on")
        whenever(webdav.invites).thenReturn(listOf(fileItem))
    }

    @Test
    fun testRefreshInvitesExecuteTwice() {
        prepareToReturnOneFileItem()
        //1
        command.execute(RefreshInvitesListCommandRequest())
        //2
        command.execute(RefreshInvitesListCommandRequest())
    }

    override fun tearDown() {
        cursorTracker.checkState()
        super.tearDown()
    }
}
