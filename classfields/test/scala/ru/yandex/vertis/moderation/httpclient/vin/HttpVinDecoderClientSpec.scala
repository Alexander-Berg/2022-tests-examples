package ru.yandex.vertis.moderation.httpclient.vin

import org.apache.http.HttpHost
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.auto.api.vin.VinResolutionEnums.{ResolutionPart, Status}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.client.http.HttpCodeException
import ru.yandex.vertis.moderation.httpclient.vin.VinDecoderClient._
import ru.yandex.vertis.moderation.httpclient.vin.impl.http.HttpVinDecoderClient

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class HttpVinDecoderClientSpec extends SpecBase {

  lazy val client =
    new HttpVinDecoderClient(
      new HttpHost("auto-vin-decoder-api.vrts-slb.test.vertis.yandex.net", 80),
      new DefaultAsyncHttpClientConfig.Builder().build()
    )

  "VinDecoderClient" should {

    "getRegistration: successfully gets autocode info for JTHBK262602084125" in {
      val autocode = client.autocode("JTHBK262602084125").futureValue
      autocode.map(_.getRegistration.getVin) shouldBe Some("JTHBK262602084125")
    }

    "autocode: return None for JTHBK262084125" in {
      client.autocode("JTHBK262084125").futureValue shouldBe None
    }

    "autocode: return 400 exception for empty vin" in {
      client.autocode("").failed.futureValue shouldBe a[HttpCodeException]
    }

    "decode: successfully decode vin Z94CC41BBER184593 assign" in {
      val expected =
        Some(
          DecodedVin(
            "Z94CC41BBER184593",
            Seq(
              Result("HYUNDAI", "ACCENT", year = 2014),
              Result("HYUNDAI", "SOLARIS", year = 2014),
              Result("HYUNDAI", "SONATA", year = 2014),
              Result("KIA", "RIO", year = 2014),
              Result("KIA", "NIRO", year = 2014)
            )
          )
        )
      client.decode("Z94CC41BBER184593").futureValue shouldBe expected
    }

    "decode: successfully decode vin Z94CC41BBER184593 assign only_mark" in {
      val expected =
        Some(
          DecodedVin(
            "Z94CC41BBER184593",
            Seq(Result("HYUNDAI"), Result("KIA"))
          )
        )
      client.decode("Z94CC41BBER184593", onlyMark = true).futureValue shouldBe expected
    }

    "decode: successfully decode incorrect vin" in {
      client.decode("Z94CC41BBER184").futureValue shouldBe Some(DecodedVin("Z94CC41BBER184"))
    }

    "successfully gets offers for X4XJC39480WB39444" in {
      val expectedOfferId = "1068251448-95115"
      client
        .offerHistory("X4XJC39480WB39444")
        .futureValue
        .exists(_.getOfferId == expectedOfferId) shouldBe true
    }

    "successfully gets offers for SALWA2FK7HA135034" in {
      val expectedOfferId = "1047720202-d9b0dd"
      client
        .offerHistory("SALWA2FK7HA135034")
        .futureValue
        .exists(_.getOfferId == expectedOfferId) shouldBe true
    }

    "offerHistory: return empty Seq for incorrect vin" in {
      client.offerHistory("Z94CC41BBER184").futureValue shouldBe empty
    }

    "offerHistory: return empty Seq for vin with no offers" in {
      client.offerHistory("XW8ZZZ71ZDG005710").futureValue shouldBe empty
    }

    "resolution: OK for VIN Z8T4C5FS9BM005269" in {
      val resolutionRequest =
        ResolutionRequest(
          offerId = "1066271252-e521c",
          vin = "Z8T4C5FS9BM005269",
          mark = None,
          model = None,
          year = None,
          powerHp = None,
          displacement = Some(1600),
          bodyType = None,
          ownersCount = None,
          sellerType = None,
          color = None,
          format = None,
          kmAge = None,
          price = None
        )

      val entities =
        client
          .resolution(resolutionRequest)
          .futureValue
          .get
          .getEntriesList
          .asScala
          .map(x => x.getPart -> x.getStatus)
          .toMap

      entities(ResolutionPart.SUMMARY) shouldBe Status.UNKNOWN
    }
  }
}
