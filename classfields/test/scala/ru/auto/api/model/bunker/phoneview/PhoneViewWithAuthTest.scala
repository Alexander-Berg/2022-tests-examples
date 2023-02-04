package ru.auto.api.model.bunker.phoneview

import org.scalatest.funsuite.AnyFunSuite

import java.io.ByteArrayInputStream
import java.net.URL

class PhoneViewWithAuthTest extends AnyFunSuite {

  test("parse PhoneViewWithAuth from json") {
    val in = new ByteArrayInputStream("""[
                                        |  {
                                        |    "fullName": "/auto_ru/common/phone_view_with_auth",
                                        |    "content": {
                                        |      "descriptionRu": "test",
                                        |      "geobaseIds": [
                                        |        1,
                                        |        2,
                                        |        3
                                        |      ],
                                        |      "withoutGosuslugi": {
                                        |        "descriptionRu": "without gosuslugi",
                                        |        "limit": 100,
                                        |        "url": "https://auto.ru/some_url1"
                                        |      },
                                        |      "withGosuslugi": {
                                        |        "descriptionRu": "with gosuslugi",
                                        |        "limit": 200,
                                        |        "url": "https://auto.ru/some_url2"
                                        |      }
                                        |    }
                                        |  }
                                        |]""".stripMargin.getBytes("UTF-8"))
    val actual = PhoneViewWithAuth.parse(in)
    val expected = PhoneViewWithAuth(
      descriptionRu = Some("test"),
      geobaseIds = Set(1L, 2L, 3L),
      withoutGosuslugi = Some(
        WithoutGosuslugi(
          descriptionRu = Some("without gosuslugi"),
          limit = Some(100),
          url = Some(new URL("https://auto.ru/some_url1"))
        )
      ),
      withGosuslugi = Some(
        WithGosuslugi(
          descriptionRu = Some("with gosuslugi"),
          limit = Some(200),
          url = Some(new URL("https://auto.ru/some_url2"))
        )
      )
    )
    assert(actual == expected)
  }

  test("parse PhoneViewWithAuth from empty json") {
    val in = new ByteArrayInputStream("""[{
                                        |"fullName": "/auto_ru/common/phone_view_with_auth",
                                        |"content": {}
                                        |}]""".stripMargin.getBytes("UTF-8"))
    val actual = PhoneViewWithAuth.parse(in)
    val expected = PhoneViewWithAuth.Empty
    assert(actual == expected)
  }
}
