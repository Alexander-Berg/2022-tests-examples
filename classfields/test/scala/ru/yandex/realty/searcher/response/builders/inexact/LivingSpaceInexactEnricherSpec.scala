package ru.yandex.realty.searcher.response.builders.inexact

import com.google.protobuf.util.JsonFormat
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.model.offer.{AreaInfo, AreaUnit}
import ru.yandex.realty.proto.search.inexact.{InexactMatching, LivingSpaceInexact}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.util
import ru.yandex.vertis.protobuf.ProtoInstanceProvider._

class LivingSpaceInexactEnricherSpec extends WordSpec with MockFactory with Matchers {

  import ProtoHelper._

  val searchQuery = new SearchQuery()
  private val livingSpaceRange: util.Range = util.Range.create(50.0f, 55.2f)
  searchQuery.setLivingSpace(livingSpaceRange)

  val livingSpaceEnricher = new LivingSpaceInexactEnricher(livingSpaceRange)

  val builder = InexactMatching.newBuilder()
  val parser = JsonFormat.parser()
  "LivingSpaceInexactEnricher" should {

    "checkAndEnrich" in new OfferBuilderContextFixture {
      val inexact =
        """{
          |  "inexact": {
          |   "diff": 2.8,
          |   "trend": "MORE",
          |   "unit": "AREA_UNIT_SQ_M",
          |   "value": 58.0
          |  }
          |}""".stripMargin
          .toProto[LivingSpaceInexact]

      val contextWithLivingSpaceGreaterThanMaxBorder =
        dummyOfferBuilderContext.copy(livingSpace = AreaInfo.create(AreaUnit.SQUARE_METER, 58.0f))
      val builder = InexactMatching.newBuilder()
      livingSpaceEnricher
        .checkAndEnrich(builder, contextWithLivingSpaceGreaterThanMaxBorder)
        .getLivingSpace shouldBe inexact
    }

  }
}
