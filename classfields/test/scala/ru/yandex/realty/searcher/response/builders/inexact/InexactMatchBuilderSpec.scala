package ru.yandex.realty.searcher.response.builders.inexact

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.search.inexact.InexactMatching
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.vertis.protobuf.ProtoInstanceProvider._
import ProtoHelper._
import ru.yandex.realty.model.offer.ApartmentImprovements
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class InexactMatchBuilderSpec
  extends WordSpec
  //    with MockFactory
  with Matchers {

  "InexactMatchBuilder" should {
    "return default InexactMatching in case of empty SearchQuery" in new OfferBuilderContextFixture {
      val searchQuery = new SearchQuery()
      private val expected: InexactMatching = InexactMatching.getDefaultInstance
      InexactMatchBuilder.build(searchQuery, dummyOfferBuilderContext) shouldBe expected
    }

    "return InexactMatching object filled according query and offer " in new OfferBuilderContextFixture {
      private val improvements =
        Set(ApartmentImprovements.WASHING_MACHINE, ApartmentImprovements.REFRIGERATOR)
      val context = dummyOfferBuilderContext.copy(improvements = improvements.asJava)
      val searchQuery = new SearchQuery()
      searchQuery.setPrice(ru.yandex.realty.util.Range.create(100f, 500f))
      searchQuery.setTimeToMetro(10)
      searchQuery.setHasRefrigerator(true)
      searchQuery.setHasWashingMachine(true)
      searchQuery.setHasTelevision(true)
      searchQuery.setPrice(ru.yandex.realty.util.Range.create(100f, 500f))

      (context.metro.getTimeToMetro _).expects().returning(5)
      (context.offer.getTemporaryPrice _).expects().returning(null)

      val expected =
        """{
          |"price" :{
          |  "inexact" : {
          |    "value": 50.0,
          |    "diff": 50.0,
          |    "trend": "LESS"
          |  }
          |},
          |has_television : {
          |  absent :{
          |  }
          |}
          |}""".stripMargin
          .toProto[InexactMatching]

      InexactMatchBuilder.build(searchQuery, context) shouldBe expected

    }
  }
}
