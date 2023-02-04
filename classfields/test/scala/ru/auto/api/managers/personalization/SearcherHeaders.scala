package ru.auto.api.managers.personalization

import org.scalatest.OptionValues
import ru.auto.api.BaseSpec
import ru.auto.api.managers.personalization.PersonalizationManager.BigBrotherProfile.extractCountersValues
import ru.yandex.proto.crypta.Profile

class SearcherHeaders extends BaseSpec with OptionValues {
  "produce valid map" in {
    println(SearcherCounterHeaders.countersValuesMap)

    SearcherCounterHeaders.countersValuesMap.values.flatten.size shouldBe SearcherCounterHeaders.headers.size

    val tuples = SearcherCounterHeaders.countersValuesMap.toSeq.flatMap {
      case (keyword, keys) => keys.map(k => (keyword, k))
    }

    val produceSequentialResult = tuples.zip(SearcherCounterHeaders.headers).forall {
      case (value, string) =>
        SearcherCounterHeaders.calculateMapping(string) == value
    }

    produceSequentialResult shouldBe (true)
  }

  "calculate test counter" in {
    val counter = "N_BB_COUNTER#1197#CARD_VIEW#A0"
    SearcherCounterHeaders.calculateMapping(counter) shouldBe ((1197, 300))
  }

  "extract from crypta" in {
    val profile = Profile.newBuilder()
    profile
      .addCountersBuilder()
      .setCounterId(1197)
      .addKey(301)
      .addKey(304)
      .addKey(401)
      .addKey(406)
      .addValue(4.00012)
      .addValue(1.0)
      .addValue(1.0)
      .addValue(1.0)

    extractCountersValues(profile.build()).take(11) shouldBe
      Seq("0", "0", "0", "4.00012", "0", "0", "1.0", "0", "0", "1.0", "1.0")

  }

  "extract derived features" in {
    SearcherCounterHeaders.getDerivedFeatures(Map(1201 -> Map(301L -> 1d, 302L -> 3d))) shouldBe Seq("0.75")
    SearcherCounterHeaders.getDerivedFeatures(Map(1201 -> Map(301L -> 0d, 302L -> 0d))) shouldBe Seq("0")
    SearcherCounterHeaders.getDerivedFeatures(Map.empty) shouldBe Seq("0")
  }

  "extract derived features from crypta" in {
    val profile = Profile.newBuilder()
    profile
      .addCountersBuilder()
      .setCounterId(1349)
      .addKey(404)
      .addValue(4.00012)

    profile
      .addCountersBuilder()
      .setCounterId(1201)
      .addKey(301)
      .addKey(302)
      .addValue(2.0)
      .addValue(2.0)

    extractCountersValues(profile.build()).takeRight(5) shouldBe
      Seq("0", "0", "0", "4.00012", "0.5")

  }

}
