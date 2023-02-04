package ru.auto.salesman.client.uaas.impl

import com.google.common.io.BaseEncoding
import ru.auto.salesman.client.uaas.UaasClient.UaasClientException.RequestException
import ru.auto.salesman.model.user.Experiments
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.sttp.SttpClient
import sourcecode.{File, Name}
import sttp.client3.{Request, Response}
import sttp.model.{Header, StatusCode}

class UaasClientImplSpec extends BaseSpec {
  import UaasExperimentExtractorSpec._
  import UaasClientImplSpec._

  private val sttp = mock[SttpClient]
  private val client = new UaasClientImpl("not used in test", sttp)

  "fail with RequestException on http error" in {
    (sttp
      .send(_: Request[String, Any])(
        _: File,
        _: Name
      ))
      .expects(*, *, *)
      .throwingZ(new Exception("bla"))

    val result = client.getExperiments(testUuid).failure.exception
    result shouldBe an[RequestException]
  }

  "return experiments" in {
    val response = Response[String](
      body = "",
      code = StatusCode.Ok,
      statusText = "",
      headers = List(
        Header("X-Yandex-ExpFlags", testExperimentBase64),
        Header("X-Yandex-ExpBoxes", boxesHeaderValue)
      )
    )

    (sttp
      .send(_: Request[String, Any])(
        _: File,
        _: Name
      ))
      .expects(*, *, *)
      .returningZ(response)

    val expected =
      Some(Experiments(boxesHeaderValue, List(expectedFullExperiment)))

    val result = client.getExperiments(testUuid).success.value
    result shouldBe expected
  }

  "return empty experiments if no experiments header returns" in {
    val response = Response[String](
      body = "",
      code = StatusCode.Ok
    )

    (sttp
      .send(_: Request[String, Any])(
        _: File,
        _: Name
      ))
      .expects(*, *, *)
      .returningZ(response)

    val result = client.getExperiments(testUuid).success.value
    result shouldBe empty
  }

  "return empty experiments if ExpFlags contains empty json" in {
    val response = Response[String](
      body = "",
      code = StatusCode.Ok,
      statusText = "",
      headers = List(
        Header("X-Yandex-ExpFlags", emptyExperimentBase64),
        Header("X-Yandex-ExpBoxes", boxesHeaderValue)
      )
    )

    (sttp
      .send(_: Request[String, Any])(
        _: File,
        _: Name
      ))
      .expects(*, *, *)
      .returningZ(response)

    val expected = Some(Experiments(boxesHeaderValue, Nil))

    val result = client.getExperiments(testUuid).success.value
    result shouldBe expected
  }

}

object UaasClientImplSpec {
  val testUuid = "123"

  val emptyExperimentBase64: String = BaseEncoding
    .base64()
    .encode(
      """[
        |  {
        |    "HANDLER": "AUTORU_SALESMAN_USER",
        |    "CONTEXT": {
        |      "MAIN": {}
        |    }
        |  }
        |]""".stripMargin
        .getBytes()
    )
}
