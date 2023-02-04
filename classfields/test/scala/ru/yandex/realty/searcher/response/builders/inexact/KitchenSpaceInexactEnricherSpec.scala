package ru.yandex.realty.searcher.response.builders.inexact

import com.google.protobuf.util.JsonFormat
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.model.offer.{AreaInfo, AreaUnit}
import ru.yandex.realty.proto.search.inexact.{InexactMatching, KitchenSpaceInexact}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.util

class KitchenSpaceInexactEnricherSpec extends WordSpec with MockFactory with Matchers {

  val searchQuery = new SearchQuery()
  private val kitchenSpaceRange: util.Range = util.Range.create(8.0f, 8.0f)
  searchQuery.setKitchenSpace(kitchenSpaceRange)

  val kitchenSpaceEnricher = new KitchenSpaceInexactEnricher(kitchenSpaceRange)

  val builder = InexactMatching.newBuilder()
  val parser = JsonFormat.parser()

  "KitchenSpaceInexactEnricher" should {
    "checkAndEnrich" in new OfferBuilderContextFixture {
      val contextWithSameKitchenSpace =
        dummyOfferBuilderContext.copy(kitchenSpace = AreaInfo.create(AreaUnit.SQUARE_METER, 8.0f))
      val builder = InexactMatching.newBuilder()
      private val default: KitchenSpaceInexact = KitchenSpaceInexact.getDefaultInstance
      kitchenSpaceEnricher.checkAndEnrich(builder, contextWithSameKitchenSpace).getKitchenSpace shouldBe default
    }
  }
}
