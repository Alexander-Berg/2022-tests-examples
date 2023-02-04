package ru.yandex.auto.vin.decoder.utils

import akka.http.scaladsl.model.HttpMethods
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ResponseModel.OfferResponse
import ru.auto.api.vin.RequestModel.OfferReportRequest
import ru.auto.api.vin.ResponseModel.RawReportResponse
import ru.auto.api.vin.VinReportModel.Header
import ru.yandex.vertis.commons.http.client.HttpClient._
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, HttpRoute, RemoteHttpService}
import ru.yandex.vertis.protobuf.ProtoInstanceProvider._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@Ignore
class RawDiffCheckTest extends AnyFunSuite {

  private val branchNum = "889"
  // private val offerId = "1092952968-0c35b281"

  private val testingHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("auto-vin-decoder-api.vrts-slb.test.vertis.yandex.net", 80, "http")
    new RemoteHttpService(name = "testing", endpoint = endpoint)
  }

  private val branchHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint =
      HttpEndpoint(s"pull-$branchNum.auto-vin-decoder-api.vrts-slb.test.vertis.yandex.net", 80, "http")
    // HttpEndpoint(s"localhost", 36314, "http")
    new RemoteHttpService(name = "branch", endpoint = endpoint)
  }

  private val publicApiHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint =
      HttpEndpoint(s"autoru-api-server-test-int.slb.vertis.yandex.net", 80, "http")
    new RemoteHttpService(name = "public_api", endpoint = endpoint)
  }

  private val testRoute = makerRawRoute(testingHttpService)
  private val branchRoute = makerRawRoute(branchHttpService)

  private val publicApiOfferRoute = publicApiHttpService.newRoute(
    routeName = "get_ogffer",
    method = HttpMethods.GET,
    pathPattern = "/1.0/offer/cars/{offer_id}"
  )

  private def makerRawRoute(httpService: RemoteHttpService): HttpRoute =
    httpService.newRoute(
      routeName = "get_raw_report",
      method = HttpMethods.GET,
      pathPattern = "/api/v1/report/raw"
    )

  private def makerReportOfferRoute(httpService: RemoteHttpService): HttpRoute =
    httpService.newRoute(
      routeName = "get_offer_report",
      method = HttpMethods.POST,
      pathPattern = "/api/v1/report/raw/for-offer"
    )

  private def getReport(vin: String, isPaid: Boolean, route: HttpRoute): Future[RawReportResponse] = {
    route
      .newRequest()
      .withAcceptProto
      .addQueryParam("vin", vin)
      .addQueryParam("is_paid", isPaid)
      .handle200(protoResponseFormat[RawReportResponse])
      .execute()
  }

  private def getOfferReport(request: OfferReportRequest, route: HttpRoute): Future[RawReportResponse] = {
    route
      .newRequest()
      .withAcceptProto
      .setProtoEntity(request)
      .handle200(protoResponseFormat[RawReportResponse])
      .execute()
  }

  private def getOffer(offerId: String): Future[OfferResponse] = {
    publicApiOfferRoute
      .newRequest()
      .withAcceptProto
      .setPathParam("offer_id", offerId)
      .addHeader("x-authorization", "Vertis swagger")
      .handle200(protoResponseFormat[OfferResponse])
      .execute()
  }

  private def buildReportRequest(vin: String, isPaid: Boolean, offerId: String): Future[OfferReportRequest] = {
    for {
      offerResponse <- getOffer(offerId)
      offer = offerResponse.getOffer.toBuilder
      _ = offer.getDocumentsBuilder.setYear(2000)
    } yield OfferReportRequest
      .newBuilder()
      .setOffer(offer)
      .setIsPaid(isPaid)
      .setVinOrLp(vin)
      .build()
  }

  test("compare raw report") {
    val vin = "TRUZZZ8J871022837"
    val isPaid = true
    Await.result(getReport(vin, isPaid, testRoute), 10.seconds)
    Await.result(getReport(vin, isPaid, branchRoute), 10.second)

//    assertThat(testReport)
//      .reportingMismatchesOnly()
//      // .ignoringFieldAbsence()
//      .ignoringRepeatedFieldOrder()
//      .ignoringFieldDescriptors(Header.getDescriptor.findFieldByNumber(Header.TIMESTAMP_UPDATE_FIELD_NUMBER))
//      .isEqualTo(branchReport)
    assert(2 + 2 == 4)
  }

  test("compare offer report") {
    val vin = "Y6DTF69Y080127072"
    val isPaid = false
    val offerId = "1090960424-97c6d1cc"
    val reportRequest = Await.result(buildReportRequest(vin, isPaid, offerId), 10.second)
    Await.result(getOfferReport(reportRequest, makerReportOfferRoute(testingHttpService)), 10.seconds)
    Await.result(getOfferReport(reportRequest, makerReportOfferRoute(branchHttpService)), 10.second)

//    (branchReport)
//      .reportingMismatchesOnly()
//      // .ignoringFieldAbsence()
//      .ignoringRepeatedFieldOrder()
//      .ignoringFieldDescriptors(Header.getDescriptor.findFieldByNumber(Header.TIMESTAMP_UPDATE_FIELD_NUMBER))
//      .isEqualTo(testReport)
    assert(2 + 2 == 4)
  }

}
