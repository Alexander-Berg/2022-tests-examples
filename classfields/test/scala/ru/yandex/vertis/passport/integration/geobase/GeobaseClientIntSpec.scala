package ru.yandex.vertis.passport.integration.geobase

import akka.stream.ActorMaterializer
import com.google.common.net.InetAddresses
import org.scalatest.{Ignore, WordSpec}
import ru.yandex.vertis.passport.AkkaSupport
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.util.http.AkkaHttpClient

/**
  *
  * @author zvez
  */
@Ignore // the test is flaky because geo base service is not very stable
class GeobaseClientIntSpec extends WordSpec with SpecBase with AkkaSupport {

  val client =
    new GeobaseClientImpl(
      GeobaseConfig("http://geobase.qloud.yandex.ru"),
      new AkkaHttpClient(ActorMaterializer())
    )

  "GeobaseClient.getRegionIdByIp" should {
    "resolve real address" in {
      val ip = InetAddresses.forString("176.59.133.165")
      client.getRegionByIp(ip).futureValue shouldBe 59
    }

    "resolve region for ipv4 address" in {
      val ip = InetAddresses.forString(ModelGenerators.ipv4Address.next)
      client.getRegionByIp(ip).futureValue
    }

    "resolve region for ipv6 address" in {
      val ip = InetAddresses.forString(ModelGenerators.ipv6Address.next)
      client.getRegionByIp(ip).futureValue
    }
  }

  "GeobaseClient.findCountry" should {
    "find country by region id" in {
      client.findCountry(59).futureValue shouldBe RussiaRegionId
    }
  }

}
