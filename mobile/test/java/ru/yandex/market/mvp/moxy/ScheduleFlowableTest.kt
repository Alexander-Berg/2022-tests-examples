package ru.yandex.market.mvp.moxy

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.reactivestreams.Subscription
import ru.yandex.market.base.presentation.core.schedule.DisposableManager

class ScheduleFlowableTest : ScheduleTestCases<Flowable<String>, TestSubscriber<String>, Subscription>() {

    override fun getInstanceFromCallable(callable: () -> String) = Flowable.fromCallable(callable)

    override fun getInstance() = Flowable.just("")

    override fun getCompleteInstance(): Flowable<String> = Flowable.empty()

    override fun getErrorInstance() = Flowable.error<String>(RuntimeException())

    override fun callSchedule(
        rxType: Flowable<String>,
        observeOnScheduler: Scheduler,
        subscribeOnScheduler: Scheduler,
        disposableManager: DisposableManager<String>,
        channel: String?,
        observer: TestSubscriber<String>,
        doFinally: (() -> Unit)?
    ) {
        ru.yandex.market.base.presentation.core.schedule.schedule(
            flowable = rxType,
            observeOnScheduler = observeOnScheduler,
            subscribeOnScheduler = subscribeOnScheduler,
            disposableManager = disposableManager,
            channel = channel,
            subscriber = observer,
            doFinally = doFinally
        )
    }

    override fun callSchedule(
        rxType: Flowable<String>,
        observeOnScheduler: Scheduler,
        subscribeOnScheduler: Scheduler,
        disposableManager: DisposableManager<String>,
        channel: String?,
        onError: (Throwable) -> Unit,
        onSubscribe: (Subscription) -> Unit,
        doFinally: (() -> Unit)?
    ) {
        ru.yandex.market.base.presentation.core.schedule.schedule(
            flowable = rxType,
            observeOnScheduler = observeOnScheduler,
            subscribeOnScheduler = subscribeOnScheduler,
            disposableManager = disposableManager,
            channel = channel,
            onNext = {},
            onError = onError,
            onSubscribe = {
                onSubscribe(it)
                it.request(Long.MAX_VALUE)
            },
            doFinally = doFinally,
            onComplete = null
        )
    }

    override val observerDelegate = object : ObserverDelegate<TestSubscriber<String>> {

        override fun getTestObserver(
            onSubscribe: () -> Unit
        ): TestSubscriber<String> {
            return object : TestSubscriber<String>(Long.MAX_VALUE) {
                override fun onSubscribe(s: Subscription) {
                    super.onSubscribe(s)
                    onSubscribe()
                }
            }
        }

        override fun assertNoErrors(observer: TestSubscriber<String>) {
            observer.assertNoErrors()
        }

        override fun assertError(observer: TestSubscriber<String>) {
            observer.assertError { true }
        }

        override fun assertNotComplete(observer: TestSubscriber<String>) {
            observer.assertNotComplete()
        }

        override fun assertSuccess(observer: TestSubscriber<String>) {
            assertThat(observer.valueCount()).isGreaterThan(0)
        }

    }

    override fun `Using correct observe scheduler when schedule with lambdas`() {
        val onNext = mock<(String) -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            flowable = getInstance(),
            observeOnScheduler = mainScheduler,
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onNext = onNext,
            onSubscribe = { it.request(Long.MAX_VALUE) }
        )

        verify(onNext, never()).invoke(any())
        mainScheduler.triggerActions()
        verify(onNext).invoke(any())
    }

    @Test
    fun `Calls onComplete action when schedule with lambda`() {
        val onComplete = mock<() -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            flowable = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onComplete = onComplete,
            onSubscribe = { it.request(Long.MAX_VALUE) }
        )

        verify(onComplete).invoke()
    }

    @Test
    fun `Process uncaught error in onComplete action with lambda`() {
        val exception = IllegalStateException()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            flowable = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onComplete = { throw exception },
            onSubscribe = { it.request(Long.MAX_VALUE) }
        )

        assertUncaughtError(exception)
    }

    @Test
    fun `Calls onNext action when schedule with lambda`() {
        val onNext = mock<(String) -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            flowable = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onNext = onNext,
            onSubscribe = { it.request(Long.MAX_VALUE) }
        )

        verify(onNext).invoke(any())
    }

    @Test
    fun `Pass exception in onNext action to onError with lambda`() {
        val exception = IllegalStateException()
        val onError = mock<(Throwable) -> Unit>()

        ru.yandex.market.base.presentation.core.schedule.schedule(
            flowable = getInstance(),
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            onNext = { throw exception },
            onError = onError,
            onSubscribe = { it.request(Long.MAX_VALUE) }
        )

        verify(onError).invoke(same(exception))
    }
}