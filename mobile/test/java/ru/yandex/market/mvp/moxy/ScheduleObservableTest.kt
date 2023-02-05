package ru.yandex.market.mvp.moxy

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.base.presentation.core.schedule.DisposableManager

open class ScheduleObservableTest : ScheduleTestCases<Observable<String>, TestObserver<String>, Disposable>() {

    override fun getInstanceFromCallable(callable: () -> String) = Observable.fromCallable(callable)

    override fun getInstance() = Observable.just("")

    override fun getCompleteInstance(): Observable<String> = Observable.empty()

    override fun getErrorInstance() = Observable.error<String>(RuntimeException())

    override fun callSchedule(
        rxType: Observable<String>,
        observeOnScheduler: Scheduler,
        subscribeOnScheduler: Scheduler,
        disposableManager: DisposableManager<String>,
        channel: String?,
        observer: TestObserver<String>,
        doFinally: (() -> Unit)?
    ) {
        ru.yandex.market.base.presentation.core.schedule.schedule(
            observable = rxType,
            observeOnScheduler = observeOnScheduler,
            subscribeOnScheduler = subscribeOnScheduler,
            disposableManager = disposableManager,
            channel = channel,
            observer = observer,
            doFinally = doFinally
        )
    }

    override fun callSchedule(
        rxType: Observable<String>,
        observeOnScheduler: Scheduler,
        subscribeOnScheduler: Scheduler,
        disposableManager: DisposableManager<String>,
        channel: String?,
        onError: (Throwable) -> Unit,
        onSubscribe: (Disposable) -> Unit,
        doFinally: (() -> Unit)?
    ) {
        ru.yandex.market.base.presentation.core.schedule.schedule(
            observable = rxType,
            observeOnScheduler = observeOnScheduler,
            subscribeOnScheduler = subscribeOnScheduler,
            disposableManager = disposableManager,
            channel = channel,
            onNext = {},
            onError = onError,
            onSubscribe = onSubscribe,
            doFinally = doFinally,
            onComplete = null
        )
    }

    override val observerDelegate = object : TestObserverDelegate<String>() {

        override fun assertSuccess(observer: TestObserver<String>) {
            assertThat(observer.values().size).isGreaterThan(0)
        }
    }

    override fun `Using correct observe scheduler when schedule with lambdas`() {
        val onNext = mock<(String) -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            observable = getInstance(),
            observeOnScheduler = mainScheduler,
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onNext = onNext
        )

        verify(onNext, never()).invoke(any())
        mainScheduler.triggerActions()
        verify(onNext).invoke(any())
    }

    @Test
    fun `Calls onComplete action when schedule with lambda`() {
        val onComplete = mock<() -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            observable = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onComplete = onComplete
        )

        verify(onComplete).invoke()
    }

    @Test
    fun `Process uncaught error in onComplete action with lambda`() {
        val exception = IllegalStateException()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            observable = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onComplete = { throw exception }
        )

        assertUncaughtError(exception)
    }

    @Test
    fun `Calls onNext action when schedule with lambda`() {
        val onNext = mock<(String) -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            observable = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onNext = onNext
        )

        verify(onNext).invoke(any())
    }

    @Test
    fun `Pass exception in onNext action to onError with lambda`() {
        val exception = IllegalStateException()
        val onError = mock<(Throwable) -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            observable = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onNext = { throw exception },
            onError = onError
        )

        verify(onError).invoke(same(exception))
    }
}