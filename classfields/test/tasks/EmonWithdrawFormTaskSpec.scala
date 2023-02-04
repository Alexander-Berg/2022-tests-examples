package ru.yandex.vertis.billing.tasks

import billing.CommonModel
import billing.emon.Model.{EventState, EventStateId, EventTypeNamespace}
import com.typesafe.config.ConfigFactory
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.SupportedServices
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.balance.model.Balance
import ru.yandex.vertis.billing.dao.EmonEventDao.Event
import ru.yandex.vertis.billing.dao.gens.ValidEmonEventStateGen
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDao
import ru.yandex.vertis.billing.dao.impl.jdbc.{
  JdbcCustomerDao,
  JdbcEmonBillingOperationTaskDao,
  JdbcEmonEventDao,
  JdbcSpecTemplate
}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{Producer, ProtobufMessageGenerators}
import ru.yandex.vertis.billing.service.OrderService
import ru.yandex.vertis.billing.service.impl.OrderServiceImpl
import ru.yandex.vertis.billing.service.metered.MeteredStub
import ru.yandex.vertis.billing.settings.BalanceSettings
import ru.yandex.vertis.billing.util.EmonUtils.RichEventState
import ru.yandex.vertis.billing.util.clean.{CleanableEmonBillingOperationTaskDao, CleanableEmonEventDao}
import ru.yandex.vertis.generators.BasicGenerators.readableString
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.protobuf.ProtoInstanceProvider.defaultInstanceByType

import scala.jdk.CollectionConverters._

class EmonWithdrawFormTaskSpec
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

  private val orderDao = new JdbcOrderDao(billingDualDatabase)

  private val customerDao = new JdbcCustomerDao(billingDatabase)

  private val balanceSettings =
    BalanceSettings(
      "realty",
      ConfigFactory
        .parseResources("application-test.conf")
        .getConfig("service.realty.balance")
    )

  private val balance = mock[Balance]

  private val realtyOrderService =
    new OrderServiceImpl(orderDao, balance, balanceSettings)
  private val domainOrderService = Map(SupportedServices.Realty -> realtyOrderService)

  private val task = new EmonWithdrawFormTask(emonEventDao, domainOrderService, emonBillingOperationTaskDao)
    with MeteredStub

  private val resource = PartnerRef("1")
  private val customer = CustomerHeader(CustomerId(1, None), resource)
  private val orderProperties = OrderProperties("Order 1", Some("Description 1"))
  private val income = 100000000L

  customerDao.create(customer).get

  private val orderId: OrderId = orderDao.create(customer.id, orderProperties).get.id

  orderDao
    .totalIncome(
      TotalIncomesFromBeginningsRequest(1, "1", orderId, income)
    )
    .get

  override def beforeEach(): Unit = {
    emonEventDao.clean().get
    emonBillingOperationTaskDao.clean().get
    super.beforeEach()
  }

  "EmonWithdrawFormTask" should {
    "do nothing without modified groups" in {
      runTask()
    }

    "form transaction for modified groups" in {
      val events =
        (1 to (10 + EmonWithdrawFormTask.GroupBatchSize)).map("group" + _).map(g => Event(supportedEventGen.next, g))

      emonEventDao.insert(events).get
      events.map(_.groupId).foreach(getTransaction(_, OrderTransactions.Withdraw) shouldBe None)
      runTask()
      events.map(_.groupId).foreach(getTransaction(_, OrderTransactions.Withdraw).isDefined shouldBe true)
    }

    "set transaction for processed group events" in {
      val events = Seq("group1", "group2", "group3", "group3").map(g => Event(supportedEventGen.next, g))

      emonEventDao.insert(events).get
      runTask()
      val updated = emonEventDao.getEvents(events.map(_.event.eventStateId)).get
      updated.map(_.copy(transactionId = None)).toSet shouldBe events.toSet
      updated.forall(e => e.transactionId.contains(e.groupId)) shouldBe true
    }

    "add billing operation task for processed groups" in {
      val events = Seq("group1", "group2").map(g => Event(supportedEventGen.next, g))

      emonEventDao.insert(events).get
      emonBillingOperationTaskDao.stat.get.taskCount shouldBe 0

      runTask()

      emonBillingOperationTaskDao.stat.get.taskCount shouldBe events.map(_.groupId).distinct.size
      val tasks = emonBillingOperationTaskDao.peek(events.map(_.groupId).distinct.size).get
      tasks.forall(_.payload.hasOrderState) shouldBe true
      tasks.forall(_.payload.hasTransaction) shouldBe true
      tasks.forall(_.payload.hasTransactionEpoch) shouldBe true
      tasks.map(_.payload.getTransaction.getId).toSet shouldBe events.map(_.groupId).toSet
    }

    "form valid billing operation task for processed group" in {
      val event = Event(supportedEventGen.next, readableString.next)
      emonEventDao.insert(Seq(event)).get
      emonBillingOperationTaskDao.stat.get.taskCount shouldBe 0
      val order = orderDao.get(OrderService.GetFilter.ForOrderIds(orderId)).get.head
      runTask()
      emonBillingOperationTaskDao.stat.get.taskCount shouldBe 1
      val task = emonBillingOperationTaskDao.peek(1).get.head
      val actualOrder = task.payload.getOrderState
      actualOrder.getBalance shouldBe order.balance2.current - event.event.price
      actualOrder.getId shouldBe order.id
      actualOrder.getTotalSpent shouldBe order.balance2.totalSpent + event.event.price

      val transaction = task.payload.getTransaction
      transaction.getAmount shouldBe event.event.price
      transaction.getDetails.getEmonEventList.asScala.map(_.getEventStateId) shouldBe Seq(event.event.eventStateId)
      transaction.getDetails.getEmonEventList.asScala.map(_.getRevenue) shouldBe Seq(event.event.price)
      transaction.getId shouldBe event.groupId
      transaction.getOrderId shouldBe event.event.orderId
    }

  }

  private def runTask(): Unit = {
    task.execute(ConfigFactory.empty()).futureValue
  }

  private def getTransaction(id: TransactionId, transactionType: TransactionType): Option[OrderTransaction] = {
    orderDao
      .getTransaction(id, orderId, transactionType, withDetails = true)
      .get
  }

  private def customEventStateGen(orderId: OrderId, price: Funds, eventStateId: EventStateId): Gen[EventState] = {
    ValidEmonEventStateGen
      .map { e =>
        val b = e.toBuilder
        b.getEventBuilder.setEventId(eventStateId.getEventId)
        b.setSnapshotId(eventStateId.getSnapshotId)
        b.getEventBuilder.getPayerBuilder.getBalanceBuilder.setOrderId(orderId)
        b.getPriceBuilder.getResponseBuilder.getRuleBuilder.getPriceBuilder.setKopecks(price)
        b.build()
      }
  }

  private lazy val supportedEventStateIdGen: Gen[EventStateId] =
    ProtobufMessageGenerators.generate[EventStateId](depth = 5).map { e =>
      val b = e.toBuilder
      b.getEventIdBuilder.setEventType(EventTypeNamespace.EventType.REALTY_DEVCHAT)
      b.getEventIdBuilder.setProject(CommonModel.Project.REALTY)
      b.setSnapshotId(Gen.choose(0, 100).next)
      b.build()
    }

  private lazy val supportedEventGen: Gen[EventState] =
    for {
      eventStateId <- supportedEventStateIdGen
      price <- Gen.choose(1000, 10000)
      event <- customEventStateGen(orderId, price, eventStateId)
    } yield event

}
