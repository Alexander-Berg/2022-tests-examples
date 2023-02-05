package ru.yandex.yandexmaps.common.test

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.yandexmaps.common.utils.extensions.rx.fallbackAfterTimeout
import java.util.concurrent.TimeUnit

class FallbackTimeoutTest {

    private sealed class Status {
        object Success : Status()
        object Loading : Status()
        object Error : Status()
    }

    @Test
    fun fallbackAfterTimeoutMaybeTest() {
        val scheduler = TestScheduler()
        val source = Maybe.timer(10, TimeUnit.MILLISECONDS, scheduler).map<Status> { Status.Success }

        val resultList = mutableListOf<Status>()
        source.fallbackAfterTimeout(5, TimeUnit.MILLISECONDS, Status.Loading, scheduler).subscribe { resultList.add(it) }
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        Assertions.assertThat(resultList).hasSameElementsAs(listOf(Status.Loading, Status.Success))
    }

    @Test
    fun completeBeforeFallbackMaybeTest() {
        val scheduler = TestScheduler()
        val source = Maybe.timer(2, TimeUnit.MILLISECONDS, scheduler).map<Status> { Status.Success }

        val resultList = mutableListOf<Status>()
        source.fallbackAfterTimeout(5, TimeUnit.MILLISECONDS, Status.Loading, scheduler).subscribe { resultList.add(it) }
        scheduler.advanceTimeBy(5, TimeUnit.MILLISECONDS)

        Assertions.assertThat(resultList).hasSameElementsAs(listOf(Status.Success))
    }

    @Test
    fun fallbackAfterTimeoutSingleTest() {
        val scheduler = TestScheduler()
        val source = Single.timer(10, TimeUnit.MILLISECONDS, scheduler).map<Status> { Status.Success }

        val resultList = mutableListOf<Status>()
        source.fallbackAfterTimeout(5, TimeUnit.MILLISECONDS, Status.Loading, scheduler).subscribe { resultList.add(it) }
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        Assertions.assertThat(resultList).hasSameElementsAs(listOf(Status.Loading, Status.Success))
    }

    @Test
    fun completeBeforeFallbackSingleTest() {
        val scheduler = TestScheduler()
        val source = Single.timer(2, TimeUnit.MILLISECONDS, scheduler).map<Status> { Status.Success }

        val resultList = mutableListOf<Status>()
        source.fallbackAfterTimeout(5, TimeUnit.MILLISECONDS, Status.Loading, scheduler).subscribe { resultList.add(it) }
        scheduler.advanceTimeBy(5, TimeUnit.MILLISECONDS)

        Assertions.assertThat(resultList).hasSameElementsAs(listOf(Status.Success))
    }
}
