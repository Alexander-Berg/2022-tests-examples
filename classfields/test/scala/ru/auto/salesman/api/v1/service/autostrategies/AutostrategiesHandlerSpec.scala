package ru.auto.salesman.api.v1.service.autostrategies

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.RouteTestTimeout
import org.scalacheck.Gen
import org.scalatest.Assertion
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.api.SalesmanApiUtils._
import ru.auto.salesman.api.view.autostrategies.{
  AutostrategyIdView,
  AutostrategyView,
  OfferAutostrategiesView
}
import ru.auto.salesman.model.autostrategies.{
  AlwaysAtFirstPagePayload,
  Autostrategy,
  AutostrategyId
}
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.service.AutostrategiesService
import ru.auto.salesman.test.IntegrationPropertyCheckConfig
import ru.auto.salesman.test.model.gens._
import spray.json._

import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions

class AutostrategiesHandlerSpec extends RoutingSpec with IntegrationPropertyCheckConfig {

  implicit val routeTestTimeout = RouteTestTimeout(2.seconds)

  private val service = mock[AutostrategiesService]

  implicit private def asStrings(offerIds: List[OfferIdentity]): List[String] =
    offerIds.map(_.value)

  private val route = new AutostrategiesHandler(service).route

  "GET /" should {

    "get autostrategies successfully" in {
      forAll(Gen.nonEmptyListOf(AutoruOfferIdGen), OfferAutostrategiesGen) {
        (offerIds, offerAutostrategies) =>
          (service.get _)
            .expects {
              assertArgs { receivedOfferIds: Iterable[OfferIdentity] =>
                receivedOfferIds.toSet == offerIds.toSet
              }
            }
            .returningZ(offerAutostrategies)
          request(offerIds) ~> seal(route) ~> check {
            status shouldBe OK
            val response = responseAs[Iterable[OfferAutostrategiesView]]
            response.map(
              _.asModel
            ) should contain theSameElementsAs offerAutostrategies
          }
      }
    }

    "throw 400 on offer id with empty hash" in {
      forAll(Gen.nonEmptyListOf(Gen.posNum[Long].map(_.toString + "-"))) { badOfferIds =>
        request(badOfferIds) ~> seal(route) ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    "throw 400 on alphastr offer id" in {
      forAll(Gen.nonEmptyListOf(Gen.alphaStr)) { badOfferIds =>
        request(badOfferIds) ~> seal(route) ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    "throw 404 on no ids" in {
      request(Nil) ~> seal(route) ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "throw 500 on unexpected error" in {
      forAll(Gen.nonEmptyListOf(AutoruOfferIdGen)) { offerIds =>
        (service.get _)
          .expects {
            assertArgs { receivedOfferIds: Iterable[OfferIdentity] =>
              receivedOfferIds.toSet == offerIds.toSet
            }
          }
          .throwingZ(new RuntimeException)
        request(offerIds) ~> seal(route) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    def request(offerIds: List[String]): HttpRequest = {
      val uri = "/?" + offerIds.map("offerId=" + _).mkString("&")
      Get(uri).withSalesmanTestHeader()
    }
  }

  "PUT /" should {

    "put autostrategies successfully" in {
      forAll(AutostrategyListGen) { autostrategies =>
        (service.put _).expects(autostrategies).returningZ(())
        request(autostrategies) ~> seal(route) ~> check {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "throw 400 on autostrategies with empty hash offer id" in {
      forAll(AutostrategyListGen) { autostrategies =>
        val put = Put("/")
          .withSalesmanTestHeader()
          .withEntity(
            HttpEntity(
              `application/json`,
              autostrategies
                .map(AutostrategyView.asView)
                .map(withEmptyHashOfferId)
                .toJson
                .compactPrint
            )
          )

        put ~> seal(route) ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    "throw 400 on autostrategies with alphastr offer id" in {
      forAll(AutostrategyListGen) { autostrategies =>
        val put = Put("/")
          .withSalesmanTestHeader()
          .withEntity(
            HttpEntity(
              `application/json`,
              autostrategies
                .map(AutostrategyView.asView)
                .map(withAlphaStrOfferId)
                .toJson
                .compactPrint
            )
          )

        put ~> seal(route) ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    "throw 400 on autostrategies with bad interval" in {
      check400(withWrongInterval)
    }

    "throw 400 on autostrategies with from and to in past" in {
      check400(withFromAndToInPast)
    }

    "throw 400 on autostrategies with from in past" in {
      check400(withFromInPast)
    }

    "throw 400 on autostrategies with bad apps per day" in {
      check400(withWrongApplicationsPerDay)
    }

    "throw 400 on autostrategies with bad always at first page payload" in {
      check400(withWrongAlwaysAtFirstPage)
    }

    "throw 500 on unexpected error" in {
      forAll(AutostrategyListGen) { autostrategies =>
        (service.put _).expects(autostrategies).throwingZ(new RuntimeException)
        request(autostrategies) ~> seal(route) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    def request(autostrategies: Iterable[Autostrategy]): HttpRequest =
      Put("/")
        .withSalesmanTestHeader()
        .withEntity(
          HttpEntity(
            `application/json`,
            autostrategies.map(AutostrategyView.asView).toJson.compactPrint
          )
        )

    def check400(wrongConversion: Autostrategy => Autostrategy): Assertion =
      forAll(AutostrategyListGen.map(_.map(wrongConversion))) { autostrategies =>
        request(autostrategies) ~> seal(route) ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }

    def withEmptyHashOfferId(autostrategy: AutostrategyView): AutostrategyView =
      autostrategy.copy(offerId = toEmptyHashOfferId(autostrategy.offerId))

    def withAlphaStrOfferId(autostrategy: AutostrategyView): AutostrategyView =
      autostrategy.copy(offerId = alphaStrOfferId)

    def withWrongInterval(autostrategy: Autostrategy): Autostrategy =
      autostrategy.copy(
        fromDate = autostrategy.toDate.plusDays(1),
        toDate = autostrategy.fromDate
      )

    def withFromAndToInPast(autostrategy: Autostrategy): Autostrategy = {
      val old = OldAutostrategyGen.next
      autostrategy.copy(
        fromDate = old.fromDate,
        toDate = old.toDate
      )
    }

    def withFromInPast(autostrategy: Autostrategy): Autostrategy = {
      val old = OldAutostrategyGen.next
      autostrategy.copy(fromDate = old.fromDate)
    }

    def withWrongApplicationsPerDay(autostrategy: Autostrategy): Autostrategy =
      autostrategy.copy(maxApplicationsPerDay = Some(Gen.negNum[Int].next))

    def withWrongAlwaysAtFirstPage(autostrategy: Autostrategy): Autostrategy =
      autostrategy.copy(
        payload = AlwaysAtFirstPagePayload(
          forMarkModelListing = false,
          forMarkModelGenerationListing = false
        )
      )
  }

  "PUT /delete" should {

    "delete autostrategies successfully" in {
      forAll(AutostrategyIdListGen) { ids =>
        (service.delete _).expects(ids).returningZ(())
        uri(ids) ~> seal(route) ~> check {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "throw 400 on empty hash offer id" in {
      forAll(AutostrategyIdListGen) { ids =>
        val put = Put("/delete")
          .withSalesmanTestHeader()
          .withEntity(
            HttpEntity(
              `application/json`,
              ids
                .map(AutostrategyIdView.asView)
                .map(withWrongOfferId)
                .toJson
                .compactPrint
            )
          )

        put ~> seal(route) ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    "throw 400 on alphastr offer id" in {
      forAll(AutostrategyIdListGen) { ids =>
        val put = Put("/delete")
          .withSalesmanTestHeader()
          .withEntity(
            HttpEntity(
              `application/json`,
              ids
                .map(AutostrategyIdView.asView)
                .map(withAlphaStrOfferId)
                .toJson
                .compactPrint
            )
          )

        put ~> seal(route) ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    "throw 500 on unexpected error" in {
      forAll(AutostrategyIdListGen) { ids =>
        (service.delete _).expects(ids).throwingZ(new RuntimeException)
        uri(ids) ~> seal(route) ~> check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    def uri(ids: Iterable[AutostrategyId]): HttpRequest = {
      val print = ids.map(AutostrategyIdView.asView).toJson.compactPrint
      Put("/delete")
        .withSalesmanTestHeader()
        .withEntity(
          HttpEntity(
            `application/json`,
            print
          )
        )
    }

    def withWrongOfferId(id: AutostrategyIdView): AutostrategyIdView =
      id.copy(offerId = toEmptyHashOfferId(id.offerId))

    def withAlphaStrOfferId(id: AutostrategyIdView): AutostrategyIdView =
      id.copy(offerId = alphaStrOfferId)
  }

  def toEmptyHashOfferId(offerId: String): String = offerId.split("-")(0) + "-"

  def alphaStrOfferId: String = Gen.alphaStr.next
}
