package ru.yandex.market.internal.rx

import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RxSerialJobExecutorTest {

    private val jobFactory = mock<(String) -> Observable<String>>()
    private val jobExecutor = RxSerialJobExecutor(jobFactory)

    @Test
    fun `Remove current job dispose observable`() {
        whenever(jobFactory.invoke(any())).thenReturn(Observable.just(""))

        val disposable = jobExecutor.executeJob("").subscribe()
        jobExecutor.removeCurrentJob()

        assertThat(disposable.isDisposed).isEqualTo(true)
    }

    @Test
    fun `Requesting job for different key dispose previous job`() {
        val jobCallable = mock<() -> String>()
        val valve = PublishSubject.create<Unit>()
        whenever(jobFactory.invoke(any()))
            .thenReturn(valve.flatMap { Observable.fromCallable(jobCallable) })
            .thenReturn(Observable.empty())

        jobExecutor.executeJob("first")
            .test()
            .assertNoErrors()
            .assertNotComplete()

        jobExecutor.executeJob("second")
            .test()
            .assertNoErrors()
            .assertComplete()

        valve.onNext(Unit)

        verify(jobCallable, never()).invoke()
    }

    @Test
    fun `Requesting job for same key returns same job`() {
        val output = "output"
        val jobCallable = mock<() -> String> { on { invoke() } doReturn output }
        whenever(jobFactory.invoke(any()))
            .thenReturn(Observable.fromCallable(jobCallable).concatWith(Observable.never()))

        val input = "input"
        jobExecutor.executeJob(input)
            .test()
            .assertNoErrors()
            .assertValue(output)

        jobExecutor.executeJob(input)
            .test()
            .assertNoErrors()
            .assertValue(output)

        verify(jobCallable, times(1)).invoke()
    }
}