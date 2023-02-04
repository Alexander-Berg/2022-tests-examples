package ru.auto.salesman.api.v1

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import ru.auto.salesman.api.v1.service.sale.SaleService
import ru.auto.salesman.backend.UserApiBackend
import ru.auto.salesman.model.DomainAware
import ru.auto.salesman.service.PromocodeListingService
import ru.auto.salesman.service.async.{
  AsyncProductService,
  AsyncSubscriptionService,
  AsyncTransactionService,
  AsyncUserPaysysService
}
import ru.auto.salesman.service.user.PriceService
import ru.auto.salesman.service.schedules.AsyncScheduleCrudService
import ru.auto.salesman.service.user.personal_discount.PersonalDiscountService
import ru.auto.salesman.service.user.{
  PaymentHistoryService,
  PaymentService,
  PeriodicalDiscountService
}
import ru.auto.salesman.test.DeprecatedMockitoBaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.feature.model.FeatureRegistry

trait DeprecatedMockitoHandlerBaseSpec
    extends DeprecatedMockitoBaseSpec
    with ScalatestRouteTest
    with DomainAware
    with UserModelGenerators {

  implicit val requestContext: RequestContext = AutomatedContext("test")

  def userPaysysService: AsyncUserPaysysService = mock[AsyncUserPaysysService]
  def paymentService: PaymentService = mock[PaymentService]

  def transactionService: AsyncTransactionService =
    mock[AsyncTransactionService]
  def productService: AsyncProductService = mock[AsyncProductService]
  def priceService: PriceService = mock[PriceService]

  def subscriptionService: AsyncSubscriptionService =
    mock[AsyncSubscriptionService]

  def featureRegistry: FeatureRegistry = mock[FeatureRegistry]

  val scheduleCrudService: AsyncScheduleCrudService =
    mock[AsyncScheduleCrudService]

  val periodicalDiscountService: PeriodicalDiscountService =
    mock[PeriodicalDiscountService]

  val personalDiscountService: PersonalDiscountService =
    mock[PersonalDiscountService]

  val saleService: SaleService = mock[SaleService]

  val promocodeListingService: PromocodeListingService =
    mock[PromocodeListingService]

  val paymentHistoryService = mock[PaymentHistoryService]

  def backend: UserApiBackend =
    UserApiBackend(
      userPaysysService,
      paymentService,
      transactionService,
      productService,
      subscriptionService,
      priceService,
      scheduleCrudService,
      featureRegistry,
      periodicalDiscountService,
      personalDiscountService,
      saleService,
      promocodeListingService,
      paymentHistoryService
    )

  private val handler = new service.HandlerImpl(backend)

  //for withName directive support we will wrap request
  val route: Route = handler.wrapRequest {
    handler.route
  }

  override def testConfig: Config = ConfigFactory.empty()
}
