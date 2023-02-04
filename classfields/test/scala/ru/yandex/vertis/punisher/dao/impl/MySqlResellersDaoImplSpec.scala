package ru.yandex.vertis.punisher.dao.impl

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.punisher.dao.impl.mysql.MySqlResellersDaoImpl
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model.{TaskContext, TaskDomainImpl}
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.punisher.{AutoruStagesBuilder, BaseSpec}
import ru.yandex.vertis.quality.cats_utils.Awaitable._

import scala.concurrent.duration._

@Ignore
@RunWith(classOf[JUnitRunner])
class MySqlResellersDaoImplSpec extends BaseSpec {

  private lazy val dao = new MySqlResellersDaoImpl(AutoruStagesBuilder.dbMain)

  implicit protected val context: TaskContext =
    TaskContext.Batch(
      taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers),
      timeInterval = TimeInterval(DateTimeUtils.now.minusHours(3), 1.hour, None)
    )

  "AutoruActivityDao" should {
    "return active users" in {
      val actual = dao.resellers(context).await
      println(s"${actual.size} recorde selected")
    }
  }
}
