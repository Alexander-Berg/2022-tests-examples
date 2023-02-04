package ru.auto.salesman.api.v1.service.schedules

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
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
import ru.auto.salesman.api.v1.HandlerBaseSpec
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Boost
import ru.auto.salesman.model.{AutoruUser, DeprecatedDomain}
import ru.auto.salesman.test.model.gens._

import scala.concurrent.Future

class SchedulesHandlerSpec extends HandlerBaseSpec {
  implicit override def domain: DeprecatedDomain = AutoRu

  val user: AutoruUser = AutoruUserGen.sample.getOrElse(
    throw new Exception("Unable to generate AutoruUser")
  )

  val offerId: AutoruOfferId = AutoruOfferIdGen.sample.getOrElse(
    throw new Exception("Unable to generate OfferId")
  )

  val anotherOfferId: AutoruOfferId = AutoruOfferIdGen.sample.getOrElse(
    throw new Exception("Unable to generate OfferId")
  )

  def SampleGetResponse = Future {
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
  }

  def SamplePutRequest: ScheduleRequest = ScheduleRequest.getDefaultInstance

  def successResponse =
    Future(
      SuccessResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()
    )

  "Get /schedules/user/{user}" should {
    "works" in {
      (scheduleCrudService.getSchedules _)
        .expects(user, None, None)
        .returning(SampleGetResponse)
      Get(s"/api/1.x/service/autoru/schedules/user/${user.toString}") ~>
        Route.seal(route) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ScheduleResponse]
        }
    }

    "works with empty product" in {
      (scheduleCrudService.getSchedules _)
        .expects(user, None, Some(NonEmptyList.of(offerId)))
        .returning(SampleGetResponse)
      Get(
        s"/api/1.x/service/autoru/schedules/user/${user.toString}?offerId=$offerId"
      ) ~>
        Route.seal(route) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ScheduleResponse]
        }
    }

    "works with empty offers" in {
      (scheduleCrudService.getSchedules _)
        .expects(user, Some(NonEmptyList.of(Boost)), None)
        .returning(SampleGetResponse)
      Get(
        s"/api/1.x/service/autoru/schedules/user/${user.toString}?product=boost"
      ) ~>
        Route.seal(route) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ScheduleResponse]
        }
    }

    "works with products and offers" in {
      (scheduleCrudService.getSchedules _)
        .expects(
          user,
          Some(NonEmptyList.of(Boost)),
          Some(NonEmptyList.of(anotherOfferId, offerId))
        )
        .returning(SampleGetResponse)
      Get(
        s"/api/1.x/service/autoru/schedules/user/${user.toString}?product=boost&offerId=$offerId&offerId=$anotherOfferId"
      ) ~>
        Route.seal(route) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ScheduleResponse]
        }
    }
  }

  "DELETE /schedules/user/{user}/product/{product}" should {
    "works" in {
      (scheduleCrudService.deleteSchedules _)
        .expects(user, Some(NonEmptyList.of(Boost)), NonEmptyList.of(offerId))
        .returning(successResponse)
      Delete(
        s"/api/1.x/service/autoru/schedules/user/${user.toString}?product=boost&offerId=$offerId"
      ) ~>
        Route.seal(route) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[SuccessResponse]
        }
    }
  }

  "PUT /service/{service}/schedules/user/{user}/product/{product}" should {
    "works" in {
      (scheduleCrudService.putSchedules _)
        .expects(
          user,
          Boost,
          Iterable(anotherOfferId, offerId),
          SamplePutRequest
        )
        .returning(successResponse)
      Put(
        s"/api/1.x/service/autoru/schedules/user/${user.toString}/product/$Boost?offerId=$offerId&offerId=$anotherOfferId",
        SamplePutRequest
      ) ~>
        Route.seal(route) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[SuccessResponse]
        }
    }
  }
}
