package ru.yandex.vertis.general.wizard.scheduler.services.impl

import cats.instances.either._
import cats.instances.vector._
import cats.syntax.traverse._
import ru.yandex.vertis.general.wizard.model.converters.StockOfferRowConverter
import ru.yandex.vertis.general.wizard.model.{StockOfferCtrYtRow, StockOfferWithCtr, StockOfferYtRow}
import ru.yandex.vertis.general.wizard.scheduler.storage.OffersWithCtrDao
import ru.yandex.vertis.mockito.MockitoSupport
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Chunk, RIO, ZIO}

import java.time.Instant

object YtOffersWithCtrScanServiceSpec extends DefaultRunnableSpec with MockitoSupport {

  private trait TestEnv {

    protected val stockOffersTableResult: Chunk[StockOfferYtRow]
    protected val stockOffersCtrTableResult: Chunk[StockOfferCtrYtRow]

    private val offersWithCtrDao = {
      val dao = mock[OffersWithCtrDao]

      when(dao.getExportedOffersWithCtr).thenReturn(
        ZStream.fromChunk(
          for {
            stockOffer <- stockOffersTableResult
            stockOfferCtrFiltered = stockOffersCtrTableResult.filter(_.offerId == stockOffer.offerId)
            result <-
              if (stockOfferCtrFiltered.isEmpty)
                Chunk((stockOffer, StockOfferCtrYtRow(stockOffer.offerId, None, None)))
              else
                for {
                  correspondingStockOfferCtr <- stockOfferCtrFiltered
                } yield (stockOffer, correspondingStockOfferCtr)
          } yield result
        )
      )

      dao
    }

    protected val serviceToTest: YtOffersWithCtrScanService = new YtOffersWithCtrScanService(offersWithCtrDao)

    def testZ: RIO[Any, TestResult]
  }

  override def spec: ZSpec[TestEnvironment, Any] = suite("YtOffersWithCtrScanServiceSpec")(
    testM("stream returns offers with ctr, if there are matching ctr") {
      val test: TestEnv = new TestEnv {
        private val stockOffer1 = StockOfferYtRow(
          "Stock offer 1",
          "123",
          "123",
          "1",
          None,
          "https://example.com/img.jpg",
          "https://example.com/img.jpg",
          "https://example.com/img.jpg",
          List(),
          "321",
          isMordaApproved = false,
          isYanApproved = false,
          Some(true),
          List(),
          List(),
          "",
          Instant.parse("2021-08-23T15:00:00Z")
        )
        private val stockOffer2 = stockOffer1.copy(offerId = "2")

        private val stockOffer1Ctr = StockOfferCtrYtRow(
          "1",
          Some(10),
          Some(0)
        )

        override val stockOffersTableResult: Chunk[StockOfferYtRow] = Chunk(
          stockOffer1,
          stockOffer2
        )
        override val stockOffersCtrTableResult: Chunk[StockOfferCtrYtRow] = Chunk(
          stockOffer1Ctr
        )

        override def testZ: RIO[Any, TestResult] =
          for {
            scannedOffersWithCtr <- serviceToTest.stream.runCollect
            expectedOffersWithCtrEither =
              Vector(
                StockOfferRowConverter.toOfferMaybeWithCtr(stockOffer1, stockOffer1Ctr),
                StockOfferRowConverter.toOfferMaybeWithCtr(
                  stockOffer2,
                  StockOfferCtrYtRow(stockOffer2.offerId, None, None)
                )
              )
            eitherExpectedOffersWithCtr = expectedOffersWithCtrEither
              .sequence[Either[Throwable, *], StockOfferWithCtr]
            expectedOffersWithCtr <- ZIO.fromEither(eitherExpectedOffersWithCtr)
          } yield assert(scannedOffersWithCtr)(hasSameElements(expectedOffersWithCtr))
      }

      test.testZ
    }
  )
}
