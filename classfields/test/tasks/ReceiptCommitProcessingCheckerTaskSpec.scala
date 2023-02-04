package ru.yandex.vertis.billing.banker.tasks

import com.codahale.metrics.health.{HealthCheck, HealthCheckRegistry}
import org.scalatest.BeforeAndAfterEach
import ru.yandex.common.monitoring.CompoundHealthCheckRegistry
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.Domains
import ru.yandex.vertis.billing.banker.model.Epoch
import ru.yandex.vertis.billing.banker.service.{EpochService, ReceiptService}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.billing.banker.model.Receipt
import ru.yandex.vertis.billing.banker.model.gens.{receiptGen, Producer}
import ru.yandex.vertis.billing.banker.util.DateTimeUtils
import org.mockito.Mockito.{reset, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import concurrent.duration.DurationInt
import scala.concurrent.Future

class ReceiptCommitProcessingCheckerTaskSpec
  extends Matchers
  with AnyWordSpecLike
  with MockitoSupport
  with AsyncSpecBase
  with BeforeAndAfterEach {

  val receiptService = mock[ReceiptService]

  def mockGetReceipts(receipts: Iterable[Receipt]): Unit =
    when(receiptService.get(?)(?)).thenReturn(Future.successful(receipts)): Unit

  val epochService = mock[EpochService]

  def mockEpochGet(epoch: Epoch): Unit =
    when(epochService.get(?)).thenReturn(Future.successful(epoch)): Unit

  var checkerOpt: Option[HealthCheck] = None

  private val healthChecks = {
    val registryMock = mock[HealthCheckRegistry]
    stub(registryMock.register(_: String, _: HealthCheck)) { case (_, checker) =>
      checkerOpt = Some(checker)
    }
    new CompoundHealthCheckRegistry(registryMock)
  }

  def receiptWithCreateTime(createTime: Epoch): Receipt = {
    receiptGen().next.copy(createTime = Some(DateTimeUtils.timeAt(createTime)))
  }

  override def beforeEach(): Unit = {
    reset[Any](epochService, receiptService)
    super.beforeEach()
  }

  val receiptCommitCheckerTask =
    new ReceiptCommitProcessingCheckerTask(receiptService, epochService)(Domains.AutoRu, ec, healthChecks)

  "ReceiptCommitCheckerTaskSpec" should {

    "be healthy" in {
      mockEpochGet(0L)
      mockGetReceipts(Iterable(receiptWithCreateTime(5.minute.toMillis)))
      receiptCommitCheckerTask.execute().await
      verify(epochService).get(?)
      verify(receiptService).get(?)(?)
      checkerOpt.isDefined shouldBe true
      checkerOpt.get.execute().isHealthy shouldBe true
    }

    "be warn" in {
      mockEpochGet(0L)
      mockGetReceipts(Iterable(receiptWithCreateTime(15.minute.toMillis)))
      receiptCommitCheckerTask.execute().await
      verify(epochService).get(?)
      verify(receiptService).get(?)(?)
      checkerOpt.isDefined shouldBe true
      checkerOpt.get.execute().isHealthy shouldBe false
    }

    "be danger" in {
      mockEpochGet(0L)
      mockGetReceipts(Iterable(receiptWithCreateTime(25.minute.toMillis)))
      receiptCommitCheckerTask.execute().await
      verify(epochService).get(?)
      verify(receiptService).get(?)(?)
      checkerOpt.isDefined shouldBe true
      checkerOpt.get.execute().isHealthy shouldBe false
    }

  }

}
