package auto.dealers.dealer_stats.logic

import auto.dealers.dealer_stats.logic.DealerBalanceManager.DealerBalanceManager
import auto.dealers.dealer_stats.model.{ClientId, DailyBalance, DealerBalance}
import auto.dealers.dealer_stats.storage.dao.DealerBalanceDao
import auto.dealers.dealer_stats.storage.dao.DealerBalanceDao.DealerBalanceDao
import auto.dealers.dealer_stats.storage.testkit.InMemoryDealerBalanceDao
import zio.{Has, NonEmptyChunk, ZLayer}
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment
import zio.magic._

import java.time.{LocalDate, OffsetDateTime}

object DealerBalanceManagerLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = (suite("DealerBalanceManagerLive")(
    testM("no balances") {
      for {
        res <- DealerBalanceManager.dealerBalanceByDay(from, until, clientId)
      } yield assertTrue(res.isEmpty)
    },
    testM("fetch as usual") {
      for {
        _ <- DealerBalanceDao.insert(
          NonEmptyChunk(
            DealerBalance(clientId, offsetDateTime("2021-07-31"), 80),
            DealerBalance(clientId, offsetDateTime("2021-08-01"), 100),
            DealerBalance(clientId, offsetDateTime("2021-08-02"), 90),
            DealerBalance(clientId, offsetDateTime("2021-08-03"), 120)
          )
        )
        res <- DealerBalanceManager.dealerBalanceByDay(from, until, clientId)
      } yield assertTrue(
        res == List(
          DailyBalance(localDate("2021-08-01"), 80),
          DailyBalance(localDate("2021-08-02"), 100),
          DailyBalance(localDate("2021-08-03"), 90),
          DailyBalance(localDate("2021-08-04"), 120)
        )
      )
    },
    testM("no value at start of period") {
      for {
        _ <- DealerBalanceDao.insert(
          NonEmptyChunk(
            DealerBalance(clientId, offsetDateTime("2021-07-15"), 80),
            DealerBalance(clientId, offsetDateTime("2021-08-01"), 100),
            DealerBalance(clientId, offsetDateTime("2021-08-02"), 90),
            DealerBalance(clientId, offsetDateTime("2021-08-03"), 120)
          )
        )
        res <- DealerBalanceManager.dealerBalanceByDay(from, until, clientId)
      } yield assertTrue(
        res == List(
          DailyBalance(localDate("2021-08-01"), 80),
          DailyBalance(localDate("2021-08-02"), 100),
          DailyBalance(localDate("2021-08-03"), 90),
          DailyBalance(localDate("2021-08-04"), 120)
        )
      )
    },
    testM("plug the holes") {
      for {
        _ <- DealerBalanceDao.insert(
          NonEmptyChunk(
            DealerBalance(clientId, offsetDateTime("2021-07-31"), 80),
            DealerBalance(clientId, offsetDateTime("2021-08-03"), 120)
          )
        )
        res <- DealerBalanceManager.dealerBalanceByDay(from, until, clientId)
      } yield assertTrue(
        res == List(
          DailyBalance(localDate("2021-08-01"), 80),
          DailyBalance(localDate("2021-08-02"), 80),
          DailyBalance(localDate("2021-08-03"), 80),
          DailyBalance(localDate("2021-08-04"), 120)
        )
      )
    },
    testM("don't return balances if no data") {
      for {
        _ <- DealerBalanceDao.insert(
          NonEmptyChunk(
            DealerBalance(clientId, offsetDateTime("2021-08-03"), 120)
          )
        )
        res <- DealerBalanceManager.dealerBalanceByDay(from, until, clientId)
      } yield assertTrue(
        res == List(
          DailyBalance(localDate("2021-08-04"), 120)
        )
      )
    }
  ) @@ after(InMemoryDealerBalanceDao.clean)).provideCustomLayer {
    ZLayer
      .fromSomeMagic[TestEnvironment, Has[InMemoryDealerBalanceDao] with DealerBalanceDao with DealerBalanceManager](
        InMemoryDealerBalanceDao.test,
        DealerBalanceManager.live
      )
  }

  private val from = LocalDate.parse("2021-08-01")
  private val until = LocalDate.parse("2021-08-05")
  private val clientId = ClientId(20101)

  private def localDate(str: String): LocalDate = LocalDate.parse(str)
  private def offsetDateTime(str: String): OffsetDateTime = OffsetDateTime.parse(str + "T00:00:01+03:00")
}
