package ru.yandex.intranet.d

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Supplier
import kotlin.concurrent.withLock

/**
 * LogCollector.
 * Для тестирование логирования.
 *
 * @author Petr Surkov <petrsurkov@yandex-team.ru>
 */
@Plugin(name = "LogCollector", category = "Core", elementType = "appender")
class LogCollector private constructor(name: String?,
                                       filter: Filter?,
                                       layout: Layout<out Serializable>?,
                                       ignoreExceptions: Boolean,
                                       properties: Array<out Property>?) :
    AbstractAppender(name, filter, layout, ignoreExceptions, properties) {

    override fun append(event: LogEvent) {
        if (openCounter.get() > 0) {
            logLock.withLock {
                events.add(event)
            }
        }
    }

    companion object {
        private val openCounter = AtomicInteger(0)
        private val events = mutableListOf<LogEvent>()
        private val logLock = ReentrantLock()

        @PluginFactory
        @JvmStatic
        fun createAppender(
            @PluginAttribute("name") name: String?,
            @PluginElement("Layout") layout: Layout<out Serializable>?,
            @PluginElement("Filter") filter: Filter?,
            @PluginAttribute("otherAttribute") otherAttribute: String?
        ): LogCollector {
            return LogCollector(name, filter, layout, false, arrayOf())
        }

        /**
         * Выполняет переданный [supplier], возвращая результат его выполнения, попутно собирая с него логи.
         * Важно, что собираются также и логи от всех других потоков, так что в результате будет
         * собрано больше логов (служебных), чем от запуска только лишь этого [supplier].
         *
         * В рамках одного теста после использования одного или нескольких [collectLogs]
         * стоит не забыть сделать [takeLogs], чтобы собранные логи с одного теста не попали в другой.
         */
        @JvmStatic
        fun <T> collectLogs(supplier: Supplier<T>): T {
            openCounter.incrementAndGet()
            val result = supplier.get()
            openCounter.decrementAndGet()
            return result
        }

        suspend fun <T> collectLogs(supplier: suspend () -> T): T {
            openCounter.incrementAndGet()
            val result = supplier()
            openCounter.decrementAndGet()
            return result
        }

        /**
         * Вынимает накопленные логи: возвращает их и очищает их хранилище.
         * @return [Logs]
         */
        @JvmStatic
        fun takeLogs(): Logs = logLock.withLock {
            val logs = Logs(events.toList())
            events.clear()
            return logs
        }

        @JvmStatic
        fun takeLogEvents(from: Int = 0): List<LogEvent> = logLock.withLock {
            return events.slice(from until events.size)
        }

        @JvmStatic
        fun clear() = logLock.withLock { events.clear() }
    }

    class Logs(val events: List<LogEvent>) {

        private val eventByFormat: Map<String, Set<LogEvent>> = events.groupBy { it.message.format }
            .mapValues { it.value.toSet() }

        /**
         * Проверяет наличие лога уровня [level] формата [format] в логах.
         *
         * Пример:
         * ```
         * logs.contains(Level.INFO, "Provider request \"{}\", opId: {}, requestId: {}")
         * ```
         *
         * Писать логи необходимо используя аргументы, например:
         * ```
         * logger.info("Provider request \"{}\", opId: {}, requestId: {}", name, operationId, requestId);
         * ```
         */
        fun contains(level: Level, format: String): Boolean {
            val byFormat = eventByFormat[format] ?: setOf()
            return byFormat.any { it.level == level }
        }
    }
}
