package ru.yandex.realty.cache.redis

import org.junit.runner.RunWith
import ru.yandex.realty.AsyncSpecBase
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.cache.couchbase.layout.UserOffersStatisticsKey
import ru.yandex.realty.model.response.statistics.{
  RenderableNameValue,
  RenderableStatisticsItem,
  RenderableStatisticsResult,
  RenderableStatsExt
}

@RunWith(classOf[JUnitRunner])
class UserOffersStatsRedisCodecSpec extends AsyncSpecBase {

  private val statsResult = RenderableStatisticsResult(
    Seq(
      RenderableStatisticsItem(
        offerType = "SELL",
        status = "ACTIVE",
        hasActiveProducts = Some(true),
        fromFeed = Some(true),
        category = Some("HOUSE"),
        count = 4,
        isClusterHead = Some(true),
        cadastrReportStatus = None,
        roomsTotal = Some("2"),
        dealStatus = Some("SALE")
      )
    ),
    RenderableStatsExt(
      Seq(
        RenderableNameValue("RAISING", 1),
        RenderableNameValue("PROMOTION", 2)
      )
    )
  )

  "UserOffersStatsRedisCodec" should {

    "encode/decode simple key" in {
      val key = UserOffersStatisticsKey("123456789", Set.empty)
      val res = UserOffersStatsRedisCodec.decodeKey(
        UserOffersStatsRedisCodec.encodeKey(key)
      )
      res should equal(key)
    }

    "encode/decode  key" in {
      val key = UserOffersStatisticsKey("123456789", Set("15678", "56789"))
      UserOffersStatsRedisCodec.decodeKey(
        UserOffersStatsRedisCodec.encodeKey(key)
      ) should equal(key)
    }
  }

  "encode/decode value" in {
    UserOffersStatsRedisCodec.decodeValue(
      UserOffersStatsRedisCodec.encodeValue(statsResult)
    ) should equal(statsResult)
  }

}
