package auto.dealers.multiposting.logic.test.avito

import auto.dealers.multiposting.clients.avito.model.{Product, Token, VasApplyResult, VasInfo, VasPackageApplyResult}
import auto.dealers.multiposting.model
import auto.dealers.multiposting.model.{AvitoOfferId, ClientId}
import auto.dealers.multiposting.logic.auth.CredentialsService
import auto.dealers.multiposting.logic.avito.AvitoProductService
import auto.dealers.multiposting.logic.avito.AvitoProductService.AvitoProductService
import common.zio.logging.Logging
import auto.dealers.multiposting.clients.avito.testkit.AvitoClientMock
import zio.{IO, ZIO, ZLayer}
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.magic._

import java.time.{Instant, OffsetDateTime}
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation._

object AvitoProductServiceSpec extends DefaultRunnableSpec {
  private val avitoOfferId = AvitoOfferId("adcervs-123")
  private val emptyAvitoOfferId = AvitoOfferId("")
  private val userId = model.AvitoUserId("42")
  private val token = Token("granted_42", Instant.now().getEpochSecond.toInt, "type 1")

  private val credentialsService =
    ZLayer.succeed {
      new CredentialsService.Service {
        override def getAvitoCredentials(
            client: ClientId): IO[CredentialsService.CredentialsException, (model.AvitoUserId, Token)] = {
          ZIO.succeed((userId, token))
        }
      }
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AvitoProductService")(
      testM("Should not buy package if avito offer id is empty") {
        val products = Seq("xl")

        val env = ZLayer.fromSomeMagic[ZTestEnv, AvitoProductService](
          credentialsService,
          Logging.live,
          AvitoClientMock.empty,
          AvitoProductService.live
        )

        AvitoProductService
          .applyProduct(ClientId(12345), emptyAvitoOfferId, products)
          .provideLayer(env)
          .map(assert(_)(isUnit))
      },
      testM("Buying Vas is calling byVas method") {
        val products = Seq("xl")

        val env = ZLayer.fromSomeMagic[ZTestEnv, AvitoProductService](
          credentialsService,
          Logging.live,
          AvitoClientMock
            .BuyVas(
              equalTo((userId, avitoOfferId, token, Product.XL)),
              value(VasApplyResult(42, VasInfo("42", OffsetDateTime.now(), Seq())))
            )
            .toLayer,
          AvitoProductService.live
        )

        AvitoProductService
          .applyProduct(ClientId(12345), avitoOfferId, products)
          .provideLayer(env)
          .map(assert(_)(isUnit))
      },
      testM("Buying VasPackage is calling buyVasPackage method") {
        val products = Seq("x2_1")

        val env = ZLayer.fromSomeMagic[ZTestEnv, AvitoProductService](
          credentialsService,
          Logging.live,
          AvitoClientMock
            .BuyVasPackage(equalTo((userId, avitoOfferId, token, Product.X2_1)), value(VasPackageApplyResult(42)))
            .toLayer,
          AvitoProductService.live
        )

        AvitoProductService
          .applyProduct(ClientId(12345), avitoOfferId, products)
          .provideLayer(env)
          .map(assert(_)(isUnit))
      },
      testM("Log error with parsing product and ignore it") {
        val products = Seq("error", "error2")

        val env = ZLayer.fromSomeMagic[ZTestEnv, AvitoProductService](
          credentialsService,
          Logging.live,
          AvitoClientMock.empty,
          AvitoProductService.live
        )

        AvitoProductService
          .applyProduct(ClientId(12345), avitoOfferId, products)
          .provideLayer(env)
          .map(assert(_)(isUnit))
      }
    )
}
