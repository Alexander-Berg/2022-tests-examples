package ru.yandex.disk.audio

import org.mockito.kotlin.*
import org.junit.Test
import ru.yandex.disk.event.DiskEvents.PlaylistTrackChanged
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.provider.DiskContract
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.provider.DiskFileCursor
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.spaceutils.ByteUnit
import ru.yandex.disk.util.MapCursorWrapper
import ru.yandex.disk.util.assertHasEvent
import ru.yandex.disk.util.assertNoEvent

private val MOCK_SIZE = ByteUnit.MB.toBytes(7)
private const val MOCK_DIR = "/disk/audio"
private const val MOCK_TRACK_1 = "Smells like teen spirit.mp3"
private const val MOCK_TRACK_2 = "We are the champions.mp3"
private const val MOCK_TRACK_3 = "Thunderstruck.mp3"
private val CURRENT_TRACKS = listOf(MOCK_TRACK_1, MOCK_TRACK_2)
private val ALL_TRACKS = listOf(MOCK_TRACK_1, MOCK_TRACK_2, MOCK_TRACK_3)

class StartPlaybackInDirCommandTest : TestCase2() {

    private val playlistManager = mock<PlaylistManager>()
    private val eventLogger = EventLogger()
    private val cursorElements = createCursorElements()
    private val innerCursor = MapCursorWrapper(cursorElements)
    private val diskDatabase = mock<DiskDatabase> {
        on { query(any(), any(), any(), anyOrNull()) } doReturn(DiskFileCursor(innerCursor))
    }
    private val command = StartPlaybackInDirCommand(playlistManager, eventLogger, diskDatabase)

    @Test
    fun `should create new playlist`() {
        whenever(playlistManager.current).thenReturn(null)

        command.execute(getRequest())

        verify(playlistManager).setPlaylist(any())
        eventLogger.assertHasEvent<PlaylistTrackChanged>()
    }


    @Test
    fun `should change track`() {
        setupAlreadyPlayFirstTrack()

        command.execute(getRequest(MOCK_TRACK_2))

        verify(playlistManager).setPlaylist(argForWhich { position == 1})
        eventLogger.assertHasEvent<PlaylistTrackChanged>()
    }

    @Test
    fun `should not notify if track is not changed`() {
        setupAlreadyPlayFirstTrack()

        command.execute(getRequest())

        eventLogger.assertNoEvent<PlaylistTrackChanged>()
    }

    @Test
    fun `should re init playlist if unknown track`() {
        setupAlreadyPlayFirstTrack()

        command.execute(getRequest(MOCK_TRACK_3))

        verify(playlistManager).setPlaylist(argForWhich { position == 2})
        eventLogger.assertHasEvent<PlaylistTrackChanged>()
    }

    @Test
    fun `should play next if incorrect track`() {
        setupAlreadyPlayFirstTrack()

        command.execute(getRequest("WrongTrack"))

        verify(playlistManager).setPlaylist(argForWhich { position == 1})
    }

    @Test
    fun `should release manager if nothing to play`() {
        whenever(playlistManager.current).thenReturn(null)

        command.execute(getRequest("WrongTrack"))

        verify(playlistManager).release()
    }

    private fun setupAlreadyPlayFirstTrack() {
        val tracks = CURRENT_TRACKS.map { Track(MOCK_DIR, it, MOCK_SIZE) }
        val playlist = PlaylistManager.Playlist(MOCK_DIR, null, tracks, setOf(MOCK_DIR), 0)
        whenever(playlistManager.current).thenReturn(playlist)
    }

    private fun getRequest(trackName: String = MOCK_TRACK_1) =
            StartPlaybackInDirCommandRequest(MOCK_DIR, trackName, null, false)

    private fun createCursorElements() = ALL_TRACKS
            .map { createMapForTrack(it) }
            .toTypedArray()

    private fun createMapForTrack(trackName : String) : Map<String, Any?> =
            mapOf(DiskContract.DiskFile.PARENT to MOCK_DIR,
                    DiskContract.DiskFile.DISPLAY_NAME to trackName,
                    DiskContract.DiskFile.SIZE to MOCK_SIZE)
}
