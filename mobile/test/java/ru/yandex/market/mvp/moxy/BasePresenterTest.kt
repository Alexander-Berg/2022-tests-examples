package ru.yandex.market.mvp.moxy

import moxy.MvpView
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import org.junit.Test
import ru.yandex.market.base.presentation.core.mvp.presenter.BasePresenter
import ru.yandex.market.base.presentation.core.schedule.PresentationSchedulers
import ru.yandex.market.presentationSchedulersMock

class BasePresenterTest {

    private val schedulers = presentationSchedulersMock()
    private val presenter = TestPresenter(schedulers)

    @Test
    fun `Rescheduling to channel does not dispose new subscription immediately`() {
        val channel = BasePresenter.Channel()
        val value = "abracadabra"
        val subject = BehaviorSubject.createDefault(value)

        presenter.schedule(subject.hide(), channel)
            .assertNoErrors()
            .assertValue(value)

        presenter.schedule(subject.hide(), channel)
            .assertNoErrors()
            .assertValue(value)
    }

    interface TestView : MvpView

    class TestPresenter(schedulers: PresentationSchedulers) : BasePresenter<TestView>(schedulers) {

        fun <T : Any> schedule(observable: Observable<T>, channel: Channel): TestObserver<T> {
            val observer = TestObserver<T>()
            observable.schedule(channel = channel, observer = observer)
            return observer
        }
    }
}