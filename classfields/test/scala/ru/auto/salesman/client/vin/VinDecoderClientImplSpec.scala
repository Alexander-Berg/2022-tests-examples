package ru.auto.salesman.client.vin

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.vin.RequestModel.EssentialsOfferReportRequest
import ru.auto.api.vin.ResponseModel.{
  RawEssentialsReportResponse,
  VinDecoderError,
  VinResponse
}
import ru.auto.api.vin.VinReportModel.PtsBlock.{IntItem, StringItem}
import ru.auto.api.vin.VinReportModel.{PtsBlock, RawVinEssentialsReport}
import ru.auto.salesman.client.proto.impl.ResponseNotReadyException
import ru.auto.salesman.model.VinReportParams
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.sttp.SttpClientImpl
import ru.auto.salesman.util.sttp.SttpProtobufSupport.SttpProtoException._
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import java.io.IOException
import java.net.URI

class VinDecoderClientImplSpec extends BaseSpec with ProtobufSupport {

  import VinDecoderClientImplSpec.TestCase._
  import VinDecoderClientImplSpec._

  @volatile private var testCase: TestCase = Ok

  private val testingBackend = SttpClientImpl(TestOperationalSupport)

  private val client =
    new VinDecoderClientImpl(serverAddress.toString, testingBackend)

  private def rawEssentialsRoute: Route =
    post {
      pathPrefix("api" / "v1" / "report" / "raw" / "essentials" / "for-offer") {
        entity(as[Array[Byte]]) { body =>
          val parsed = EssentialsOfferReportRequest.parser.parseFrom(body)

          if (
            parsed.getOffer.getId == offerId && parsed.getVinOrLp == vin && testCase == Ok
          )
            complete(vinResOk)
          else if (
            parsed.getOffer.getId == offerId && parsed.getVinOrLp == vin && testCase == ProtoError
          )
            complete(vinResStatusError)
          else if (
            parsed.getOffer.getId == offerId && parsed.getVinOrLp == vin && testCase == Empty
          )
            complete(NotFound)
          else if (
            parsed.getOffer.getId == offerId && parsed.getVinOrLp == vin && testCase == RequestError
          )
            complete(BadRequest)
          else
            fail(
              "Request was matched with raw/essentials route but required parameters were not found"
            )
        }
      }
    }

  private def rawEssentialsReport: Route =
    get {
      pathPrefix("api" / "v1" / "report" / "raw" / "essentials") {
        parameter("vin") { rqVin =>
          if (rqVin == "XXX123" && testCase == Ok)
            complete(successRawEssentials)
          else if (rqVin == "XXX123" && testCase == ProtoError)
            complete(errorInProgressRawEssentials)
          else if (rqVin == "XXX123" && testCase == UnexpectedError)
            complete(errorRawEssentials)
          else if (rqVin == "XXX123" && testCase == Empty)
            complete(NotFound)
          else if (rqVin == "XXX123" && testCase == RequestError)
            complete(BadRequest)
          else
            fail(
              "Request was matched with raw/essentials route but required parameters were not found"
            )
        }
      }
    }

  private def relationshipsRoute: Route =
    get {
      pathPrefix("api" / "v1" / "relationship") {
        parameter("license_plate") { rqLp =>
          if (rqLp == "B777OP77" && testCase == Ok)
            complete(successResolveVinResponse)
          else if (rqLp == "B777OP77" && testCase == ProtoError)
            complete(errorInProgressResolveVinResponse)
          else if (rqLp == "B777OP77" && testCase == UnexpectedError)
            complete(errorResolveVinResponse)
          else if (rqLp == "B777OP77" && testCase == Empty)
            complete(NotFound)
          else if (rqLp == "B777OP77" && testCase == RequestError)
            complete(BadRequest)
          else
            fail(
              "Request was matched with relationship route but required parameters were not found"
            )
        }
      }
    }

  private def serverAddress: URI =
    runServer(
      concat(
        rawEssentialsRoute,
        rawEssentialsReport,
        relationshipsRoute
      )
    )

  private def offerBuilder: Offer.Builder = ApiOfferModel.Offer.newBuilder()

  "VinDecoderClientImpl.getContentQuality" should {
    "return quality as is" in {
      val b = offerBuilder
      b.setId(offerId)
      b.getDocumentsBuilder.setVin(vin)
      b.getDocumentsBuilder.setLicensePlate(lp)

      testCase = Ok

      client.getContentQuality(b.build, None).success.value shouldBe 1
    }

    "return 0 when status not success" in {
      val b = offerBuilder
      b.setId(offerId)
      b.getDocumentsBuilder.setVin(vin)
      b.getDocumentsBuilder.setLicensePlate(lp)

      testCase = ProtoError

      client.getContentQuality(b.build, None).success.value shouldBe 0
    }

    "return 0 when response was empty" in {
      val b = offerBuilder
      b.setId(offerId)
      b.getDocumentsBuilder.setVin(vin)
      b.getDocumentsBuilder.setLicensePlate(lp)

      testCase = Empty

      client.getContentQuality(b.build, None).success.value shouldBe 0
    }

    "return failure when client failed" in {
      val b = offerBuilder
      b.setId(offerId)
      b.getDocumentsBuilder.setVin(vin)
      b.getDocumentsBuilder.setLicensePlate(lp)

      testCase = RequestError

      client
        .getContentQuality(b.build, None)
        .failure
        .exception shouldBe a[BadRequestException]
    }
  }

  "VinDecoderClientImpl.getReportPreview" should {
    "work fine" in {
      testCase = Ok

      val result = VinReportParams(
        mark = Some("BMW"),
        model = Some("m3"),
        year = Some(2020)
      )

      client
        .getReportPreview("XXX123")
        .success
        .value shouldBe result
    }

    "fail with in progress error" in {
      testCase = ProtoError

      client
        .getReportPreview("XXX123")
        .failure
        .exception shouldBe ResponseNotReadyException("IN_PROGRESS error")
    }

    "fail with some other error" in {
      testCase = UnexpectedError

      client
        .getReportPreview("XXX123")
        .failure
        .exception shouldBe an[IOException]
    }

    "fail on empty response" in {
      testCase = Empty

      client
        .getReportPreview("XXX123")
        .failure
        .exception shouldBe an[NoSuchElementException]
    }

    "fail on request errors" in {
      testCase = RequestError

      client
        .getReportPreview("XXX123")
        .failure
        .exception shouldBe a[BadRequestException]
    }
  }

  "VinDecoderClientImpl.resolveVin" should {
    "work fine" in {
      testCase = Ok

      client
        .resolveVin("B777OP77")
        .success
        .value shouldBe successResolveVinResponse
    }

    "fail with in progress error" in {
      testCase = ProtoError

      client
        .resolveVin("B777OP77")
        .failure
        .exception shouldBe ResponseNotReadyException("IN_PROGRESS error")
    }

    "fail with some other error" in {
      testCase = UnexpectedError

      client.resolveVin("B777OP77").failure.exception shouldBe an[IOException]
    }

    "fail on empty response" in {
      testCase = Empty

      client
        .resolveVin("B777OP77")
        .failure
        .exception shouldBe an[NoSuchElementException]
    }

    "fail on request errors" in {
      testCase = RequestError

      client
        .resolveVin("B777OP77")
        .failure
        .exception shouldBe a[BadRequestException]
    }
  }

}

object VinDecoderClientImplSpec {
  sealed trait TestCase

  object TestCase {
    case object Ok extends TestCase
    case object ProtoError extends TestCase
    case object UnexpectedError extends TestCase
    case object Empty extends TestCase
    case object RequestError extends TestCase
  }

  val offerId = "12-dasfs"
  val vin = "4123412sdfas"
  val lp = "A333AA777"

  val vinResOk = {
    val b = RawEssentialsReportResponse.newBuilder()
    b.setReport(RawVinEssentialsReport.newBuilder().setQuality(1))
    b.build()
  }

  val vinResStatusError = {
    val b = RawEssentialsReportResponse.newBuilder()
    b.setError {
      VinDecoderError
        .newBuilder()
        .setErrorCode(VinDecoderError.Code.UNKNOWN_ERROR)
        .setDetailedError("SOME error")
    }
    b.build()
  }

  val successRawEssentials = {
    val ptsInfo =
      PtsBlock
        .newBuilder()
        .setMark(StringItem.newBuilder().setValueText("BMW"))
        .setModel(StringItem.newBuilder().setValueText("m3"))
        .setYear(IntItem.newBuilder().setValue(2020))
        .build
    val report = RawVinEssentialsReport
      .newBuilder()
      .setPtsInfo(ptsInfo)
      .build
    RawEssentialsReportResponse
      .newBuilder()
      .setReport(report)
      .build()
  }

  val errorInProgressRawEssentials =
    RawEssentialsReportResponse
      .newBuilder()
      .setError {
        VinDecoderError
          .newBuilder()
          .setErrorCode(VinDecoderError.Code.IN_PROGRESS)
          .setDetailedError("IN_PROGRESS error")
      }
      .build()

  val errorRawEssentials =
    RawEssentialsReportResponse
      .newBuilder()
      .setError {
        VinDecoderError
          .newBuilder()
          .setErrorCode(VinDecoderError.Code.UNKNOWN_ERROR)
          .setDetailedError("SOME error")
      }
      .build()

  val successResolveVinResponse =
    VinResponse
      .newBuilder()
      .setVin("XXX123")
      .build()

  val errorInProgressResolveVinResponse =
    VinResponse
      .newBuilder()
      .setError {
        VinDecoderError
          .newBuilder()
          .setErrorCode(VinDecoderError.Code.IN_PROGRESS)
          .setDetailedError("IN_PROGRESS error")
      }
      .build()

  val errorResolveVinResponse =
    VinResponse
      .newBuilder()
      .setError {
        VinDecoderError
          .newBuilder()
          .setErrorCode(VinDecoderError.Code.UNKNOWN_ERROR)
          .setDetailedError("SOME error")
      }
      .build()
}
