package com.yandex.vanga

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.collection.ArrayMap
import androidx.test.filters.LargeTest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class RatingManagerConcurrencyTest {

    private lateinit var counter: CounterLogger
    private lateinit var rm: RatingManager

    @Before
    fun setUp() {
        counter = CounterLogger()
        rm = createRatingManager()
        setRatingCalculatorInstance(object : RatingCalculator {
            override fun getScore(features: FloatArray): Double {
                return 1.0
            }
        })
    }

    @LargeTest
    @Test
    fun `many rating managers, updateVisitsAndRating no exceptions`() {
        assertNoExceptionsWhileRunningConcurrently {
            createRatingManager().updateVisitsAndRating(getContext(), "{test/test}")
        }
    }

    @LargeTest
    @Test
    fun `single rating manager, updateCounters no exceptions`() {
        val testClientItems = getTestClientItems(2)

        assertNoExceptionsWhileRunningConcurrently {
            rm.updateCounters(getContext(), testClientItems, emptyList())
        }
    }

    @LargeTest
    @Test
    fun `many rating managers, updateCounters no exceptions`() {
        val testClientItems = getTestClientItems(2)

        assertNoExceptionsWhileRunningConcurrently {
            createRatingManager().updateCounters(getContext(), testClientItems, null)
        }
    }

    @LargeTest
    @Test
    fun `many rating managers, updateRating no exceptions`() {
        assertNoExceptionsWhileRunningConcurrently {
            rm.updateRating(getContext())
        }
    }

    private fun assertNoExceptionsWhileRunningConcurrently(task: () -> Unit) {
        val threadPool = Executors.newFixedThreadPool(8)

        try {
            threadPool.submit(12, task)

            Thread.sleep(5000)

            counter.stop()

            Assert.assertThat(counter.logException.get(), equalTo(0))
        } finally {
            threadPool.shutdownNow()
        }
    }

    private fun getContext(): Context {
        val context = spy(RuntimeEnvironment.application)

        val packageManager: PackageManager = mock {
            on { getPackageInfo(ArgumentMatchers.anyString(), any()) } doReturn (PackageInfo())
        }
        `when`(context.packageManager).thenReturn(packageManager)
        return context
    }

    private fun ExecutorService.submit(count: Int, task: () -> Unit) {
        repeat(count) { index ->
            this.submit {
                println("task $index start")
                val timeMs = measureTimeMillis {
                    task()
                }

                println("task $index finish $timeMs ms")
            }
        }
    }

    private fun getTestClientItems(count: Int): List<ClientVangaItem> {
        val hoursMap = stubArrayMap(24)
        val daysMap = stubArrayMap(7)

        val hoursTotal = hoursMap.values.sum()

        return (0 until count).asSequence().map {
            ClientVangaItem("{test.pckg/class.a$it}", hoursTotal, hoursMap, daysMap, hoursTotal, hoursMap, daysMap)
        }.toList()
    }

    private fun stubArrayMap(size: Int) = ArrayMap<Int, Int>().apply {
        (0 until size).forEach { put(it, it) }
    }

    private fun createRatingManager() = RatingManager(storageName, counter, null, true)
}

class CounterLogger : Logger {
    @Volatile
    private var isStopped: Boolean = false

    val logException = AtomicInteger()
    val e = AtomicInteger()
    val d = AtomicInteger()

    override fun logException(message: String, t: Throwable) {
        if (isStopped) {
            return
        }
        println("exception msg: $message")
        t.printStackTrace()
        logException.incrementAndGet()
    }

    override fun e(message: String) {
        if (isStopped) {
            return
        }
        e.incrementAndGet()
    }

    override fun d(message: String) {
        if (isStopped) {
            return
        }
        d.incrementAndGet()
    }

    fun stop() {
        isStopped = true
    }
}
