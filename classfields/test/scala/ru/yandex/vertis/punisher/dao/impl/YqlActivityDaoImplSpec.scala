package ru.yandex.vertis.punisher.dao.impl

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import cats.effect.Clock
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.config.YqlLagDaoConfig
import ru.yandex.vertis.punisher.model.TaskContext.Batch
import ru.yandex.vertis.punisher.model.TaskDomain
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.quality.yql_utils.config.{YqlExecutorConfig, YqlQueryConfig}
import ru.yandex.vertis.quality.yql_utils.impl.YqlQueryExecutorImpl
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.yql_utils.YqlQueryExecutor

@Ignore
@RunWith(classOf[JUnitRunner])
class YqlActivityDaoImplSpec extends BaseSpec {
  implicit private val clock: Clock[F] = Clock.create[F]

  val config =
    YqlExecutorConfig(
      "jdbc:yql://yql.yandex.net:443?syntaxVersion=1",
      "robot-vsq-test",
      "*",
      queryConfig = YqlQueryConfig(tmpFolder = Some("//home/verticals/.tmp"))
    )

  val daoConf =
    YqlLagDaoConfig(
      "home/verticals/broker/test/warehouse/holocron/auto/full",
      "home/verticals/broker/test/warehouse/holocron/realty/full/events/1d",
      "home/logfeller/logs/vertis-event-log/1h",
      "STABLE",
      "*",
      "*"
    )
  val executorF: F[YqlQueryExecutor[F]] = YqlQueryExecutorImpl.initialize(config)

  "YqlActivityDaoImpl" should {
    "find active users" in {
      val interval =
        TimeInterval(
          DateTimeUtils.now.minusHours(2),
          DateTimeUtils.now.minusHours(1)
        )
      implicit val batch = Batch(taskDomain = TaskDomain.Unknown, timeInterval = interval)
      val policy = VertisRealtyEventActivityYqlQueryPolicy(daoConf)
      val ids =
        for {
          ex <- executorF
          dao = new YqlActivityDaoImpl(policy, ex)
          res <- dao.activeUsers(interval)
        } yield res

      println(ids.await)
    }
  }

}
