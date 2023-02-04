package ru.yandex.realty.model.geosrc

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.geometry.Polygon
import ru.yandex.realty.model.location.GeoPoint

@RunWith(classOf[JUnitRunner])
class ToponymProtoConverterSpec extends SpecBase {

  "ToponymProtoConverter" should {
    "correctly serialize/deserialize toponym" in {
      val toponym = Toponym(
        id = 10,
        kind = "locality",
        names = List(Name("А", "display", "ru"), Name("город А", "official", "ru")),
        geoId = 10,
        point = GeoPoint.getPoint(1.5f, 1.5f),
        envelope = Envelope(GeoPoint.getPoint(1f, 1f), GeoPoint.getPoint(2f, 2f)),
        arrivalPoints = Seq(GeoPoint.getPoint(1.5f, 1.5f)),
        geometry = Some(new Polygon(Seq(1f, 1f, 2f, 2f).toArray, Seq(1f, 2f, 2f, 1f).toArray))
      )
      ToponymProtoConverter.fromMessage(ToponymProtoConverter.toMessage(toponym)) shouldEqual toponym
    }
  }
}
