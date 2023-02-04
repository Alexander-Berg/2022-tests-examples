package ru.yandex.vertis.moderation.httpclient.vin

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.httpclient.vin.impl.http.HttpVinDecoderProtocol
import ru.yandex.vertis.moderation.httpclient.vin.VinDecoderClient._

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class HttpVinDecoderProtocolSpec extends SpecBase {

  import HttpVinDecoderProtocol._

  "HttpVinDecoderProtocol" should {
    "successfully parse response json only_mark" in {
      val input = readResource("/vin-decoder-tests/decode-1.json")
      Json.parse(input).as[DecodedVin] shouldBe
        DecodedVin(
          "Z94CC41BBER184593",
          Seq(
            Result("HYUNDAI"),
            Result("KIA"),
            Result("VAZ"),
            Result("BMW", yearFrom = Some(2015)),
            Result("NISSAN", Some("ALMERA"), yearTo = Some(2015))
          )
        )
    }

    "successfully parse response json" in {
      val input = readResource("/vin-decoder-tests/decode-2.json")
      Json.parse(input).as[DecodedVin] shouldBe
        DecodedVin(
          "Z94CC41BBER184593",
          Seq(Result("HYUNDAI", "ACCENT", 2015), Result("HYUNDAI", "SOLARIS", 2015), Result("KIA", "RIO", 2015))
        )
    }

    "fail in case absence of mandatory mark field" in {
      val input = readResource("/vin-decoder-tests/decode-3.json")
      intercept[java.util.NoSuchElementException] {
        Json.parse(input).as[DecodedVin]
      }
    }
  }
}
