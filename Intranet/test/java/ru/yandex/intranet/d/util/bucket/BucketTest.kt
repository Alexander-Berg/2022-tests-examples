package ru.yandex.intranet.d.util.bucket

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

class BucketTest {

    @Test
    fun testBucket() {
        var timestamp = System.nanoTime()
        val bucket = Bucket(BucketConfiguration(listOf(Bandwidth(10, 10, Refill.greedy(10, Duration.ofSeconds(1)), "rps")))) { timestamp }
        for (i in 1..10) {
            Assertions.assertTrue(bucket.getAvailableTokens() > 0)
            Assertions.assertTrue(bucket.tryConsume(1))
            timestamp += 1
        }
        Assertions.assertFalse(bucket.getAvailableTokens() > 0)
        Assertions.assertFalse(bucket.tryConsume(1))
        timestamp += 100 * 1000 * 1000
        Assertions.assertTrue(bucket.getAvailableTokens() > 0)
        Assertions.assertTrue(bucket.tryConsume(1))
        timestamp += 1
        Assertions.assertFalse(bucket.getAvailableTokens() > 0)
        Assertions.assertFalse(bucket.tryConsume(1))
        timestamp += 1000 * 1000 * 1000
        for (i in 1..10) {
            Assertions.assertTrue(bucket.getAvailableTokens() > 0)
            Assertions.assertTrue(bucket.tryConsume(1))
            timestamp += 1
        }
        Assertions.assertFalse(bucket.getAvailableTokens() > 0)
        Assertions.assertFalse(bucket.tryConsume(1))
    }
}
