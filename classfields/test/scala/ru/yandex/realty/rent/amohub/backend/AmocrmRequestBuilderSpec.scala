package ru.yandex.realty.rent.amohub.backend

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.amohub.backend.manager.AmocrmRequestBuilder

@RunWith(classOf[JUnitRunner])
class AmocrmRequestBuilderSpec extends WordSpec with Matchers {

  "AmocrmRequestBuilder.buildSpecialPhone" should {
    "build successfully" in new Data {
      samples foreach {
        case (sample, expected) =>
          AmocrmRequestBuilder.buildSpecialPhone(sample) shouldEqual expected
      }
    }
  }

  trait Data {

    val samples = Seq(
      ("+79251282427", "+179251282427")
    )
  }
}
