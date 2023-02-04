package ru.yandex.vertis.scheduler

import java.io.Closeable
import scala.concurrent.duration._

/**
 * [[Closeable]] wrapper for [[Scheduler]]
 *
 * @author dimas
 */
case class CloseableScheduler(s: Scheduler) extends Closeable {
  def close(): Unit = s.shutdown(100.millis)
}
