package ru.yandex.realty.giraffic.service

import org.junit.runner.RunWith
import org.scalatest.Matchers
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.canonical.base.params.RequestParameter.{Rgid, Type}
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.giraffic.service.GroupPatternsBuilder.GroupPatternsBuilder
import ru.yandex.realty.giraffic.service.impl.patternBuilders.DeveloperGroupPatternsBuilder
import ru.yandex.realty.giraffic.service.mock.TestData
import ru.yandex.realty.model.message.ExtDataSchema.DeveloperGeoStatistic
import ru.yandex.realty.model.offer.OfferType
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.model.sites.Company
import ru.yandex.realty.sites.DeveloperGeoStatisticStorage.DeveloperId
import ru.yandex.realty.sites.{CompaniesStorage, DeveloperGeoStatisticStorage}
import ru.yandex.vertis.mockito.MockitoSupport
import zio._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}
import zio.test.{assertTrue, ZSpec}

@RunWith(classOf[ZTestJUnitRunner])
class DeveloperGroupPatternsBuilderSpec extends JUnitRunnableSpec with MockitoSupport with Matchers {

  private val developerId = 1L
  private val developerId2 = 2L

  private val companyMsk = {
    val c = new Company(developerId)
    c.setName("Застройщик MSK")
    c
  }

  private val companySpb = {
    val c = new Company(2)
    c.setName("Застройщик SPB")
    c
  }
  private val companies = Map(companyMsk.getId -> companyMsk, companySpb.getId -> companySpb)

  val developerGeoStatistic = DeveloperGeoStatistic.newBuilder().build()

  private val statisticByDeveloper: Map[DeveloperId, Map[Long, DeveloperGeoStatistic]] =
    Map(
      developerId -> Map(NodeRgid.MOSCOW -> developerGeoStatistic),
      developerId2 -> Map(NodeRgid.SPB -> developerGeoStatistic)
    )
  private val developerGeoStatisticStorage = mock[DeveloperGeoStatisticStorage]
  when(developerGeoStatisticStorage.statisticByDeveloper).thenReturn(statisticByDeveloper)

  private val developerGeoStatisticStorageProvider: Provider[DeveloperGeoStatisticStorage] =
    () => developerGeoStatisticStorage

  private val companiesStorage = mock[CompaniesStorage]
  when(companiesStorage.get(developerId)).thenReturn(companies(developerId))
  private val companiesStorageProvider: Provider[CompaniesStorage] = () => companiesStorage

  private lazy val serviceLayer: ULayer[GroupPatternsBuilder] = {
    val developerSearcher = TestData.regionServiceLayer ++
      ZLayer.succeed(developerGeoStatisticStorageProvider) ++
      ZLayer.succeed(companiesStorageProvider) >>> DeveloperSearcher.live

    developerSearcher >>> (new DeveloperGroupPatternsBuilder(_)).toLayer
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("DeveloperGroupPatternsBuilder should")(
      shouldFilterByRgid()
    )

  private def shouldFilterByRgid() =
    testM("should correctly return developers depend on rgid") {
      val request = Request.Raw(
        RequestType.NewbuildingSearch,
        Seq(
          Rgid(NodeRgid.MOSCOW),
          Type(OfferType.SELL)
        )
      )

      for {
        result <- GroupPatternsBuilder.buildGroupsPattern(request).provideLayer(serviceLayer)
      } yield {
        val links = result.groups
          .map(_.linksPattern)
          .flatMap(_.links)
          .filter(_.linkRequest.`type` == RequestType.Developer)
          .toSeq

        assertTrue(
          links.size == 1 && links.map(_.title).forall(t => t == s"от застройщика ${companyMsk.getName}")
        )
      }
    }
}
