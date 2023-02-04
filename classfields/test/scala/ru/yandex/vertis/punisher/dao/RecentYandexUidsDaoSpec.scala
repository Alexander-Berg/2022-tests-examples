package ru.yandex.vertis.punisher.dao

import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.vertis.Domain
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.dao.RecentYandexUidsDao.{All, ByUserId}
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model.{TaskContext, TaskDomainImpl}
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.quality.cats_utils.Awaitable._

import scala.concurrent.duration._

trait RecentYandexUidsDaoSpec extends BaseSpec {

  protected def dao: RecentYandexUidsDao[F]

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(300, Seconds), interval = Span(500, Millis))

  "RecentYandexUidsDaoSpec" should {
    "allYandexUids size greater than zero" in {
      implicit val context: TaskContext.Batch =
        TaskContext.Batch(
          taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers),
          timeInterval = TimeInterval(DateTimeUtils.now.minusHours(3), 1.hour, None)
        )

      val it1 = dao.get(All)(context).await
      it1.size should be(it1.map(_.user).toSet.size)

      it1.foreach { oneOf =>
        dao.get(ByUserId(oneOf.user))(context).await.head shouldBe oneOf
      }
    }
  }
}
