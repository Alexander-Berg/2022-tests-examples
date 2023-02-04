package ru.yandex.auto.searcher.http.action

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.Request
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.auto.searcher.http.action.api.tax.TaxRequest

@RunWith(classOf[JUnitRunner])
class TransportTaxHandlerSpec extends WordSpec with Matchers with MockFactory with OneInstancePerTest {

  "ParamsBuilder" should {

    "fail on wrong-typed query" in withRequest(
      "complectation_id" -> "3480339",
      "car_production_year" -> "2006",
      "rid" -> "Moscow"
    ) { req =>
      val e = TransportTaxHandler.buildParams(req)
      val expectedParams = "rid is empty or invalid"
      e.isFailure should be(true)
      e.failed.get.getMessage should include(expectedParams)
    }

    "fail on non-complete query" in withRequest(
      "car_production_year" -> "2019",
      "test" -> "test"
    ) { req =>
      val e = TransportTaxHandler.buildParams(req)
      val expectedParams = "rid is empty or invalid"

      e.isFailure should be(true)
      e.failed.get.getMessage should include(expectedParams)
    }

    "fail on missed required params" in withRequest(
      "car_production_year" -> "2019",
      "rid" -> "213"
    ) { req =>
      val e = TransportTaxHandler.buildParams(req)
      val expectedParams = "Required params: [tech_param_id] or [mark, model]"

      e.isFailure should be(true)
      e.failed.get.getMessage should include(expectedParams)
    }

    "fail on missed required params when russians all regions" in withRequest(
      "car_production_year" -> "2019",
      "rid" -> "225"
    ) { req =>
      val e = TransportTaxHandler.buildParams(req)
      val expectedParams = "Required params: [tech_param_id] or [mark, model, super_gen_id]"

      e.isFailure should be(true)
      e.failed.get.getMessage should include(expectedParams)
    }

    "success on required params when russians all regions 2" in withRequest(
      "mark" -> "KIA",
      "model" -> "K900",
      "super_gen_id" -> "21481320",
      "rid" -> "225"
    ) { req =>
      val e = TransportTaxHandler.buildParams(req)

      e.isFailure should be(false)
      e.get.paramsRequest.rid should be(225)
    }

    "return params on correct query" in withRequest(
      "tech_param_id" -> "2307173",
      "complectation_id" -> "2307173",
      "car_production_year" -> "2006",
      "rid" -> "213",
      "mark" -> "KIA",
      "model" -> "RIO",
      "super_gen_id" -> "21003710",
      "configuration_id" -> "21003765",
      "horse_power" -> "97"
    ) { req =>
      val expected = TaxRequest(
        rid = 213,
        mark = Some("KIA"),
        model = Some("RIO"),
        productionYear = Some(2006),
        horsePower = Some(97),
        maybeSuperGenId = Some(21003710),
        maybeComplectationId = Some(2307173),
        maybeConfigurationId = Some(21003765),
        maybeTechParamId = Some(2307173)
      )
      TransportTaxHandler.buildParams(req).get.paramsRequest should be(expected)
    }
  }

  def withRequest[T](params: (String, String)*)(code: AHandler.RequestInfo => T): T = {
    import scala.collection.JavaConverters._
    val baseRequest = new Request()
    baseRequest.setMethod("GET")

    val map = params.toMap.mapValues(Array(_)).asJava

    val request = stub[HttpServletRequest]
    (request.getParameterMap _).when().returns(map)

    val response = stub[HttpServletResponse]

    val info = AHandler.RequestInfo("test_target", baseRequest, request, response)
    code(info)
  }

}
