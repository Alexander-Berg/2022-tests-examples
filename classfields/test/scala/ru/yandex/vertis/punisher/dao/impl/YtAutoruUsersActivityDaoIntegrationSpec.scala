package ru.yandex.vertis.punisher.dao.impl

import org.junit.Ignore
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.config.{EventEodConfig, YqlAutoruUsersActivityConfig}
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.yql_utils.config.{YqlExecutorConfig, YqlQueryConfig}
import ru.yandex.vertis.quality.yql_utils.impl.YqlQueryExecutorImpl

import java.time.ZonedDateTime

@Ignore
class YtAutoruUsersActivityDaoIntegrationSpec extends BaseSpec {

  "YtAutoruUsersActivityDao" should {
    "yt goes brrrrrr" in {
      val config =
        YqlAutoruUsersActivityConfig(
          EventEodConfig(
            "home/verticals/broker/prod/warehouse/holocron/auto/full/cars/events/1d",
            "home/verticals/broker/prod/warehouse/holocron/auto/full/cars/eod/1d"
          ),
          EventEodConfig(
            "home/verticals/broker/prod/warehouse/holocron/auto/full/moto/events/1d",
            "home/verticals/broker/prod/warehouse/holocron/auto/full/moto/eod/1d"
          ),
          EventEodConfig(
            "home/verticals/broker/prod/warehouse/holocron/auto/full/trucks/events/1d",
            "home/verticals/broker/prod/warehouse/holocron/auto/full/trucks/eod/1d"
          ),
          "home/verticals/moderation/user-clustering/clusters/auto.ru/last"
        )

      val yqlConfig =
        YqlExecutorConfig(
          "jdbc:yql://yql.yandex.net:443?syntaxVersion=1",
          "awethon",
          "token",
          queryConfig = YqlQueryConfig.Empty
        )
      val executor = YqlQueryExecutorImpl.initialize[F](yqlConfig).await
      val dao = new YtAutoruUsersActivityDao(executor, config)

      val result =
        dao
          .clusterOffersActivity(
            1.to(50000).toSet[Int].map(_.toString),
            TimeInterval(ZonedDateTime.now().minusDays(14), ZonedDateTime.now())
          )(null)
          .await

      println(result.keys.size)
    }

  }

}
