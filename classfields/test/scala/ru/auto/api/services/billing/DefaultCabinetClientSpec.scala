package ru.auto.api.services.billing

import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.StatusCodes.BadRequest
import org.scalatest.Assertion
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.exceptions.NotEnoughFundsOnAccount
import ru.auto.api.model.AutoruProduct.{Boost, PackageTurbo, SpecialOffer}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.services.billing.AutoruProductActions.{Activate, Deactivate}
import ru.auto.api.services.octopus.OctopusClient
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.Request
import ru.auto.salesman.SalesmanModel.AutoruProduct

import scala.jdk.CollectionConverters._

class DefaultCabinetClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with MockitoSugar {

  val client = new DefaultCabinetClient(http)
  val dealerRegionId = 1L

  "applyProduct()" should {

    "apply product for car offer" in {
      forAll(CarsOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectCarsUrl(offer, product)(request)
        respondTrue()
        shouldApplyProduct(offer, product)(request)
      }
    }

    "apply product for moto offer" in {
      forAll(MotoOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectMotoUrl(offer, product)(request)
        respondTrue()
        shouldApplyProduct(offer, product)(request)
      }
    }

    "apply product for trucks offer" in {
      forAll(TruckOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectTruckUrl(offer, product)(request)
        respondTrue()
        shouldApplyProduct(offer, product)(request)
      }
    }

    "apply badge for car offer" in {
      forAll(CarsOfferGen, AutoruBadgeProductGen, dealerRequestGen) { (offer, product, request) =>
        expectCarsUrl(offer, product)(request)
        respondTrue()
        shouldApplyProduct(offer, product)(request)
      }
    }

    "apply badge for moto offer" in {
      forAll(CarsOfferGen, AutoruBadgeProductGen, dealerRequestGen) { (offer, product, request) =>
        expectCarsUrl(offer, product)(request)
        respondTrue()
        shouldApplyProduct(offer, product)(request)
      }
    }

    "apply badge for trucks offer" in {
      forAll(CarsOfferGen, AutoruBadgeProductGen, dealerRequestGen) { (offer, product, request) =>
        expectCarsUrl(offer, product)(request)
        respondTrue()
        shouldApplyProduct(offer, product)(request)
      }
    }

    "deactivate empty badge for cars offer" in {
      forAll(CarsOfferGen, dealerRequestGen) { (offer, request) =>
        val product = AutoruProduct.newBuilder().setCode("all_sale_badge").build()
        expectCarsUrl(offer, product, Deactivate)(request)
        respondTrue()
        shouldDeactivateProduct(offer, product)(request)
      }
    }

    "throw not enough funds on charge false for cars" in {
      forAll(CarsOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectCarsUrl(offer, product)(request)
        respondFalse()
        shouldNotApplyProduct(offer, product)(request)
      }
    }

    "throw not enough funds on charge false for moto" in {
      forAll(MotoOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectMotoUrl(offer, product)(request)
        respondFalse()
        shouldNotApplyProduct(offer, product)(request)
      }
    }

    "throw not enough funds on charge false for trucks" in {
      forAll(TruckOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectTruckUrl(offer, product)(request)
        respondFalse()
        shouldNotApplyProduct(offer, product)(request)
      }
    }

    "throw runtime exception on charge error for cars" in {
      forAll(CarsOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectCarsUrl(offer, product)(request)
        respondError()
        shouldThrowRuntimeException(offer, product)(request)
      }
    }

    "throw runtime exception on charge error for moto" in {
      forAll(MotoOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectMotoUrl(offer, product)(request)
        respondError()
        shouldThrowRuntimeException(offer, product)(request)
      }
    }

    "throw runtime exception on charge error for trucks" in {
      forAll(TruckOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectTruckUrl(offer, product)(request)
        respondError()
        shouldThrowRuntimeException(offer, product)(request)
      }
    }

    "throw runtime exception on empty charge response for cars" in {
      forAll(CarsOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectCarsUrl(offer, product)(request)
        respondEmpty()
        shouldThrowRuntimeException(offer, product)(request)
      }
    }

    "throw runtime exception on empty charge response for moto" in {
      forAll(MotoOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectMotoUrl(offer, product)(request)
        respondEmpty()
        shouldThrowRuntimeException(offer, product)(request)
      }
    }

    "throw runtime exception on empty charge response for trucks" in {
      forAll(TruckOfferGen, AutoruProductGen, dealerRequestGen) { (offer, product, request) =>
        expectTruckUrl(offer, product)(request)
        respondEmpty()
        shouldThrowRuntimeException(offer, product)(request)
      }
    }

    def expectCarsUrl(
        offer: Offer,
        product: AutoruProduct,
        action: AutoruProductAction = Activate
    )(implicit request: Request): Unit =
      expectUrl(offer, "CARS", product, action)

    def expectMotoUrl(
        offer: Offer,
        product: AutoruProduct,
        action: AutoruProductAction = Activate
    )(implicit request: Request): Unit =
      expectUrl(offer, "MOTO", product, action)

    def expectTruckUrl(
        offer: Offer,
        product: AutoruProduct,
        action: AutoruProductAction = Activate
    )(implicit request: Request): Unit =
      expectUrl(offer, "COMMERCIAL", product, action)

    def expectUrl(
        offer: Offer,
        categoryCode: String,
        product: AutoruProduct,
        action: AutoruProductAction
    )(implicit request: Request): Unit =
      http.expectUrl(
        HttpMethods.GET,
        s"/common/v1.0.0/services/post" +
          s"?access_key=${OctopusClient.accessKey}" +
          s"&remote_ip=${request.user.ip}" +
          s"&sale_id=${offer.id.id}" +
          s"&category_code=$categoryCode" +
          s"&alias=${product.getCode}" +
          s"&client_id=${request.user.userRef.asDealer.clientId}" +
          s"&action=$action" +
          product.getBadgesList.asScala.zipWithIndex.map {
            case (badge, idx) => s"&badges%5B$idx%5D=$badge"
          }.mkString
      )

    def shouldApplyProduct(offer: Offer, product: AutoruProduct)(implicit request: Request): Assertion =
      client.postProduct(offer.id, offer.getCategory, product, Activate).futureValue shouldBe (())

    def shouldDeactivateProduct(offer: Offer, product: AutoruProduct)(implicit request: Request): Assertion =
      client.postProduct(offer.id, offer.getCategory, product, Deactivate).futureValue shouldBe (())

    def shouldNotApplyProduct(offer: Offer, product: AutoruProduct)(implicit request: Request): Assertion = {
      val ex = client.postProduct(offer.id, offer.getCategory, product, Activate).failed.futureValue
      ex shouldBe a[NotEnoughFundsOnAccount]
    }

    def shouldThrowRuntimeException(offer: Offer, product: AutoruProduct)(implicit request: Request): Assertion = {
      val ex = client.postProduct(offer.id, offer.getCategory, product, Activate).failed.futureValue
      ex shouldBe a[RuntimeException]
    }
  }

  "requestServices" should {

    "request turbo" in {
      forAll(TruckOfferGen, dealerRequestGen) { (baseOffer, request) =>
        http.reset()
        val offer = baseOffer.toBuilder.setId("15858730-aae109ab").build()
        http.expectUrl(
          s"/desktop/v1.0.0/services/request/" +
            s"?access_key=${OctopusClient.accessKey}" +
            s"&remote_ip=${request.user.ip}" +
            s"&sale_id=15858730" +
            s"&category_code=COMMERCIAL" +
            s"&session_id=${request.user.sessionID.value}" +
            s"&aliases%5B0%5D=package_turbo"
        )
        http.respondWith("")
        client
          .requestServices(
            offer,
            Seq(PackageTurbo)
          )(request)
          .futureValue shouldBe (())
        http.verifyRequestDone()
      }
    }

    "request special for cars new" in {
      forAll(DealerCarsNewOfferGen, dealerRequestGen) { (baseOffer, request) =>
        http.reset()
        val offer = baseOffer.toBuilder.setId("15858730-aae109ab").build()
        http.expectUrl(
          s"/desktop/v1.0.0/services/request/" +
            s"?access_key=${OctopusClient.accessKey}" +
            s"&remote_ip=${request.user.ip}" +
            s"&sale_id=15858730" +
            s"&category_code=CARS" +
            s"&session_id=${request.user.sessionID.value}" +
            s"&aliases%5B0%5D=all_sale_fresh" +
            s"&aliases%5B1%5D=all_sale_special"
        )
        http.respondWith("")
        client
          .requestServices(
            offer,
            Seq(Boost, SpecialOffer)
          )(request)
          .futureValue shouldBe (())
        http.verifyRequestDone()
      }
    }

  }

  def respondTrue(): Unit =
    http.respondWithJsonFrom("/billing/billing_true.json")

  def respondFalse(): Unit =
    http.respondWithJsonFrom("/billing/billing_false.json")

  def respondError(): Unit =
    http.respondWithJsonFrom(BadRequest, "/billing/billing_error.json")

  def respondEmpty(): Unit =
    http.respondWith("{}")

  def respondCrmClientMultiPostingEnabled(): Unit =
    http.respondWithJsonFrom("/cabinet/crm_client_multiposting_enabled.json")
}
