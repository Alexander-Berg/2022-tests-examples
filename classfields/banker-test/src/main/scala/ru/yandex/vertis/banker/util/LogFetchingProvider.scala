package ru.yandex.vertis.banker.util

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import ru.yandex.vertis.banker.util.LogFetchingProvider.ResultWithLoggingEvents

import scala.reflect.{classTag, ClassTag}
import scala.jdk.CollectionConverters._

trait LogFetchingProvider {

  def withLogFetching[T: ClassTag, R](action: => R): ResultWithLoggingEvents[R] = {
    val appender = new ListAppender[ILoggingEvent]
    appender.start()

    val clazz = classTag[T].runtimeClass
    val logger = LoggerFactory
      .getLogger(clazz)
      .asInstanceOf[ch.qos.logback.classic.Logger]

    logger.addAppender(appender)

    val result = action

    logger.detachAppender(appender)

    ResultWithLoggingEvents(result, appender.list.asScala.toSeq)
  }

}

object LogFetchingProvider {

  case class ResultWithLoggingEvents[R](result: R, logEvents: Seq[ILoggingEvent])

}
