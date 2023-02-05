package ru.yandex.market.clean.domain.usecase.plushome

import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.repository.plushome.PlusHomeWidgetRepository

class GetPlusHomeWidgetWasInteractedUseCaseTest {

    private val interactionsSubject: PublishSubject<List<String>> = PublishSubject.create()
    private val pusHomeWidgetRepository = mock<PlusHomeWidgetRepository> {
        on { observeInteractedWidgetsTags() } doReturn interactionsSubject
    }

    private val useCase = GetPlusHomeWidgetWasInteractedUseCase(pusHomeWidgetRepository)

    @Test
    fun `observe interactions change`() {
        val testObserver = useCase.execute("widget1").test()

        interactionsSubject.onNext(listOf())
        interactionsSubject.onNext(listOf("widget2"))
        interactionsSubject.onNext(listOf("widget1", "widget2"))

        testObserver.assertValues(false, false, true)
    }
}