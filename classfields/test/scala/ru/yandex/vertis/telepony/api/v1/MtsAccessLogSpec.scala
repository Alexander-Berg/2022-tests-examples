package ru.yandex.vertis.telepony.api.v1

import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import org.scalactic.anyvals.{PosInt, PosZInt}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.json.EventParser
import ru.yandex.vertis.telepony.model.{Block, Generators, Meta, TypedDomains}

/**
  * @author evans
  */
class MtsAccessLogSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  implicit def propertyCheckConfiguration: PropertyCheckConfiguration =
    PropertyCheckConfiguration(sizeRange = PosZInt(100), minSuccessful = PosInt(100))

  private val mtsAccessLog = new MtsEventLog("telepony-event-log")

  "Mts access log" should {
    "build correct tskv log" in {
      val headers = List(Authorization(BasicHttpCredentials("autoru_def-stable", "autoru_def")))
      //scalastyle:off
      val body =
        """{"CallID":32159737159,"EventTime":"2016-09-03T05:07:56.6984644+03:00","AN":"9264961328","UN":"9165112603","DN1":null,"DN2":null,"EXT1":null,"EXT2":null,"DTMF":null,"Result":null,"EventType":1}"""
      val request = HttpRequest(entity = HttpEntity(body)).withHeaders(headers)
      val event = EventParser.parse(body).get
      val actual = mtsAccessLog.asTskv(TypedDomains.autoru_def, Some(request), event, Meta(Some(Block), None, None))
      val meta = """{"resolution":"Block"}"""

      val data = actual.split("\t").toSet
      val expected =
        s"tskv\ttskv_format=telepony-event-log\tcomponent=telepony-mts-event\tlocale=ru\tproject=telepony\tbody=$body\tdomain=${TypedDomains.autoru_def}\tauthorization=Basic YXV0b3J1X2RlZi1zdGFibGU6YXV0b3J1X2RlZg==\tmeta=$meta"
          .split("\t")
          .toSet
      data should contain allElementsOf expected
      //scalastyle:on
    }
  }

  "Meta" should {
    "serialize/deserialize" in {
      forAll(Generators.MetaGen) { meta =>
        Meta.fromJson(meta.toJson) shouldEqual meta
      }
    }
  }
}
