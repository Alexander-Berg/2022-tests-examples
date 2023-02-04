package ru.yandex.vertis.feedprocessor.autoru.scheduler.services.salesman

import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.dao.MainOffice7Dao
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.cabinet.CabinetClient
import ru.yandex.vertis.feedprocessor.util.DummyOpsSupport
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * @author pnaydenov
  */
class SalesmanServiceTest
  extends WordSpecBase
  with MockitoSupport
  with ScalaFutures
  with TestApplication
  with DummyOpsSupport {

  private val salesmanClient = mock[SalesmanClient]
  private val cabinetClient = mock[CabinetClient]
  private val office7Dao = mock[MainOffice7Dao]

  private val regionsWithoutTurboFeature = new Feature[String] {
    override val name = ""
    override def value = ""
  }

  private val regionsWithoutPremiumAndBoostFeature = new Feature[String] {
    override val name = ""
    override def value = ""
  }

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(1, Seconds))

  "SalesmanService.restrictIdsSize" should {
    val service = new SalesmanService(
      salesmanClient,
      cabinetClient,
      office7Dao,
      operationalSupport,
      regionsWithoutTurboFeature,
      regionsWithoutPremiumAndBoostFeature
    )

    val fetched = collection.mutable.ArrayBuffer.empty[Seq[Long]]

    def fetch(ids: Seq[Long]): Future[Seq[String]] =
      Future {
        fetched.synchronized {
          fetched += ids
        }
        ids.map(_.toString)
      }

    "correctly handle few ids" in {
      fetched.clear()
      val ids = (0L until 3).toList
      service.restrictBatchRequestSize(ids, identity[Long], fetch, 10)(_ ++ _).futureValue shouldEqual ids.map(
        _.toString
      )
    }

    "correctly handle exactly max ids" in {
      fetched.clear()
      val ids = (0L until 10).toList
      service.restrictBatchRequestSize(ids, identity[Long], fetch, 10)(_ ++ _).futureValue shouldEqual ids.map(
        _.toString
      )
    }

    "correctly handle many ids" in {
      fetched.clear()
      val ids = (0L until 100).toList
      service.restrictBatchRequestSize(ids, identity[Long], fetch, 10)(_ ++ _).futureValue shouldEqual ids.map(
        _.toString
      )
      service.restrictBatchRequestSize(-1L :: ids, identity[Long], fetch, 10)(_ ++ _).futureValue shouldEqual
        (-1 :: ids).map(_.toString)
    }

    "fail all result in case of single failure" in {
      val atomic = new AtomicInteger(0)

      def unsafeFetch(ids: Seq[Long]): Future[Seq[String]] =
        Future {
          if (atomic.incrementAndGet() == 7) throw new RuntimeException("Expected error")
          else if (atomic.incrementAndGet() > 7) throw new RuntimeException("Should stop requests after first failure")
          ids.map(_.toString)
        }

      val error = intercept[RuntimeException] {
        service.restrictBatchRequestSize((0L until 100).toList, identity[Long], unsafeFetch, 10)(_ ++ _).futureValue
      }
      error.getCause.getMessage shouldEqual "Expected error"
    }

    "pack all single offer goods in one batch" in {
      type Good = (Long, String)
      val fetched = collection.mutable.ArrayBuffer.empty[List[Good]]
      def fetch(ids: Seq[Good]): Future[Seq[Good]] =
        Future {
          fetched.synchronized {
            fetched += ids.toList
          }
          ids
        }

      val requests =
        List((1L, "good1"), (2L, "good1"), (1L, "good2"), (2L, "good2"), (1L, "good3"), (2L, "good3"), (3L, "good1"))
      service.restrictBatchRequestSize(requests, (g: Good) => g._1, fetch, 5)(_ ++ _).futureValue
      fetched should have size (2)
      fetched(0) shouldEqual List((1L, "good1"), (1L, "good2"), (1L, "good3"))
      fetched(1) shouldEqual List((2L, "good1"), (2L, "good2"), (2L, "good3"), (3L, "good1"))
    }
  }
}
