package ru.yandex.market.mvp.moxy

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import ru.yandex.market.base.presentation.core.schedule.DisposableManager

class ScheduleMaybeTest : ScheduleTestCases<Maybe<String>, TestObserver<String>, Disposable>() {

    override fun getInstanceFromCallable(callable: () -> String) = Maybe.fromCallable(callable)

    override fun getInstance() = Maybe.just("")

    override fun getCompleteInstance(): Maybe<String> = Maybe.empty()

    override fun getErrorInstance() = Maybe.error<String>(RuntimeException())

    override fun callSchedule(
        rxType: Maybe<String>,
        observeOnScheduler: Scheduler,
        subscribeOnScheduler: Scheduler,
        disposableManager: DisposableManager<String>,
        channel: String?,
        observer: TestObserver<String>,
        doFinally: (() -> Unit)?
    ) {
        ru.yandex.market.base.presentation.core.schedule.schedule(
            maybe = rxType,
            observeOnScheduler = observeOnScheduler,
            subscribeOnScheduler = subscribeOnScheduler,
            disposableManager = disposableManager,
            channel = channel,
            observer = observer,
            doFinally = doFinally
        )
    }

    override fun callSchedule(
        rxType: Maybe<String>,
        observeOnScheduler: Scheduler,
        subscribeOnScheduler: Scheduler,
        disposableManager: DisposableManager<String>,
        channel: String?,
        onError: (Throwable) -> Unit,
        onSubscribe: (Disposable) -> Unit,
        doFinally: (() -> Unit)?
    ) {
        ru.yandex.market.base.presentation.core.schedule.schedule(
            maybe = rxType,
            observeOnScheduler = observeOnScheduler,
            subscribeOnScheduler = subscribeOnScheduler,
            disposableManager = disposableManager,
            channel = channel,
            onSuccess = {},
            onError = onError,
            onSubscribe = onSubscribe,
            doFinally = doFinally,
            onComplete = null
        )
    }

    override val observerDelegate = object : TestObserverDelegate<String>() {

        override fun assertSuccess(observer: TestObserver<String>) {
            observer.assertValueCount(1)
        }
    }

    override fun `Using correct observe scheduler when schedule with lambdas`() {
        val onSuccess = mock<(String) -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            maybe = getInstance(),
            observeOnScheduler = mainScheduler,
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onSuccess = onSuccess
        )

        verify(onSuccess, never()).invoke(any())
        mainScheduler.triggerActions()
        verify(onSuccess).invoke(any())
    }

    @Test
    fun `Calls onSuccess action when schedule with lambda`() {
        val onSuccess = mock<(String) -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            maybe = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onSuccess = onSuccess
        )

        verify(onSuccess).invoke(any())
    }

    @Test
    fun `Process uncaught error in onSuccess with lambda`() {
        val exception = IllegalStateException()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            maybe = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onSuccess = { throw exception }
        )

        assertUncaughtError(exception)
    }

    @Test
    fun `Calls onComplete action when schedule with lambda`() {
        val onComplete = mock<() -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            maybe = getCompleteInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onComplete = onComplete
        )

        verify(onComplete).invoke()
    }

    @Test
    fun `Process uncaught error in onComplete with lambda`() {
        val exception = IllegalStateException()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            maybe = getCompleteInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onComplete = { throw exception }
        )

        assertUncaughtError(exception)
    }
}