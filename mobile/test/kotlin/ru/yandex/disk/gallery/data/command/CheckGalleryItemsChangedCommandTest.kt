package ru.yandex.disk.gallery.data.command

import org.mockito.kotlin.*
import org.junit.Test
import ru.yandex.disk.gallery.data.database.GalleryChangedNotifier
import ru.yandex.disk.photoslice.MomentsDatabase

private const val MOCK_ETAG = "testEtag"

class CheckGalleryItemsChangedCommandTest {

    private val momentsDatabase = mock<MomentsDatabase>()
    private val galleryNotifier = mock<GalleryChangedNotifier>()
    private val command = CheckGalleryItemsChangedCommand(galleryNotifier, momentsDatabase)
    private val request = CheckGalleryItemsChangedCommandRequest(listOf(MOCK_ETAG))

    private val listMatcher = argThat<MutableList<String>> {
        size == 1 && get(0) == (MOCK_ETAG)
    }

    @Test
    fun `should notify gallery changed if etags presented in moments db`() {
        whenever(momentsDatabase.containsItems(listMatcher)).thenReturn(true)

        command.execute(request)

        verify(galleryNotifier).forceNotify()
    }


    @Test
    fun `should not notify gallery changed if etags not presented int moments db`() {
        whenever(momentsDatabase.containsItems(listMatcher)).thenReturn(false)

        command.execute(request)

        verifyNoMoreInteractions(galleryNotifier)
    }
}
