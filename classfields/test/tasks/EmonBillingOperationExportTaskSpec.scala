package ru.yandex.vertis.billing.tasks

import billing.emon.Model.EventState
import com.typesafe.config.ConfigFactory
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.BillingEvent.BillingOperation
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.Dao.EmonBillingOperationTaskPayload
import ru.yandex.vertis.billing.dao.EmonBillingOperationTaskDao.NewTask
import ru.yandex.vertis.billing.dao.EmonEventDao.Event
import ru.yandex.vertis.billing.dao.gens.EmonEventGen
import ru.yandex.vertis.billing.dao.impl.jdbc.order.WithdrawHelper
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcEmonBillingOperationTaskDao, JdbcEmonEventDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{OrderGen, Producer}
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.billing.service.delivery.MessageDeliveryService
import ru.yandex.vertis.billing.service.metered.MeteredStub
import ru.yandex.vertis.billing.util.DateTimeUtils
import ru.yandex.vertis.billing.util.EmonUtils.RichEventState
import ru.yandex.vertis.billing.util.clean.{CleanableEmonBillingOperationTaskDao, CleanableEmonEventDao}
import ru.yandex.vertis.billing.util.mock.MessageDeliveryServiceMockBuilder
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.reflect.ClassTag

class EmonBillingOperationExportTaskSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with AsyncSpecBase
  with JdbcSpecTemplate
  with BeforeAndAfterEach {

  private val emonEventDao =
    new JdbcEmonEventDao(eventStorageDatabase) with CleanableEmonEventDao

  private val emonBillingOperationTaskDao =
    new JdbcEmonBillingOperationTaskDao(eventStorageDatabase) with CleanableEmonBillingOperationTaskDao

  override def beforeEach(): Unit = {
    emonEventDao.clean().get
    emonBillingOperationTaskDao.clean().get
    super.beforeEach()
  }

  "EmonBillingOperationExportTask" should {
    "do nothing without tasks" in {
      val sender = MessageDeliveryServiceMockBuilder().build
      val task = new EmonBillingOperationExportTask(emonBillingOperationTaskDao, emonEventDao, sender) with MeteredStub
      runTask(task)
    }

    "export billing operation based on task payload" in {
      val sender = mock[MessageDeliveryService]
      val sent = new ConcurrentLinkedQueue[BillingOperation]()
      stub(sender.sendBatch(_: Seq[BillingOperation])(_: ClassTag[BillingOperation])) { case (operations, _) =>
        sent.addAll(operations.asJava)
        Future.unit
      }

      val events = EmonEventGen.next((EmonBillingOperationExportTask.TaskBatchSize * 1.5).intValue).toSeq
      val tasks = events.map(e => e -> billingOperationTaskGen(e).next).toMap

      emonEventDao.insert(events).get
      emonBillingOperationTaskDao.add(tasks.values.map(NewTask(_)).toSeq).get
      val exportTask = new EmonBillingOperationExportTask(emonBillingOperationTaskDao, emonEventDao, sender)
        with MeteredStub
      runTask(exportTask)

      emonBillingOperationTaskDao.stat.get.taskCount shouldBe 0
      sent.asScala.toSet shouldBe events.map(e => expectedBillingOperation(e.event, tasks(e))).toSet
    }

  }

  private def expectedBillingOperation(event: EventState, task: EmonBillingOperationTaskPayload): BillingOperation = {
    Conversions.toBillingOperation(
      task.getTransaction,
      task.getTransactionEpoch,
      task.getOrderState,
      event,
      event.price,
      task.getTransaction.getAmount,
      EmonBillingOperationExportTask.getProjectDomain(event.getEvent.getEventId.getProject)
    )
  }

  private def billingOperationTaskGen(event: Event): Gen[EmonBillingOperationTaskPayload] = {
    for {
      txPrice <- Gen.choose(0L, event.event.price)
      order <- OrderGen
    } yield {
      val b = EmonBillingOperationTaskPayload.newBuilder()
      b.setOrderState(
        Conversions.toOrderStateMessage(order.copy(id = event.event.orderId, owner = event.event.customerId))
      )
      val etr = EmonTransactionRequest(Seq(event.event), event.groupId)
      val withdraw = WithdrawHelper.buildWithdraw(etr, txPrice, DateTimeUtils.fromMillis(event.epoch))
      b.setTransaction(Conversions.toMessage(withdraw).get)
      b.setTransactionEpoch(event.epoch)
      b.build()
    }
  }

  private def runTask(task: EmonBillingOperationExportTask): Unit = {
    task.execute(ConfigFactory.empty()).futureValue
  }

}
