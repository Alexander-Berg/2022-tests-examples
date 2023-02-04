package auto.dealers.application.scheduler.test.s3

import auto.common.clients.salesman.testkit.SalesmanClientMock
import common.zio.s3edr.{S3EdrConfig, S3EdrUploader}
import ru.auto.api.api_offer_model.Category.CARS
import ru.auto.api.api_offer_model.Section.{NEW, USED}
import ru.auto.api.api_offer_model.{Category, Section}
import auto.dealers.application.model.ECreditId
import ru.auto.application.palma.proto.application_palma_model.{
  CreditApplicationSearchIndex,
  CreditConfiguration,
  CreditConfigurationContainer,
  ExternalIntegration,
  ExternalSystem
}
import auto.dealers.application.scheduler.s3.ConfigurationSearchIndexService.ApplicationsSearchIndexUploader
import auto.dealers.application.scheduler.s3.{ConfigurationSearchIndexService, IllegalTariffScopeException}
import auto.dealers.application.storage.ExternalClientIdRepository.ExternalClientIdFetchingError
import auto.dealers.application.storage.testkit.InMemoryExternalClientIdRepository
import ru.auto.salesman.tariffs.credit_tariffs.DealersWithActiveApplicationCredit.GroupedDealers
import ru.auto.salesman.tariffs.credit_tariffs.TariffScope
import common.zio.ops.prometheus.Prometheus
import zio.ZLayer
import zio.magic._
import zio.test.Assertion._
import zio.test.environment.testEnvironment
import zio.test.mock.Expectation._
import zio.test.{mock => _, _}

import java.net.SocketTimeoutException
import scala.collection.mutable

object ConfigurationSearchIndexServiceSpec extends DefaultRunnableSpec {

  import ApplicationsSearchIndexUploaderSpecOps._

  override def spec =
    suite("DefaultApplicationsSearchIndexUploaderSpec")(
      testM("filter configurations with irrelevant dealer_id") {
        ConfigurationSearchIndexService.fetchConfigurationsFilteredByActiveProducts
          .map(response => assert(response)(isEmpty))
      }.provideLayer {
        mockedEnv(
          Seq(buildConfiguration(1, CARS, USED)),
          Seq(GroupedDealers(TariffScope.CARS_USED, Seq("2")))
        )
      },
      testM("filter several configurations by one dealer with multiple products") {
        ConfigurationSearchIndexService.fetchConfigurationsFilteredByActiveProducts
          .map(response => assert(response.size)(equalTo(2)))
      }.provideLayer {
        mockedEnv(
          Seq(
            buildConfiguration(1, CARS, USED),
            buildConfiguration(1, CARS, NEW)
          ),
          Seq(
            GroupedDealers(TariffScope.CARS_USED, Seq("1")),
            GroupedDealers(TariffScope.CARS_NEW, Seq("1"))
          )
        )
      },
      testM("filter configurations but only category and section matched") {
        ConfigurationSearchIndexService.fetchConfigurationsFilteredByActiveProducts
          .map(response => assert(response.size)(equalTo(0)))
      }.provideLayer {
        mockedEnv(
          Seq(
            buildConfiguration(1, CARS, USED),
            buildConfiguration(1, CARS, NEW)
          ),
          Seq(
            GroupedDealers(TariffScope.CARS_USED, Seq("2")),
            GroupedDealers(TariffScope.CARS_NEW, Seq("2"))
          )
        )
      },
      testM("multiple dealers in tariff scope") {
        ConfigurationSearchIndexService.fetchConfigurationsFilteredByActiveProducts.map(response =>
          assert(response)(hasSize(equalTo(2)))
        )
      }.provideLayer {
        mockedEnv(
          Seq(
            buildConfiguration(1, CARS, NEW),
            buildConfiguration(2, CARS, NEW)
          ),
          Seq(
            GroupedDealers(TariffScope.CARS_NEW, Seq("1", "2"))
          )
        )
      },
      testM("multiple dealers in tariff scope") {
        assertM(ConfigurationSearchIndexService.fetchConfigurationsFilteredByActiveProducts.run)(
          fails(isSubtype[IllegalTariffScopeException](anything))
        )
      }.provideLayer {
        mockedEnv(
          Seq.empty,
          Seq(
            GroupedDealers(TariffScope.TARIFF_UNDEFINED, Seq("1", "2"))
          )
        )
      },
      testM("buildIndex with several credit configurations") {
        ConfigurationSearchIndexService.build
          .map(response => {
            val dealerIdToConfigurations = new mutable.HashMap[Int, CreditConfigurationContainer]()
            dealerIdToConfigurations.put(
              1,
              CreditConfigurationContainer(
                Seq(
                  buildConfiguration(1, CARS, USED),
                  buildConfiguration(1, CARS, NEW)
                )
              )
            )

            val expectedIndex = new CreditApplicationSearchIndex(dealerIdToConfigurations.toMap)
            assert(response)(equalTo(expectedIndex))
          })
      }.provideLayer {
        mockedEnv(
          Seq(
            buildConfiguration(1, CARS, USED),
            buildConfiguration(1, CARS, NEW)
          ),
          Seq(
            GroupedDealers(TariffScope.CARS_USED, Seq("1")),
            GroupedDealers(TariffScope.CARS_NEW, Seq("1"))
          )
        )
      },
      testM("buildIndex with external integration") {
        ConfigurationSearchIndexService.build
          .map(response => {
            val dealerIdToConfigurations = new mutable.HashMap[Int, CreditConfigurationContainer]()
            dealerIdToConfigurations.put(
              1,
              CreditConfigurationContainer(
                Seq(
                  buildConfiguration(
                    1,
                    CARS,
                    USED,
                    externalIntegrations =
                      Seq(ExternalIntegration(source = ExternalSystem.ECREDIT, externalId = "123", enabled = true))
                  ),
                  buildConfiguration(1, CARS, NEW)
                )
              )
            )

            val expectedIndex = new CreditApplicationSearchIndex(dealerIdToConfigurations.toMap)
            assertTrue(response == expectedIndex)
          })
      }.provideLayer {
        mockedEnv(
          Seq(
            buildConfiguration(
              1,
              CARS,
              USED,
              externalIntegrations = Seq(ExternalIntegration(source = ExternalSystem.ECREDIT, enabled = true))
            ),
            buildConfiguration(1, CARS, NEW)
          ),
          Seq(
            GroupedDealers(TariffScope.CARS_USED, Seq("1")),
            GroupedDealers(TariffScope.CARS_NEW, Seq("1"))
          ),
          externalIds = Map(1L -> ECreditId("123"))
        )
      },
      testM("buildIndex with missing external integration") {
        ConfigurationSearchIndexService.build
          .map(response => {
            val dealerIdToConfigurations = new mutable.HashMap[Int, CreditConfigurationContainer]()
            dealerIdToConfigurations.put(
              1,
              CreditConfigurationContainer(
                Seq(
                  buildConfiguration(
                    1,
                    CARS,
                    USED,
                    externalIntegrations =
                      Seq(ExternalIntegration(source = ExternalSystem.ECREDIT, externalId = "missing", enabled = true))
                  ),
                  buildConfiguration(1, CARS, NEW)
                )
              )
            )

            val expectedIndex = new CreditApplicationSearchIndex(dealerIdToConfigurations.toMap)
            assertTrue(response == expectedIndex)
          })
      }.provideLayer {
        mockedEnv(
          Seq(
            buildConfiguration(
              1,
              CARS,
              USED,
              externalIntegrations =
                Seq(ExternalIntegration(source = ExternalSystem.ECREDIT, externalId = "missing", enabled = true))
            ),
            buildConfiguration(1, CARS, NEW)
          ),
          Seq(
            GroupedDealers(TariffScope.CARS_USED, Seq("1")),
            GroupedDealers(TariffScope.CARS_NEW, Seq("1"))
          )
        )
      },
      testM("fail buildIndex on external client id system failure") {
        assertM(ConfigurationSearchIndexService.build.run)(
          fails(equalTo(ExternalClientIdFetchingError.InvalidId(1, null)))
        )
      }.provideLayer {
        mockedEnvWithFailedExternalIdsSystem(
          Seq(
            buildConfiguration(
              1,
              CARS,
              USED,
              externalIntegrations =
                Seq(ExternalIntegration(source = ExternalSystem.ECREDIT, externalId = "missing", enabled = true))
            ),
            buildConfiguration(1, CARS, NEW)
          ),
          Seq(
            GroupedDealers(TariffScope.CARS_USED, Seq("1")),
            GroupedDealers(TariffScope.CARS_NEW, Seq("1"))
          )
        )
      }
    )
}

object ApplicationsSearchIndexUploaderSpecOps {
  val TimeoutException = new SocketTimeoutException()

  def mockedEnv(
      configurations: Seq[CreditConfiguration],
      groupedDealers: Seq[GroupedDealers],
      hasExceptionRisen: Boolean = false,
      externalIds: Map[Long, ECreditId] = Map.empty) =
    ZLayer.fromMagic[ApplicationsSearchIndexUploader](
      SalesmanClientMock.GetDealersWithActiveCreditApplicationProduct(value(groupedDealers)),
      CreditConfigurationDictionaryServiceTest.buildLayer(configurations, hasExceptionRisen),
      ZLayer.succeed(new S3EdrConfig("test", "test", "test", "test", "test", 2)),
      S3EdrUploader.live[CreditApplicationSearchIndex],
      ZLayer.succeed(externalIds) >>> InMemoryExternalClientIdRepository.test,
      testEnvironment,
      Prometheus.live,
      ConfigurationSearchIndexService.live
    )

  def mockedEnvWithFailedExternalIdsSystem(
      configurations: Seq[CreditConfiguration],
      groupedDealers: Seq[GroupedDealers],
      hasExceptionRisen: Boolean = false) =
    ZLayer.fromMagic[ApplicationsSearchIndexUploader](
      SalesmanClientMock.GetDealersWithActiveCreditApplicationProduct(value(groupedDealers)),
      CreditConfigurationDictionaryServiceTest.buildLayer(configurations, hasExceptionRisen),
      ZLayer.succeed(new S3EdrConfig("test", "test", "test", "test", "test", 2)),
      S3EdrUploader.live[CreditApplicationSearchIndex],
      ZLayer.succeed[ExternalClientIdFetchingError](
        ExternalClientIdFetchingError.InvalidId(1, null)
      ) >>> InMemoryExternalClientIdRepository.failing,
      testEnvironment,
      Prometheus.live,
      ConfigurationSearchIndexService.live
    )

  def buildConfiguration(
      dealerId: Int,
      category: Category,
      section: Section,
      externalIntegrations: Seq[ExternalIntegration] = Seq.empty): CreditConfiguration =
    CreditConfiguration.of(
      id = (dealerId, category, section).hashCode().toString,
      creditTermValues = Seq(0),
      creditDefaultTerm = 1,
      creditAmountSliderStep = 1,
      creditMinAmount = 1,
      creditMaxAmount = 1,
      creditMinRate = 1.00,
      creditStep = 1,
      creditOfferInitialPaymentRate = 1.00,
      dealerId = dealerId,
      category = category,
      section = section,
      externalIntegrations = externalIntegrations
    )
}
