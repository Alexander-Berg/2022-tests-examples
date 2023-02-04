package ru.auto.salesman.api.v1

import akka.http.scaladsl.testkit.RouteTestTimeout
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import ru.auto.salesman.api.v1.service.sale.{SaleDao, SaleService}
import ru.auto.salesman.client.PromocoderClient
import ru.auto.salesman.dao.impl.jdbc.JdbcVinHistoryDao
import ru.auto.salesman.dao.impl.jdbc.user._
import ru.auto.salesman.dao.user.TransactionDao.Filter.ForTransactionId
import ru.auto.salesman.dao.user.TransactionDao.Patch.Status
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user.product.AutoruProduct
import ru.auto.salesman.model.user.product.Products._
import ru.auto.salesman.model.user.{
  CreateTransactionResult,
  Prolongable,
  Transaction,
  TransactionRequest
}
import ru.auto.salesman.model.{TransactionId, TransactionStatus, UserId}
import ru.auto.salesman.service.ProlongableExtractor
import ru.auto.salesman.service.async.{AsyncProductService, AsyncTransactionService}
import ru.auto.salesman.service.banker.BankerService
import ru.auto.salesman.service.impl.UserPromocodesServiceImpl
import ru.auto.salesman.service.impl.user._
import ru.auto.salesman.service.impl.user.goods.BoostScheduler
import ru.auto.salesman.service.impl.user.prolongation.FailedProlongationProcessor
import ru.auto.salesman.service.impl.user.vin.history.{ReportAdder, ReportGetter}
import ru.auto.salesman.service.user.UserProductService.{Request, Response}
import ru.auto.salesman.service.user.{
  PaidProductService,
  PaymentService,
  UserFeatureService
}
import ru.auto.salesman.test.IntegrationPropertyCheckConfig
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import ru.auto.salesman.util.TimeUtils.Time
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import zio.{UIO, ZIO}

import scala.concurrent.duration.DurationInt
import scala.slick.jdbc.StaticQuery

// at the moment of 2018-04-27 there is not a single test for daos
// so, it's better to use real daos instead of mocks in handler tests for now
trait JdbcProductServices
    extends HandlerBaseSpec
    with SalesmanUserJdbcSpecTemplate
    with MockFactory
    with UserModelGenerators
    with IntegrationPropertyCheckConfig {

  implicit val rc: RequestContext = AutomatedContext("unit-test")

  implicit val routeTestTimeout: RouteTestTimeout = RouteTestTimeout(15.seconds)
  lazy val promocoderClient: PromocoderClient = mock[PromocoderClient]
  lazy val promocoderService = new PromocoderServiceImpl(promocoderClient)

  lazy val prolongableExtractor: ProlongableExtractor =
    mock[ProlongableExtractor]
  lazy val bankerApi = mock[BankerService]
  lazy val productScheduleDao = new JdbcProductScheduleDao(database)
  lazy val goodsDao = new JdbcGoodsDao(database)
  lazy val bundleDao = new JdbcBundleDao(database)
  lazy val subscriptionDao = new JdbcSubscriptionDao(database)
  lazy val vinHistoryDao = new JdbcVinHistoryDao(database)
  lazy val transactionDao = new JdbcTransactionDao(database)
  lazy val userCashbackDao = new JdbcUserCashbackDao(transactor, transactor)

  lazy val userPromocodesService =
    new UserPromocodesServiceImpl(promocoderService, userCashbackDao)

  lazy val boostScheduler = new BoostScheduler(productScheduleDao)

  private lazy val featureService = new UserFeatureService {
    def cashbackFullPaymentRestrictionEnabled: Boolean = true
    def vinDecoderSttpClientEnabled: Boolean = ???
    def bestServicePriceEnabled: Boolean = ???
    def increasePlacementByVipReleaseDateFeature: DateTime = ???
    def prolongationFailedPushNotificationEnabled: Boolean = ???
    def prolongationFailedSmsNotificationEnabled: Boolean = ???
    def prolongationFailedEmailNotificationEnabled: Boolean = ???
    def enableAsyncSendingProlongationFailedNotification: Boolean = ???
    def requestPriceFromVosMasterEnabled: Boolean = ???
    def userQuotaModerationEventsEnabled: Boolean = ???
    def statisticLogBrokerEnabled: Boolean = ???
    def statisticLogBrokerSyncLogEnabled: Boolean = ???
    def useTrustGate: Boolean = ???
    def callCarfaxForReportPrice: Boolean = ???
    def usePriceFromCarfaxForReportPrice: Boolean = ???
    def useNewRecurrentPaymentWay: Boolean = ???
    def useTrustForScheduledPayments: Boolean = ???

    override val vipBoostUsingCustomPriceEnabled: UIO[Boolean] =
      ZIO.succeed(false)
  }

  lazy val goodsService =
    new GoodsServiceImpl(
      goodsDao,
      bundleDao,
      promocoderClient,
      userPromocodesService,
      boostScheduler,
      featureService
    )

  lazy val bundleService =
    new BundleServiceImpl(
      bundleDao,
      goodsService,
      promocoderClient,
      featureService
    )

  lazy val time = mock[Time]

  lazy val reportAdder =
    new ReportAdder(subscriptionDao, vinHistoryDao, time, database)
  lazy val reportGetter = new ReportGetter(vinHistoryDao)

  lazy val vinHistoryUserService =
    new VinHistoryUserService(reportAdder, reportGetter)

  lazy val syncSubscriptionService =
    new SubscriptionServiceImpl(
      subscriptionDao,
      vinHistoryUserService,
      promocoderClient
    )

  lazy val syncProductService =
    new UserProductServiceImpl(
      goodsService,
      bundleService,
      syncSubscriptionService
    )

  lazy val syncTransactionService =
    new TransactionServiceImpl(
      transactionDao,
      goodsService,
      bundleService,
      syncSubscriptionService,
      bankerApi
    )

  override def productService: AsyncProductService =
    new AsyncProductService(syncProductService)

  override def transactionService: AsyncTransactionService =
    new AsyncTransactionService(syncTransactionService)

  val failedProlongationProcessor = mock[FailedProlongationProcessor]

  private lazy val syncPaymentService =
    new PaymentServiceImpl(
      syncTransactionService,
      syncProductService,
      failedProlongationProcessor
    )

  override def paymentService: PaymentService = syncPaymentService

  override lazy val saleService: SaleService =
    new SaleService(new SaleDao(database))

  def addProduct(
      user: UserId,
      offerId: Option[OfferIdentity],
      product: AutoruProduct,
      prolongable: Prolongable
  ): Unit = {
    val transaction = paidTransactionGen(user).next
    val request =
      productRequestGen(
        product,
        offerId,
        selfActivatedProductPriceGen,
        prolongable
      ).next
    service(product).add(transaction, request).success
  }

  def addProduct(
      user: UserId,
      product: AutoruProduct,
      prolongable: Prolongable
  ): Unit = {
    val transaction = paidTransactionGen(user).next
    val request =
      productRequestGen(product).next.copy(prolongable = prolongable)
    service(product).add(transaction, request).success
  }

  def getProduct(
      user: UserId,
      offerId: OfferIdentity,
      product: OfferProduct
  ): Response = {
    val request = product match {
      case _: Goods => Request.Goods(product, user, offerId)
      case _: GoodsBundle => Request.Bundle(product, user, Some(offerId))
    }
    syncProductService.get(request).success.value
  }

  def getProduct(user: UserId, product: UserProduct): Response = {
    val request = product match {
      case _: Subscription => Request.Subscription(product, user)
    }
    syncProductService.get(request).success.value
  }

  def createTransaction(request: TransactionRequest): CreateTransactionResult =
    syncTransactionService.create(request).success.value

  def updateTransactionStatus(
      id: TransactionId,
      status: TransactionStatus
  ): Unit =
    syncTransactionService.update(id, Status(status)).success.value

  def getTransaction(id: TransactionId): Transaction =
    syncTransactionService.getTransaction(ForTransactionId(id)).success.value

  def service(product: AutoruProduct): PaidProductService[_] =
    product match {
      case _: Goods => goodsService
      case _: GoodsBundle => bundleService
      case _: Subscription => syncSubscriptionService
    }

  def expectChangeFeatureCount(): Unit =
    (promocoderClient.changeFeatureCount _)
      .expects(*, *)
      .returningZ(featureInstanceGen.next)
      .noMoreThanOnce()

  def expectCreateFeatures(): Unit =
    (promocoderClient.createFeatures _)
      .expects(*, *, *)
      .returningZ(Gen.listOf(featureInstanceGen).next)
      .noMoreThanOnce()

  def cleanTransactions(): Unit = database.withSession { implicit session =>
    StaticQuery.queryNA[Int](s"delete from `transactions_identity`").execute
    StaticQuery.queryNA[Int](s"delete from `transactions_lock`").execute
    StaticQuery.queryNA[Int](s"delete from `transactions`").execute
  }
}
