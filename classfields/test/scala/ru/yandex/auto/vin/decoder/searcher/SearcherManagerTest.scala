package ru.yandex.auto.vin.decoder.searcher

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.auto.searcher.filters.MarkModelFilters.MarkModelFiltersResultMessage
import auto.carfax.common.utils.protobuf.ProtobufConverterOps._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class SearcherManagerTest extends AsyncFunSuite with MockitoSupport {

  implicit val t: Traced = Traced.empty

  val searcherClient = mock[SearcherClient]
  val searcherManager = new SearcherManager(searcherClient)

  val mark = "MERCEDES"
  val model = "C_KLASSE"
  val generation = 3480545L

  test("getHealthScoringHistogram") {
    when(searcherClient.getCarMarkModelFilters(?, ?, ?)(?))
      .thenReturn(Future.successful {
        val builder = MarkModelFiltersResultMessage.newBuilder()
        val generationBuilder = builder
          .addMarkEntriesBuilder()
          .addModelsBuilder()
          .addSupergensBuilder()
          .setOffersCount(10)
        generationBuilder.getHealthScoreBuilder
          .setMin(64f.toFloatValue)
          .setMax(87f.toFloatValue)
        builder.build()
      })

    searcherManager.getHealthScoringHistogram(mark, model, generation).map { histogram =>
      assert(histogram.nonEmpty)
      assert(histogram.get.offerCount == 10)
      assert(histogram.get.minScore == 64)
      assert(histogram.get.maxScore == 87)
    }
  }
}
