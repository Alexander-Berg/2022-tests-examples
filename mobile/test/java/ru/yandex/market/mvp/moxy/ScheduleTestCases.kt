package ru.yandex.market.mvp.moxy

import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.yandex.market.base.presentation.core.schedule.DisposableManager
import ru.yandex.market.utils.Watson

@Suppress("MemberVisibilityCanBePrivate")
abstract class ScheduleTestCases<RxType : Any, RxObserver : Any, RxSubscription> {

    protected val mainScheduler = TestScheduler()
    protected val workerScheduler = TestScheduler()
    protected val disposableManager = mock<DisposableManager<String>>()
    protected val pluginErrors = mutableListOf<Throwable>()
    protected val watsonErrors = mutableListOf<Throwable>()

    protected abstract val observerDelegate: ObserverDelegate<RxObserver>

    protected abstract fun getInstanceFromCallable(callable: () -> String): RxType

    protected abstract fun getInstance(): RxType

    protected abstract fun getCompleteInstance(): RxType

    protected abstract fun getErrorInstance(): RxType

    protected abstract fun callSchedule(
        rxType: RxType,
        observeOnScheduler: Scheduler = Schedulers.trampoline(),
        subscribeOnScheduler: Scheduler = Schedulers.trampoline(),
        disposableManager: DisposableManager<String>,
        channel: String? = null,
        observer: RxObserver,
        doFinally: (() -> Unit)? = null
    )

    protected abstract fun callSchedule(
        rxType: RxType,
        observeOnScheduler: Scheduler = Schedulers.trampoline(),
        subscribeOnScheduler: Scheduler = Schedulers.trampoline(),
        disposableManager: DisposableManager<String>,
        channel: String? = null,
        onError: (Throwable) -> Unit = {},
        onSubscribe: (RxSubscription) -> Unit = {},
        doFinally: (() -> Unit)? = null
    )

    protected fun assertUncaughtError(exception: Throwable) {
        assertThat(pluginErrors).anyMatch { it === exception}
        assertThat(watsonErrors).anyMatch { it === exception}
    }

    @Before
    fun setUp() {
        RxJavaPlugins.setErrorHandler { pluginErrors.add(it) }
        Watson.setReporter(
            object : Watson.Reporter {
                override fun analyzeAndReport(throwable: Throwable) {
                    watsonErrors.add(throwable)
                }
            }
        )
    }

    @Test
    fun `Using correct subscribe scheduler when schedule with observer`() {
        var isInvoked = false
        val observable = getInstanceFromCallable {
            isInvoked = true
            ""
        }

        callSchedule(
            rxType = observable,
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = workerScheduler,
            disposableManager = disposableManager,
            observer = observerDelegate.getTestObserver()
        )

        assertFalse(isInvoked)
        workerScheduler.triggerActions()
        assertTrue(isInvoked)
    }

    @Test
    fun `Using correct observe scheduler when schedule with observer`() {
        val observer = observerDelegate.getTestObserver()
        val observable = getInstance()

        callSchedule(
            rxType = observable,
            observeOnScheduler = mainScheduler,
            subscribeOnScheduler = Schedulers.trampoline(),
            disposableManager = disposableManager,
            observer = observer
        )
        workerScheduler.triggerActions()

        observerDelegate.apply {
            assertNoErrors(observer)
            assertNotComplete(observer)
        }
        mainScheduler.triggerActions()
        observerDelegate.assertSuccess(observer)
    }

    @Test
    fun `Dispose channel after complete with observer`() {
        val observer = observerDelegate.getTestObserver()
        val channel = "channel"

        callSchedule(
            rxType = getInstance(),
            disposableManager = disposableManager,
            channel = channel,
            observer = observer
        )

        observerDelegate.assertNoErrors(observer)
        verify(disposableManager).dispose(any(), same(channel))
    }

    @Test
    fun `Execute action in do finally with observer`() {
        val observer = observerDelegate.getTestObserver()
        val doFinally = mock<() -> Unit>()

        callSchedule(
            rxType = getInstance(),
            disposableManager = disposableManager,
            channel = null,
            observer = observer,
            doFinally = doFinally
        )

        observerDelegate.assertNoErrors(observer)
        verify(doFinally).invoke()
    }

    @Test
    fun `Register channel disposable on subscribe when schedule with observer`() {
        val observer = observerDelegate.getTestObserver()
        val channel = "channel"

        callSchedule(
            rxType = getInstance(),
            disposableManager = disposableManager,
            channel = channel,
            observer = observer,
            doFinally = null
        )

        observerDelegate.assertNoErrors(observer)
        verify(disposableManager).registerDisposable(any(), argThat { this == channel })
    }

    @Test
    fun `Register general disposable on subscribe when schedule with observer`() {
        val observer = observerDelegate.getTestObserver()

        callSchedule(
            rxType = getInstance(),
            disposableManager = disposableManager,
            channel = null,
            observer = observer,
            doFinally = null
        )

        observerDelegate.assertNoErrors(observer)
        verify(disposableManager).registerDisposable(any(), anyOrNull())
    }

    @Test
    fun `Calls error action when schedule with observer`() {
        val observer = observerDelegate.getTestObserver()

        callSchedule(
            rxType = getErrorInstance(),
            disposableManager = disposableManager,
            observer = observer
        )

        observerDelegate.assertError(observer)
    }

    @Test
    fun `Calls subscribe action when schedule with observer`() {
        var isInvoked = false
        val observer = observerDelegate.getTestObserver { isInvoked = true }

        callSchedule(
            rxType = getInstance(),
            disposableManager = disposableManager,
            observer = observer
        )

        assertTrue(isInvoked)
    }

    @Test
    fun `Using correct subscribe scheduler when schedule with lambdas`() {
        var isInvoked = false
        val observable = getInstanceFromCallable {
            isInvoked = true
            ""
        }

        callSchedule(
            rxType = observable,
            observeOnScheduler = Schedulers.trampoline(),
            subscribeOnScheduler = workerScheduler,
            disposableManager = disposableManager
        )

        assertFalse(isInvoked)
        workerScheduler.triggerActions()
        assertTrue(isInvoked)
    }

    @Test
    abstract fun `Using correct observe scheduler when schedule with lambdas`()

    @Test
    fun `Execute action in do finally with lambdas`() {
        val doFinally = mock<() -> Unit>()

        callSchedule(
            rxType = getInstance(),
            disposableManager = disposableManager,
            channel = null,
            doFinally = doFinally
        )

        verify(doFinally).invoke()
    }

    @Test
    fun `Register channel disposable on subscribe when schedule with lambdas`() {
        val channel = "channel"

        callSchedule(
            rxType = getInstance(),
            disposableManager = disposableManager,
            channel = channel,
            doFinally = null
        )

        verify(disposableManager).registerDisposable(any(), argThat { this == channel })
    }

    @Test
    fun `Register general disposable on subscribe when schedule with lambdas`() {
        callSchedule(
            rxType = getInstance(),
            disposableManager = disposableManager
        )

        verify(disposableManager).registerDisposable(any(), anyOrNull())
    }

    @Test
    fun `Calls subscribe action when schedule with lambdas`() {
        val onSubscribe = mock<() -> Unit>()

        callSchedule(
            rxType = getInstance(),
            disposableManager = disposableManager,
            onSubscribe = { onSubscribe() }
        )

        verify(onSubscribe).invoke()
    }

    @Test
    fun `Calls error action when schedule with lambdas`() {
        val onError = mock<(Throwable) -> Unit>()

        callSchedule(
            rxType = getErrorInstance(),
            disposableManager = disposableManager,
            onError = onError
        )

        verify(onError).invoke(any())
    }

    @Test
    fun `Pass uncaught error during onError to watson and rxJava plugins`() {
        val exception: Throwable = IllegalStateException()
        val onError: (Throwable) -> Unit = { throw exception }

        callSchedule(
            rxType = getErrorInstance(),
            disposableManager = disposableManager,
            onError = onError
        )

        @Suppress("UNCHECKED_CAST")
        assertThat(pluginErrors).anyMatch { it is CompositeException}
        assertThat(watsonErrors).anyMatch { it === exception}
    }

    @Test
    fun `Pass uncaught error during onSubscribe to watson and rxJava plugins`() {
        val exception: Throwable = IllegalStateException()
        val onSubscribe: (RxSubscription) -> Unit = { throw exception }

        callSchedule(
            rxType = getInstance(),
            disposableManager = disposableManager,
            onSubscribe = onSubscribe
        )
        assertThat(pluginErrors).anyMatch { it === exception }
        assertThat(watsonErrors).anyMatch { it == exception }
    }

    protected interface ObserverDelegate<RxObserver> {

        fun getTestObserver(onSubscribe: () -> Unit = {}): RxObserver

        fun assertNoErrors(observer: RxObserver)

        fun assertError(observer: RxObserver)

        fun assertNotComplete(observer: RxObserver)

        fun assertSuccess(observer: RxObserver)
    }

    protected abstract class TestObserverDelegate<T> : ObserverDelegate<TestObserver<T>> {

        override fun getTestObserver(
            onSubscribe: () -> Unit
        ): TestObserver<T> {

            return object : TestObserver<T>() {

                override fun onSubscribe(d: Disposable) {
                    super.onSubscribe(d)
                    onSubscribe()
                }
            }
        }

        override fun assertNoErrors(observer: TestObserver<T>) {
            observer.assertNoErrors()
        }

        override fun assertError(observer: TestObserver<T>) {
            observer.assertError { true }
        }

        override fun assertNotComplete(observer: TestObserver<T>) {
            observer.assertNotComplete()
        }
    }
}