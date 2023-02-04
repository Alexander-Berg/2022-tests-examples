package ru.yandex.realty.geohub.distance

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.flatgeoindex.GeoIndex.NearbyElement
import ru.yandex.realty.model.geometry.Polygon
import ru.yandex.realty.model.location.{GeoPoint, Pond, PondDistance, PondType}
import ru.yandex.realty.transit.{Transit, TransitPart, Transport, Wait, Walk}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class PondDistanceServiceSpec extends SpecBase {
  private val pondPolygon = new Polygon(Array(65.03f, 65.04f, 65.04f, 65.03f), Array(63.03f, 63.03f, 63.04f, 63.04f))

  private val pond = new Pond(123L, "pond", PondType.POND, "address", pondPolygon)

  private val point = new GeoPoint(64.0f, 62.0f)

  private val nearestPoint = new GeoPoint(65.03f, 63.03f)

  private val pondPoint = NearbyElement(pond, nearestPoint, 124663.02999454128)

  "PondDistanceServiceSpec in getDistance" should {

    val b = Transit.builder()
    b.setDist(10)
    b.setTime(100)
    b.setTransfers(2)
    b.setPoints(Seq(point, nearestPoint).asJava)

    "in getFastestFootTransit" should {
      "return None if transit contains transport part" in {
        b.setTransitParts(Seq[TransitPart](new Wait(1), new Walk(2, 3), new Transport(5)).asJava)
        val transit = b.build()

        val result = PondDistanceService.getDistance(point, pondPoint, Seq[Transit](transit))

        val expectedResult = None

        result shouldEqual expectedResult
      }

      "return Some if transit does not contain transport part" in {
        b.setTransitParts(Seq[TransitPart](new Wait(1), new Walk(2, 3)).asJava)
        val transit = b.build()

        val result = PondDistanceService.getDistance(point, pondPoint, Seq[Transit](transit)).get

        val expectedResult = PondTransit(
          pondPoint.element,
          new PondDistance(pondPoint.element.getId, transit.getTime, transit.getWalkingDist, pondPoint.point)
        )

        result shouldEqual expectedResult
      }

      "return Some(transit) with mim time" in {
        b.setTransitParts(Seq[TransitPart](new Wait(1), new Walk(2, 3)).asJava)
        val transit1 = b.build()
        b.setTime(50)
        b.setDist(5)
        val transit2 = b.build()

        val result = PondDistanceService.getDistance(point, pondPoint, Seq[Transit](transit1, transit2)).get

        val expectedResult = PondTransit(
          pondPoint.element,
          new PondDistance(pondPoint.element.getId, transit2.getTime, transit2.getWalkingDist, pondPoint.point)
        )

        result shouldEqual expectedResult
      }
    }

    "in updateDistanceIfNeeded" should {
      "update pond dist if needed" in {
        b.setDist(10000000)
        val transit = b.build()

        val result = PondDistanceService.getDistance(point, pondPoint, Seq[Transit](transit)).get

        val expectedResult = PondTransit(
          pondPoint.element,
          new PondDistance(pondPoint.element.getId, 89700, 124663, pondPoint.point)
        )

        result shouldEqual expectedResult
      }

      "don't update pond dist if don't needed" in {
        b.setDist(10)
        b.setTime(100)
        val transit = b.build()

        val result = PondDistanceService.getDistance(point, pondPoint, Seq[Transit](transit)).get

        val expectedResult = PondTransit(
          pondPoint.element,
          new PondDistance(pondPoint.element.getId, transit.getTime, transit.getWalkingDist, pondPoint.point)
        )

        result shouldEqual expectedResult
      }
    }
  }
}
