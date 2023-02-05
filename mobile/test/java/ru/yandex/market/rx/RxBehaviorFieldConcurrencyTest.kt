package ru.yandex.market.rx

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

internal class RxBehaviorFieldConcurrencyTest {

    private val field = RxBehaviorField<Int>()

    @Before
    fun setup() {
        field.update { INITIAL_VALUE }
    }

    @Test
    fun test1() {
        val runnablePlus = Runnable { field.update { it.getNonNullForTest() + 1 } }
        val runnableMinus = Runnable { field.update { it.getNonNullForTest() - 1 } }

        val threads = mutableListOf<Thread>()
        repeat(THREADS_FOR_OPERATION) {
            threads.add(
                Thread(runnablePlus)
            )
            threads.add(
                Thread(runnableMinus)
            )
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertThat(field.get()).isEqualTo(INITIAL_VALUE)
        field.valuesStream()
            .test()
            .assertValue {
                it.isPresent && it.get() == INITIAL_VALUE
            }
    }

    private companion object {

        const val INITIAL_VALUE = 1
        const val THREADS_FOR_OPERATION = 10

        fun Int?.getNonNullForTest(): Int {
            return this ?: error("Не может проявится в данном тесте")
        }
    }
}
