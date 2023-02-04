package ru.yandex.realty.flatgeoindex

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.flatgeoindex.MultiGeoIndex.{GeoElement, GeoReference}
import ru.yandex.realty.model.geometry.{HolePolygon, MultiPolygon, Point, Polygon}

@RunWith(classOf[JUnitRunner])
class GeoElementProtoConverterSpec extends WordSpec with Matchers {

  private val polygon = new Polygon(Array(1.0f, 2.0f, 3.0f), Array(1.0f, 2.0f, 3.0f))
  private val holePolygon = new HolePolygon(polygon, polygon, polygon)
  private val multiPolygon = new MultiPolygon(holePolygon, holePolygon, holePolygon)

  private val geometries = Seq(
    polygon,
    multiPolygon,
    holePolygon,
    new Point(1.0f, 2.0f)
  )

  private val references =
    Seq(
      GeoReference.Railway(1L),
      GeoReference.Metro(123L),
      GeoReference.Street(2),
      GeoReference.District(3L)
    )

  "GeoElementProtoConverter" should {
    "correctly convert elements" in {
      for {
        g <- geometries
        r <- references
        elem = GeoElement(g, r)
      } withClue(s"on $elem") {
        val actual = GeoElementProtoConverter.fromProto(GeoElementProtoConverter.toProto(elem))
        actual shouldBe elem
      }

    }
  }
}
