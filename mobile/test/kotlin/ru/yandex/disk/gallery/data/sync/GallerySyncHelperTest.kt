package ru.yandex.disk.gallery.data.sync

import org.mockito.kotlin.*
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import ru.yandex.disk.gallery.data.database.GalleryDao
import ru.yandex.disk.gallery.data.database.GalleryDataProvider
import ru.yandex.disk.provider.DatabaseTransactions
import ru.yandex.disk.test.Assert2.assertThat

class GallerySyncHelperTest {

    private val handler = GallerySyncerHelper.Handler()
    private val databaseTransactions = mock<DatabaseTransactions>()
    private val dataProvider = mock<GalleryDataProvider>()
    private val itemsProcessor = mock<ItemsSyncerProcessor<Unit>>()
    private val dao = mock<GalleryDao>()
    private val headersProcessor = GalleryHeadersProcessor(dataProvider, dao)
    private val helper = GallerySyncerHelper(
        handler, databaseTransactions, dataProvider, itemsProcessor, headersProcessor,
        Unit, "")

    @Test
    fun `should return false if items processor not start`() {
        setupItemsProcessorError()

        val result = helper.sync()

        assertThat(result, equalTo(false))
    }

    @Test
    fun `should not sync if items processor not start`() {
        reset(dataProvider)
        setupItemsProcessorError()

        helper.sync()

        verifyNoMoreInteractions(dataProvider)
    }

    private fun setupItemsProcessorError() {
        whenever(itemsProcessor.start(any())).thenReturn(false)
    }
}
