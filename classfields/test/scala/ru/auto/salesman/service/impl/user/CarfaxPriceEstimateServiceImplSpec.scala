package ru.auto.salesman.service.impl.user

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MarshallingDirectives
import org.scalamock.scalatest.MockFactory
import ru.auto.api.vin.money.ResponseModel.CarfaxRawPriceResponse
import ru.auto.salesman.model.ProductId
import ru.auto.salesman.service.user.CarfaxPriceEstimateService.{
  CarfaxReportException,
  NotFoundProductException
}

import ru.auto.salesman.proto.user.ModelProtoFormats
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.BasicSalesmanGenerators
import ru.auto.salesman.util.CacheControl.NoCache
import ru.auto.salesman.util.sttp.SttpClientImpl
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.banker.model.ApiModel._
import ru.yandex.vertis.banker.model.{ApiModel}
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.protobuf.ProtobufUtils
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport
import ru.auto.api.vin.money.RequestModel.{CarfaxPriceRequest => ProtoCarfaxPriceRequest}
import ru.auto.api.vin.money.RequestModel.{UserInfo => ProtoUserInfo}
import ru.auto.api.vin.money.RequestModel.VinHistoryContext
import ru.auto.api.vin.money.Model.RawServicePrice
import ru.auto.api.vin.money.ResponseModel
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import com.google.protobuf.StringValue
import scala.collection.JavaConverters._

class CarfaxPriceEstimateServiceImplSpec extends BaseSpec {

  implicit val rc: RequestContext = AutomatedContext("unit-test", NoCache)

  val testApiModelAccount: Account = ApiModel.Account.newBuilder().build()

  private val carfaxSuccesResponse = CarfaxRawPriceResponse
    .newBuilder()
    .addAllRawServicePrices(
      List(
        RawServicePrice
          .newBuilder()
          .setExperiment(StringValue.newBuilder().build())
          .setServicePrice(3456)
          .setServiceDuration(24)
          .setServiceName("offers-history-reports-5")
          .build()
      ).asJava
    )
    .build()

  private val carfaxSuccesEmptyResponse = CarfaxRawPriceResponse
    .newBuilder()
    .build()

  private val carfaxFailureResponse = CarfaxRawPriceResponse
    .newBuilder()
    .setError(ResponseModel.CarfaxRawPriceError.newBuilder().build())
    .build()

  val api = new CarfaxPriceEstimateServiceImpl(
    baseUrl = runServer(
      concat(
        CarfaxPriceEstimateServiceImplSpec.getPriceRoute(
          successResponse = carfaxSuccesResponse,
          successEmptyResponse = carfaxSuccesEmptyResponse,
          failureResponse = carfaxFailureResponse
        )
      )
    ).toString,
    backend = SttpClientImpl(TestOperationalSupport)
  )

  "CarfaxPriceEstimateServiceImpl" should {
    "correct should return price " in {
      val res = api
        .estimate(
          product = OffersHistoryReports(5),
          request = ProtoCarfaxPriceRequest
            .newBuilder()
            .setUserInfo(
              ProtoUserInfo
                .newBuilder()
                .setUserId(StringValue.newBuilder().setValue("user_1"))
                .build()
            )
            .build()
        )
        .success
        .value
      res.duration.map(_.days.getDays) shouldBe Some(24)
      res.product shouldBe ProductId.OffersHistoryReports
      res.price.amount shouldBe 3456
    }

    "should return failure NotFoundProductException if products not found" in {
      val res = api
        .estimate(
          product = OffersHistoryReports(5),
          request = ProtoCarfaxPriceRequest
            .newBuilder()
            .build()
        )
        .failure
        .cause
        .failures
        .head
      res shouldBe a[NotFoundProductException]
    }

    "should return CarfaxReportException " in {
      val res = api
        .estimate(
          product = OffersHistoryReports(5),
          request = ProtoCarfaxPriceRequest
            .newBuilder()
            .setVinContext(
              VinHistoryContext.newBuilder().setVinOrLicensePlate("test").build()
            )
            .build()
        )
        .failure
        .exception
      res shouldBe a[CarfaxReportException]
    }
  }
}

object CarfaxPriceEstimateServiceImplSpec
    extends MockFactory
    with BasicSalesmanGenerators
    with MarshallingDirectives
    with ModelProtoFormats
    with ProtobufSupport
    with ProtobufUtils {

  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  private def getPriceRoute(
      successResponse: CarfaxRawPriceResponse,
      successEmptyResponse: CarfaxRawPriceResponse,
      failureResponse: CarfaxRawPriceResponse
  ): Route =
    (post & entity(as[ProtoCarfaxPriceRequest])) { request =>
      path(
        "api" / "v1" / "prices"
      ) {
        if (request.hasUserInfo)
          complete(successResponse)
        else if (request.hasVinContext)
          complete(failureResponse)
        else complete(successEmptyResponse)

      }
    }
}
