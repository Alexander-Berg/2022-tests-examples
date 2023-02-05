package ru.yandex.market.internal.rx

import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.datetime.CacheDuration
import ru.yandex.market.rx.RxSharingJobExecutor
import ru.yandex.market.test.extensions.asObservable

class RxSharingJobExecutorTest {

    private val executor = RxSharingJobExecutor<String, String>(isFailedEvicted = true)
    private val jobCreator = mock<() -> Observable<String>>()

    @Before
    fun setUp() {
        RxSharingJobExecutor.setTimeProvider { System.currentTimeMillis() }
    }

    @Test
    fun `Method 'getSharedOrExecute' returns new job instance if job is not started yet`() {
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable()

        executor.getSharedOrExecute("job", jobCreator).test().assertNoErrors()

        verify(jobCreator, times(1)).invoke()
    }

    @Test
    fun `Method 'getSharedOrExecute' returns same job instance if previous job is not yet finished`() {
        val key = "job"
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable().subscribeOn(TestScheduler())

        executor.getSharedOrExecute(key, jobCreator).test().assertNoErrors()
        executor.getSharedOrExecute(key, jobCreator).test().assertNoErrors()

        verify(jobCreator, times(1)).invoke()
    }

    @Test
    fun `Method 'getSharedOrExecute' returns new job if previous job is finished and not cached`() {
        val key = "job"
        val scheduler = TestScheduler()
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable()

        executor.getSharedOrExecute(key, jobCreator).test().assertNoErrors()
        scheduler.triggerActions()
        executor.getSharedOrExecute(key, jobCreator).test().assertNoErrors()

        verify(jobCreator, times(2)).invoke()
    }

    @Test
    fun `Method 'getSharedOrExecute' returns same job instance if previous job is finished but cached`() {
        val key = "job"
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable()

        executor.getSharedOrExecute(key, CacheDuration.Unlimited, jobCreator).test().assertNoErrors()
        executor.getSharedOrExecute(key, jobCreator).test().assertNoErrors()

        verify(jobCreator, times(1)).invoke()
    }

    @Test
    fun `Method 'getSharedOrExecute' returns different jobs for different keys`() {
        val firstKey = "key1"
        val secondKey = "key2"
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable().subscribeOn(TestScheduler())

        executor.getSharedOrExecute(firstKey, jobCreator).test().assertNoErrors()
        executor.getSharedOrExecute(secondKey, jobCreator).test().assertNoErrors()

        verify(jobCreator, times(2)).invoke()
    }

    @Test
    fun `Method 'forceExecute' returns new job even when previous job is not finished`() {
        val key = "job"
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable().subscribeOn(TestScheduler())

        executor.getSharedOrExecute(key, jobCreator).test().assertNoErrors()
        executor.forceExecute(key, jobCreator).test().assertNoErrors()

        verify(jobCreator, times(2)).invoke()
    }

    @Test
    fun `Method 'forceExecute' returns new job even when previous job is cached`() {
        val key = "job"
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable()

        executor.getSharedOrExecute(key, CacheDuration.Unlimited, jobCreator).test().assertNoErrors()
        executor.forceExecute(key, jobCreator).test().assertNoErrors()

        verify(jobCreator, times(2)).invoke()
    }

    @Test
    fun `Method 'forceExecute' returns job available for sharing`() {
        val key = "job"
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable().subscribeOn(TestScheduler())

        executor.forceExecute(key, jobCreator).test().assertNoErrors()
        executor.getSharedOrExecute(key, jobCreator).test().assertNoErrors()

        verify(jobCreator, times(1)).invoke()
    }

    @Test
    fun `Method 'getShared' returns executing job`() {
        val key = "job"
        val scheduler = TestScheduler()
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable().subscribeOn(scheduler)

        executor.getSharedOrExecute(key, jobCreator).test().assertNoErrors()
        val connect = executor.getShared(key).test()
        connect.assertNoErrors().assertNotComplete()
        scheduler.triggerActions()
        connect.assertNoErrors().assertComplete()
    }

    @Test
    fun `Method 'getShared' returns empty observable if shared job not found`() {
        executor.getShared("job").test().assertComplete().assertNoErrors()
    }

    @Test
    fun `Test executor share job after all subscribers gone`() {
        val key = "job"
        val scheduler = TestScheduler()
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable().subscribeOn(scheduler)

        val disposable = executor.getSharedOrExecute(key, jobCreator).subscribe()
        disposable.dispose()
        val testObserver = executor.getSharedOrExecute(key, jobCreator).test().assertNotComplete().assertNoErrors()
        scheduler.triggerActions()
        testObserver.assertComplete()

        verify(jobCreator, times(1)).invoke()
    }

    @Test
    fun `Test executor propagates exception from job`() {
        whenever(jobCreator.invoke()) doReturn Observable.error<String>(RuntimeException())

        executor.getSharedOrExecute("job", jobCreator)
            .test()
            .assertError(RuntimeException::class.java)
    }

    @Test
    fun `Test failed job is not sharing`() {
        val key = "job"
        whenever(jobCreator.invoke()) doReturn Observable.error<String>(RuntimeException())

        executor.getSharedOrExecute(key, CacheDuration.Unlimited, jobCreator)
            .test()
            .assertError(RuntimeException::class.java)
        whenever(jobCreator.invoke()) doReturn "Hello, world!".asObservable()
        executor.getSharedOrExecute(key, jobCreator)
            .test()
            .assertNoErrors()

        verify(jobCreator, times(2)).invoke()
    }

    @Test
    fun `Test clearing all jobs`() {
        val key1 = "key1"
        val key2 = "key2"

        val value = "Hello, world!"
        whenever(jobCreator.invoke()) doReturn value.asObservable()
        executor.getSharedOrExecute(key1, CacheDuration.Unlimited, jobCreator).test().assertValue(value)
        executor.getSharedOrExecute(key2, CacheDuration.Unlimited, jobCreator).test().assertValue(value)

        executor.clearAllJobs()

        executor.getShared(key1).test().assertNoValues()
        executor.getShared(key2).test().assertNoValues()
    }

    @Test
    fun `Test clearing specific key`() {
        val key1 = "key1"
        val key2 = "key2"

        val value = "Hello, world!"
        whenever(jobCreator.invoke()) doReturn value.asObservable()
        executor.getSharedOrExecute(key1, CacheDuration.Unlimited, jobCreator).test().assertValue(value)
        executor.getSharedOrExecute(key2, CacheDuration.Unlimited, jobCreator).test().assertValue(value)

        executor.clearJob(key2)

        executor.getShared(key1).test().assertValues(value, value)
        executor.getShared(key2).test().assertNoValues()
    }

    // не работает из-за https://st.yandex-team.ru/BLUEMARKETAPPS-20346
    /*@Test
    fun `Test getShared emits 1 value`() {
        val key = "key"
        val value = "Hello, world!"
        whenever(jobCreator.invoke()) doReturn value.asObservable()
        executor.getSharedOrExecute(key, CacheDuration.Unlimited, jobCreator).test().assertValue(value)

        executor.getShared(key).test().assertValue(value)
    }*/

}