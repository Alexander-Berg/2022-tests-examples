package ru.yandex.vertis.billing.banker.tasks

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.service.{EpochService, ReceiptApi, ReceiptService}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.billing.banker.model.gens.{receiptGen, Producer}
import org.mockito.Mockito.{reset, times, verify}
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.model.{Epoch, Receipt}
import ru.yandex.vertis.billing.banker.util.DateTimeUtils
import ru.yandex.vertis.billing.banker.tasks.ReceiptCommitTaskSpec.EpochServiceMock

import scala.concurrent.{ExecutionContext, Future}

class ReceiptCommitTaskSpec
  extends Matchers
  with AnyWordSpecLike
  with MockitoSupport
  with AsyncSpecBase
  with BeforeAndAfterEach {

  val receiptService = mock[ReceiptService]

  def mockGetReceipts(receipts: Iterable[Receipt]): Unit =
    when(receiptService.get(?)(?)).thenReturn(Future.successful(receipts)): Unit

  val receiptApi = mock[ReceiptApi]

  def mockCommit(): Unit =
    when(receiptApi.commit(?)).thenReturn(Future.successful(())): Unit

  def mockCommit(receipt: Receipt, throwable: Throwable): Unit = {
    stub(receiptApi.commit(_: Receipt)) {
      case r if r == receipt =>
        Future.failed(throwable)
      case _ =>
        Future.successful(())
    }
    ()
  }

  val epochService = EpochServiceMock()

  var lastCreateTime = DateTimeUtils.now()

  def nextCreateTime(): DateTime =
    lastCreateTime.plusMinutes(1)

  def receiptsWithCreateTime(count: Int): Iterable[Receipt] =
    receiptGen().next(count).map { r =>
      r.copy(createTime = Some(nextCreateTime()))
    }

  override def beforeEach(): Unit = {
    reset[Any](receiptService, receiptApi)
    super.beforeEach()
  }

  val receiptCommitTask = new ReceiptCommitTask(receiptApi, receiptService, epochService)

  "ReceiptCommitTaskSpec" should {

    "process all" in {
      val count = 100
      val receipts = receiptsWithCreateTime(count)
      val expected = receipts.flatMap(_.createTime).map(_.getMillis).max
      mockGetReceipts(receipts)
      mockCommit()
      receiptCommitTask.execute().await
      verify(receiptService).get(?)(?)
      verify(receiptApi, times(count)).commit(?)
      epochService.get("test").await shouldBe expected
    }

    "correctly fail if commit fail" in {
      val count = 100
      val receipts = receiptsWithCreateTime(count)
      val successCount = Gen.choose(2, count - 1).next
      val successReceipts = receipts.take(successCount)
      val failReceipt = receipts.drop(successCount).head
      val expected = successReceipts.flatMap(_.createTime).map(_.getMillis).max
      mockGetReceipts(receipts)
      mockCommit(failReceipt, new IllegalArgumentException("test"))
      intercept[IllegalArgumentException] {
        receiptCommitTask.execute().await
      }
      verify(receiptService).get(?)(?)
      verify(receiptApi, times(successCount + 1)).commit(?)
      epochService.get("test").await shouldBe expected
    }
  }
}

object ReceiptCommitTaskSpec {

  case class EpochServiceMock()(implicit val ec: ExecutionContext) extends EpochService {

    var value: Option[Epoch] = None

    override def get(marker: String): Future[Epoch] = Future {
      value.getOrElse(0L)
    }

    override def set(marker: String, epoch: Epoch): Future[Unit] = Future {
      value = Some(epoch)
    }

    def reset(): Unit = value = None

  }

}
