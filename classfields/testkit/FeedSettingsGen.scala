package ru.yandex.vertis.general.feed.model.testkit

import ru.yandex.vertis.general.feed.model._
import zio.random.Random
import zio.test.{Gen, Sized}

object FeedSettingsGen {

  val anyUrl = Gen.alphaNumericStringBounded(3, 10)

  def anyOneShotFeed(
      url: Gen[Random with Sized, String] = anyUrl): Gen[Random with Sized, OneShotFeed] = {
    url.map(OneShotFeed.apply)
  }

  val anyOneShotFeed: Gen[Random with Sized, OneShotFeed] = anyOneShotFeed()

  def anyRegularFeed(
      url: Gen[Random with Sized, String] = anyUrl): Gen[Random with Sized, RegularFeed] = {
    url.map(RegularFeed.apply)
  }

  val anyRegularFeed: Gen[Random with Sized, OneShotFeed] = anyOneShotFeed()

  val anyFeedSettings: Gen[Random with Sized, FeedSettings] = Gen.oneOf(anyOneShotFeed, anyRegularFeed)
}
