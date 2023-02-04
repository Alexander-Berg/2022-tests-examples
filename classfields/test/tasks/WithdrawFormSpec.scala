package ru.yandex.vertis.billing.tasks

import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.SupportedServices
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.balance.model.Balance
import ru.yandex.vertis.billing.dao.impl.jdbc._
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDao
import ru.yandex.vertis.billing.event._
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{CampaignEventsGen, EpochGen, Producer}
import ru.yandex.vertis.billing.service.OrderService.Transparent
import ru.yandex.vertis.billing.service.impl.{EpochServiceImpl, OrderServiceImpl}
import ru.yandex.vertis.billing.settings._
import ru.yandex.vertis.billing.tasks.WithdrawFormTask._
import ru.yandex.vertis.billing.util.{DateTimeInterval, Range}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.billing.util.TestingHelpers.RichOrderTransaction

import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Tests for withdraw form task
  *
  * @author alesavin
  */
class WithdrawFormSpec
  extends AnyWordSpec
  with Matchers
  with JdbcSpecTemplate
  with EventsProviders
  with MockitoSupport
  with AsyncSpecBase {

  private val campaignEventDao = new JdbcCampaignEventDao(campaignEventDatabase)
  private val orderDao = new JdbcOrderDao(billingDualDatabase)
  private val customerDao = new JdbcCustomerDao(billingDatabase)
  private val keyValueDao = new JdbcKeyValueDao(billingDatabase)

  private val balanceSettings =
    BalanceSettings("realty", ConfigFactory.parseResources("application-test.conf").getConfig("service.realty.balance"))
  private val balance = mock[Balance]

  private val orderService =
    new OrderServiceImpl(orderDao, balance, balanceSettings) with HoldMemorizedOrderService

  private val epochService = new EpochServiceImpl(keyValueDao)

  var order: OrderId = -1L

  val interval = DateTimeInterval.currentDay.copy(from = DateTimeInterval.previousDay.from)

  private val withdrawFormTask =
    new WithdrawFormTask(
      campaignEventDao,
      orderService,
      epochService,
      keyValueDao,
      new TasksServiceComponents {

        override def isWellTimedEvent(now: DateTime)(e: CampaignEvents) =
          interval.contains(e.snapshot.time)

        override def serviceName: String = SupportedServices.AutoRu
      }
    )

  val resource = PartnerRef("1")
  val customer = CustomerHeader(CustomerId(1, None), resource)
  val orderProperties = OrderProperties("Order 1", Some("Description 1"))

  assert(customerDao.create(customer).isSuccess)

  orderDao.create(customer.id, orderProperties) match {
    case Success(o) => order = o.id
    case other =>
      fail(s"Unpredicted: $other")
  }

  private val campaignEvents = {
    CampaignEventsGen.next(100).map { e =>
      val product = Product(Raising(CostPerIndexing(300L)))
      e.copy(
        snapshot = e.snapshot.copy(
          campaignId = "Campaign 1",
          orderId = order,
          product = product
        ),
        stat = e.stat.copy(
          epoch = Some(EpochGen.next)
        )
      )
    }
  }

  private def getTransactions(): Seq[OrderTransaction] = {
    orderDao
      .listTransactions(
        customer.id,
        order,
        interval,
        Transparent,
        Range(0, 100)
      )
      .get
      .iterator
      .toList
  }

  "WithdrawFormTask" should {

    "generate withdraws without epoch" in {
      withdrawFormTask.run() match {
        case Failure(_: NoSuchElementException) =>
          info("Done")
        case other =>
          fail(s"Unpredicted: $other")
      }
    }

    "generate withdraws from generated events data" in {
      campaignEventDao.write(campaignEvents).futureValue

      val revenue = calEventsRevenue()

      epochService.set(WithdrawFormTask.EpochMarker, 0L).futureValue
      assert(withdrawFormTask.run().isSuccess)

      val transactions = getTransactions()

      transactions.size should be > 0
      transactions.find(_.orderId != order) should be(None)
      transactions.foreach {
        case Withdraw2(_, CampaignSnapshot(_, _, orderId, product, _), amount, _, _) =>
          orderId should be(order)
          amount should be >= 0L
          product.goods.find(!_.cost.isInstanceOf[CostPerIndexing]) shouldBe None
        case _: Overdraft => ()
        case other =>
          fail(s"Unpredicted: $other")
      }
      transactions.map(_.amount).sum should be(revenue)

      epochService.getTry(EpochMarker) match {
        case Success(epoch) => epoch should be > 0L
        case other => fail(s"Unpredicted: $other")
      }

      val holds = extractHolds()
      orderService.getHolds should be(holds)
    }

    "correctly operate with changed events (change only epoch)" in {
      val prevTransactions = getTransactions()

      val prevEpochMarker = epochService.getTry(EpochMarker).get
      val prevHolds = orderService.getHolds

      val changedEventRecords = campaignEvents.map { e =>
        e.copy(stat = e.stat.copy(epoch = Some(prevEpochMarker + 1000)))
      }

      campaignEventDao.write(changedEventRecords, writeEpoch = true).futureValue

      assert(withdrawFormTask.run().isSuccess)

      val actualTransactions = getTransactions()
      actualTransactions.size shouldBe prevTransactions.size
      val zipped = actualTransactions.sortBy(_.id).zip(prevTransactions.sortBy(_.id))
      zipped.foreach { case (actualTr, prevTr) =>
        actualTr.withoutEpoch shouldBe prevTr.withoutEpoch
        actualTr.extractEpoch.get should be > prevTr.extractEpoch.get
      }

      epochService.get(EpochMarker).futureValue shouldBe (prevEpochMarker + 1000)
      orderService.getHolds shouldBe prevHolds
    }

  }

  private def calEventsRevenue(): Funds =
    actualEvents().flatMap(toWithdrawRequest).map(_.amount).sum

  private def extractHolds(): Set[TransactionId] =
    actualEvents().flatMap(toWithdrawRequest).flatMap(_.holds).toSet

  private def actualEvents(): Seq[CampaignEvents] = campaignEventDao.read(0L) match {
    case Success(events) =>
      events.filter(e => interval.contains(e.snapshot.time))
    case Failure(e) => throw e
  }

}
