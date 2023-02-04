package ru.yandex.realty.clients.distancematrix

import java.time._

import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.distancematrix.Matrix.MatrixRow
import ru.yandex.realty.clients.distancematrix.MatrixRequest.RequestMode
import ru.yandex.realty.http.HttpClientMock
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TimeUtils

// scalastyle:off
@RunWith(classOf[JUnitRunner])
class DefaultDistanceMatrixClientSpec extends SpecBase with HttpClientMock with ScalaFutures {

  implicit val trace: Traced = Traced.empty

  private val spb = MatrixRequest.GeoPoint(59.9311f, 30.3609f)
  private val ekb = MatrixRequest.GeoPoint(56.8431f, 60.6454f)
  private val msk = MatrixRequest.GeoPoint(55.7558f, 37.6173f)
  private val hbr = MatrixRequest.GeoPoint(48.4814f, 135.0721f)

  "DefaultDistanceMatrixClient.getDistanceMatrix" should {
    "return correct response for expected url" in {
      val departureTimeInstant = ZonedDateTime
        .of(
          LocalDate.of(2022, 6, 19),
          LocalTime.of(15, 13),
          TimeUtils.MSK
        )
        .toInstant

      val request1 = MatrixRequest(
        origins = List(spb, ekb),
        destinations = List(msk, hbr),
        mode = RequestMode.Walking,
        departureTime = Some(departureTimeInstant)
      )

      val key = "test-key-123"
      val vbar = "%7C"
      val expectedUrl =
        s"/v2/distancematrix?apikey=$key&origins=${spb.latitude},${spb.longitude}$vbar${ekb.latitude},${ekb.longitude}" +
          s"&destinations=${msk.latitude},${msk.longitude}$vbar${hbr.latitude},${hbr.longitude}" +
          s"&mode=walking&departure_time=${departureTimeInstant.getEpochSecond}&avoid_tolls=false"

      val client = new DefaultDistanceMatrixClient(httpService, key)

      httpClient.expect(GET, expectedUrl)
      httpClient.respondWith(StatusCodes.OK, responseJson)

      val result = client.getDistanceMatrix(request1).futureValue

      result shouldEqual responseScala
    }

    "throw exception if the response is missing some values" in {
      val request1 = MatrixRequest(
        origins = List(spb),
        destinations = List(ekb),
        mode = RequestMode.Driving,
        departureTime = None,
        avoidTolls = true
      )

      val key = "test-key-2"

      val expectedUrl =
        s"/v2/distancematrix?apikey=$key&origins=${spb.latitude},${spb.longitude}" +
          s"&destinations=${ekb.latitude},${ekb.longitude}&mode=driving&avoid_tolls=true"

      val client = new DefaultDistanceMatrixClient(httpService, key)

      httpClient.expect(GET, expectedUrl)
      httpClient.respondWith(StatusCodes.OK, invalidResponseJson)

      val result = client.getDistanceMatrix(request1).failed.futureValue

      result shouldBe a[RuntimeException]
    }

    "throw exception if origins and destinations lists are empty" in {
      val request1 = MatrixRequest(
        origins = List(),
        destinations = List(),
        mode = RequestMode.Driving,
        departureTime = None,
        avoidTolls = true
      )
      val key = "test-key-3"

      val client = new DefaultDistanceMatrixClient(httpService, key)

      val result = client.getDistanceMatrix(request1).failed.futureValue

      result shouldBe a[IllegalArgumentException]
    }
  }

  private val responseJson: String =
    s"""
        |{
        |  "rows": [
        |    {
        |      "elements": [
        |        {
        |          "status": "OK",
        |          "distance": {
        |            "value": 706612
        |          },
        |          "duration": {
        |            "value": 27009
        |          }
        |        },
        |        {
        |          "status": "OK",
        |          "distance": {
        |            "value": 9020529
        |          },
        |          "duration": {
        |            "value": 449932
        |          }
        |        }
        |      ]
        |    },
        |    {
        |      "elements": [
        |        {
        |          "status": "OK",
        |          "distance": {
        |            "value": 1845203
        |          },
        |          "duration": {
        |            "value": 78038
        |          }
        |        },
        |        {
        |          "status": "FAIL"
        |        }
        |      ]
        |    }
        |  ]
        |}
      """.stripMargin

  private val invalidResponseJson: String =
    s"""
         |{
         |  "rows": [
         |    {
         |      "elements": [
         |        {
         |          "status": "OK",
         |          "distance": {
         |            "value": 706612
         |          }
         |        }
         |      ]
         |    }
         |  ]
         |}
      """.stripMargin

  private val responseScala = Matrix(
    List(
      MatrixRow(
        List(
          DistanceMatrixElement.CellOk(Duration.ofSeconds(27009), 706612),
          DistanceMatrixElement.CellOk(Duration.ofSeconds(449932), 9020529)
        )
      ),
      MatrixRow(
        List(
          DistanceMatrixElement.CellOk(Duration.ofSeconds(78038), 1845203),
          DistanceMatrixElement.CellFailed
        )
      )
    )
  )
}
