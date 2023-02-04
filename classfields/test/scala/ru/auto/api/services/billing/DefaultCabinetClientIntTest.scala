package ru.auto.api.services.billing

import org.scalatestplus.mockito.MockitoSugar
import ru.auto.api.ApiOfferModel.Category.CARS
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.auth.Application.swagger
import ru.auto.api.exceptions.ApplyProductNotAllowedException
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.CategorySelector.{Cars, Moto, StrictCategory, Trucks}
import ru.auto.api.model.DealerUserRoles._
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.{UserRef, _}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.billing.AutoruProductActions.{Activate, Deactivate}
import ru.auto.api.services.vos.DefaultVosClient
import ru.auto.api.util.offer.MinimalTestOffers
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.salesman.SalesmanModel.AutoruProduct
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.duration.DurationDouble

class DefaultCabinetClientIntTest extends HttpClientSuite with MockitoSugar with MinimalTestOffers {

  override protected def config: HttpClientConfig =
    HttpClientConfig("https", "cabinet-api.test.avto.ru", 443)

  private val client = new DefaultCabinetClient(http)

  private val vosClient = {
    new DefaultVosClient(
      HttpClientSuite.createClientForTests(
        HttpClientConfig("http", "vos2-autoru-api.vrts-slb.test.vertis.yandex.net", 80),
        cachingProxy
      ),
      // copy-paste from vos client integration test
      addTimeout = 30.seconds,
      rotateTimeout = 10.seconds,
      blurTimeout = 30.seconds
    )
  }

  private val testDealer = UserRef.dealer(20101)
  private val poorDealer = UserRef.dealer(8452)
  private val testDealerRequest = requestForDealer(testDealer.clientId)
  private val poorDealerRequest = requestForDealer(poorDealer.clientId)

  private lazy val testDealerCarsOffer =
    createAndPublishDraft(Cars, testDealer, minimalCarOffer)

  private lazy val testDealerTruckOffer =
    createAndPublishDraft(Trucks, testDealer, minimalTruckOffer)

  private lazy val testDealerMotoOffer =
    createAndPublishDraft(Moto, testDealer, minimalMotoOffer)

  private lazy val poorDealerMotoOffer =
    createAndPublishDraft(Moto, poorDealer, minimalMotoOffer)

  test("applyProduct should deactivate premium, then activate, then deactivate") {
    val product = AutoruProduct.newBuilder().setCode("all_sale_premium").build()
    implicit val r: Request = testDealerRequest
    val offerId = testDealerCarsOffer.id
    // if premium is already applied, php returns error
    // so, first invocation may return error, that's why we need recover here
    client.postProduct(offerId, CARS, product, Deactivate).recover { case _ => () }.futureValue
    client.postProduct(offerId, CARS, product, Activate).futureValue shouldBe (())
    client.postProduct(offerId, CARS, product, Activate).failed.futureValue shouldBe
      an[ApplyProductNotAllowedException]
    client.postProduct(offerId, CARS, product, Deactivate).futureValue shouldBe (())
    client.postProduct(offerId, CARS, product, Deactivate).failed.futureValue shouldBe
      an[ApplyProductNotAllowedException]
  }

  test("applyProduct should deactivate special, then activate, then deactivate") {
    val product = AutoruProduct.newBuilder().setCode("all_sale_special").build()
    implicit val r: Request = testDealerRequest
    val offerId = testDealerCarsOffer.id
    // if special is already applied, php returns error
    // so, first invocation may return error, that's why we need recover here
    client.postProduct(offerId, CARS, product, Deactivate).recover { case _ => () }.futureValue
    client.postProduct(offerId, CARS, product, Activate).futureValue shouldBe (())
    client.postProduct(offerId, CARS, product, Activate).failed.futureValue shouldBe
      an[ApplyProductNotAllowedException]
    client.postProduct(offerId, CARS, product, Deactivate).futureValue shouldBe (())
    client.postProduct(offerId, CARS, product, Deactivate).failed.futureValue shouldBe
      an[ApplyProductNotAllowedException]
  }

  test("applyProduct should activate fresh") {
    val product = AutoruProduct.newBuilder().setCode("all_sale_fresh").build()
    client.postProduct(testDealerCarsOffer.id, CARS, product, Activate)(testDealerRequest).futureValue shouldBe (())
  }

  test("applyProduct should activate badges") {
    val product = AutoruBadgeProductGen.next
    client.postProduct(testDealerCarsOffer.id, CARS, product, Activate)(testDealerRequest).futureValue shouldBe (())
  }

  private def requestForDealer(dealerId: Long, sessionId: Option[SessionID] = None) =
    generateRequest(UserRef.dealer(dealerId), sessionId)

  private def generateRequest(userRef: UserRef, sessionId: Option[SessionID] = None) = {
    val request = new RequestImpl
    request.setTrace(Traced.empty)
    request.setRequestParams(RequestParams.construct(ip = "1.1.1.1", sessionId = sessionId))
    request.setUser(userRef)
    request.setDealerRole(Manager)
    request
  }

  private def createAndPublishDraft(category: StrictCategory, user: RegisteredUserRef, offer: Offer) = {
    implicit val request: RequestImpl = new RequestImpl
    request.setApplication(swagger)
    request.setUser(user)
    request.setRequestParams(RequestParams.construct("1.1.1.1"))
    val draft = vosClient.createDraft(category, user, offer).futureValue
    vosClient.publishDraft(category, user, draft.id, AdditionalDraftParams(None, None, None)).futureValue
  }
}
