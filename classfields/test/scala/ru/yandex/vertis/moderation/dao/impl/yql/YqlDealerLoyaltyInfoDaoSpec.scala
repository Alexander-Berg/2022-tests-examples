package ru.yandex.vertis.moderation.dao.impl.yql

import java.sql.DriverManager

import org.junit.Ignore
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.util.yql.YqlConfig
import ru.yandex.vertis.moderation.util.yt.YtDealerLoyaltyConfig
import ru.yandex.yql.YqlDriver

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

@Ignore("For manual running only")
class YqlDealerLoyaltyInfoDaoSpec extends SpecBase {

  DriverManager.registerDriver(new YqlDriver)

  val config =
    YqlConfig(
      url = "jdbc:yql://yql.yandex.net:443?syntaxVersion=1",
      user = "robot-vs-moderation",
      token = "<Your token here>"
    )

  val ytConfig =
    YtDealerLoyaltyConfig(
      totalInfoTablePath = "//home/verticals/moderation/checked_dealers/total_info",
      resolutionTablePath = "//home/verticals/moderation/checked_dealers/resolution",
      tmpPath = "//home/verticals/.tmp"
    )

  val yql = new YqlDealerLoyaltyInfoDao(config, ytConfig)(scala.concurrent.ExecutionContext.global)

  "" should {
    "" in {
      val map1 = Await.result(yql.getDealerLoyalty(false), FiniteDuration.apply(10L, "minutes"))
      val map2 = Await.result(yql.getDealerLoyalty(true), FiniteDuration.apply(10L, "minutes"))

      println(map1.size)
      println(map2.size)
      println(map1.take(5))
      println(map2.take(5))
      println(map1.keys)
      println(map2.keys)
    }
  }
}
