package ru.yandex.realty.misc.servantlets.location

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.searcher.jetty.HttpServRequest

@RunWith(classOf[JUnitRunner])
class CheckLocationParamsParserSpec extends SpecBase {

  private val commonRequestParams = java.util.Map.of(
    "locale",
    "ru",
    "country",
    "Russia",
    "longitude",
    "36,76",
    "latitude",
    "56,33",
    "category",
    "APARTMENT"
  )

  "CheckLocationParamsParserSpec" should {
    "parse http request with address " in {
      val request = new HttpServRequest()
      val address = "my address"
      val params = new java.util.HashMap[String, String]()
      params.put("address", address)
      params.putAll(commonRequestParams)
      request.setParams(params)

      val checkLocationParams = CheckLocationParamsParser.parse(request)

      checkLocationParams.address shouldBe address
      checkLocationParams.buildingId shouldBe null
    }
    "parse http request with buildingId " in {
      val request = new HttpServRequest()
      val buildingId = "3331123657455347"
      val params = new java.util.HashMap[String, String]()
      params.put("buildingId", buildingId)
      params.putAll(commonRequestParams)
      request.setParams(params)

      val checkLocationParams = CheckLocationParamsParser.parse(request)

      checkLocationParams.address shouldBe null
      checkLocationParams.buildingId shouldBe buildingId.toLong
    }
  }
}
