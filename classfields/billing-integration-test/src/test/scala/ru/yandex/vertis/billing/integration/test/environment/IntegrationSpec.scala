package ru.yandex.vertis.billing.integration.test.environment

import org.scalatest.Suite
import ru.yandex.vertis.billing.BillingEvent.CampaignActiveStatusChangeEvent
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.integration.test.mocks.{
  TestKafkaProtoProducer,
  TestMessageDeliveryService,
  TestS3CampaignStorage
}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.{CampaignService, OrderService}
import ru.yandex.vertis.billing.util.OperatorContext
import ru.yandex.vertis.scheduler.model.Payload.{Async, Sync, SyncTry}
import ru.yandex.vertis.scheduler.model.Task

import java.util.concurrent.atomic.AtomicLong

trait IntegrationSpec extends AsyncSpecBase with JdbcSpecTemplate { this: Suite =>

  implicit class RichTask(task: Task) {

    def run(): Unit = task.payload match {
      case t: Sync => t.run()
      case t: SyncTry => t.run().get
      case t: Async => t.run().futureValue
    }
  }

  def runTasks(task: Task*): Unit = task.foreach(_.run())

  private val seqId = new AtomicLong(System.currentTimeMillis)
  def nextSeqId: Long = seqId.incrementAndGet

  val messageDeliveryService = new TestMessageDeliveryService
  val campaignActiveStatusProducer = new TestKafkaProtoProducer[CampaignActiveStatusChangeEvent]
  val s3CampaignStorage = new TestS3CampaignStorage

  val tms = TestTmsEnvironment(fullBillingDatabase.database, eventStorageDatabase)

  private val config = TestConfig.load(fullBillingDatabase.name)

  val indexer =
    TestIndexerEnvironment.create(config, messageDeliveryService, campaignActiveStatusProducer, s3CampaignStorage)

  tms.keyValueDao.putT("epoch_CallsStatistics", "0")
  tms.keyValueDao.putT("epoch_WithdrawForm", "0")
  tms.keyValueDao.putT("epoch_hobo-task", "0")

  implicit val requestContext = OperatorContext("1", Uid(1))

  def createCustomer(): CustomerHeader =
    tms.customerDao.create(CustomerHeader(CustomerId(nextSeqId, Some(nextSeqId)), DealerRef("1"))).get

  def createOrder(customer: CustomerHeader, balance: Funds = 1000000L): Order = {
    val order = tms.orderDao.create(customer.id, OrderProperties("someOrder", None)).get
    tms.orderDao.upsertIncome(nextSeqId.toString, order.id, balance)
    order
  }

  def getOrder(orderId: OrderId): Order =
    tms.orderDao
      .get(OrderService.GetFilter.ForOrderIds(List(orderId)))
      .get
      .head

  def createCallCampaign(order: Order, callObjectId: String, productId: String = "call"): CampaignHeader = {
    val campaignSource = Data.getCampaign(
      order.id,
      Product(Custom(productId, CostPerCall(FixPrice(1)))),
      Data.getCampaignSettings(
        enabled = true,
        dailyLimit = None,
        callSettings = Some(CallSettings(None, Some(callObjectId)))
      )
    )

    tms.campaignService
      .create(order.owner, campaignSource)
      .get
  }

  def updateCampaign(campaign: CampaignHeader, dailyLimit: Option[TypedLimitSource] = None): CampaignHeader = {
    tms.campaignService
      .update(
        campaign.customer.id,
        campaign.id,
        CampaignService.Patch(dailyLimit = dailyLimit)
      )
      .get
  }

  private val dictionaryTables = Set(
    "spending_limit_type",
    "order_transaction_type",
    "event_type",
    "discount_type",
    "campaign_call_status",
    "redirect_source"
  )

  def clearDatabase() = {
    import slick.jdbc.MySQLProfile.api._
    val tables = tms.database.run(sql"SHOW TABLES".as[String]).futureValue.toList
    val tablesForCleaning = tables
      .filterNot(dictionaryTables.contains)
      .filterNot(_ == "key_value") // пока трудно сделать очистку таблицы key_value из-за начальных значений эпох

    tms.database
      .run(
        DBIO
          .sequence(
            sql"SET FOREIGN_KEY_CHECKS = 0;".asUpdate +:
              tablesForCleaning.map(table => sql"TRUNCATE #$table; ".asUpdate) :+
              sql"SET FOREIGN_KEY_CHECKS = 1;".asUpdate
          )
          .transactionally
      )
      .futureValue
  }

}
