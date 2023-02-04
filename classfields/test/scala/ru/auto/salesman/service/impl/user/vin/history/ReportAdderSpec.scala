package ru.auto.salesman.service.impl.user.vin.history

import cats.data.NonEmptyList
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfter, LoneElement}
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse}
import ru.auto.salesman.dao.VinHistoryDao
import ru.auto.salesman.dao.VinHistoryDao.Filter.ForUser
import ru.auto.salesman.dao.impl.jdbc.JdbcVinHistoryDao
import ru.auto.salesman.dao.impl.jdbc.user.JdbcSubscriptionDao
import ru.auto.salesman.dao.user.SubscriptionDao
import ru.auto.salesman.dao.user.SubscriptionDao.Filter.ForActiveProductUser
import ru.auto.salesman.model.user.ApiModel.VinHistoryBoughtReport
import ru.auto.salesman.model.user.Prolongable
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, ProductStatuses}
import ru.auto.salesman.service.UserPaymentRequiredException
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.{UserModelGenerators, VinHistoryGenerators}
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import ru.auto.salesman.util.AutomatedContext
import ru.auto.salesman.util.TimeUtils.Time
import ru.yandex.vertis.scalatest.BetterTryValues
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.util.Try

class ReportAdderSpec
    extends BaseSpec
    with SalesmanUserJdbcSpecTemplate
    with VinHistoryGenerators
    with IntegrationPatience
    with BeforeAndAfter
    with UserModelGenerators
    with BetterTryValues
    with LoneElement {

  def vinHistoryDao = new JdbcVinHistoryDao(database)
  def subscriptionsDao = new JdbcSubscriptionDao(database)

  val timeProvider = mock[Time]

  val vinHistoryUserService =
    new ReportAdder(subscriptionsDao, vinHistoryDao, timeProvider, database)

  implicit private val rc = AutomatedContext("test")

  private val report = vinHistoryBoughtReportGenerator.next
  private val autoruUser = AutoruUserGen.next
  private val offerIdOpt = Gen.option(OfferIdentityGen).next
  private val vin = vinGenerator.next
  private val context = SubscriptionContextGen.next

  "VinHistoryUserService" should {
    "successfully put report with deadline in report" in {
      val subscription = SubscriptionOffersHistoryReportsGen.next
      val activated = dateTimeInPast.next
      val deadline = dateTimeInFuture().next
      subscriptionsDao
        .insertIfNotExists(
          SubscriptionDao.Request(
            autoruUser.toString,
            subscription,
            counter = 10L,
            amount = 0L,
            status = ProductStatuses.Active,
            transactionId = "123",
            activated,
            deadline,
            context,
            prolongable = Prolongable(false)
          )
        )
        .success

      addReportAndAssertSuccess(report, expectedDeadline = getDeadline(report))
    }

    "successfully put report with default deadline" in {
      val report = vinHistoryBoughtReportGenerator
        .map(_.toBuilder.clearDeadline.build)
        .next
      val subscription = SubscriptionOffersHistoryReportsGen.next
      val activated = dateTimeInPast.next
      val deadline = dateTimeInFuture().next
      subscriptionsDao
        .insertIfNotExists(
          SubscriptionDao.Request(
            autoruUser.toString,
            subscription,
            counter = 10L,
            amount = 0L,
            status = ProductStatuses.Active,
            transactionId = "123",
            activated,
            deadline,
            context,
            prolongable = Prolongable(false)
          )
        )
        .success

      val now = DateTimeUtil.now
      (timeProvider.now _).expects().returning(now)

      addReportAndAssertSuccess(report, expectedDeadline = now.plusDays(365))

      val subscriptions = subscriptionsDao
        .get(ForActiveProductUser(OffersHistoryReports(1), autoruUser.toString))
        .success
        .value

      subscriptions.loneElement.counter shouldBe 9
    }

    "don't add report if subscription quota = 0" in {
      val subscription = SubscriptionOffersHistoryReportsGen.next
      val activated = dateTimeInPast.next
      val deadline = dateTimeInFuture().next
      subscriptionsDao
        .insertIfNotExists(
          SubscriptionDao.Request(
            autoruUser.toString,
            subscription,
            counter = 0L,
            amount = 0L,
            status = ProductStatuses.Active,
            transactionId = "123",
            activated,
            deadline,
            context,
            prolongable = Prolongable(false)
          )
        )
        .success

      val ex = addReport(report).failure.exception

      ex shouldBe UserPaymentRequiredException(
        autoruUser,
        OffersHistoryReports(1)
      )

      val addedReports = Try {
        database.withTransaction { implicit transaction =>
          vinHistoryDao
            .getQuery(NonEmptyList(ForUser(autoruUser), List()))
            .list
        }
      }.success.value

      addedReports.size shouldBe 0
    }

    "don't decrease quota if report already existed" in {
      val subscription = SubscriptionOffersHistoryReportsGen.next
      val activated = dateTimeInPast.next
      val oldDeadline = dateTimeInFuture().next
      val deadline = dateTimeInFuture().next
      subscriptionsDao
        .insertIfNotExists(
          SubscriptionDao.Request(
            autoruUser.toString,
            subscription,
            counter = 10L,
            amount = 0L,
            status = ProductStatuses.Active,
            transactionId = "123",
            activated,
            deadline,
            context,
            prolongable = Prolongable(false)
          )
        )
        .success

      vinHistoryDao.put(
        VinHistoryDao.Source(
          autoruUser,
          vin,
          offerIdOpt,
          None,
          oldDeadline,
          garageId = None
        )
      )

      addReportAndAssertSuccess(report, expectedDeadline = oldDeadline)

      val subscriptions = subscriptionsDao
        .get(ForActiveProductUser(OffersHistoryReports(1), autoruUser.toString))
        .success
        .value

      subscriptions.loneElement.counter shouldBe 10
    }

    "decrement counter only on package with earlier deadline, if there are two active packages, and package with earlier deadline was activated earlier" in {
      val activated1 = DateTime.now().minusDays(2)
      val deadline1 = activated1.plusDays(32)
      val activated2 = DateTime.now().minusDays(1)
      val deadline2 = activated2.plusDays(365)
      val subscriptionId1 = insertPackage(
        OffersHistoryReports(50),
        "transaction-id-1",
        activated1,
        deadline1
      )
      val subscriptionId2 = insertPackage(
        OffersHistoryReports(10),
        "transaction-id-2",
        activated2,
        deadline2
      )

      addReportAndAssertSuccess(report, expectedDeadline = getDeadline(report))

      getSubscriptionCounter(subscriptionId1) shouldBe 49
      getSubscriptionCounter(subscriptionId2) shouldBe 10
    }

    "decrement counter only on package with earlier deadline, if there are two active packages, and package with earlier deadline was activated later" in {
      val activated1 = DateTime.now().minusDays(2)
      val deadline1 = activated1.plusDays(365)
      val activated2 = DateTime.now().minusDays(1)
      val deadline2 = activated2.plusDays(32)
      val subscriptionId1 = insertPackage(
        OffersHistoryReports(10),
        "transaction-id-1",
        activated1,
        deadline1
      )
      val subscriptionId2 = insertPackage(
        OffersHistoryReports(50),
        "transaction-id-2",
        activated2,
        deadline2
      )

      addReportAndAssertSuccess(report, expectedDeadline = getDeadline(report))

      getSubscriptionCounter(subscriptionId1) shouldBe 10
      getSubscriptionCounter(subscriptionId2) shouldBe 49
    }

    "decrement counter once (not twice), if there are two active packages with the same deadline" in {
      val activated = DateTime.now().minusDays(2)
      val deadline = activated.plusDays(365)
      val subscriptionId1 = insertPackage(
        OffersHistoryReports(10),
        "transaction-id-1",
        activated,
        deadline
      )
      val subscriptionId2 = insertPackage(
        OffersHistoryReports(10),
        "transaction-id-2",
        activated,
        deadline
      )

      addReportAndAssertSuccess(report, expectedDeadline = getDeadline(report))

      getSubscriptionCounter(subscriptionId1) + getSubscriptionCounter(
        subscriptionId2
      ) shouldBe 19
    }
  }

  private def getDeadline(report: VinHistoryBoughtReport) =
    new DateTime(Timestamps.toMillis(report.getDeadline))

  private def addReportAndAssertSuccess(
      report: VinHistoryBoughtReport,
      expectedDeadline: DateTime
  ) = {
    addReport(report).success.value shouldBe SuccessResponse.newBuilder
      .setStatus(ResponseStatus.SUCCESS)
      .build

    val addedReports = Try {
      database.withTransaction { implicit transaction =>
        vinHistoryDao
          .getQuery(NonEmptyList(VinHistoryDao.Filter.Active, List()))
          .list
      }
    }.success.value

    addedReports.loneElement.vin shouldBe vin
    addedReports.loneElement.offerId shouldBe offerIdOpt
    addedReports.loneElement.deadline shouldBe expectedDeadline
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  type SubscriptionId = String

  private def addReport(report: VinHistoryBoughtReport) =
    vinHistoryUserService
      .addReport(
        autoruUser,
        vin,
        offerIdOpt,
        withDecrementCounter = true,
        1,
        report
      )

  private def insertPackage(
      product: OffersHistoryReports,
      transactionId: String,
      activated: DateTime,
      deadline: DateTime
  ): SubscriptionId = {
    val request = SubscriptionDao.Request(
      autoruUser.toString,
      product,
      counter = product.counter,
      amount = 0L,
      status = ProductStatuses.Active,
      transactionId = transactionId,
      activated,
      deadline,
      context,
      prolongable = Prolongable(false)
    )
    subscriptionsDao.insertIfNotExists(request).success
    request.subscriptionId
  }

  private def getSubscriptionCounter(id: SubscriptionId): Long =
    subscriptionsDao
      .get(
        SubscriptionDao.Filter
          .ForSubscriptionId(id)
      )
      .success
      .value
      .loneElement
      .counter
}
