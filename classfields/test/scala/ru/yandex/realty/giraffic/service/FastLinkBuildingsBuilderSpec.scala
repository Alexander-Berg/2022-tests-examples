package ru.yandex.realty.giraffic.service

import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineMV
import org.junit.runner.RunWith
import ru.yandex.realty.giraffic.model.links.FastLink
import ru.yandex.realty.giraffic.service.FastLinkBuildingsBuilder.FastLinkBuildingsBuilder
import ru.yandex.realty.giraffic.service.mock.TestData
import ru.yandex.realty.giraffic.service.mock.TestData.MoscowSellApartmentArbat
import ru.yandex.realty.searcher.api.SearcherApi.{BuildingWithStats, BuildingWithStatsResponse}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.urls.router.model.ViewType
import ru.yandex.vertis.mockito.MockitoSupport
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}
import zio.{Task, ULayer, ZIO, ZLayer}

import scala.collection.JavaConverters._

@RunWith(classOf[ZTestJUnitRunner])
class FastLinkBuildingsBuilderSpec extends JUnitRunnableSpec with MockitoSupport {
  private val searcher = mock[RealtySearcher.Service]

  private def serviceLayer: ULayer[FastLinkBuildingsBuilder] = {
    ZLayer.succeed[RealtySearcher.Service](searcher) ++
      TestData.routerLive >>> FastLinkBuildingsBuilder.live
  }

  private def serviceCall(urlPath: String, viewType: ViewType): Task[Iterable[FastLink]] = {
    (for {
      builder <- ZIO.service[FastLinkBuildingsBuilder.Service]
      res <- builder.getFastLinks(urlPath, viewType, Some(30))
    } yield res).provideLayer(ZLayer.succeed[Traced](Traced.empty) ++ serviceLayer)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("FastLinkBuildingsBuilder")(
      testM("build fast links") {
        val buildings = Seq(
          buildBuildingWithStats(1, "1", 10),
          buildBuildingWithStats(2, "2", 5),
          buildBuildingWithStats(3, "3", 4),
          buildBuildingWithStats(4, "4", 3),
          buildBuildingWithStats(5, "5", 1)
        )
        when(searcher.countOffersByBuilding(MoscowSellApartmentArbat.request.paramsMap ++ Map("limit" -> List("30"))))
          .thenReturn(Task.succeed(buildBuildingWithStatsResponse(buildings)))

        val expected = Set(
          FastLink("Дом 1", "/moskva/kupit/kvartira/st-ulica-arbat-75348/dom-1-1/", refineMV[Positive](10)),
          FastLink("Дом 2", "/moskva/kupit/kvartira/st-ulica-arbat-75348/dom-2-2/", refineMV[Positive](5)),
          FastLink("Дом 3", "/moskva/kupit/kvartira/st-ulica-arbat-75348/dom-3-3/", refineMV[Positive](4))
        )
        serviceCall(MoscowSellApartmentArbat.parsePath, ViewType.Desktop)
          .map(result => assertTrue(result.toSet == expected))
      }
    )

  private def buildBuildingWithStatsResponse(buildings: Seq[BuildingWithStats]): BuildingWithStatsResponse = {
    BuildingWithStatsResponse
      .newBuilder()
      .addAllBuildings(buildings.asJava)
      .build()
  }

  private def buildBuildingWithStats(buildingId: Long, houseNumber: String, totalOffers: Int): BuildingWithStats = {
    BuildingWithStats
      .newBuilder()
      .setBuildingId(buildingId)
      .setHouseNumber(houseNumber)
      .setTotalOffers(totalOffers)
      .build()
  }
}
