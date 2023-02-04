package ru.auto.salesman.api.v1.service.telepony

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.RouteTestTimeout
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel._
import ru.auto.api.api_offer_model.{Category => ScalaCategory, Section => ScalaSection}
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.calls.CallsTariffResponse.{
  CallsTariffsResponse => JavaCallsTariffsResponse
}
import ru.auto.salesman.calls.calls_tariff_response.{
  CallTariff,
  CallTariffResponse,
  CallsTariffsResponse
}
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.environment._
import ru.auto.salesman.model.telepony.ApiModel.{CallCostRequest, CallCostResponse}
import ru.auto.salesman.model.{CityId, Client, ClientStatuses, RegionId}
import ru.auto.salesman.service.call.cashback.domain.CallId
import ru.auto.salesman.service.call.price.CallPriceService
import ru.auto.salesman.service.call.client.CallTargetService
import ru.auto.salesman.service.call.client.CallTargetService.CallTargetServiceError.ValidationError
import ru.auto.salesman.service.telepony.cost.TeleponyCallCostServiceImpl
import ru.auto.salesman.service.telepony.tariff.TeleponyCallTariffService
import ru.auto.salesman.test.model.gens.clientRecordGen
import ru.auto.salesman.util.telepony.{InvalidObjectIdFormat, ObjectId}

import scala.concurrent.duration._
import scala.collection.JavaConverters._

class TeleponyHandlerSpec extends RoutingSpec {

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5.seconds)

  private val defaultCallTimestamp =
    Timestamps.parse("2019-09-27T10:00:20.021+03:00")

  private val capitalPoiId = 18225L
  private val regionalPoiId = 500L

  private val client =
    Client(
      clientId = 23965,
      agencyId = None,
      categorizedClientId = None,
      companyId = None,
      RegionId(1000L),
      CityId(2L),
      ClientStatuses.Active,
      singlePayment = Set(),
      firstModerated = false,
      paidCallsAvailable = false,
      priorityPlacement = true
    )

  private val capitalClient =
    Client(
      clientId = 23965,
      agencyId = None,
      categorizedClientId = None,
      companyId = None,
      RegionId(1L),
      CityId(2L),
      ClientStatuses.Active,
      singlePayment = Set(),
      firstModerated = false,
      paidCallsAvailable = false,
      priorityPlacement = true
    )

  private val callTargetService = mock[CallTargetService]
  private val priceService = mock[CallPriceService]
  private val vosClient = mock[VosClient]

  private val callCostService = new TeleponyCallCostServiceImpl(
    vosClient,
    callTargetService,
    priceService
  )

  private val callTariffService = mock[TeleponyCallTariffService]

  (callTargetService.getTarget _)
    .expects(ObjectId(s"dealer-$capitalPoiId"))
    .returningZ(capitalClient)
    .anyNumberOfTimes

  (callTargetService.getTarget _)
    .expects(ObjectId(s"dealer-$regionalPoiId"))
    .returningZ(client)
    .anyNumberOfTimes

  (callTargetService.getTarget _)
    .expects(ObjectId("dealr-18225"))
    .throwingZ(ValidationError(InvalidObjectIdFormat("dealr-18225")))
    .anyNumberOfTimes

  private val route =
    new TeleponyHandler(callCostService, callTariffService).route

  "POST /telepony/call_cost" should {
    val uri = "/call_cost"

    "return cost for priority-placement calls" in {
      val req =
        CallCostRequest
          .newBuilder()
          .setObjectId(s"dealer-$capitalPoiId")
          .setTag("category=CARS#section=NEW")
          .setTimestamp(defaultCallTimestamp)
          .build()

      val defaultCallDateTime = defaultCallTimestamp.asDateTime

      (priceService
        .getCallPrice(
          _: Client,
          _: Option[Offer],
          _: Option[Section],
          _: Option[CallId],
          _: DateTime
        ))
        .expects(capitalClient, None, Some(Section.NEW), None, defaultCallDateTime)
        .returningZ(300000)

      Post(uri)
        .withHeaders(RequestIdentityHeaders)
        .withEntity(HttpEntity(req.toByteArray)) ~> seal(route) ~> check {

        status shouldBe StatusCodes.OK
        val info = responseAs[CallCostResponse]
        info.getCost shouldBe 300000
        info.getClientId shouldBe 23965
      }
    }

    "return 400 on bad objectId" in {
      val req =
        CallCostRequest
          .newBuilder()
          .setObjectId(s"dealr-18225")
          .setTag("category=CARS#section=NEW")
          .setTimestamp(defaultCallTimestamp)
          .build()

      Post(uri)
        .withHeaders(RequestIdentityHeaders)
        .withEntity(HttpEntity(req.toByteArray)) ~> seal(route) ~> check {

        status shouldBe StatusCodes.BadRequest
      }
    }
  }

  "GET /telepony/call/client/{client}/tariffs" should {
    "return tariffs" in {
      val client = clientRecordGen().next

      val singleWithCalls = CallTariffResponse(
        category = ScalaCategory.CARS,
        section = ScalaSection.USED,
        callTariff = CallTariff.SINGLE_WITH_CALLS
      )

      val calls = CallTariffResponse(
        category = ScalaCategory.CARS,
        section = ScalaSection.NEW,
        callTariff = CallTariff.SINGLE_WITH_CALLS
      )

      (callTariffService.getCallTariffs _)
        .expects(*)
        .returningZ(
          CallsTariffsResponse(
            client.clientId.toString,
            client.regionId.toLong,
            Seq(calls, singleWithCalls)
          )
        )

      Get(
        s"/client/${client.clientId}/call/tariffs?regionId=${client.regionId}"
      ).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val rs = responseAs[JavaCallsTariffsResponse]
        rs.getClientId shouldBe client.clientId.toString
        rs.getRegionId shouldBe client.regionId.toLong
        rs.getCallTariffResponseList.asScala should contain theSameElementsAs Seq(
          CallTariffResponse.toJavaProto(calls),
          CallTariffResponse.toJavaProto(singleWithCalls)
        )
      }
    }
  }

}
