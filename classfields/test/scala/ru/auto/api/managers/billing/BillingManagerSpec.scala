package ru.auto.api.managers.billing

import akka.http.scaladsl.model.headers.RawHeader
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{never, verify, verifyNoMoreInteractions}
import org.mockito.{ArgumentMatcher, Mockito}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.PriceModel.KopeckPrice
import ru.auto.api.ResponseModel.ActivationResponse
import ru.auto.api.ResponseModel.HelloResponse.ExperimentsConfig
import ru.auto.api.auth.ApplicationToken
import ru.auto.api.billing.BillingManager
import ru.auto.api.billing.BillingManager.{remapProduct, OldKassaCardPrefix}
import ru.auto.api.billing.booking.BookingBillingManager
import ru.auto.api.billing.trust.TrustGateConditions
import ru.auto.api.billing.v2.BillingModelV2
import ru.auto.api.billing.v2.BillingModelV2.InitPaymentRequest.{Product, PurchaseCase, StatisticsParameters}
import ru.auto.api.billing.v2.BillingModelV2.{InitPaymentRequest, InitPaymentResponse, PaymentStatusResponse, ProcessPaymentResponse}
import ru.auto.api.exceptions._
import ru.auto.api.experiments.{ForceDisableTrust, ForceIgnoreTrustExpResult}
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.billing.subscription.SubscriptionManager
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.garage.GarageManager
import ru.auto.api.managers.offers.OffersManager
import ru.auto.api.managers.promocoder.PromocoderManager
import ru.auto.api.model.AutoruProduct.{OffersHistoryReports, Premium}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model._
import ru.auto.api.model.gen.BankerModelGenerators
import ru.auto.api.model.gen.BankerModelGenerators.{paymentRequestWithStatus, AccountGen, AccountInfoGen, PaymentMethodGen, PaymentRequestFormGen, PaymentRequestWithoutState, TiedCardGenV2}
import ru.auto.api.model.gen.BillingModelGenerators._
import ru.auto.api.model.gen.BookingModelGenerators.initBookingPaymentRequestGen
import ru.auto.api.model.gen.SalesmanModelGenerators.{createTransactionResultGen, paymentPageGen, productResponseGen, ProductPriceGen, SalesmanDomainGen, TransactionGen}
import ru.auto.api.model.uaas.UaasResponseHeaders
import ru.auto.api.services.billing.BankerClient.{BankerDomain, BankerDomains, PaymentMethodFilter}
import ru.auto.api.services.billing.{extractStatus, getProductDescription, BankerClient}
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.keys.TokenServiceImpl.web
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.salesman.SalesmanUserClient
import ru.auto.api.services.salesman.SalesmanUserClient.SalesmanDomain
import ru.auto.api.services.salesman.SalesmanUserClient.SalesmanDomain.AutoruSalesmanDomain
import ru.auto.api.services.uaas.UaaSClient
import ru.auto.api.user.PaymentOuterClass.PaymentPage
import ru.auto.api.util.ProtobufOptions.extractDescriptionRu
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.salesman.model.user.ApiModel.{ProductPrice, ProductResponse, Transaction, TransactionRequest}
import ru.yandex.passport.model.api.ApiModel.UserResult
import ru.yandex.passport.model.common.CommonModel.{DomainBan, UserModerationStatus}
import ru.yandex.vertis.Platform
import ru.yandex.vertis.banker.model.ApiModel._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.util.control.NoStackTrace

/**
  * @author ponydev
  */
class BillingManagerSpec extends BaseSpec with TestRequest with MockitoSupport with ScalaCheckPropertyChecks {

  import ru.auto.api.managers.billing.trust.TrustUaasExperimentExtractorSpec._

  private val offersManager = mock[OffersManager]

  private val promocoderManager = mock[PromocoderManager]

  private val passportClient = mock[PassportClient]

  private val bankerClient = mock[BankerClient]

  private val garageManager = mock[GarageManager]

  private val bankerManager = new BankerManager(bankerClient, passportClient)

  private val salesmanUserClient = mock[SalesmanUserClient]

  private val subscriptionManager = new SubscriptionManager(salesmanUserClient)

  private val bookingBillingManager = mock[BookingBillingManager]

  private val featureManager: FeatureManager = mock[FeatureManager]

  private val fakeManager: FakeManager = mock[FakeManager]

  private val uaasClient: UaaSClient = mock[UaaSClient]

  private val trustGateConditionsFeature: Feature[TrustGateConditions] = mock[Feature[TrustGateConditions]]
  private val feature: Feature[Boolean] = mock[Feature[Boolean]]

  when(fakeManager.shouldSkipPaymentRequestForRobots(?)).thenReturn(false)
  when(fakeManager.shouldSkipPaymentRequestForBannedUsers(?)).thenReturn(false)
  when(fakeManager.shouldFakeBankerStatus(?)).thenReturn(false)
  when(feature.value).thenReturn(true)
  when(featureManager.skipFreePlacementsPayment).thenReturn(feature)
  when(featureManager.disableSberOnOldAndroidDevices).thenReturn(feature)
  when(featureManager.trustGateConditions).thenReturn(trustGateConditionsFeature)
  when(featureManager.paymentRestrictionsForBanned).thenReturn(feature)
  when(featureManager.useTrustUaaSExperiment).thenReturn(feature)
  when(featureManager.enableValidateYandexSession).thenReturn(feature)

  private val billingManager = new BillingManager(
    offersManager,
    bankerManager,
    bankerClient,
    salesmanUserClient,
    subscriptionManager,
    bookingBillingManager,
    garageManager,
    featureManager,
    fakeManager,
    uaasClient
  )

  before {
    Mockito.clearInvocations(
      promocoderManager,
      passportClient,
      bankerClient,
      salesmanUserClient
    )
    Mockito.reset(
      promocoderManager,
      passportClient,
      bankerClient,
      salesmanUserClient
    )
  }

  implicit override val request: Request = privateRequestGen.next

  private val user = request.user.privateRef

  def productResponseGenFromProduct(productList: Seq[AutoruProduct]): Seq[ProductResponse] =
    productList.map(product => productResponseGen().next.toBuilder.setProduct(product.name).build())

  def activeProductResponseGenFromProduct(productList: Seq[AutoruProduct]): Seq[ProductResponse] =
    productList.map { product =>
      productResponseGen().next.toBuilder
        .setProduct(product.name)
        .setStatus(ProductResponse.Status.ACTIVE)
        .setRecoverable(false)
        .setUser(request.user.ref.asPrivate.toPlain)
        .build()
    }

  def productGenFromProductList(productList: Seq[InitPaymentRequest.Product]): Seq[AutoruProduct] =
    productList.flatMap(product => remapProduct(product))

  def productPriceGenFromProduct(productList: Seq[AutoruProduct]): List[ProductPrice] =
    productList
      .groupBy(_.name)
      .flatMap {
        case (name, products) =>
          val pp = ProductPriceGen.next.toBuilder.setProduct(name).build()
          Seq.fill(products.size)(pp)
      }
      .toList

  def checkPrices(productPrices: Seq[ProductPrice], initResponse: InitPaymentResponse): Unit = {
    val productToBase =
      initResponse.getDetailedProductInfosList.asScala.toSeq.map(p => p.getService -> p.getBasePrice).toMap

    val productToEffective = initResponse.getDetailedProductInfosList.asScala.toSeq.map { p =>
      p.getService -> p.getEffectivePrice
    }.toMap

    val productToProlong = initResponse.getDetailedProductInfosList.asScala.toSeq.map { p =>
      p.getService -> p.getAutoProlongPrice
    }.toMap

    val productToApply = initResponse.getDetailedProductInfosList.asScala.toSeq.map { p =>
      p.getService -> p.getAutoApplyPrice
    }.toMap

    val totalBasePrice = productPrices.map(_.getPrice.getBasePrice).sum
    val totalEffectivePrice = productPrices.map(_.getPrice.getEffectivePrice).sum

    totalBasePrice shouldBe initResponse.getBaseCost
    totalEffectivePrice shouldBe initResponse.getCost

    productPrices.foreach { pp =>
      val name = AutoruProduct.forName(pp.getProduct).get.salesName
      productToBase(name) shouldBe pp.getPrice.getBasePrice
      productToEffective(name) shouldBe pp.getPrice.getEffectivePrice
      productToProlong(name) shouldBe pp.getPrice.getProlongPrice
      productToApply(name) shouldBe pp.getProductPriceInfo.getAutoApplyPrice
    }
  }

  private def genCustomRequest(token: ApplicationToken, experiments: Set[String] = Set.empty) = {
    val request = new RequestImpl
    request.setTrace(Traced.empty)
    request.setRequestParams(RequestParams.construct("1.1.1.1", experiments = experiments))
    request.setToken(token)
    request.setNewDeviceUid(DeviceUidGen.next)
    request.setUser(PrivateUserRefGen.next)
    request.setSession(YandexSessionResultGen.next)
    TokenServiceImpl.getStaticApplication(token).foreach(request.setApplication)
    request
  }

  private val InvalidTicketStatusProducerTrStatues = Seq(
    Transaction.Status.UNKNOWN_TRANSACTION_STATUS,
    Transaction.Status.PAID,
    Transaction.Status.CANCELED
  )

  private val TransactionInvalidTicketStatusProducerGen = for {
    tr <- TransactionGen
    trStatus <- protoEnumWithZero(InvalidTicketStatusProducerTrStatues)
  } yield tr.toBuilder.setStatus(trStatus).build()

  "BillingManager" when {
    "getPaymentStatus" should {
      val salesmanDomain = SalesmanDomainGen.next
      "return status from salesman if transaction id passed" in {
        val salesmanTicketId = SalesmanTicketIdGen.next
        val transaction = TransactionGen.next

        when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(transaction)

        val response = billingManager.getPaymentStatus(salesmanTicketId, salesmanTicketId.salesmanDomain)
        verify(salesmanUserClient).getTransaction(user, salesmanTicketId)
        verifyNoMoreInteractions(bankerClient)
        response.futureValue.getPaymentStatus shouldBe extractStatus(transaction)
      }
      "return status from banker if banker payment id passed" in {
        val bankerTicketId = BankerTicketIdGen.next
        val paymentRequest = paymentRequestWithStatus(Payment.Status.VALID).next

        when(bankerClient.getPaymentStatus(?, ?, ?, ?, ?)(?)).thenReturnF(paymentRequest)

        val response = billingManager.getPaymentStatus(bankerTicketId, salesmanDomain)
        verify(bankerClient).getPaymentStatus(
          user,
          bankerTicketId.gate,
          bankerTicketId.prId,
          salesmanDomain.toBankerDomain,
          None
        )
        verifyNoMoreInteractions(salesmanUserClient)
        response.futureValue.getPaymentStatus shouldBe PaymentStatusResponse.Status.CLOSED
      }
      "return status from banker if banker payment in process" in {
        val bankerTicketId = BankerTicketIdGen.next
        val paymentRequest = PaymentRequestWithoutState.next

        when(bankerClient.getPaymentStatus(?, ?, ?, ?, ?)(?)).thenReturnF(paymentRequest)

        val response = billingManager.getPaymentStatus(bankerTicketId, salesmanDomain)
        verify(bankerClient).getPaymentStatus(
          user,
          bankerTicketId.gate,
          bankerTicketId.prId,
          salesmanDomain.toBankerDomain,
          None
        )
        verifyNoMoreInteractions(salesmanUserClient)
        response.futureValue.getPaymentStatus shouldBe PaymentStatusResponse.Status.PROCESS
      }
      "return status from banker if banker payment cancelled" in {
        val bankerTicketId = BankerTicketIdGen.next
        val paymentRequest = paymentRequestWithStatus(Payment.Status.CANCELLED).next

        when(bankerClient.getPaymentStatus(?, ?, ?, ?, ?)(?)).thenReturnF(paymentRequest)
        val response = billingManager.getPaymentStatus(bankerTicketId, salesmanDomain)
        verify(bankerClient).getPaymentStatus(
          user,
          bankerTicketId.gate,
          bankerTicketId.prId,
          salesmanDomain.toBankerDomain,
          None
        )
        verifyNoMoreInteractions(salesmanUserClient)
        response.futureValue.getPaymentStatus shouldBe PaymentStatusResponse.Status.FAILED
      }
      "handle salesman client TransactionNotFound" in {
        val salesmanTicketId = SalesmanTicketIdGen.next
        when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturn(Future.failed(new TransactionNotFound))
        an[TransactionNotFound] should be thrownBy
          billingManager.getPaymentStatus(salesmanTicketId, salesmanTicketId.salesmanDomain).await
      }
      "handle salesman client SalesmanBadRequest" in {
        val salesmanTicketId = SalesmanTicketIdGen.next
        when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturn(Future.failed(new SalesmanBadRequest("artificial")))
        an[SalesmanBadRequest] should be thrownBy
          billingManager.getPaymentStatus(salesmanTicketId, salesmanTicketId.salesmanDomain).await
      }
      "handle banker client PaymentRequestNotFound" in {
        val bankerTicketId = BankerTicketIdGen.next
        when(bankerClient.getPaymentStatus(?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new PaymentRequestNotFound))
        an[PaymentRequestNotFound] should be thrownBy
          billingManager.getPaymentStatus(bankerTicketId, salesmanDomain).await
      }
      "return CLOSED for Booking salesman domain" in {
        val salesmanTicketId = SalesmanTicketIdGen.next
        val response = billingManager.getPaymentStatus(salesmanTicketId, SalesmanDomain.Booking)
        response.futureValue.getPaymentStatus shouldBe PaymentStatusResponse.Status.CLOSED
      }
      "return status from banker if compound payment id passed and banker status is CANCELLED" in {
        val compoundTicketId = CompoundTicketIdGen.next
        val bankerTicketId = compoundTicketId.bankerTicketId
        val paymentRequest = paymentRequestWithStatus(Payment.Status.CANCELLED).next

        when(bankerClient.getPaymentStatus(?, ?, ?, ?, ?)(?)).thenReturnF(paymentRequest)

        val response = billingManager.getPaymentStatus(compoundTicketId, salesmanDomain)
        verify(bankerClient).getPaymentStatus(
          user,
          bankerTicketId.gate,
          bankerTicketId.prId,
          salesmanDomain.toBankerDomain,
          None
        )
        verifyNoMoreInteractions(salesmanUserClient)
        response.futureValue.getPaymentStatus shouldBe PaymentStatusResponse.Status.FAILED
      }
      "return status from salesman if compound payment id passed and banker status is VALID" in {
        val compoundTicketId = CompoundTicketIdGen.next
        val bankerTicketId = compoundTicketId.bankerTicketId
        val salesmanTicketId = compoundTicketId.salesmanTicketId
        val paymentRequest = paymentRequestWithStatus(Payment.Status.VALID).next
        val transaction = TransactionGen.next

        when(bankerClient.getPaymentStatus(?, ?, ?, ?, ?)(?)).thenReturnF(paymentRequest)
        when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(transaction)

        val response = billingManager.getPaymentStatus(compoundTicketId, salesmanTicketId.salesmanDomain)
        verify(bankerClient).getPaymentStatus(
          user,
          bankerTicketId.gate,
          bankerTicketId.prId,
          salesmanTicketId.salesmanDomain.toBankerDomain,
          None
        )
        verify(salesmanUserClient).getTransaction(user, salesmanTicketId)
        response.futureValue.getPaymentStatus shouldBe extractStatus(transaction)
      }
    }
    "getTiedCards" should {
      "correctly list tied cards" in {
        val salesmanDomain = SalesmanDomainGen.next
        val tiedCards = TiedCardGenV2.listOf(10)
        val ps = PaymentSystemId.YANDEXKASSA_V3
        when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(tiedCards)
        val responseFiltered = billingManager.getTiedCards(salesmanDomain, Some(ps))
        verify(bankerClient).getPaymentMethods(
          user,
          PaymentMethodFilter.ForPaymentSystem(ps),
          salesmanDomain.toBankerDomain,
          None
        )
        responseFiltered.await.getCardPsList should have size tiedCards.size
      }
    }
    "removeCard" should {
      "correctly remove card" in {
        val salesmanDomain = SalesmanDomainGen.next
        val tiedCards = TiedCardGenV2.listOf(10)
        val psId = tiedCards.head.getPsId
        val cardIdToDelete = tiedCards.head.getProperties.getCard.getCddPanMask
        val resultCardId = psId match {
          case PaymentSystemId.YANDEXKASSA =>
            OldKassaCardPrefix + cardIdToDelete
          case _ =>
            cardIdToDelete
        }
        when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(tiedCards)
        when(bankerClient.deleteMethod(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        billingManager.removeCard(cardIdToDelete, psId, salesmanDomain).await
        verify(bankerClient).deleteMethod(?, ?, eq(resultCardId), ?)(?)
      }
    }
    "initPayment" when {
      "AUTORU_PURCHASE" should {
        val salesmanDomain = SalesmanDomainGen.next
        val salesmanTransaction = createTransactionResultGen.next
        val accountInfo = AccountInfoGen.next
        val paymentMethods = PaymentMethodGen.listOf(4)
        val experimentsConfig = ExperimentsConfig.newBuilder.build()

        "correctly init with hashed id" in {
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = productResponseGenFromProduct(products)
          val productPrice = productPriceGenFromProduct(products)

          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse)
          when(salesmanUserClient.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice)
          when(salesmanUserClient.createTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(TrustGateConditions.default)
          when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(paymentMethods)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, Seq.empty[RawHeader]))

          val response = billingManager.initPayment(initAutoruPayment, salesmanDomain).await
          response.getAccountBalance shouldBe accountInfo.getBalance
          checkPrices(productPrice, response)
        }
        "skip free placements and init payment for other products" in {
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = productResponseGenFromProduct(products)
          val productPrice = productPriceGenFromProduct(products)

          val placements = (0 to 5).map(_ => AutoruProduct.Placement)
          val placementsResponse = productResponseGenFromProduct(placements)
          val freePlacementsPrices = productPriceGenFromProduct(placements).map { productPrice =>
            productPrice.toBuilder
              .setPrice(productPrice.getPrice.toBuilder.setBasePrice(0))
              .build()
          }

          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse ++ placementsResponse)
          when(salesmanUserClient.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice ::: freePlacementsPrices)
          when(salesmanUserClient.createTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          when(offersManager.activateAutoRu(?, ?, ?[OfferID])(?)).thenReturnF(ActivationResponse.getDefaultInstance)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(TrustGateConditions.default)
          when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(paymentMethods)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, Seq.empty[RawHeader]))

          val response = billingManager.initPayment(initAutoruPayment, salesmanDomain).await
          response.getAccountBalance shouldBe accountInfo.getBalance
          checkPrices(productPrice, response)
        }
        "throw ProductAlreadyActivated if all products are active" in {
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = activeProductResponseGenFromProduct(products)

          when(trustGateConditionsFeature.value).thenReturn(TrustGateConditions.default)
          when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(paymentMethods)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, Seq.empty[RawHeader]))

          billingManager.initPayment(initAutoruPayment, salesmanDomain).failed.futureValue match {
            case i: ProductAlreadyActivated =>
              i.descriptionRu shouldBe extractDescriptionRu(i.code)
            case other =>
              fail(s"Unexpected $other exception")
          }
        }
        "throw NoPayableProductsFoundException if there are no products with price > 0" in {
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = productResponseGenFromProduct(products)
          val productPrice = productPriceGenFromProduct(products).map { productPrice =>
            productPrice.toBuilder
              .setProduct(AutoruProduct.Placement.name)
              .setPrice(productPrice.getPrice.toBuilder.setBasePrice(0))
              .build()
          }

          when(trustGateConditionsFeature.value).thenReturn(TrustGateConditions.default)
          when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(paymentMethods)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse)
          when(salesmanUserClient.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice)
          when(offersManager.activateAutoRu(?, ?, ?[OfferID])(?)).thenReturnF(ActivationResponse.getDefaultInstance)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, Seq.empty[RawHeader]))

          billingManager
            .initPayment(initAutoruPayment, salesmanDomain)
            .failed
            .futureValue shouldBe an[NoPayableProductsFoundException]
        }
        "use TRUST gate" in {
          val yandexPassportRequest = privateYandexSessionRequestGen.next
          val product = InitPaymentRequest.Product
            .newBuilder()
            .setName("premium")
            .setCount(1)
            .build()
          val stats = StatisticsParameters.newBuilder().setPlatform(Platform.PLATFORM_DESKTOP).build()
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next.toBuilder
            .setStatisticsParameters(stats)
            .clearPurchase()
            .setAutoruPurchase(AutoruPurchaseWithHashGen.next)
            .clearProduct()
            .addProduct(product)
            .build()
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = productResponseGenFromProduct(products)
          val productPrice = productPriceGenFromProduct(products)
          val methodsFilter = BankerClient.PaymentMethodFilter.ForPaymentSystem(PaymentSystemId.TRUST)
          val trustGateConditions = TrustGateConditions(
            enabled = true,
            enabledUserIds = Set.empty,
            platforms = Set(Platform.PLATFORM_DESKTOP),
            minAndroidAppVersion = None,
            minIosAppVersion = None,
            purchaseCases = Set(PurchaseCase.AUTORU_PURCHASE),
            products = Set(Premium),
            firstPayment = true,
            probabilityPercent = 100
          )
          val experimentsHeaders = Seq(RawHeader(UaasResponseHeaders.expFlags, trustExpFlags))

          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse)
          when(salesmanUserClient.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice)
          when(salesmanUserClient.createTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          when(salesmanUserClient.getPaymentsHistory(?, ?, ?)(?)).thenReturnF(PaymentPage.getDefaultInstance)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(trustGateConditions)
          when(bankerClient.getPaymentMethods(?, eq(methodsFilter), ?, ?)(?)).thenReturnF(paymentMethods)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, experimentsHeaders))

          val response = billingManager.initPayment(initAutoruPayment, salesmanDomain)(yandexPassportRequest).await
          response.getAccountBalance shouldBe accountInfo.getBalance
          checkPrices(productPrice, response)
        }
        "use TRUST gate when userIds set" in {
          val yandexPassportRequest = privateYandexSessionRequestGen.next
          val product = InitPaymentRequest.Product
            .newBuilder()
            .setName("offers-history-reports")
            .setCount(1)
            .build()
          val stats = StatisticsParameters.newBuilder().setPlatform(Platform.PLATFORM_DESKTOP).build()
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next.toBuilder
            .setStatisticsParameters(stats)
            .clearPurchase()
            .setAutoruPurchase(AutoruPurchaseWithHashGen.next)
            .clearProduct()
            .addProduct(product)
            .build()
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = productResponseGenFromProduct(products)
          val productPrice = productPriceGenFromProduct(products)
          val methodsFilter = BankerClient.PaymentMethodFilter.ForPaymentSystem(PaymentSystemId.TRUST)
          val trustGateConditions = TrustGateConditions(
            enabled = false,
            enabledUserIds = Set(yandexPassportRequest.user.ref.asPrivate.toRaw),
            platforms = Set(Platform.PLATFORM_DESKTOP),
            minAndroidAppVersion = None,
            minIosAppVersion = None,
            purchaseCases = Set(PurchaseCase.AUTORU_PURCHASE),
            products = Set(OffersHistoryReports),
            firstPayment = true,
            probabilityPercent = 100
          )

          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse)
          when(salesmanUserClient.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice)
          when(salesmanUserClient.createTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(trustGateConditions)
          when(bankerClient.getPaymentMethods(?, eq(methodsFilter), ?, ?)(?)).thenReturnF(paymentMethods)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, Seq.empty[RawHeader]))

          val response = billingManager.initPayment(initAutoruPayment, salesmanDomain)(yandexPassportRequest).await
          verify(salesmanUserClient, never()).getPaymentsHistory(?, ?, ?)(?)
          response.getAccountBalance shouldBe accountInfo.getBalance
          checkPrices(productPrice, response)
        }
        "use TRUST gate when enable TRUST flag set" in {
          val product = InitPaymentRequest.Product
            .newBuilder()
            .setName("premium")
            .setCount(1)
            .build()
          val stats = StatisticsParameters.newBuilder().setPlatform(Platform.PLATFORM_DESKTOP).build()
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next.toBuilder
            .setStatisticsParameters(stats)
            .clearPurchase()
            .setAutoruPurchase(AutoruPurchaseWithHashGen.next)
            .clearProduct()
            .addProduct(product)
            .build()
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = productResponseGenFromProduct(products)
          val productPrice = productPriceGenFromProduct(products)
          val methodsFilter = BankerClient.PaymentMethodFilter.ForPaymentSystem(PaymentSystemId.TRUST)
          val trustGateConditions = TrustGateConditions(
            enabled = true,
            enabledUserIds = Set.empty,
            platforms = Set(Platform.PLATFORM_DESKTOP),
            minAndroidAppVersion = None,
            minIosAppVersion = None,
            purchaseCases = Set(PurchaseCase.AUTORU_PURCHASE),
            products = Set(Premium),
            firstPayment = true,
            probabilityPercent = 100
          )
          val requestWithFlag = genCustomRequest(web, Set(ForceIgnoreTrustExpResult.desktopExp))

          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse)
          when(salesmanUserClient.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice)
          when(salesmanUserClient.createTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          when(salesmanUserClient.getPaymentsHistory(?, ?, ?)(?)).thenReturnF(PaymentPage.getDefaultInstance)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(trustGateConditions)
          when(bankerClient.getPaymentMethods(?, eq(methodsFilter), ?, ?)(?)).thenReturnF(paymentMethods)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, Seq.empty[RawHeader]))

          val response = billingManager.initPayment(initAutoruPayment, salesmanDomain)(requestWithFlag).await
          response.getAccountBalance shouldBe accountInfo.getBalance
          checkPrices(productPrice, response)
        }
        "use YooMoney gate if haven't received experiment" in {
          val yandexPassportRequest = privateYandexSessionRequestGen.next
          val product = InitPaymentRequest.Product
            .newBuilder()
            .setName("premium")
            .setCount(1)
            .build()
          val stats = StatisticsParameters.newBuilder().setPlatform(Platform.PLATFORM_DESKTOP).build()
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next.toBuilder
            .setStatisticsParameters(stats)
            .clearPurchase()
            .setAutoruPurchase(AutoruPurchaseWithHashGen.next)
            .clearProduct()
            .addProduct(product)
            .build()
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = productResponseGenFromProduct(products)
          val productPrice = productPriceGenFromProduct(products)
          val methodsFilter = BankerClient.PaymentMethodFilter.All
          val trustGateConditions = TrustGateConditions(
            enabled = true,
            enabledUserIds = Set.empty,
            platforms = Set(Platform.PLATFORM_DESKTOP),
            minAndroidAppVersion = None,
            minIosAppVersion = None,
            purchaseCases = Set(PurchaseCase.AUTORU_PURCHASE),
            products = Set(Premium),
            firstPayment = true,
            probabilityPercent = 100
          )

          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse)
          when(salesmanUserClient.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice)
          when(salesmanUserClient.createTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          when(salesmanUserClient.getPaymentsHistory(?, ?, ?)(?)).thenReturnF(PaymentPage.getDefaultInstance)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(trustGateConditions)
          when(bankerClient.getPaymentMethods(?, eq(methodsFilter), ?, ?)(?)).thenReturnF(paymentMethods)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, Seq.empty[RawHeader]))

          val response = billingManager.initPayment(initAutoruPayment, salesmanDomain)(yandexPassportRequest).await
          response.getAccountBalance shouldBe accountInfo.getBalance
          checkPrices(productPrice, response)
        }
        "use YooMoney gate if haven't get into basket with TRUST" in {
          val yandexPassportRequest = privateYandexSessionRequestGen.next
          val product = InitPaymentRequest.Product
            .newBuilder()
            .setName("premium")
            .setCount(1)
            .build()
          val stats = StatisticsParameters.newBuilder().setPlatform(Platform.PLATFORM_DESKTOP).build()
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next.toBuilder
            .setStatisticsParameters(stats)
            .clearPurchase()
            .setAutoruPurchase(AutoruPurchaseWithHashGen.next)
            .clearProduct()
            .addProduct(product)
            .build()
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = productResponseGenFromProduct(products)
          val productPrice = productPriceGenFromProduct(products)
          val methodsFilter = BankerClient.PaymentMethodFilter.All
          val trustGateConditions = TrustGateConditions(
            enabled = true,
            enabledUserIds = Set.empty,
            platforms = Set(Platform.PLATFORM_DESKTOP),
            minAndroidAppVersion = None,
            minIosAppVersion = None,
            purchaseCases = Set(PurchaseCase.AUTORU_PURCHASE),
            products = Set(Premium),
            firstPayment = true,
            probabilityPercent = 100
          )
          val experimentsHeaders = Seq(RawHeader(UaasResponseHeaders.expFlags, falseTrustExpFlags))

          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse)
          when(salesmanUserClient.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice)
          when(salesmanUserClient.createTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          when(salesmanUserClient.getPaymentsHistory(?, ?, ?)(?)).thenReturnF(PaymentPage.getDefaultInstance)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(trustGateConditions)
          when(bankerClient.getPaymentMethods(?, eq(methodsFilter), ?, ?)(?)).thenReturnF(paymentMethods)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, experimentsHeaders))

          val response = billingManager.initPayment(initAutoruPayment, salesmanDomain)(yandexPassportRequest).await
          response.getAccountBalance shouldBe accountInfo.getBalance
          checkPrices(productPrice, response)
        }
        "use YooMoney gate if disable TRUST flag set" in {
          val product = InitPaymentRequest.Product
            .newBuilder()
            .setName("premium")
            .setCount(1)
            .build()
          val stats = StatisticsParameters.newBuilder().setPlatform(Platform.PLATFORM_DESKTOP).build()
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next.toBuilder
            .setStatisticsParameters(stats)
            .clearPurchase()
            .setAutoruPurchase(AutoruPurchaseWithHashGen.next)
            .clearProduct()
            .addProduct(product)
            .build()
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = productResponseGenFromProduct(products)
          val productPrice = productPriceGenFromProduct(products)
          val methodsFilter = BankerClient.PaymentMethodFilter.All
          val trustGateConditions = TrustGateConditions(
            enabled = true,
            enabledUserIds = Set.empty,
            platforms = Set(Platform.PLATFORM_DESKTOP),
            minAndroidAppVersion = None,
            minIosAppVersion = None,
            purchaseCases = Set(PurchaseCase.AUTORU_PURCHASE),
            products = Set(Premium),
            firstPayment = true,
            probabilityPercent = 100
          )
          val experimentsHeaders = Seq(RawHeader(UaasResponseHeaders.expFlags, trustExpFlags))
          val requestWithFlag = genCustomRequest(web, Set(ForceDisableTrust.desktopExp))

          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse)
          when(salesmanUserClient.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice)
          when(salesmanUserClient.createTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          when(salesmanUserClient.getPaymentsHistory(?, ?, ?)(?)).thenReturnF(PaymentPage.getDefaultInstance)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(trustGateConditions)
          when(bankerClient.getPaymentMethods(?, eq(methodsFilter), ?, ?)(?)).thenReturnF(paymentMethods)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, experimentsHeaders))

          val response = billingManager.initPayment(initAutoruPayment, salesmanDomain)(requestWithFlag).await
          response.getAccountBalance shouldBe accountInfo.getBalance
          checkPrices(productPrice, response)
        }
        "use YooMoney gate if trust not eligible" in {
          val initAutoruPayment = InitPaymentAutoruWithHashRequestGen.next
          val products = initAutoruPayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productResponse = productResponseGenFromProduct(products)
          val productPrice = productPriceGenFromProduct(products)
          val methodsFilter = BankerClient.PaymentMethodFilter.All
          val experimentsHeaders = Seq(RawHeader(UaasResponseHeaders.expFlags, trustExpFlags))

          when(salesmanUserClient.getActiveProducts(?)(?)).thenReturnF(productResponse)
          when(salesmanUserClient.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice)
          when(salesmanUserClient.createTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(TrustGateConditions.default)
          when(bankerClient.getPaymentMethods(?, eq(methodsFilter), ?, ?)(?)).thenReturnF(paymentMethods)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, experimentsHeaders))

          val response = billingManager.initPayment(initAutoruPayment, salesmanDomain).await
          response.getAccountBalance shouldBe accountInfo.getBalance
          checkPrices(productPrice, response)
        }
      }

      "BOOKING" should {
        val booking = initBookingPaymentRequestGen().next
        val product = Product.newBuilder().setName("booking").build()
        val initRequest = InitPaymentRequest.newBuilder().addProduct(product).setBooking(booking).build()
        val accountInfo = AccountInfoGen.next
        val response = InitPaymentResponseGen.next
        val experimentsConfig = ExperimentsConfig.newBuilder.build()

        "just delegate to BookingBillingManager" in {
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(bookingBillingManager.initPayment(?)(?)).thenReturnF(response)
          billingManager.initPayment(initRequest, SalesmanDomain.Booking).futureValue shouldBe response
          verify(bookingBillingManager).initPayment(booking)(request)
        }

        // Несмотря на то, что оплата бронирования с кошелька невозможна,
        // для оплаты картой всё равно нужно создать кошелёк, если его ещё нет.
        "create banker account if it doesn't exist" in {
          when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(UserResult.getDefaultInstance)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenThrowF(new AccountNotFoundException)
          when(bankerClient.createAccount(?, ?, ?)(?)).thenReturnF(Account.getDefaultInstance)
          when(bookingBillingManager.initPayment(?)(?)).thenReturnF(response)
          billingManager.initPayment(initRequest, SalesmanDomain.Booking).futureValue shouldBe response
          verify(bankerClient).getAccountInfo(user, user.toPlain, BankerDomains.Autoru)(request)
          verify(bankerClient).createAccount(eq(user), argThat[Account] { account =>
            account.getId == user.toPlain && account.getUser == user.toPlain
          }, eq(BankerDomains.Autoru))(eq(request))
        }

        "reject request if couldn't create banker account" in {
          val expectedResult = new Exception
          when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(UserResult.getDefaultInstance)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenThrowF(new AccountNotFoundException)
          when(bankerClient.createAccount(?, ?, ?)(?)).thenThrowF(expectedResult)
          billingManager.initPayment(initRequest, SalesmanDomain.Booking).failed.futureValue shouldBe expectedResult
        }

        "reject request with domain = autoru" in {
          when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(PaymentMethodGen.listOf(4))
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(uaasClient.getExperiments(?)(?)).thenReturnF((experimentsConfig, Seq.empty[RawHeader]))
          billingManager
            .initPayment(initRequest, AutoruSalesmanDomain)
            .failed
            .futureValue shouldBe an[IllegalArgumentException]
        }

        "reject request without product = booking" in {
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          val requestWithoutProduct = initRequest.toBuilder.clearProduct().build()
          billingManager
            .initPayment(requestWithoutProduct, SalesmanDomain.Booking)
            .failed
            .futureValue shouldBe an[IllegalArgumentException]
        }
      }

      "SUBSCRIBE_PURCHASE" should {

        "correctly init with subscribe on vin for " in {
          val salesmanDomain = AutoruSalesmanDomain
          val salesmanTransaction = createTransactionResultGen.next
          val accountInfo = AccountInfoGen.next
          val paymentMethods = PaymentMethodGen.listOf(4)
          val initSubscribePaymentBuilder = InitPaymentSubscribeRequestGen
            .suchThat(_.getProductList.asScala.toSeq.exists(_.getName == OffersHistoryReports.salesName))
            .next
            .toBuilder
          val vinOrPlate = "A777AA777"
          initSubscribePaymentBuilder.getSubscribePurchaseBuilder.setVinOrLicensePlate(vinOrPlate)
          val initSubscribePayment = initSubscribePaymentBuilder.build
          val products = initSubscribePayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productPrice = productPriceGenFromProduct(products)

          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(TrustGateConditions.default)
          when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(paymentMethods)
          when(salesmanUserClient.getConcreteSubscriptionPrice(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice.head)
          stub(salesmanUserClient.createTransaction(_: TransactionRequest, _: SalesmanDomain)(?)) {
            case (trn, _) =>
              val actualRequests = trn.getPayloadList.asScala.toSeq
              actualRequests.foreach { req =>
                val actualVin = req.getContext.getSubscription.getVinOrLicensePlate
                actualVin shouldBe vinOrPlate
              }
              Future.successful(salesmanTransaction)
          }

          val response = billingManager.initPayment(initSubscribePayment, salesmanDomain).await

          checkPrices(productPrice, response)
        }

        "correctly init for vin package when car info isn't given" in {
          val salesmanDomain = AutoruSalesmanDomain
          val salesmanTransaction = createTransactionResultGen.next
          val accountInfo = AccountInfoGen.next
          val paymentMethods = PaymentMethodGen.listOf(4)
          val initSubscribePaymentBuilder = InitPaymentSubscribeRequestGen.next.toBuilder
          initSubscribePaymentBuilder.getSubscribePurchaseBuilder.setCount(10).clearOfferId().clearVinOrLicensePlate()
          val initSubscribePayment = initSubscribePaymentBuilder.build
          val products = initSubscribePayment.getProductList.asScala.toSeq.flatMap(remapProduct)
          val productPrice = productPriceGenFromProduct(products)

          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(TrustGateConditions.default)
          when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(paymentMethods)
          when(salesmanUserClient.getConcreteSubscriptionPrice(?, ?, ?, ?, ?)(?)).thenReturnF(productPrice.head)
          when(salesmanUserClient.createTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)

          val response = billingManager.initPayment(initSubscribePayment, salesmanDomain).await

          checkPrices(productPrice, response)
        }

        "fail init for single vin report when car info isn't given" in {
          val salesmanDomain = AutoruSalesmanDomain
          val accountInfo = AccountInfoGen.next
          val paymentMethods = PaymentMethodGen.listOf(4)
          val initSubscribePaymentBuilder = InitPaymentSubscribeRequestGen.next.toBuilder
          initSubscribePaymentBuilder.getSubscribePurchaseBuilder.setCount(1).clearOfferId().clearVinOrLicensePlate()
          val initSubscribePayment = initSubscribePaymentBuilder.build

          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(TrustGateConditions.default)
          when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(paymentMethods)

          val res = billingManager.initPayment(initSubscribePayment, salesmanDomain).failed.futureValue

          res shouldBe a[SalesmanBadRequest]
          verifyNoMoreInteractions(salesmanUserClient)
        }

        "fail init with subscribe if invalid vin or licenseplate " in {
          val salesmanDomain = AutoruSalesmanDomain
          val accountInfo = AccountInfoGen.next
          val paymentMethods = PaymentMethodGen.listOf(4)
          val initSubscribePaymentBuilder = InitPaymentSubscribeRequestGen
            .suchThat(_.getProductList.asScala.toSeq.exists(_.getName == OffersHistoryReports.salesName))
            .next
            .toBuilder
          initSubscribePaymentBuilder.getSubscribePurchaseBuilder.setVinOrLicensePlate(".._\\/,sadf")
          val initSubscribePayment = initSubscribePaymentBuilder.build

          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(TrustGateConditions.default)
          when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(paymentMethods)

          val res = billingManager.initPayment(initSubscribePayment, salesmanDomain).failed.futureValue
          res shouldBe an[SalesmanBadRequest]
        }
      }
      "ACCOUNT_REFILL_PURCHASE" should {
        "correctly init" in {
          val salesmanDomain = AutoruSalesmanDomain
          val accountInfo = AccountInfoGen.next
          val paymentMethods = PaymentMethodGen.listOf(4)
          val initAccountRefillPayment = InitPaymentAccountRefillRequestGen.next

          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(trustGateConditionsFeature.value).thenReturn(TrustGateConditions.default)
          when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenReturnF(paymentMethods)

          billingManager.initPayment(initAccountRefillPayment, salesmanDomain).await
          verifyNoMoreInteractions(salesmanUserClient)
        }
      }
    }
    "processPayment" when {
      "AccountRefill" should {
        "correctly process" in {
          val salesmanDomain = AutoruSalesmanDomain
          val processPaymentRequest = ProcessPaymentAccountRefillRequestGen.next
          val form = PaymentRequestFormGen.next
          val account = AccountGen.listOf(3)
          val accountMain = AccountGen.next
          val userResult = PassportUserResultGen.next

          when(bankerClient.createAccount(?, ?, ?)(?)).thenReturnF(accountMain)
          when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(userResult)
          when(bankerClient.getAccounts(?, ?)(?)).thenReturnF(account)
          when(bankerClient.requestPayment(?, ?, ?, ?, ?, ?)(?)).thenReturnF(form)

          billingManager.processPayment(processPaymentRequest, salesmanDomain).await
        }
      }
      "PayByAccount" should {
        "correctly process" in {
          val salesmanDomain = AutoruSalesmanDomain
          val accountInfo = AccountInfoGen.next
          val salesmanTransaction = TransactionGen.next.toBuilder.setStatus(Transaction.Status.PROCESS).build()
          val processPaymentRequest = ProcessPaymentPayByAccountRequestGen.next
          val bankerTransaction = BankerModelGenerators.TransactionGen.next
          val account = AccountGen.listOf(3)
          val accountMain = AccountGen.next
          val userResult = PassportUserResultGen.next
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(bankerClient.payByAccount(?, ?, ?)(?)).thenReturnF(bankerTransaction)
          when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          when(bankerClient.getAccounts(?, ?)(?)).thenReturnF(account)
          when(bankerClient.createAccount(?, ?, ?)(?)).thenReturnF(accountMain)
          when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(userResult)
          billingManager.processPayment(processPaymentRequest, salesmanDomain).await
        }
        "handle transaction which process InvalidTicketStatus exception" in {
          forAll(ProcessPaymentPayByAccountRequestGen, TransactionInvalidTicketStatusProducerGen) {
            (processPaymentRequest, salesmanTransaction) =>
              val salesmanDomain = AutoruSalesmanDomain
              when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
              billingManager.processPayment(processPaymentRequest, salesmanDomain).failed.futureValue match {
                case i: InvalidTicketStatus =>
                  i.descriptionRu shouldBe extractDescriptionRu(i.code)
                case other =>
                  fail(s"Unexpected $other exception")
              }
          }
        }
      }
      "PayByMethod" should {
        "correctly process" in {
          val salesmanDomain = AutoruSalesmanDomain
          val accountInfo = AccountInfoGen.next
          val account = AccountGen.listOf(3)
          val accountMain = AccountGen.next
          val userResult = PassportUserResultGen.next
          val salesmanTransaction = TransactionGen.next.toBuilder.setStatus(Transaction.Status.PROCESS).build()
          val processPaymentRequest = ProcessPaymentPayByPMRequestGen.next
          val form = PaymentRequestFormGen.next

          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(bankerClient.createAccount(?, ?, ?)(?)).thenReturnF(accountMain)
          when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(userResult)
          when(bankerClient.getAccounts(?, ?)(?)).thenReturnF(account)
          when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          stub(
            bankerClient
              .requestPayment(
                _: AutoruUser,
                _: PaymentSystemId,
                _: String,
                _: PaymentRequest.Source,
                _: BankerDomain,
                _: Option[String]
              )(
                _: Request
              )
          ) {
            case (_, _, _, source, _, _, _) if source != null =>
              source.getContext.getTarget shouldBe Target.PURCHASE
              val goods = source.getReceipt.getGoodsList.asScala.toSeq.map(_.getName).toSet
              val products = salesmanTransaction.getPayloadList.asScala.toSeq.map(_.getProduct).toSet
              val descriptions = products.flatMap(p => getProductDescription(p).map(_.oldReceiptDescription))
              descriptions should contain theSameElementsAs goods
              Future.successful(form)
          }

          billingManager.processPayment(processPaymentRequest, salesmanDomain).await
        }
        "handle already closed transaction" in {
          val salesmanDomain = AutoruSalesmanDomain
          val salesmanTransaction = TransactionGen.next.toBuilder.setStatus(Transaction.Status.CLOSED).build()
          val processPaymentRequest = ProcessPaymentPayByPMRequestGen.next
          when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
          intercept[ProductAlreadyActivated] {
            billingManager.processPayment(processPaymentRequest, salesmanDomain).await
          }
        }
        "handle transaction which process InvalidTicketStatus exception" in {
          forAll(ProcessPaymentPayByPMRequestGen, TransactionInvalidTicketStatusProducerGen) {
            (processPaymentRequest, salesmanTransaction) =>
              val salesmanDomain = AutoruSalesmanDomain
              when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(salesmanTransaction)
              billingManager.processPayment(processPaymentRequest, salesmanDomain).failed.futureValue match {
                case i: InvalidTicketStatus =>
                  i.descriptionRu shouldBe extractDescriptionRu(i.code)
                case other =>
                  fail(s"Unexpected $other exception")
              }
          }
        }

        "correctly parse badges" in {
          val badgeCount = 3
          val requestProducts = Seq(
            BillingModelV2.InitPaymentRequest.Product
              .newBuilder()
              .setName(AutoruProduct.Badge.name)
              .setCount(badgeCount)
              .build(),
            BillingModelV2.InitPaymentRequest.Product
              .newBuilder()
              .setName(AutoruProduct.PackageTurbo.name)
              .setCount(1)
              .build()
          )
          val expected = Seq.fill(badgeCount)(AutoruProduct.Badge) :+ AutoruProduct.PackageTurbo
          val products = requestProducts.flatMap(remapProduct)
          products shouldBe expected
        }
      }

      "receive booking payment" should {
        val accountInfo = AccountInfoGen.next
        val account = AccountGen.listOf(3)
        val accountMain = AccountGen.next
        val userResult = PassportUserResultGen.next
        val bookingTransactionId = readableString.next
        val rawPaymentCost = 1500000
        val paymentCost = KopeckPrice.newBuilder().setKopecks(rawPaymentCost).build()
        val processPaymentRequest = {
          val b = ProcessPaymentPayByPMRequestGen.next.toBuilder
          b.getPayByPaymentMethodBuilder.setTicketId(bookingTransactionId)
          b.build()
        }
        val form = PaymentRequestFormGen.next

        def initMocks(): Unit = {
          when(bookingBillingManager.getPaymentCost(?)(?)).thenReturnF(paymentCost)
          when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenReturnF(accountInfo)
          when(bankerClient.createAccount(?, ?, ?)(?)).thenReturnF(accountMain)
          when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(userResult)
          when(bankerClient.getAccounts(?, ?)(?)).thenReturnF(account)
          when(bankerClient.requestPayment(?, ?, ?, ?, ?, ?)(?)).thenReturnF(form)
        }

        def processPayment(): ProcessPaymentResponse = {
          initMocks()
          billingManager.processPayment(processPaymentRequest, SalesmanDomain.Booking).futureValue
        }

        def verifyRequestPayment(matcher: ArgumentMatcher[PaymentRequest.Source]): Unit =
          verify(bankerClient).requestPayment(?, ?, ?, argThat(matcher), ?, ?)(?)

        "request booking cost from booking-api for requested transaction" in {
          processPayment()
          verify(bookingBillingManager).getPaymentCost(bookingTransactionId)
        }

        // Чтобы банкир дедублицировал запросы по id транзакции
        "pass booking transaction id from request in request payment options" in {
          processPayment()
          // 4 -- это id домена booking для банкира, см. определение SalesmanDomain.Booking
          verifyRequestPayment(_.getOptions.getId == s"$bookingTransactionId@4")
        }

        // Чтобы банкир отправил id транзакции и домен (тест ниже) в нотификации оплаты в кафку
        "pass booking transaction id from request in request payment payload" in {
          processPayment()
          verifyRequestPayment(
            _.getPayload.getStruct.getFieldsOrThrow("transaction").getStringValue == bookingTransactionId
          )
        }

        "pass booking domain in request payment payload" in {
          processPayment()
          verifyRequestPayment(_.getPayload.getStruct.getFieldsOrThrow("domain").getStringValue == "booking")
        }

        "request payment for cost received from booking-api" in {
          processPayment()
          verifyRequestPayment(_.getAmount == rawPaymentCost)
        }

        "request payment for target = SECURITY_DEPOSIT" in {
          processPayment()
          verifyRequestPayment(_.getContext.getTarget == Target.SECURITY_DEPOSIT)
        }

        "request payment in autoru banker domain" in {
          processPayment()
          verify(bankerClient).requestPayment(?, ?, ?, ?, eq(BankerDomains.Autoru), ?)(?)
        }
      }
    }
    "getPaymentsHistory" should {
      "return payments from salesman" in {
        val paging = Paging(1, 10)
        val salesmanDomain = SalesmanDomainGen.next
        val payments = paymentPageGen(paging.pageSize, paging.page, 3).next

        when(salesmanUserClient.getPaymentsHistory(?, ?, ?)(?)).thenReturnF(payments)
        val response = billingManager.getPaymentsHistory(paging, salesmanDomain)
        verify(salesmanUserClient).getPaymentsHistory(
          user,
          salesmanDomain,
          paging
        )
        response.futureValue shouldBe payments
      }
    }

    "antirobot" should {
      "work for banned users" in {
        val salesmanDomain = AutoruSalesmanDomain
        val processPaymentRequest = ProcessPaymentAccountRefillRequestGen.next
        val form = PaymentRequestFormGen.next
        val account = AccountGen.listOf(3)
        val accountMain = AccountGen.next
        val userResult = PassportUserResultGen.next
        when(fakeManager.shouldSkipPaymentRequestForBannedUsers(?)).thenReturn(true)
        when(bankerClient.createAccount(?, ?, ?)(?)).thenReturnF(accountMain)
        when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(userResult)
        when(bankerClient.getAccounts(?, ?)(?)).thenReturnF(account)
        when(bankerClient.requestPayment(?, ?, ?, ?, ?, ?)(?)).thenReturnF(form)

        val user = PrivateUserRefGen.next
        val token = TestTokenGen.next
        val session = {
          val builder =
            SessionResultGen.next.toBuilder
          val b = UserModerationStatus
            .newBuilder()
            .putAllBans(Map("someDomain" -> DomainBan.newBuilder.addReasons("USER_HACKED").build).asJava)
          builder.getUserBuilder.setModerationStatus(b)
          builder.build()
        }
        val request = new RequestImpl
        request.setTrace(Traced.empty)
        request.setRequestParams(RequestParams.construct("1.1.1.1"))
        request.setToken(token)
        request.setNewDeviceUid(DeviceUidGen.next)
        request.setUser(user)
        request.setSession(session)
        TokenServiceImpl.getStaticApplication(token).foreach(request.setApplication)

        an[UnknownProcessPaymentRequest] shouldBe thrownBy(
          billingManager.processPayment(processPaymentRequest, salesmanDomain)(request).await
        )
      }
      "work for robots" in {
        val salesmanDomain = AutoruSalesmanDomain
        val processPaymentRequest = ProcessPaymentAccountRefillRequestGen.next
        val form = PaymentRequestFormGen.next
        val account = AccountGen.listOf(3)
        val accountMain = AccountGen.next
        val userResult = PassportUserResultGen.next
        when(fakeManager.shouldSkipPaymentRequestForRobots(?)).thenReturn(true)
        when(fakeManager.getFakeProcessPaymentResponse(?)).thenReturn(ProcessPaymentResponseGen.next)
        when(bankerClient.createAccount(?, ?, ?)(?)).thenReturnF(accountMain)
        when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(userResult)
        when(bankerClient.getAccounts(?, ?)(?)).thenReturnF(account)
        when(bankerClient.requestPayment(?, ?, ?, ?, ?, ?)(?)).thenReturnF(form)

        val user = PrivateUserRefGen.next
        val token = TestTokenGen.next
        val session = {
          val builder =
            SessionResultGen.next.toBuilder
          builder.build()
        }
        val request = new RequestImpl
        request.setTrace(Traced.empty)
        request.setRequestParams(RequestParams.construct("1.1.1.1", antirobotDegradation = true))
        request.setToken(token)
        request.setNewDeviceUid(DeviceUidGen.next)
        request.setUser(user)
        request.setSession(session)

        TokenServiceImpl.getStaticApplication(token).foreach(request.setApplication)

        billingManager.processPayment(processPaymentRequest, salesmanDomain)(request).await
        verifyNoMoreInteractions(bankerClient)
      }
    }
  }
}

object BillingManagerSpec {
  val dummyException = new RuntimeException("artificial") with NoStackTrace
}
