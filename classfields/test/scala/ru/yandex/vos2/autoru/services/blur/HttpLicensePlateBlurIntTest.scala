package ru.yandex.vos2.autoru.services.blur

import java.io.File

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.util.IO

@RunWith(classOf[JUnitRunner])
class HttpLicensePlateBlurIntTest extends AnyFunSuite {
  val file = IO.resourceToFile(this, "/license_plate_photo.jpg", "source", ".jpg")
  val client = new HttpLicensePlateBlur("https://yandex.ru/images-apphost/cbir-features")

  implicit val trace = Traced.empty

  test("recognize some") {
    val results = client.recognizeNumber((file))
    assert(results.length == 1)
    assert(results.head.number.getNumber == "T435OY77")
  }
}
