package ru.yandex.vertis.punisher.dao

import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.vertis.Domain
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model.{TaskContext, TaskDomainImpl}
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.quality.cats_utils.Awaitable._

import scala.concurrent.duration._

trait ActivityDaoSpec extends BaseSpec {

  protected def dao: ActivityDao[F]

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(900, Seconds), interval = Span(500, Millis))

  private val interval = TimeInterval(DateTimeUtils.now.minusHours(3), 1.hour, None)

  implicit protected val context: TaskContext.Batch =
    TaskContext.Batch(taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers), timeInterval = interval)

  "ActivityDao" should {
    "getActiveUsers" in {
      dao.activeUsers(interval).await should not be empty
    }
  }
}
