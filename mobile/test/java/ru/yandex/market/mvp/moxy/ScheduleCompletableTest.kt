package ru.yandex.market.mvp.moxy

import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import ru.yandex.market.base.presentation.core.schedule.DisposableManager

class ScheduleCompletableTest : ScheduleTestCases<Completable, TestObserver<String>, Disposable>() {

    override fun getInstanceFromCallable(callable: () -> String) = Completable.fromCallable(callable)

    override fun getInstance() = Completable.complete()

    override fun getCompleteInstance(): Completable = Completable.complete()

    override fun getErrorInstance() = Completable.error(RuntimeException())

    override fun callSchedule(
        rxType: Completable,
        observeOnScheduler: Scheduler,
        subscribeOnScheduler: Scheduler,
        disposableManager: DisposableManager<String>,
        channel: String?,
        observer: TestObserver<String>,
        doFinally: (() -> Unit)?
    ) {
        ru.yandex.market.base.presentation.core.schedule.schedule(
            completable = rxType,
            observeOnScheduler = observeOnScheduler,
            subscribeOnScheduler = subscribeOnScheduler,
            disposableManager = disposableManager,
            channel = channel,
            observer = observer,
            doFinally = doFinally
        )
    }

    override fun callSchedule(
        rxType: Completable,
        observeOnScheduler: Scheduler,
        subscribeOnScheduler: Scheduler,
        disposableManager: DisposableManager<String>,
        channel: String?,
        onError: (Throwable) -> Unit,
        onSubscribe: (Disposable) -> Unit,
        doFinally: (() -> Unit)?
    ) {
        ru.yandex.market.base.presentation.core.schedule.schedule(
            completable = rxType,
            observeOnScheduler = observeOnScheduler,
            subscribeOnScheduler = subscribeOnScheduler,
            disposableManager = disposableManager,
            channel = channel,
            onComplete = {},
            onError = onError,
            onSubscribe = onSubscribe,
            doFinally = doFinally
        )
    }

    override val observerDelegate = object : TestObserverDelegate<String>() {

        override fun assertSuccess(observer: TestObserver<String>) {
            observer.assertComplete()
        }
    }

    override fun `Using correct observe scheduler when schedule with lambdas`() {
        val onComplete = mock<() -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            completable = getInstance(),
            observeOnScheduler = mainScheduler,
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onComplete = onComplete
        )

        verify(onComplete, never()).invoke()
        mainScheduler.triggerActions()
        verify(onComplete).invoke()
    }

    @Test
    fun `Calls complete action when schedule with lambda`() {
        val onComplete = mock<() -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            completable = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onComplete = onComplete
        )

        verify(onComplete).invoke()
    }

    @Test
    fun `Process uncaught error in complete action with lambda`() {
        val exception = IllegalStateException()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            completable = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onComplete = { throw exception }
        )

        assertUncaughtError(exception)
    }
}