package ru.yandex.realty.relevance

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import RelevanceModelDescription._

@RunWith(classOf[JUnitRunner])
class RelevanceModelDescriptionTest extends WordSpec with Matchers {

  "RelevanceModelDescriptionTest" should {

    "shortName" in {

      SellRealtime.shortName shouldBe "all"
      SellRealtimeExperiment.shortName shouldBe "all_experiment"

    }
  }
}
