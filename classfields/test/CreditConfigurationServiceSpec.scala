package auto.dealers.application.api.test

import common.palma.testkit.MockPalma
import ru.auto.api.api_offer_model.{Category, Section}
import auto.dealers.application.api.CreditConfigurationService
import ru.auto.application.palma.proto.application_palma_model.{
  CreditConfiguration,
  ExternalIntegration,
  ExternalSystem
}
import common.zio.logging.Logging
import zio.{Has, ZIO, ZLayer}
import zio.magic._
import zio.test.environment.TestEnvironment
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._

import scala.language.postfixOps

object CreditConfigurationServiceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("CreditConfigurationService")(
      testM("return empty config if not found") {
        assertM(CreditConfigurationService.fetchConfiguration(404, Category.CARS, Section.NEW))(isNone)
      },
      testM("fetch existing config") {
        checkNM(10)(Gens.CreditConfigurationGen) { config =>
          for {
            _ <- CreditConfigurationService.updateConfiguration(config)
            res <- CreditConfigurationService.fetchConfiguration(config.dealerId, config.category, config.section)
            _ <- MockPalma.clean[CreditConfiguration]
          } yield assert(res)(isSome(equalTo(config)))
        }
      },
      testM("update existing config") {
        checkNM(10)(Gens.CreditConfigurationGen) { config =>
          for {
            _ <- CreditConfigurationService.updateConfiguration(config)
            newVersion = config.copy(creditMinAmount = config.creditMaxAmount + 1)
            updated <- CreditConfigurationService.updateConfiguration(newVersion)
            fetched <- CreditConfigurationService.fetchConfiguration(config.dealerId, config.category, config.section)
            _ <- MockPalma.clean[CreditConfiguration]
          } yield assertTrue(updated == newVersion) && assertTrue(fetched.contains(updated))
        }
      },
      testM("preserve external integrations") {
        checkNM(10)(Gens.CreditConfigurationGen) { rawConfig =>
          val externalIntegrations =
            Seq(ExternalIntegration(source = ExternalSystem.ECREDIT, externalId = "42", enabled = true))
          val config = rawConfig.withExternalIntegrations(externalIntegrations)
          for {
            _ <- CreditConfigurationService.updateConfiguration(config)
            newVersion = config.copy(creditMinAmount = config.creditMaxAmount + 1)
            updated <- CreditConfigurationService.updateConfiguration(newVersion)
            fetched <- CreditConfigurationService.fetchConfiguration(config.dealerId, config.category, config.section)
            _ <- MockPalma.clean[CreditConfiguration]
          } yield assertTrue(updated == newVersion) && assertTrue(fetched.contains(updated))
        }
      }
    ) @@ sequential @@ before(MockPalma.clean[CreditConfiguration])
  }.provideCustomLayerShared(
    ZLayer.fromMagic[Has[MockPalma] with Has[CreditConfigurationService.Service]](
      Logging.live,
      CreditConfigurationDictionaryServiceTest.layer,
      CreditConfigurationService.live
    )
  )
}
