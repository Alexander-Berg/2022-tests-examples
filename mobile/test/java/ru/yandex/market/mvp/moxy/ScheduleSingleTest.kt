package ru.yandex.market.mvp.moxy

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import ru.yandex.market.base.presentation.core.schedule.DisposableManager
import ru.yandex.market.base.presentation.core.schedule.schedule

class ScheduleSingleTest : ScheduleTestCases<Single<String>, TestObserver<String>, Disposable>() {

    override fun getInstanceFromCallable(callable: () -> String) = Single.fromCallable(callable)

    override fun getInstance() = Single.just("")

    override fun getCompleteInstance() = Single.just("")

    override fun getErrorInstance() = Single.error<String>(RuntimeException())

    override fun callSchedule(
        rxType: Single<String>,
        observeOnScheduler: Scheduler,
        subscribeOnScheduler: Scheduler,
        disposableManager: DisposableManager<String>,
        channel: String?,
        observer: TestObserver<String>,
        doFinally: (() -> Unit)?
    ) {
        schedule(
            single = rxType,
            observeOnScheduler = observeOnScheduler,
            subscribeOnScheduler = subscribeOnScheduler,
            disposableManager = disposableManager,
            channel = channel,
            observer = observer,
            doFinally = doFinally
        )
    }

    override fun callSchedule(
        rxType: Single<String>,
        observeOnScheduler: Scheduler,
        subscribeOnScheduler: Scheduler,
        disposableManager: DisposableManager<String>,
        channel: String?,
        onError: (Throwable) -> Unit,
        onSubscribe: (Disposable) -> Unit,
        doFinally: (() -> Unit)?
    ) {
        schedule(
            single = rxType,
            observeOnScheduler = observeOnScheduler,
            subscribeOnScheduler = subscribeOnScheduler,
            disposableManager = disposableManager,
            channel = channel,
            onSuccess = {},
            onError = onError,
            onSubscribe = onSubscribe,
            doFinally = doFinally
        )
    }

    override val observerDelegate = object : TestObserverDelegate<String>() {

        override fun assertSuccess(observer: TestObserver<String>) {
            observer.assertValueCount(1)
        }
    }

    override fun `Using correct observe scheduler when schedule with lambdas`() {
        val onSuccess = mock<(String) -> Unit>()

        schedule(
            single = getInstance(),
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

        schedule(
            single = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onSuccess = onSuccess
        )

        verify(onSuccess).invoke(any())
    }

    @Test
    fun `Process uncaught error in onSuccess action with lambda`() {
        val exception = IllegalStateException()

        schedule(
            single = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onSuccess = { throw exception }
        )

        assertUncaughtError(exception)
    }
}