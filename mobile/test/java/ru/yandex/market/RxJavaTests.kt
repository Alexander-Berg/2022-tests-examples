package ru.yandex.market

import com.jakewharton.rx.ReplayingShare
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.functions.Cancellable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.sameInstance
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.rx.SimpleSingleObserver
import ru.yandex.market.utils.Publishers
import ru.yandex.market.utils.executeOnError
import ru.yandex.market.utils.open
import ru.yandex.market.utils.valve
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RxJavaTests {

    @field:Rule
    @JvmField
    var thrownExceptions: ExpectedException = ExpectedException.none()

    private val globalExceptions: MutableList<Throwable> = mutableListOf()

    @Before
    fun setUp() {
        RxJavaPlugins.setErrorHandler { globalExceptions.add(it) }
    }

    @Test
    fun testTakeOperatorUnsubscribeUpstream() {
        val stringSupplier = mock<() -> String>()
        whenever(stringSupplier.invoke()).thenReturn("1", "", "")

        Observable.fromArray("A", "B", "C")
            .map { stringSupplier.invoke() }
            .filter { it.isEmpty() }
            .take(1)
            .subscribe()

        verify(stringSupplier, times(2)).invoke()
    }

    @Test
    fun `Infinite observable concatMap to empty observable does not propagate completion`() {
        Observable.just("")
            .concatWith(Observable.never())
            .concatMap { Observable.empty<String>() }
            .test()
            .assertNotComplete()
    }

    @Test
    fun `Infinite observable concatMap propagate errors`() {
        Observable.just("")
            .concatWith(Observable.never())
            .concatMap { Observable.error<String>(RuntimeException()) }
            .test()
            .assertError(RuntimeException::class.java)
            .assertTerminated()
    }

    @Test
    fun `Infinite observable concatMap another infinite observable and completes`() {
        Observable.just("")
            .concatWith(Observable.never())
            .concatMap { Observable.never<String>() }
            .test()
            .assertNotComplete()
    }

    @Test
    fun `Single created from emitter calls cancellation action on success`() {
        val cancellationAction = mock<Cancellable>()

        Single.create { emitter: SingleEmitter<String> ->
            emitter.setCancellable(cancellationAction)
            emitter.onSuccess("")
        }.subscribe()

        verify(cancellationAction).cancel()
    }

    @Test
    fun `retryWhen with timeout uses passed scheduler`() {
        val timerScheduler = TestScheduler()
        val counter = AtomicInteger(0)
        val observer = Observable.create<String> { emitter ->
            if (counter.getAndIncrement() < 2) {
                emitter.onError(RuntimeException())
            } else {
                emitter.onNext("")
                emitter.onComplete()
            }
        }
            .retryWhen {
                it.flatMap {
                    Observable.timer(2, TimeUnit.SECONDS, timerScheduler)
                }
            }
            .subscribeOn(Schedulers.trampoline())
            .test()

        observer.assertNotComplete()
            .assertNoErrors()
            .assertNoValues()

        timerScheduler.advanceTimeBy(4, TimeUnit.SECONDS)

        observer.assertResult("")
    }

    @Test
    fun `Observable concatMapMaybe to empty maybe does not terminate source`() {
        Observable.just("")
            .concatWith(Observable.never())
            .concatMapMaybe { Maybe.empty<String>() }
            .test()
            .assertNotComplete()
    }

    @Test
    fun `Replaying share disposes only when all subscribers disposed`() {
        var isDisposed = false
        val observable = Observable.just("")
            .compose(ReplayingShare.instance())
            .doOnDispose { isDisposed = true }
        val disposable1 = observable.subscribe()
        @Suppress("UNUSED_VARIABLE")
        val disposable2 = observable.subscribe()

        disposable1.dispose()

        assertThat(isDisposed).isFalse
    }

    @Test
    fun `andThen don't subscribes to inner observable if source completable emit onError`() {
        var isSubscribed = false

        Completable.error(RuntimeException())
            .andThen(Completable.complete().doOnSubscribe { isSubscribed = true })
            .test()
            .assertError(RuntimeException::class.java)

        assertThat(isSubscribed).isFalse
    }

    @Test
    fun `executeOnError executes passed completable exactly one time in case of error`() {
        var timesExecuted = 0
        Completable.error(RuntimeException())
            .executeOnError { Completable.fromAction { timesExecuted++ } }
            .test()
            .assertError(RuntimeException::class.java)

        assertThat(timesExecuted).isEqualTo(1)
    }

    @Test
    fun `Exception in SimpleSingleObserver onSuccess is thrown out from RxJava`() {

        val illegalStateException: Throwable = IllegalStateException()

        thrownExceptions.expect(NullPointerException::class.java)
        thrownExceptions.expectCause(sameInstance(illegalStateException))

        Single.just("")
            .subscribe(object : SimpleSingleObserver<String>() {

                override fun onSuccess(value: String) {
                    super.onSuccess(value)
                    throw illegalStateException
                }
            })

        assertThat(globalExceptions).anyMatch { it === illegalStateException }
    }

    @Test
    fun `Exception in Single subscribe with consumer goes to global error handler`() {
        val illegalStateException: Throwable = IllegalStateException()
        Single.just("").subscribe({ throw illegalStateException }, {})

        assertThat(globalExceptions).anyMatch { it === illegalStateException }
    }

    @Test
    fun `Exception in Observable subscribe with lambdas goes to onError`() {
        var onErrorException: Throwable? = null
        val illegalStateException: Throwable = IllegalStateException()
        Observable.just("").subscribe({ throw illegalStateException }, { onErrorException = it })

        assertThat(onErrorException).isSameAs(illegalStateException)
    }

    @Test
    fun `Valve with 0 buffer size do not emit values after open`() {
        val valveProcessor = PublishProcessor.create<Boolean>()
        val testObserver = Flowable.fromIterable(listOf(1, 2, 3))
            .valve(valveProcessor, false, 0)
            .test()
        valveProcessor.onNext(true)
        testObserver.assertNoErrors()
            .assertComplete()
            .assertNoValues()
    }

    @Test
    fun `Valve with 0 buffer emit values when open`() {
        Flowable.fromIterable(listOf(1, 2, 3))
            .valve(Publishers.empty(), true, 0)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValues(1, 2, 3)
    }

    @Test
    fun `Valve with 0 buffer emit values when closed`() {
        Flowable.fromIterable(listOf(1, 2, 3))
            .valve(Publishers.empty(), false, 0)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertNoValues()
    }

    @Test
    fun `Closed valve emit all upstream items regardless of buffer size after open`() {
        val valveProcessor = PublishProcessor.create<Boolean>()

        val upstreamItems = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val testObserver = Flowable.fromIterable(upstreamItems)
            .valve(valveProcessor, false, 3)
            .test()
        valveProcessor.open()

        testObserver.assertNoErrors()
            .assertComplete()
            .assertValueSequence(upstreamItems)
    }

    @Test
    fun `Closed valve with size 1 emit only last item when open`() {
        val valveProcessor = PublishProcessor.create<Boolean>()

        val testObserver = Flowable.fromIterable(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
            .valve(valveProcessor, false, 1)
            .test()
        valveProcessor.open()

        testObserver.assertNoErrors()
            .assertComplete()
            .assertValues(9)
    }

    @Test
    fun `Flat map to completed observable does not complete source observable`() {
        Observable.fromIterable(listOf(1, 2, 3, 4))
            .flatMap { if (it % 2 == 0) Observable.empty() else Observable.just(it) }
            .test()
            .assertNoErrors()
            .assertResult(1, 3)
    }

    @Test
    fun `Subject deadlock`() {
        val adultLock = Any()
        val accountLock = Any()
        val accountSubject = PublishSubject.create<String>().toSerialized()

        fun getAccount(): String {
            return synchronized(accountLock) { "Account" }
        }

        fun getAdultState(latch: CountDownLatch): String {
            return synchronized(adultLock) {
                latch.await()
                "${getAccount()} is adult"
            }
        }

        fun logout() {
            synchronized(accountLock) {
                accountSubject.onNext("unauthorized")
            }
        }

        fun getAccountStream(): Observable<String> {
            return Observable.fromCallable { getAccount() }
                .concatWith(accountSubject)
        }

        fun executeRequest(latch: CountDownLatch): String {
            return "Request executed for state ${getAdultState(latch)}"
        }

        val lockLatch = CountDownLatch(1)
        val streamLatch = CountDownLatch(2)
        val testObserver = getAccountStream()
            .switchMapSingle {
                Single.fromCallable { executeRequest(lockLatch) }
                    .subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
            }
            .doOnNext { streamLatch.countDown() }
            .test()

        logout()
        lockLatch.countDown()
        streamLatch.await(500, TimeUnit.MILLISECONDS)
        testObserver.assertValues(
            "Request executed for state Account is adult"
        )
    }

    @Test
    fun `Subject on next switchMap to completable with subscribe on immediately free calling thread`() {
        val subject = PublishSubject.create<String>().toSerialized()

        val latch = CountDownLatch(1)
        subject.hide()
            .switchMapCompletable {
                Completable.fromCallable { latch.await() }
                    .subscribeOn(Schedulers.newThread())
            }
            .subscribe()

        subject.onNext("")
    }
}
