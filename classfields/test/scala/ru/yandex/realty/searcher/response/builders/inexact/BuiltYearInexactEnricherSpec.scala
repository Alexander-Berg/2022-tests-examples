package ru.yandex.realty.searcher.response.builders.inexact

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.search.inexact.{BuiltYearInexact, HasWashingMachineInexact, InexactMatch, InexactMatching}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.vertis.protobuf.ProtoInstanceProvider._
import ProtoHelper._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BuiltYearInexactEnricherSpec extends WordSpec with Matchers {

  val searchQuery = new SearchQuery()
  searchQuery.setBuiltYearMin(2000)

  "BuiltYearInexactEnricherTest" should {

    "provideIfApply when only one threshold value is not null" in {
      val res = BuiltYearInexactEnricher.provideIfApply(searchQuery)
      res should not be empty
    }

    "checkAndEnrich" in new OfferBuilderContextFixture {
      val enricher: InexactMatchFieldEnricher = BuiltYearInexactEnricher.provideIfApply(searchQuery).get
      (dummyOfferBuilderContext.building.getBuiltYear _).expects().returning(1990)

      val builder = InexactMatching.newBuilder()
      enricher.checkAndEnrich(builder, dummyOfferBuilderContext)

      val expected =
        """{
          |  "inexact": {
          |    "value": 1990,
          |    "diff":  10,
          |    "trend": "LESS"
          |  }
          |}""".stripMargin
          .toProto[BuiltYearInexact]

      builder.build.getBuiltYear shouldBe expected
    }
  }
}
