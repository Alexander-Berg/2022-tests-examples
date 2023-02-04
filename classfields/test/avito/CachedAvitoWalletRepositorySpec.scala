package auto.dealers.multiposting.logic.test.avito

import com.google.protobuf.timestamp.Timestamp
import common.scalapb.ScalaProtobuf._
import auto.dealers.multiposting.cache.Cache
import auto.dealers.multiposting.cache.Cache.Cache
import auto.dealers.multiposting.cache.testkit.InMemoryCache
import auto.dealers.multiposting.logic.avito.AvitoUserInfoService.AvitoUserInfoHttpException
import auto.dealers.multiposting.logic.avito.CachedAvitoUserInfoService._
import auto.dealers.multiposting.logic.avito.{AvitoUserInfoService, CachedAvitoUserInfoService}
import auto.dealers.multiposting.logic.test.avito.AvitoUserInfoServiceSpec.nowSeconds
import auto.dealers.multiposting.logic.testkit.avito.AvitoUserInfoServiceMock
import auto.dealers.multiposting.model.ClientId
import ru.auto.multiposting.wallet_model.{AvitoTariff, AvitoWalletBalance}
import common.zio.logging.Logging
import zio.ULayer
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

import java.time.OffsetDateTime
import java.util.Date

object CachedAvitoWalletRepositorySpec extends DefaultRunnableSpec {
  val client = ClientId(1)
  val now = OffsetDateTime.now()
  val nowTimestamp: Int => Timestamp = fluctuation => Timestamp.defaultInstance.withSeconds(nowSeconds + fluctuation)
  def cacheTest: ULayer[Cache] = InMemoryCache.test

  private def cacheKey(method: CacheMethod, clientId: ClientId): String =
    s"${method.name}:${clientId.value}"

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CachedAvitoWalletBalanceRepositorySpec")(
      testM("get balance without cache and set cache") {
        val nowInSeconds = new Date().getTime / 1000
        val balanceRelevant = AvitoWalletBalance(Some(Timestamp(nowInSeconds)), 0, 0)
        val env = (cacheTest ++ Logging.live ++ Clock.live ++ AvitoUserInfoServiceMock
          .GetBalance(equalTo(client), value(balanceRelevant))
          .toLayer) >+> CachedAvitoUserInfoService.cachedLive

        (for {
          balance <- AvitoUserInfoService.getBalance(client)
          balanceInCache <- Cache.get[String, AvitoWalletBalance](cacheKey(BalanceMethod, client)).map(_.get)
        } yield assertTrue(balance == balanceRelevant && balanceInCache == balanceRelevant)).provideCustomLayer(env)
      },
      testM("get cached relevant balance") {
        val nowInSeconds = new Date().getTime / 1000
        val balanceRelevant = AvitoWalletBalance(Some(Timestamp(nowInSeconds)), 0, 0)
        val env =
          (cacheTest ++ Logging.live ++ Clock.live ++ AvitoUserInfoServiceMock.empty) >+> CachedAvitoUserInfoService.cachedLive

        (for {
          _ <- Cache.set[String, AvitoWalletBalance](cacheKey(BalanceMethod, client), balanceRelevant, None)
          balance <- AvitoUserInfoService.getBalance(client)
        } yield assertTrue(balance == balanceRelevant)).provideCustomLayer(env)
      },
      testM("get cached irrelevant balance and get normal one from api") {
        val nowInSeconds = new Date().getTime / 1000
        val thirtyMinutesAgoInSeconds = nowInSeconds - 30 * 60
        val balanceRelevant = AvitoWalletBalance(Some(Timestamp(nowInSeconds)), 0, 0)
        val balanceIrrelevant = AvitoWalletBalance(Some(Timestamp(thirtyMinutesAgoInSeconds)), 0, 0)
        val env = (cacheTest ++ Logging.live ++ Clock.live ++ AvitoUserInfoServiceMock
          .GetBalance(equalTo(client), value(balanceRelevant))
          .toLayer) >+> CachedAvitoUserInfoService.cachedLive

        (for {
          _ <- Cache.set[String, AvitoWalletBalance](cacheKey(BalanceMethod, client), balanceIrrelevant, None)
          balance <- AvitoUserInfoService.getBalance(client)
        } yield assertTrue(balance == balanceRelevant)).provideCustomLayer(env)
      },
      testM("return last cached value if errors on getting balance from api") {
        val nowInSeconds = new Date().getTime / 1000
        val thirtyMinutesAgoInSeconds = nowInSeconds - 30 * 60
        val balanceIrrelevant = AvitoWalletBalance(Some(Timestamp(thirtyMinutesAgoInSeconds)), 0, 0)
        val env = (cacheTest ++ Logging.live ++ Clock.live ++ AvitoUserInfoServiceMock
          .GetBalance(equalTo(client), failure(AvitoUserInfoHttpException(client, "Some error occured")))
          .toLayer) >+> CachedAvitoUserInfoService.cachedLive

        (for {
          _ <- Cache.set[String, AvitoWalletBalance](cacheKey(BalanceMethod, client), balanceIrrelevant, None)
          balance <- AvitoUserInfoService.getBalance(client)
        } yield assertTrue(balance == balanceIrrelevant)).provideCustomLayer(env)
      },
      testM("get tariff without cache and set cache") {
        val avitoTariff = AvitoTariff(
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
        val env = (cacheTest ++ Logging.live ++ Clock.live ++ AvitoUserInfoServiceMock
          .GetTariffInfo(equalTo(client), value(avitoTariff))
          .toLayer) >+> CachedAvitoUserInfoService.cachedLive

        (for {
          tariff <- AvitoUserInfoService.getTariffInfo(client)
          tariffInCache <- Cache.get[String, AvitoTariff](cacheKey(TariffInfoMethod, client)).map(_.get)
        } yield assertTrue(tariff == avitoTariff && tariffInCache == avitoTariff)).provideCustomLayer(env)
      },
      testM("get cached relevant balance") {
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
        val env =
          (cacheTest ++ Logging.live ++ Clock.live ++ AvitoUserInfoServiceMock.empty) >+> CachedAvitoUserInfoService.cachedLive

        (for {
          _ <- Cache.set[String, AvitoTariff](cacheKey(TariffInfoMethod, client), expectedAvitoTariff, None)
          avitoTariff <- AvitoUserInfoService.getTariffInfo(client)
        } yield assertTrue(avitoTariff == expectedAvitoTariff)).provideCustomLayer(env)
      }
    ) @@ sequential
}
