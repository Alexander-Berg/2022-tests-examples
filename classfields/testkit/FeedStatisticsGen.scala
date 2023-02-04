package ru.yandex.vertis.general.feed.model.testkit

import ru.yandex.vertis.general.feed.model.FeedStatistics
import zio.test.magnolia.DeriveGen

object FeedStatisticsGen {
  val any = DeriveGen[FeedStatistics]
}
