package ru.yandex.vertis.telepony.util.json

import org.joda.time.LocalTime
import ru.yandex.vertis.telepony.SpecBase
import spray.json._
import spray.json.DefaultJsonProtocol._

class DateTimeFormatsSpec extends SpecBase with DateTimeFormats {

  case class SomeClassWithLocalTime(localTime: LocalTime)

  implicit val format = jsonFormat1(SomeClassWithLocalTime)

  "DateTimeFormats" should {
    "Convert LocalTime from Json" in {

      val json =
        """
          |{"localTime": "08:59:59"}
          |""".stripMargin

      val parsed = json.parseJson.convertTo[SomeClassWithLocalTime]

      parsed shouldBe SomeClassWithLocalTime(new LocalTime(8, 59, 59))
    }

    "Convert LocalTime to Json" in {

      val some = SomeClassWithLocalTime(new LocalTime(8, 59, 59))
      val json = "{\"localTime\":\"08:59:59\"}"

      some.toJson.toString shouldBe json
    }

    "Convert LocalTime from Json and back" in {

      val json = "{\"localTime\":\"08:59:59\"}"

      val parsed = json.parseJson.convertTo[SomeClassWithLocalTime]

      parsed.toJson.toString shouldBe json
    }
  }
}
