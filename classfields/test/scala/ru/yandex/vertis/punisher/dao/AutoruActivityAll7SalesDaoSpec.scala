package ru.yandex.vertis.punisher.dao

import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.Domain
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model.{TaskContext, TaskDomainImpl}
import ru.yandex.vertis.punisher.tasks.settings.TaskSettings
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval

import scala.concurrent.duration._

/**
  * @author devreggs
  */
trait AutoruActivityAll7SalesDaoSpec extends BaseSpec {

  def dao: ActivityDao[F]

  implicit protected val context: TaskContext.Batch =
    TaskContext.Batch(
      taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers),
      timeInterval = TimeInterval(DateTimeUtils.now.minusHours(3), 1.hour, None)
    )

  "AutoruActivityDao" should {
    "return active users" in {

      val taskSettings: TaskSettings = TaskSettings(1.hours, 1.hours, 24.hours)

      dao
        .activeUsers(
          TimeInterval(
            DateTimeUtils.now,
            taskSettings.stepMin,
            Some(taskSettings.stepBack)
          )
        )
        .await should be(
        Set(
          "1",
          "2",
          "3",
          "8",
          "9",
          "11",
          "14",
          "15",
          "17",
          "30186344",
          "21",
          "22",
          "23",
          "24",
          "30",
          "40",
          "42",
          "50"
        )
      )
    }
  }
}
