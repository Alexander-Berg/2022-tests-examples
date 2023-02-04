package ru.auto.salesman.api.v1

import ru.auto.salesman.api.akkahttp.SalesmanExceptionHandler.specificExceptionHandler
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.typesafe.config.{Config, ConfigFactory}
import ru.auto.salesman.api.v1.SalesmanApiUtils.SalesmanHttpRequest
import ru.auto.salesman.api.v1.service.sale.SaleService
import ru.auto.salesman.backend.UserApiBackend
import ru.auto.salesman.model.DomainAware
import ru.auto.salesman.service.PromocodeListingService
import ru.auto.salesman.service.async._
import ru.auto.salesman.service.schedules.AsyncScheduleCrudService
import ru.auto.salesman.service.user.{
  PaymentHistoryService,
  PaymentService,
  PeriodicalDiscountService
}
import ru.auto.salesman.service.user.personal_discount.PersonalDiscountService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.application.deploy.Deploys
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.RuntimeConfig
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, TracingSupport}
import ru.auto.salesman.service.user.PriceService
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

trait HandlerBaseSpec
    extends BaseSpec
    with ScalatestRouteTest
    with DomainAware
    with UserModelGenerators
    with ProtobufSupport {

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

  val promocodeListingService: PromocodeListingService =
    mock[PromocodeListingService]

  def saleService: SaleService = mock[SaleService]

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

  implicit private val testOperationalSupport: OperationalSupport =
    ru.yandex.vertis.ops.test.TestOperationalSupport

  implicit private val testTracingSupport: TracingSupport = LocalTracingSupport(
    EndpointConfig.Empty
  )

  implicit private val testRuntimeConfig: RuntimeConfig = new RuntimeConfig {
    def environment: Environments.Value = Environments.Testing
    def deploy: Deploys.Value = Deploys.Container
    def hostname: String = ""
    def localDataCenter: String = ""
    def allocation: Option[String] = None
  }

  private val handler = new ru.auto.salesman.api.HandlerImpl(backend)

  //for withName directive support we will wrap request
  val route: Route = handler.wrapRequest {
    handler.route
  }

  override def testConfig: Config = ConfigFactory.empty()

  def post(uri: String, body: Array[Byte])(
      implicit timeout: RouteTestTimeout
  ): HttpResponse =
    Post(uri)
      .withEntity(body)
      .withSalesmanTestHeader() ~> Route.seal(route) ~> check {
      response
    }
}
