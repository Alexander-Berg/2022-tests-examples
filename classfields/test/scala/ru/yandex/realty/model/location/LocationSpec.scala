package ru.yandex.realty.model.location

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.prop.TableDrivenPropertyChecks._

@RunWith(classOf[JUnitRunner])
class LocationSpec extends WordSpec with Matchers {

  val point1: GeoPoint = GeoPoint.getPoint(55.974524f, 37.314098f)
  val veryCloseToPoint1Point: GeoPoint = GeoPoint.getPoint(55.974510f, 37.314099f)
  val fartherThan30MeterFromPoint1Point = GeoPoint.getPoint(55.984524f, 37.314098f)

  "Location" should {

    "getExactPoint" in {

      val data =
        Table(
          ("geocoderPoint", "manualPoint", "accuracy", "exactPoint"),
          (point1, point1, LocationAccuracy.EXACT, point1),
          (point1, veryCloseToPoint1Point, LocationAccuracy.EXACT, point1),
          (point1, veryCloseToPoint1Point, LocationAccuracy.NEAR, point1),
          (point1, fartherThan30MeterFromPoint1Point, LocationAccuracy.NEAR, fartherThan30MeterFromPoint1Point),
          (point1, fartherThan30MeterFromPoint1Point, LocationAccuracy.EXACT, point1),
          (point1, null, LocationAccuracy.EXACT, point1),
          (null, veryCloseToPoint1Point, LocationAccuracy.EXACT, veryCloseToPoint1Point),
          (point1, GeoPoint.UNKNOWN, LocationAccuracy.EXACT, point1),
          (GeoPoint.UNKNOWN, veryCloseToPoint1Point, LocationAccuracy.EXACT, veryCloseToPoint1Point),
          (null, null, LocationAccuracy.EXACT, GeoPoint.UNKNOWN)
        )

      forAll(data) {
        (geoCoderPoint: GeoPoint, manualPoint: GeoPoint, accuracy: LocationAccuracy, expectedPoint: GeoPoint) =>
          val location = new Location()

          location.setManualPoint(manualPoint)
          location.setGeocoderLocation("some address", geoCoderPoint)
          location.setAccuracy(accuracy)
          location.getExactPoint shouldBe expectedPoint
      }
    }

  }
}
