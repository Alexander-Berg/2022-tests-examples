package ru.yandex.vertis.moderation.geobase

import java.net.InetAddress

import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.geobase.GeobaseClient.{IpInfo, RegionId}

trait GeobaseClientSpecBase extends SpecBase {

  protected def geobaseClient: GeobaseClient

  private case class RegionIdByIpTestCase(ip: InetAddress, expected: RegionId)
  private case class СountryIdByRegionIdTestCase(regionId: RegionId, expected: RegionId)
  private case class IpInfoTestCase(ip: InetAddress, expected: IpInfo)

  private val regionIdByIpTestCases =
    Seq(
      RegionIdByIpTestCase(
        ip = InetAddress.getByName("77.88.8.1"),
        expected = 104357
      ),
      RegionIdByIpTestCase(
        ip = InetAddress.getByName("87.250.250.242"),
        expected = 10962
      ),
      RegionIdByIpTestCase(
        ip = InetAddress.getByName("69.10.42.209"),
        expected = 102946
      ),
      RegionIdByIpTestCase(
        ip = InetAddress.getByName("84.201.167.156"),
        expected = 213
      ),
      RegionIdByIpTestCase(
        ip = InetAddress.getByName("2a02:6b8:0:81f::1:1b"),
        expected = 213
      )
    )

  private val countryIdByRegionIdTestCases =
    Seq(
      СountryIdByRegionIdTestCase(
        regionId = 51,
        expected = 225
      ),
      СountryIdByRegionIdTestCase(
        regionId = 43,
        expected = 225
      ),
      СountryIdByRegionIdTestCase(
        regionId = 2,
        expected = 225
      ),
      СountryIdByRegionIdTestCase(
        regionId = 213,
        expected = 225
      ),
      СountryIdByRegionIdTestCase(
        regionId = 102946,
        expected = 84
      )
    )

  private val ipInfoTestCases =
    Seq(
      IpInfoTestCase(
        ip = InetAddress.getByName("2a02:6b8::3"),
        expected =
          IpInfo(
            ip = InetAddress.getByName("2a02:6b8::3"),
            isVpn = false,
            isProxy = false,
            isHosting = false,
            isTor = false,
            isYandexTurbo = false,
            isYandexStaff = false,
            isYandexNet = true
          )
      ),
      IpInfoTestCase(
        ip = InetAddress.getByName("103.249.28.195"),
        expected =
          IpInfo(
            ip = InetAddress.getByName("103.249.28.195"),
            isVpn = false,
            isProxy = false,
            isHosting = true,
            isTor = false,
            isYandexTurbo = false,
            isYandexStaff = false,
            isYandexNet = false
          )
      ),
      IpInfoTestCase(
        ip = InetAddress.getByName("185.220.101.21"),
        expected =
          IpInfo(
            ip = InetAddress.getByName("185.220.101.21"),
            isVpn = true,
            isProxy = false,
            isHosting = true,
            isTor = true,
            isYandexTurbo = false,
            isYandexStaff = false,
            isYandexNet = false
          )
      )
    )

  "GeobaseClient.regionIdByIp" should {
    regionIdByIpTestCases.foreach { case RegionIdByIpTestCase(ip, expected) =>
      ip.getHostAddress in {
        geobaseClient.regionIdByIp(ip).futureValue shouldBe expected
      }
    }
  }

  "GeobaseClient.countryIdByRegionId" should {
    countryIdByRegionIdTestCases.foreach { case СountryIdByRegionIdTestCase(regionId, expected) =>
      s"Region id $regionId" in {
        geobaseClient.countryIdByRegionId(regionId).futureValue shouldBe expected
      }
    }
  }

  "GeobaseClient.ipInfo" should {
    ipInfoTestCases.foreach { case IpInfoTestCase(ip, expected) =>
      ip.getHostAddress in {
        geobaseClient.ipInfo(ip).futureValue shouldBe expected
      }
    }
  }
}
