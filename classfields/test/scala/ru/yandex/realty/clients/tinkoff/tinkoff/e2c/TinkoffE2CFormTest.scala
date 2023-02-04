package ru.yandex.realty.clients.tinkoff.tinkoff.e2c

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.clients.tinkoff.e2c.TinkoffE2CForm
import ru.yandex.realty.clients.tinkoff.e2c.request.AddCustomerRequest

@RunWith(classOf[JUnitRunner])
class TinkoffE2CFormTest extends FunSuite {
  val form = new TinkoffE2CForm("key", AddCustomerRequest("test", Some("127.0.0.1"), Some("test@yandex-team.ru"), None))

  test("getDocument") {
    assert(form.getDocument == "testtest@yandex-team.ru127.0.0.1key")
  }

  test("getFormData") {
    assert(
      form.getFormData(BigInt("853886787330665878795494786977321693504"), "digest", "signature") == List(
        ("CustomerKey", "test"),
        ("Email", "test@yandex-team.ru"),
        ("IP", "127.0.0.1"),
        ("TerminalKey", "key"),
        ("DigestValue", "digest"),
        ("SignatureValue", "signature"),
        ("X509SerialNumber", "0282649d0028ad12954a005285cc0bb140")
      )
    )
  }
}
