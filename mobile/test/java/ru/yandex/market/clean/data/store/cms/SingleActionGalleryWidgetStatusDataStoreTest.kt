package ru.yandex.market.clean.data.store.cms

import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.common.schedulers.WorkerScheduler

class SingleActionGalleryWidgetStatusDataStoreTest {

    private val scheduler = Schedulers.trampoline()
    private val workerScheduler = mock<WorkerScheduler> {
        on { scheduler } doReturn scheduler
    }
    private val dataStore = SingleActionGalleryWidgetStatusDataStore(workerScheduler)

    @Test
    fun `Invalidate widget notify subscribers`() {
        val widgetId = "widgetId"
        val testSubscription = dataStore.getDataReloadRequestStream(widgetId).test()
        dataStore.setDataNotActual(widgetId).test().assertComplete()

        testSubscription
            .assertNotComplete()
            .assertNoErrors()
            .assertValueCount(1)
    }

    @Test
    fun `Invalidate different widget not notify subscribers`() {
        val testSubscription = dataStore.getDataReloadRequestStream("widget 1").test()
        dataStore.setDataNotActual("widget 2").test().assertComplete()

        testSubscription
            .assertNotComplete()
            .assertNoErrors()
            .assertNoValues()
    }
}