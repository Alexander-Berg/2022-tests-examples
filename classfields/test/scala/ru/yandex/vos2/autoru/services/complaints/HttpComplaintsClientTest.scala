package ru.yandex.vos2.autoru.services.complaints

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.services.compliance.HttpComplaintsClient
import ru.yandex.vos2.util.HttpBlockingPool.Instance
import ru.yandex.vos2.util.http.MockHttpClientHelper

class HttpComplaintsClientTest
  extends AnyFunSuite
  with ScalaCheckPropertyChecks
  with Matchers
  with MockitoSupport
  with MockHttpClientHelper {

  implicit val trace = Traced.empty
  val operationalSupport = TestOperationalSupport

  val responseWithCompliance = """{
                               |  "status": "OK",
                               |  "NO_ANSWER": [
                               |    {
                               |      "userId": "g5ea07ceb1melgovjlocsoludgiig2lj.495770b456f55e7a8a93d8e209ab4555",
                               |      "complaintId": "6723987241853481163",
                               |      "modobjId": "CAESJQgBEgwIATIINTEwNTQ2MzIaEzEwOTc1MzcyODgtOGIyODM4NTkaAA==",
                               |      "ctype": "PHONE_UNAVAILABLE",
                               |      "description": "",
                               |      "created": "2020-04-22T20:20Z"
                               |    }
                               |  ]
                               |}""".stripMargin

  val responseWithoutCompliance = """{
                                  |  "reason": "No complaints for offer id Plain(1097537288-8b2838591)"
                                  |}""".stripMargin

  test("No compliance found") {
    assert(mockClientSuccessWithoutCompliants.getHistory("", onlyFromGoodUsers = true).isEmpty)
  }

  test("Some compliance found") {
    assert(mockClientSuccess.getHistory("", onlyFromGoodUsers = true).nonEmpty)
  }

  def mockClientSuccess: HttpComplaintsClient = {
    new HttpComplaintsClient(hostname = "1", port = 1, operationalSupport) {
      override val client = new Instance(mockHttpClient(200, responseWithCompliance))
    }
  }

  def mockClientSuccessWithoutCompliants: HttpComplaintsClient = {
    new HttpComplaintsClient(hostname = "1", port = 1, operationalSupport) {
      override val client = new Instance(
        mockHttpClient(404, responseWithoutCompliance)
      )
    }
  }

}
