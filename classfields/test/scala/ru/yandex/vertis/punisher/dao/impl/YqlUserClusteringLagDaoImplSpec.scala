package ru.yandex.vertis.punisher.dao.impl

import cats.effect.Clock
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.config.YqlLagDaoConfig
import ru.yandex.vertis.quality.yql_utils.config.YqlExecutorConfig
import ru.yandex.vertis.quality.yql_utils.impl.YqlQueryExecutorImpl
import ru.yandex.vertis.quality.cats_utils.Awaitable._

@Ignore
@RunWith(classOf[JUnitRunner])
class YqlUserClusteringLagDaoImplSpec extends BaseSpec {

  implicit private val clock: Clock[F] = Clock.create[F]

  val config =
    YqlExecutorConfig(
      "jdbc:yql://yql.yandex.net:443?syntaxVersion=1",
      "dvarygin",
      "*"
    )

  val daoConf =
    YqlLagDaoConfig(
      "home/verticals/broker/test/warehouse/holocron/realty/full/events/1d",
      "*",
      "home/logfeller/logs/vertis-event-log/1h",
      "STABLE",
      "home/verticals/broker/prod/warehouse/auto/images/meta/1d",
      "//home/verticals/moderation/user-clustering/clusters/auto.ru/last"
    )

  val executorF = YqlQueryExecutorImpl.initialize(config)

  "YqlUserClusteringLagDaoImpl" should {
    "get lag" in {
      val getLag =
        for {
          executor <- executorF
          dao = new YqlUserClusteringLagDaoImpl(executor, daoConf)
          lag <- dao.lag
        } yield lag

      val result = getLag.await
      println(result)
    }
  }
}
