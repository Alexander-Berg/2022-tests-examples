package ru.yandex.vos2.autoru.services.parsing

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.ops.test.TestOperationalSupport

/**
  * TODO
  *
  * @author aborunov
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HttpParsingClientTest extends AnyFunSuite {

  private val client = new HttpParsingClient("host", 80, TestOperationalSupport)

  test("handlePhotosResponse: 400 incorrect params") {
    val res = client.handlePhotosResponse("remoteUrl", 400, """{ "error": "INCORRECT_PARAMS",  "status": "ERROR"}""")
    assert(res == ParsedPhotosResponse.IncorrectUrl("remoteUrl"))
  }

  test("handlePhotosResponse: 200") {
    val res = client.handlePhotosResponse("remoteUrl", 200, """{ "photo": ["photo1", "photo2", "photo3"]}""")
    assert(res == ParsedPhotosResponse.Photos(Seq("photo1", "photo2", "photo3")))
  }

  test("handlePhotosResponse: other responses") {
    val e = intercept[RuntimeException] {
      client.handlePhotosResponse("remoteUrl", 400, """{ "error": 15,  "status": "ERROR"}""")
    }
    assert(e.getMessage == """Unexpected response: 400, body = { "error": 15,  "status": "ERROR"}""")
  }

  test("handlePhotosResponse: empty") {
    val res = client.handlePhotosResponse("remoteUrl", 200, """{}""")
    assert(res == ParsedPhotosResponse.Photos(Seq.empty))
  }
}
