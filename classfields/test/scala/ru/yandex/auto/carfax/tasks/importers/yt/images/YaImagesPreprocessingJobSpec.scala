package ru.yandex.auto.carfax.tasks.importers.yt.images

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.carfax.scheduler.tasks.importers.yt.images.YaImagesPreprocessingJob

class YaImagesPreprocessingJobSpec extends AnyWordSpecLike with Matchers {

  "Licenseplate regexp" should {
    "get correct common and taxi licenseplates from slplitted source string by space symbol" in {

      val lpRegexp = YaImagesPreprocessingJob.lpRegexp.r

      val strings = List(
        "",
        " ",
        // ru
        "invalidВ123ЕК99",
        "or invalid В123ЕК99",
        "В123ЕК99",
        "invalidНО78599",
        "or invalid НО78599",
        "НО78599",
        "invalidо123рс99",
        "or invalid о123рс99",
        "о123рс99",
        "invalidух78599",
        "or invalid ух78599",
        "ух78599",
        // en
        "invalidA123PX199",
        "or invalid A123PX199",
        "A123PX199",
        "invalidTM456199",
        "or invalid TM456199",
        "TM78599",
        "invalida123em199",
        "or invalid a123em199",
        "a123em199",
        "invalidcy456199",
        "or invalid cy456199",
        "cy78599"
      )

      val res = strings.map(lpRegexp.findFirstMatchIn)

      println(res)

      res.head shouldBe empty
      res(1) shouldBe empty

      res(2) shouldBe empty
      res(3) shouldBe empty
      res(4).get.toString shouldBe "В123ЕК99"
      res(5) shouldBe empty
      res(6) shouldBe empty
      res(7).get.toString shouldBe "НО78599"

      res(8) shouldBe empty
      res(9) shouldBe empty
      res(10).get.toString shouldBe "о123рс99"
      res(11) shouldBe empty
      res(12) shouldBe empty
      res(13).get.toString shouldBe "ух78599"

      res(14) shouldBe empty
      res(15) shouldBe empty
      res(16).get.toString shouldBe "A123PX199"
      res(17) shouldBe empty
      res(18) shouldBe empty
      res(19).get.toString shouldBe "TM78599"

      res(20) shouldBe empty
      res(21) shouldBe empty
      res(22).get.toString shouldBe "a123em199"
      res(23) shouldBe empty
      res(24) shouldBe empty
      res(25).get.toString shouldBe "cy78599"
    }
  }
}
