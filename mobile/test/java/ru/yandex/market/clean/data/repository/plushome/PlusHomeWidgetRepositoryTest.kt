package ru.yandex.market.clean.data.repository.plushome

import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.store.plushome.PlusHomeWidgetInteractionDataStore

class PlusHomeWidgetRepositoryTest {

    private val widgetInteractionsSubject: PublishSubject<List<String>> = PublishSubject.create()
    private val plusHomeWidgetInteractionDataStore = mock<PlusHomeWidgetInteractionDataStore> {
        on { observeInteractedWidgetsTags() } doReturn widgetInteractionsSubject
    }

    private val repository = PlusHomeWidgetRepository(plusHomeWidgetInteractionDataStore)

    @Test
    fun `test observe interacted widget`() {
        val testObserver = repository.observeInteractedWidgetsTags().test()

        val expectedValues = arrayOf(
            listOf("widget1"),
            listOf("widget1, widget2"),
            listOf("widget3, widget4, widget5")
        )
        widgetInteractionsSubject.apply {
            onNext(expectedValues[0])
            onNext(expectedValues[1])
            onNext(expectedValues[2])
        }

        testObserver
            .assertValueCount(3)
            .assertValueAt(0, expectedValues[0])
            .assertValueAt(1, expectedValues[1])
            .assertValueAt(2, expectedValues[2])
    }

    @Test
    fun `save interacted widget`() {
        val alreadyInteracted = listOf("widget1", "widget2")
        whenever(plusHomeWidgetInteractionDataStore.getInteractedWidgetsTags()) doReturn alreadyInteracted

        repository.setWidgetWasInteracted("widget3").test().assertComplete()

        verify(plusHomeWidgetInteractionDataStore).setInteractedWidgetsTags(listOf("widget1", "widget2", "widget3"))

    }

    @Test
    fun `do nothing when trying to save already interacted widget`() {
        val alreadyInteracted = listOf("widget1", "widget2")
        whenever(plusHomeWidgetInteractionDataStore.getInteractedWidgetsTags()) doReturn alreadyInteracted

        repository.setWidgetWasInteracted("widget2").test().assertComplete()

        verify(plusHomeWidgetInteractionDataStore, never()).setInteractedWidgetsTags(any())
    }
}