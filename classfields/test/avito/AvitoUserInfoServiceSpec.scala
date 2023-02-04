package auto.dealers.multiposting.logic.test.avito

import com.google.protobuf.timestamp.Timestamp
import common.scalapb.ScalaProtobuf._

import java.time.{Instant, OffsetDateTime}
import common.zio.sttp.endpoint.Endpoint
import io.circe.syntax._
import auto.dealers.multiposting.clients.avito.AvitoClient
import auto.dealers.multiposting.clients.avito.model._
import auto.dealers.multiposting.model
import auto.dealers.multiposting.model.ClientId
import auto.dealers.multiposting.logic.auth.CredentialsService
import auto.dealers.multiposting.logic.avito.AvitoUserInfoService
import auto.dealers.multiposting.logic.avito.AvitoUserInfoService._
import ru.auto.multiposting.wallet_model.AvitoTariff
import common.zio.logging.Logging
import common.zio.sttp.Sttp
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.{Header, Method}
import zio.clock.Clock
import zio.test.Assertion._
import zio.test._
import zio.{IO, ZIO, ZLayer}

object AvitoUserInfoServiceSpec extends DefaultRunnableSpec {
  private val clientId1 = ClientId(42)

  private val avitoUser1 = model.AvitoUserId("42")

  val token1 = Token("granted_42", (Instant.now().getEpochSecond + 3000).toInt, "type 1")
  val token2 = Token("granted__", (Instant.now().getEpochSecond + 3000).toInt, "type 1")

  val balance = Balance(BigDecimal(1), BigDecimal(0))

  val now = OffsetDateTime.now()
  val nowSeconds = now.toInstant.getEpochSecond

  val nowTimestamp: Int => Timestamp = fluctuation => Timestamp.defaultInstance.withSeconds(nowSeconds + fluctuation)

  val tariff = TariffInfo(
    current = Info(
      isActive = true,
      startTime = nowSeconds - 1000,
      closeTime = Some(nowSeconds + 1000),
      packages = List(
        Package(
          remain = 999,
          total = 1000,
          categories = Nil,
          locations = Nil
        )
      ),
      level = "TARDIS",
      price = Price(
        originalPrice = 1111111111,
        price = Int.MaxValue
      ),
      bonus = 42
    ),
    scheduled = Info(
      isActive = true,
      startTime = nowSeconds - 1000,
      closeTime = Some(nowSeconds + 1000),
      packages = List(
        Package(
          remain = 999,
          total = 1000,
          categories = Nil,
          locations = Nil
        )
      ),
      level = "TARDIS",
      price = Price(
        originalPrice = 1111111111,
        price = Int.MaxValue
      ),
      bonus = 42
    )
  )

  val expectedAvitoTariff = AvitoTariff(
    timestamp = Some(toTimestamp(now)),
    name = "TARDIS",
    isActive = true,
    price = Int.MaxValue,
    originalPrice = 1111111111.0,
    bonus = 42,
    totalPlacements = 1000,
    remainingPlacements = 999,
    start = Some(nowTimestamp(-1000)),
    end = Some(nowTimestamp(+1000))
  )

  val sttpBackendStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case request
        if request.uri.path.mkString("/").endsWith(s"core/v1/accounts/${avitoUser1.value}/balance") &&
          request.method == Method.GET &&
          request.headers.contains(Header("Authorization", s"Bearer granted_${clientId1.value}")) =>
      Response.ok(balance.asJson.toString())

    case request
        if request.uri.path.mkString("/").endsWith("tariff/info/1") &&
          request.method == Method.GET &&
          request.headers.contains(Header("Authorization", s"Bearer granted_${clientId1.value}")) =>
      Response.ok(tariff.asJson.toString())
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AvitoUserInfoServiceSpec")(
      testM("get balance with fresh token") {
        for {
          resp <- AvitoUserInfoService.getBalance(clientId1)
        } yield assert(resp.rubles)(equalTo(balance.real.toDouble)) &&
          assert(resp.bonus)(equalTo(balance.bonus.toDouble))
      }.provideCustomLayer(createEnvironment(avitoUser1, token1)),
      testM("throw exception on wrong token") {
        assertM(AvitoUserInfoService.getBalance(clientId1).run)(
          fails(isSubtype[AvitoUserInfoException](anything))
        )
      }.provideCustomLayer(createEnvironment(avitoUser1, token2)),
      testM("get avito tariff info") {
        for {
          resp <- AvitoUserInfoService.getTariffInfo(clientId1)
        } yield assert(resp.name)(equalTo(expectedAvitoTariff.name)) &&
          assert(resp.isActive)(equalTo(expectedAvitoTariff.isActive)) &&
          assert(resp.price)(equalTo(expectedAvitoTariff.price)) &&
          assert(resp.originalPrice)(equalTo(expectedAvitoTariff.originalPrice)) &&
          assert(resp.bonus)(equalTo(expectedAvitoTariff.bonus)) &&
          assert(resp.start)(equalTo(expectedAvitoTariff.start)) &&
          assert(resp.end)(equalTo(expectedAvitoTariff.end)) &&
          assert(resp.totalPlacements)(equalTo(expectedAvitoTariff.totalPlacements)) &&
          assert(resp.remainingPlacements)(equalTo(expectedAvitoTariff.remainingPlacements))
      }.provideCustomLayer(createEnvironment(avitoUser1, token1))
    )

  private def createEnvironment(avitoUserId: model.AvitoUserId, token: Token) = {

    val credentialsService =
      ZLayer.succeed {
        new CredentialsService.Service {
          override def getAvitoCredentials(
              client: ClientId): IO[CredentialsService.CredentialsException, (model.AvitoUserId, Token)] = {
            ZIO.succeed((avitoUserId, token))
          }
        }
      }

    val avitoClient = Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpBackendStub) >>> AvitoClient.live

    avitoClient ++ credentialsService ++ Clock.live ++ Logging.live >>> AvitoUserInfoService.live
  }
}
