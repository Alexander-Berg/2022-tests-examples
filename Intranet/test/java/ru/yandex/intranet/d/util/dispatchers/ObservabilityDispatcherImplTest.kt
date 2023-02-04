package ru.yandex.intranet.d.util.dispatchers

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Observability dispatcher test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
class ObservabilityDispatcherImplTest {

    @Test
    fun testLaunch() {
        val dispatcher = ObservabilityDispatcherImpl()
        val counter = AtomicLong(0L)
        MDC.put("log_id", "test-log-id")
        logger.info { "Log before launch" }
        try {
            runBlocking {
                val job = dispatcher.launch {
                    logger.info { "Log after launch begin" }
                    counter.incrementAndGet()
                    delay(1)
                    counter.incrementAndGet()
                    logger.info { "Log after launch end" }
                }
                job.join()
            }
        } finally {
            dispatcher.preDestroy()
            logger.info { "Log on finish" }
            MDC.remove("log_id")
        }
        Assertions.assertEquals(2, counter.get())
    }

    @Test
    fun testLaunchException() {
        val dispatcher = ObservabilityDispatcherImpl()
        val counter = AtomicLong(0L)
        val exception = AtomicBoolean(false)
        MDC.put("log_id", "test-log-id")
        logger.info { "Log before launch" }
        try {
            runBlocking {
                val job = dispatcher.launch {
                    logger.info { "Log after launch begin" }
                    counter.incrementAndGet()
                    delay(1)
                    counter.incrementAndGet()
                    logger.info { "Log after launch end" }
                    throw RuntimeException("Test")
                }
                job.invokeOnCompletion { e ->
                    if (e != null) {
                        exception.set(true)
                    }
                }
                job.join()
            }
        } finally {
            dispatcher.preDestroy()
            logger.info { "Log on finish" }
            MDC.remove("log_id")
        }
        Assertions.assertEquals(2, counter.get())
        Assertions.assertTrue(exception.get())
    }

    @Test
    fun testLaunchStub() {
        val dispatcher = ObservabilityDispatcherBlockingStub()
        val counter = AtomicLong(0L)
        MDC.put("log_id", "test-log-id")
        logger.info { "Log before launch" }
        try {
            runBlocking {
                val job = dispatcher.launch {
                    logger.info { "Log after launch begin" }
                    counter.incrementAndGet()
                    delay(1)
                    counter.incrementAndGet()
                    logger.info { "Log after launch end" }
                }
                job.join()
            }
        } finally {
            dispatcher.preDestroy()
            logger.info { "Log on finish" }
            MDC.remove("log_id")
        }
        Assertions.assertEquals(2, counter.get())
    }

    @Test
    fun testLaunchExceptionStub() {
        val dispatcher = ObservabilityDispatcherBlockingStub()
        val counter = AtomicLong(0L)
        val exception = AtomicBoolean(false)
        MDC.put("log_id", "test-log-id")
        logger.info { "Log before launch" }
        try {
            runBlocking {
                dispatcher.launch {
                    logger.info { "Log after launch begin" }
                    counter.incrementAndGet()
                    delay(1)
                    counter.incrementAndGet()
                    logger.info { "Log after launch end" }
                    throw RuntimeException("Test")
                }
            }
        } catch (e: Exception) {
            exception.set(true)
        } finally {
            dispatcher.preDestroy()
            logger.info { "Log on finish" }
            MDC.remove("log_id")
        }
        Assertions.assertEquals(2, counter.get())
        Assertions.assertTrue(exception.get())
    }

}
