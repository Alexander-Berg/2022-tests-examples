package ru.yandex.realty.managers.unification

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.clients.unifier.UnifierClient
import ru.yandex.realty.geohub.api.GeohubApi
import ru.yandex.realty.geohub.api.GeohubApi.{UnifyLocationRequest, UnifyLocationResponse}
import ru.yandex.realty.model.message.RealtySchema.{LocationMessage, RawLocationMessage, RawOfferMessage}
import ru.yandex.realty.proto.unified.offer.address.LocationUnified
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class RawLocationUnificationManagerSpec extends AsyncSpecBase {

  implicit private val traced: Traced = Traced.empty

  val geohubClient: GeohubClient = mock[GeohubClient]
  val indexerClient: UnifierClient = mock[UnifierClient]

  trait RawLocationUnificationManagerFixture {
    val manager = new RawLocationUnificationManager(geohubClient, indexerClient)
  }

  "RawLocationUnificationManager" should {
    "return unified address " in new RawLocationUnificationManagerFixture {

      val indexerResponse: RawOfferMessage =
        RawOfferMessage
          .newBuilder()
          .setLocation(
            RawLocationMessage
              .newBuilder()
              .setAddress("address1")
              .setLongitude(1.0f)
              .setLatitude(1.0f)
          )
          .build()

      (indexerClient
        .getRawOffer(_: String)(_: Traced))
        .expects("randomId", *)
        .returning(Future.successful(Option(indexerResponse)))

      val geohubRequest: UnifyLocationRequest =
        UnifyLocationRequest.newBuilder().setRawLocation(indexerResponse.getLocation).build()

      val geohubResponse: UnifyLocationResponse = {
        UnifyLocationResponse
          .newBuilder()
          .setLocation(
            LocationMessage.newBuilder().setGeocoderAddress("address2")
          )
          .build()
      }

      (geohubClient
        .unifyLocation(_: GeohubApi.UnifyLocationRequest)(_: Traced))
        .expects(geohubRequest, *)
        .returning(Future.successful(geohubResponse))

      val result: LocationUnified = manager
        .getUnifiedLocationForRawLocation(
          RawLocationUnificationRequest("randomId", Option("address1"), None, ignoreCoordinates = false)
        )
        .futureValue

      result.getUnifiedAddress.getUnifiedOneline shouldBe "address2, address1"
    }

    "fail if no raw offer found " in new RawLocationUnificationManagerFixture {
      (indexerClient
        .getRawOffer(_: String)(_: Traced))
        .expects("notFoundId", *)
        .returning(Future.successful(None))

      try {
        manager
          .getUnifiedLocationForRawLocation(
            RawLocationUnificationRequest("notFoundId", Option("address1"), None, ignoreCoordinates = false)
          )
          .futureValue

        fail()
      } catch {
        case e: Throwable =>
          e.getMessage shouldBe "The future returned an exception of type: java.util.NoSuchElementException, with message: No offer found for requested id [notFoundId]."
      }
    }

    "fail while getting raw offer " in new RawLocationUnificationManagerFixture {
      (indexerClient
        .getRawOffer(_: String)(_: Traced))
        .expects("exceptionId", *)
        .returning(Future.failed(new Exception("something wrong")))

      try {
        manager
          .getUnifiedLocationForRawLocation(
            RawLocationUnificationRequest("exceptionId", Option("address1"), None, ignoreCoordinates = false)
          )
          .futureValue

        fail()
      } catch {
        case e: RuntimeException =>
          e.getMessage shouldBe "The future returned an exception of type: java.lang.RuntimeException, with message: Can't get raw offer for requested id [exceptionId]."
        case _: Throwable => fail()
      }
    }

    "fail on offer location unification " in new RawLocationUnificationManagerFixture {
      val indexerResponse: RawOfferMessage =
        RawOfferMessage
          .newBuilder()
          .setLocation(
            RawLocationMessage
              .newBuilder()
              .setAddress("address0")
              .setLongitude(1.0f)
              .setLatitude(1.0f)
          )
          .build()

      (indexerClient
        .getRawOffer(_: String)(_: Traced))
        .expects("randomId", *)
        .returning(Future.successful(Option(indexerResponse)))

      val addressToCheck = "address to check"

      val geohubRequest: UnifyLocationRequest =
        UnifyLocationRequest
          .newBuilder()
          .setRawLocation(indexerResponse.getLocation.toBuilder.setAddress(addressToCheck))
          .build()

      (geohubClient
        .unifyLocation(_: GeohubApi.UnifyLocationRequest)(_: Traced))
        .expects(geohubRequest, *)
        .returning(Future.failed(new Exception("unification error")))

      try {
        manager
          .getUnifiedLocationForRawLocation(
            RawLocationUnificationRequest("randomId", Option(addressToCheck), None, ignoreCoordinates = false)
          )
          .futureValue

        fail()
      } catch {
        case e: RuntimeException =>
          e.getMessage shouldBe "The future returned an exception of type: java.lang.RuntimeException, with message: Can't get unified location for updated offer address [address to check]."
        case _: Throwable => fail()
      }
    }
  }

}
