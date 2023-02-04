package ru.yandex.vos2.autoru.services

import org.scalatest.OptionValues
import ru.yandex.vertis.baker.components.http.client.tvm.TvmClientWrapper
import ru.yandex.vertis.baker.components.http.client.tvm.FixedIdTvmClientWrapper
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.services.geocoder.GeocoderClient

import java.io.{File, FileWriter}
import scala.io.Source
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BuildTinkoffOsagoCoefficientTest extends AnyFunSuite with OptionValues with InitTestDbs {

  private lazy val operationalSupport = TestOperationalSupport
  implicit val trace = Traced.empty

  private lazy val geocoderTvmClient: FixedIdTvmClientWrapper = {
    val conf = components.env.serviceConfig.getConfig("tvm")
    val tvmClientId = conf.getInt("client_id")
    val tvmSecret = conf.getString("secret")
    val geocoderClientId = conf.getInt("geocoder.id")
    TvmClientWrapper
      .build(tvmClientId, tvmSecret, geocoderClientId)
      .newFixedIdWrapper(geocoderClientId)
  }

  lazy val geocoderClient =
    new GeocoderClient("addrs-testing.search.yandex.net", 80, geocoderTvmClient, operationalSupport)

  private def readKTCoefficients(): Map[Int, Double] = {
    val is = getClass.getResourceAsStream("/osago/kt-tinkoff.csv")
    try {
      val source = Source.fromInputStream(is, "UTF-8")

      source
        .getLines()
        .map { line =>
          val Array(regionNum, cityName, coefficient) = line.split(",")
          val geoId =
            if (cityName.nonEmpty) geocoderClient.getGeoId(cityName)
            else geocoderClient.getGeoId(s"регион $regionNum")
          geoId -> coefficient.toDouble
        }
        .filter { case (geoId, _) => geoId.nonEmpty }
        .map { case (geoId, coef) => geoId.get -> coef }
        .toMap[Int, Double]
    } finally is.close()
  }

  private def writeKTCoefficients(kt: Map[Int, Double]): Unit = {
    val url = getClass.getResource("/osago/kt-autoru.csv")
    val file = new File(url.getPath.replace("/target/classes/", "/src/main/resources/"))
    val fw = new FileWriter(file)
    kt.foreach {
      case (geoId, coefficient) =>
        fw.write(s"$geoId,$coefficient\n")
    }
    fw.flush()
    fw.close()
  }

  private def readTinkoffTariff(): Map[Int, Int] = {
    val is = getClass.getResourceAsStream("/osago/tariff-tinkoff.csv")
    try {
      val source = Source.fromInputStream(is, "UTF-8")

      source
        .getLines()
        .map { line =>
          val Array(regionName, cityName, coefficient) = line.split(",")
          val geoId =
            if (cityName.nonEmpty) geocoderClient.getGeoId(cityName)
            else geocoderClient.getGeoId(regionName)
          geoId -> coefficient.toInt
        }
        .filter { case (geoId, _) => geoId.nonEmpty }
        .map { case (geoId, coef) => geoId.get -> coef }
        .toMap[Int, Int]
    } finally is.close()
  }

  private def writeTariff(tariffMap: Map[Int, Int]): Unit = {
    val url = getClass.getResource("/osago/tariff-autoru.csv")
    val file = new File(url.getPath.replace("/target/classes/", "/src/main/resources/"))
    val fw = new FileWriter(file)
    tariffMap.foreach {
      case (geoId, tariff) =>
        fw.write(s"$geoId,$tariff\n")
    }
    fw.flush()
    fw.close()
  }

  ignore("update kt") {
    val ktMap = readKTCoefficients()
    writeKTCoefficients(ktMap)
  }

  ignore("update tariff") {
    val tariff = readTinkoffTariff()
    writeTariff(tariff)
  }
}
