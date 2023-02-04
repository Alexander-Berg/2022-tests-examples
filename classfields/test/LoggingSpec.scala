package common.zio.logging.test

import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import common.zio.logging.{Logging, Slf4jLogger}
import zio.{Task, UIO}
import zio.test._
import zio.test.Assertion._

import scala.jdk.CollectionConverters._

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 16/12/2019
  */
object LoggingSpec extends DefaultRunnableSpec {

  override def spec =
    suite("LoggingSpec")(
      testM("do logging") {
        val testMessage = "test message"

        for {
          listAppender <- Task(new ListAppender[ILoggingEvent])
          _ <- Task(listAppender.start())
          root <- Task(LoggerFactory.getLogger("ROOT").asInstanceOf[Logger])
          _ <- Task(root.addAppender(listAppender))

          logger <- Slf4jLogger.make
          _ <- logger.info(testMessage)

          events <- Task(listAppender.list.asScala)
          _ <- Task(root.detachAppender(listAppender))
        } yield assert(events)(hasSize(equalTo(1))) &&
          assert(events.head.getMessage)(equalTo(testMessage)) &&
          assert(events.head.getLevel)(equalTo(Level.INFO)) &&
          assert(events.head.getLoggerName)(equalTo(LoggingSpec.getClass.getName))
      }
    )
}
