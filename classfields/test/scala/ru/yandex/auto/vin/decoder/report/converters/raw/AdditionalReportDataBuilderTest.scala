package ru.yandex.auto.vin.decoder.report.converters.raw

import auto.carfax.common.clients.vos.VosClient
import auto.carfax.common.utils.tracing.Traced
import cats.implicits.catsSyntaxOptionId
import org.mockito.Mockito._
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.auto.vin.decoder.extdata.catalog.cars.model.SuperGeneration
import ru.yandex.auto.vin.decoder.extdata.region.Tree
import ru.yandex.auto.vin.decoder.geo.GeobaseClient
import ru.yandex.auto.vin.decoder.manager.vin.VinHistoryScoresManager
import ru.yandex.auto.vin.decoder.manager.vin.catalog.UnifiedData
import ru.yandex.auto.vin.decoder.model.CatalogHolder
import ru.yandex.auto.vin.decoder.predict.PredictionManager
import ru.yandex.auto.vin.decoder.searcher.{HealthScoringHistogram, SearcherManager}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class AdditionalReportDataBuilderTest extends AsyncFunSuite with MockitoSupport {
  implicit val t: Traced = Traced.empty

  val catalogHolder = mock[CatalogHolder]
  val vinHistoryScoresManager = mock[VinHistoryScoresManager]
  val predictionManager = mock[PredictionManager]
  val tree = mock[Tree]
  val searchManager = mock[SearcherManager]
  val vosClient = mock[VosClient]
  val geobaseClient = mock[GeobaseClient]

  val additionalReportDataBuilder = new AdditionalReportDataBuilder(
    catalogHolder,
    vinHistoryScoresManager,
    predictionManager,
    tree,
    searchManager,
    vosClient,
    geobaseClient
  )

  val unifiedData =
    UnifiedData.Empty.copy(
      mark = Some("MERCEDES"),
      model = Some("C_KLASSE"),
      superGen = Some(SuperGeneration.apply(id = 2307688L, None, "name", false))
    )

  test("getHealthScoringHistogram for raw") {
    when(searchManager.getHealthScoringHistogram(?, ?, ?)(?))
      .thenReturn(Future.successful(HealthScoringHistogram(5, 0, 100).some))

    additionalReportDataBuilder.getHealthScoringHistogram(unifiedData).map { histogram =>
      verify(searchManager).getHealthScoringHistogram(?, ?, ?)(?)
      assert(histogram.nonEmpty)
    }
  }
}
