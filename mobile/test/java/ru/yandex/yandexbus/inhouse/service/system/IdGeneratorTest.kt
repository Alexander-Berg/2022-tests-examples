package ru.yandex.yandexbus.inhouse.service.system

import org.junit.Assert.assertTrue
import org.junit.Test

class IdGeneratorTest {

    @Test(expected = IllegalArgumentException::class)
    fun emptyGeneratorRejected() {
        IdGenerator(startId = 0, count = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeGeneratorRejected() {
        IdGenerator(startId = 0, count = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun overflowedGeneratorRejected() {
        IdGenerator(startId = Int.MAX_VALUE, count = 1)
    }

    @Test
    fun uniqueIds() {
        val generator = IdGenerator(startId = 10, count = 10)

        val generated = mutableSetOf<Int>()

        val generatedIdsCount = 10
        for (i in 1..generatedIdsCount) {
            generated.add(generator.acquireId())
        }
        assertTrue(generated.size == generatedIdsCount)
    }

    @Test
    fun idsRecycle() {
        val generator = IdGenerator(startId = 0, count = 1)

        for (i in 0..10) {
            val id = generator.acquireId()
            generator.releaseId(id)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun idsExhausted() {
        val generator = IdGenerator(startId = 0, count = 1)

        for (i in 0..10) {
            generator.acquireId()
        }
    }
}
