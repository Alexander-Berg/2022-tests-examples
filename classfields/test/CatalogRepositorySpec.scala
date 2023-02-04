package auto.common.manager.catalog.test

import auto.common.manager.catalog.{CatalogRepository, CatalogRepositoryLive}
import auto.common.manager.catalog.model.{CatalogFilter, CatalogNames}
import auto.common.manager.catalog.testkit.CatalogClientMock
import common.cache.memory.MemoryCache
import common.cache.memory.MemoryCache.Config
import common.scalapb.ScalaProtobuf
import org.apache.commons.io.IOUtils
import ru.auto.api.api_offer_model.Category
import ru.auto.catalog.model.api.api_model.{RawCatalog, RawCatalogFilter, RawFilterRequest}
import zio.ZLayer
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation._

object CatalogRepositorySpec extends DefaultRunnableSpec {

  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CatalogRepositorySpec")(
      testM("get human readable names") {
        val expectedCatalogFilter = RawFilterRequest(
          filters = Seq(
            RawCatalogFilter(
              mark = "HONDA",
              model = "CIVIC",
              superGen = "4569475",
              techParam = "20501785"
            )
          )
        )

        val catalogResponse = ScalaProtobuf
          .fromJson[RawCatalog] {
            IOUtils.toString(getClass.getResourceAsStream("/catalog_response.json"), "UTF-8")
          }

        val catalogMock = CatalogClientMock.GetRawCatalog(
          equalTo((Category.CARS, expectedCatalogFilter)),
          value(catalogResponse)
        )

        val catalogRepository =
          (ZLayer.succeed(Config(100, None)) >+> MemoryCache
            .live[CatalogFilter, CatalogNames] ++ catalogMock) >>> CatalogRepositoryLive.live

        val filter = CatalogFilter(
          mark = "HONDA",
          model = Some("CIVIC"),
          superGenId = Some(4569475L),
          techParamId = Some(20501785L)
        )

        val expected = CatalogNames(
          mark = "Honda",
          model = Some("Civic"),
          superGen = Some("VIII Рестайлинг"),
          techParam = Some("AMT 1.8 AMT (140 л.с.)")
        )

        assertM(CatalogRepository.getHumanNames(filter))(equalTo(expected))
          .provideCustomLayer(catalogRepository)
      }
    )

}
