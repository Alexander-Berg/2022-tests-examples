package auto.dealers.multiposting.logic.test.auth

import common.palma.Palma
import common.zio.sttp.endpoint.Endpoint
import io.circe.syntax._
import auto.dealers.multiposting.clients.avito.AvitoClient
import auto.dealers.multiposting.clients.avito.model._
import auto.dealers.multiposting.model.{AvitoUserId, ClientId}
import auto.dealers.multiposting.logic.auth.CredentialsService
import auto.dealers.multiposting.logic.auth.CredentialsService.CredentialsService
import auto.dealers.multiposting.logic.testkit.palma.AvitoTokenMock
import common.zio.logging.Logging
import common.zio.sttp.Sttp
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.Method
import zio._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object CredentialsServiceSpec extends DefaultRunnableSpec {
  private val clientId1 = ClientId(42)
  private val clientId2 = ClientId(43)
  private val clientId3 = ClientId(44)

  private val avitoUserId1 = AvitoUserId(42.toString)
  private val avitoUserId2 = AvitoUserId(43.toString)
  private val avitoUserId3 = AvitoUserId(44.toString)

  private val expectedToken1 = Token("granted_42", (Instant.now().getEpochSecond + 100000).toInt, "type 1")
  private val expectedToken2 = Token("granted_43", (Instant.now().getEpochSecond + 2000).toInt, "type 4")
  private val expectedToken3 = Token("granted_44", (Instant.now().getEpochSecond + 2000).toInt, "type 11")

  val token = Token("granted_43", (Instant.now().getEpochSecond + 2000).toInt, "type 4")
  val token2 = Token("granted_44", (Instant.now().getEpochSecond + 2000).toInt, "type 11")

  val balance = Balance(BigDecimal(1), BigDecimal(0))

  val sttpBackendStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case request
        if request.uri.path.mkString("/").endsWith("token/") &&
          request.method == Method.GET &&
          request.uri.paramsMap("client_id") == clientId2.value.toString =>
      Response.ok(token.asJson.toString())
    case request
        if request.uri.path.mkString("/").endsWith("token/") &&
          request.method == Method.GET &&
          request.uri.paramsMap("client_id") == clientId3.value.toString =>
      Response.ok(token2.asJson.toString())
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("CredentialsServiceSpec")(
      testM("get balance with existed token") {
        for {
          (avitoUserId, token) <- CredentialsService.getAvitoCredentials(clientId1)
        } yield assert(avitoUserId)(equalTo(avitoUserId1)) &&
          assert(token.accessToken)(equalTo(expectedToken1.accessToken)) &&
          assert(token.tokenType)(equalTo(expectedToken1.tokenType))
      }.provideCustomLayer(createEnvironment),
      testM("get balance with new token") {
        for {
          (avitoUserId, token) <- CredentialsService.getAvitoCredentials(clientId2)
        } yield assert(avitoUserId)(equalTo(avitoUserId2)) &&
          assert(token.accessToken)(equalTo(expectedToken2.accessToken)) &&
          assert(token.tokenType)(equalTo(expectedToken2.tokenType))
      }.provideCustomLayer(createEnvironment),
      testM("get balance with expired token") {
        for {
          (avitoUserId, token) <- CredentialsService.getAvitoCredentials(clientId3)
        } yield assert(avitoUserId)(equalTo(avitoUserId3)) &&
          assert(token.accessToken)(equalTo(expectedToken3.accessToken)) &&
          assert(token.tokenType)(equalTo(expectedToken3.tokenType))
      }.provideCustomLayer(createEnvironment)
    )
  } @@ sequential

  private def createEnvironment: ZLayer[Any, TestFailure[Palma.PalmaError], CredentialsService] = {
    val avitoClient = (
      Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpBackendStub)
    ) >>> AvitoClient.live

    val layer = Logging.live ++ AvitoTokenMock.layer ++ avitoClient >>> CredentialsService.live

    layer.mapError(TestFailure.fail)
  }

}
