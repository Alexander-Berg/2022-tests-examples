package ru.yandex.vos2.autoru.services.cabinet

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced

/**
  * Created by sievmi on 20.03.19
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HttpCabinetClientIntTest extends AnyFunSuite {

  val client = new HttpCabinetClient("autoru-cabinet-api-01-sas.test.vertis.yandex.net", 2030, TestOperationalSupport)

  implicit val traced = Traced.empty
  test("email non empty") {
    pending
    assert(client.getEmail(25838L, "money").contains("ru77re04@ilsa.ru"))
  }

}
