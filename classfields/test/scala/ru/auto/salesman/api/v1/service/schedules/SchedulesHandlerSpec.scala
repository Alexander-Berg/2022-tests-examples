package ru.auto.salesman.api.v1.service.schedules

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import cats.data.NonEmptyList
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse}
import ru.auto.api.billing.schedules.ScheduleModel.ScheduleResponse.{
  OnceAtTime,
  ScheduleParameters,
  ScheduleProducts
}
import ru.auto.api.billing.schedules.ScheduleModel.{
  ScheduleRequest,
  ScheduleResponse,
  ScheduleType
}
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.api.view.ScheduleRequestView
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Boost
import ru.auto.salesman.model.{AutoruDealer, DeprecatedDomain}
import ru.auto.salesman.service.schedules.AsyncScheduleCrudService
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.test.model.gens.{AutoruDealerGen, AutoruOfferIdGen}
import spray.json._

import scala.concurrent.Future

class SchedulesHandlerSpec extends RoutingSpec with UserModelGenerators {

  implicit override def domain: DeprecatedDomain = AutoRu

  val scheduleCrudService: AsyncScheduleCrudService =
    mock[AsyncScheduleCrudService]

  val client: AutoruDealer = AutoruDealerGen.sample.getOrElse(
    throw new Exception("Unable to generate AutoruDealer")
  )

  val offerId: AutoruOfferId = AutoruOfferIdGen.sample.getOrElse(
    throw new Exception("Unable to generate OfferId")
  )

  val anotherOfferId: AutoruOfferId = AutoruOfferIdGen.sample.getOrElse(
    throw new Exception("Unable to generate OfferId")
  )

  private val route = new SchedulesHandler(scheduleCrudService).route

  val SampleGetResponse =
    ScheduleResponse
      .newBuilder()
      .putOffers(
        offerId.toString,
        ScheduleProducts
          .newBuilder()
          .putProducts(
            "boost",
            ScheduleParameters
              .newBuilder()
              .setScheduleType(ScheduleType.ONCE_AT_TIME)
              .setTimezone("+03:00")
              .setOnceAtTime(
                OnceAtTime
                  .newBuilder()
                  .setTime("10:00")
                  .addWeekdays(1)
                  .build()
              )
              .build()
          )
          .build()
      )
      .build()

  val SamplePutRequest: ScheduleRequest =
    ScheduleRequest.getDefaultInstance

  val SampleSuccessResponse: SuccessResponse =
    SuccessResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()

  def getResponse =
    Future(SampleGetResponse)

  def successResponse =
    Future(SampleSuccessResponse)

  "Get /schedules/client/{client}" should {
    "work" in {
      (scheduleCrudService.getSchedules _)
        .expects(client, None, None)
        .returning(getResponse)
      Get(s"/client/${client.id}").withHeaders(RequestIdentityHeaders) ~>
        seal(route) ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[ScheduleResponse] shouldBe SampleGetResponse
        }
    }

    "work with empty product" in {
      (scheduleCrudService.getSchedules _)
        .expects(client, None, Some(NonEmptyList.of(offerId)))
        .returning(getResponse)
      Get(s"/client/${client.id}?offerId=$offerId")
        .withHeaders(RequestIdentityHeaders) ~>
        seal(route) ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[ScheduleResponse] shouldBe SampleGetResponse
        }
    }

    "work with empty offers" in {
      (scheduleCrudService.getSchedules _)
        .expects(client, Some(NonEmptyList.of(Boost)), None)
        .returning(getResponse)
      Get(s"/client/${client.id}?product=boost")
        .withHeaders(RequestIdentityHeaders) ~>
        seal(route) ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[ScheduleResponse] shouldBe SampleGetResponse
        }
    }

    "work with products and offers" in {
      (scheduleCrudService.getSchedules _)
        .expects(
          client,
          Some(NonEmptyList.of(Boost)),
          Some(NonEmptyList.of(offerId, anotherOfferId))
        )
        .returning(getResponse)
      Get(
        s"/client/${client.id}?product=boost&offerId=$offerId&offerId=$anotherOfferId"
      )
        .withHeaders(RequestIdentityHeaders) ~>
        seal(route) ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[ScheduleResponse] shouldBe SampleGetResponse
        }
    }
  }

  "DELETE /schedules/client/{client}/product/{product}" should {
    "work" in {
      (scheduleCrudService.deleteSchedules _)
        .expects(client, Some(NonEmptyList.of(Boost)), NonEmptyList.of(offerId))
        .returning(successResponse)
      Delete(s"/client/${client.id}?product=boost&offerId=$offerId")
        .withHeaders(RequestIdentityHeaders) ~>
        seal(route) ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[SuccessResponse]
        }
    }
  }

  "PUT /schedules/client/{client}/product/{product}" should {
    val requestEntity = HttpEntity(
      ContentTypes.`application/json`,
      ScheduleRequestView.asView(SamplePutRequest).toJson.compactPrint
    )

    "work" in {
      (scheduleCrudService.putSchedules _)
        .expects(
          client,
          Boost,
          Iterable(offerId, anotherOfferId),
          SamplePutRequest
        )
        .returning(successResponse)

      Put(
        s"/client/${client.id}/product/$Boost?offerId=$offerId&offerId=$anotherOfferId"
      )
        .withHeaders(RequestIdentityHeaders)
        .withEntity(requestEntity) ~>
        seal(route) ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[SuccessResponse]
        }
    }
  }
}
