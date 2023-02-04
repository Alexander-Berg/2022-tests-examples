package auto.dealers.multiposting.clients.avito.test

import common.zio.sttp.endpoint.Endpoint
import auto.dealers.multiposting.clients.avito.AvitoClient
import auto.dealers.multiposting.clients.avito.model._
import auto.dealers.multiposting.model._
import common.zio.sttp.Sttp
import zio._
import zio.test._
import zio.test.TestAspect.ignore

object AvitoClientSpec extends DefaultRunnableSpec {
  val avitoClientId: AvitoClientId = AvitoClientId("")
  val avitoUserId: AvitoUserId = AvitoUserId("")
  val avitoSecret: AvitoSecret = AvitoSecret("")
  val avitoOfferId: AvitoOfferId = AvitoOfferId("")

  private val getBalance = testM("getBalance") {
    for {
      tokenEither <- AvitoClient.authorize(avitoClientId, avitoSecret)
      token <- IO.fromEither(tokenEither)
      _ <- AvitoClient.getBalance(avitoUserId, token.copy(accessToken = "incorrect"))
    } yield assertCompletes
  }

  private val buyVas = testM("buyVas") {
    for {
      tokenEither <- AvitoClient.authorize(avitoClientId, avitoSecret)
      token <- IO.fromEither(tokenEither)
      _ <- AvitoClient.buyVas(avitoUserId, avitoOfferId, token, Product.Highlight)
    } yield assertCompletes
  }

  private val buyVasPackage = testM("buyVasPackage") {
    for {
      tokenEither <- AvitoClient.authorize(avitoClientId, avitoSecret)
      token <- IO.fromEither(tokenEither)
      _ <- AvitoClient.buyVasPackage(avitoUserId, avitoOfferId, token, Product.X2_1)
    } yield assertCompletes
  }

  private val getTariffInfo = testM("getTariffInfo") {
    for {
      tokenEither <- AvitoClient.authorize(avitoClientId, avitoSecret)
      token <- IO.fromEither(tokenEither)
      _ <- AvitoClient.getTariffInfo(token)
    } yield assertCompletes
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = (suite("AvitoClient")(
    getBalance,
    buyVas,
    buyVasPackage,
    getTariffInfo
  ) @@ ignore).provideCustomLayerShared(
    (ZLayer.succeed(
      Endpoint(host = "api.avito.ru", port = 443, schema = "https")
    ) ++ Sttp.live.orDie) >>> AvitoClient.live
  )
}
