package ru.yandex.vertis.punisher.dao.impl

import cats.effect.Clock
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.config.YqlLagDaoConfig
import ru.yandex.vertis.quality.yql_utils.config.{YqlExecutorConfig, YqlQueryConfig}
import ru.yandex.vertis.quality.yql_utils.impl.YqlQueryExecutorImpl
import ru.yandex.vertis.quality.cats_utils.Awaitable._

@Ignore
@RunWith(classOf[JUnitRunner])
class YqlLagDaoImplSpec extends BaseSpec {
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
      "home/verticals/broker/prod/warehouse/auto/images/meta/1d",
      "*"
    )
  val executorF = YqlQueryExecutorImpl.initialize(config)

  "YqlLagDaoImpl" should {
    "find lag for vertis" in {
      val policy =
        VertisRealtyEventLagYqlQueryPolicy(
          daoConf
        )

      val lag = runForPolicy(policy)

      println(lag)
    }

    "find lag for holo" in {
      val policy =
        HolocronRealtyTypedLagYqlQueryPolicy(
          daoConf
        )

      val lag = runForPolicy(policy)

      println(lag)
    }

    "find lag for auto images meta" in {
      val policy =
        AutoImagesMetaLagQueryPolicyImpl(
          daoConf
        )

      val lag = runForPolicy(policy)

      println(lag)

    }
  }

  private def runForPolicy(policy: YqlLagQueryPolicy) =
    executorF
      .map(ex => new YqlLagDaoImpl(policy, ex))
      .flatMap(dao => dao.lag)
      .await
}
