package ru.yandex.vertis.moderation.api

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.proto.Model.Service

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class ApiUtilsSpec extends SpecBase {

  case class TestCase(description: String, uri: String, expectedResult: Service)

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "extract service from uri",
        uri = "/api/1.x/DEALERS_AUTORU/instance/single",
        expectedResult = Service.DEALERS_AUTORU
      ),
      TestCase(
        description = "return UNKNOWN if no service",
        uri = "http://moderation-api.vrts-slb.test.vertis.yandex.net/api/1.x/feature",
        expectedResult = Service.UNKNOWN_SERVICE
      )
    )

  "extractServiceFromUri" should {
    testCases.foreach { case TestCase(description, uri, expectedResult) =>
      description in {
        val actualResult = ApiUtils.extractServiceFromUri(uri)
        actualResult shouldBe expectedResult
      }
    }
  }
}
