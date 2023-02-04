package vertis.pica.model

import vertis.pica.model.Url.IllegalUrl
import vertis.pica.util.UrlUtils
import vertis.zio.test.ZioSpecBase
import zio.IO

/** @author ruslansd
  */
class UrlUtilsSpec extends ZioSpecBase {

  "UrlNormalizer" should {
    "normalize simple urls" in {
      val scenarios = Seq(
        "https://ya.ru" -> "https://ya.ru",
        "https://yandex.ru" -> "https://yandex.ru",
        "https://yandex.ru/funnyimage.png" -> "https://yandex.ru/funnyimage.png"
      )
      testScenarios(scenarios)
    }

    "cut trailing slashes" in {
      val scenarios = Seq(
        "https://ya.ru//" -> "https://ya.ru",
        "https://yandex.ru////" -> "https://yandex.ru",
        "https://yandex.ru/funnyimage.png///" -> "https://yandex.ru/funnyimage.png"
      )
      testScenarios(scenarios)
    }

    "correctly encode cyrillic symbols" in {
      val scenarios = Seq(
        "http://test.ru/тест .jpg" -> "http://test.ru/%D1%82%D0%B5%D1%81%D1%82%20.jpg",
        "http://test.ru/тест.jpg" -> "http://test.ru/%D1%82%D0%B5%D1%81%D1%82.jpg",
        "http://россия.рф/тест.jpg" -> "http://xn--h1alffa9f.xn--p1ai/%D1%82%D0%B5%D1%81%D1%82.jpg",
        "http://%D1%80%D0%BE%D1%81%D1%81%D0%B8%D1%8F.%D1%80%D1%84/тест.jpg" -> "http://xn--h1alffa9f.xn--p1ai/%D1%82%D0%B5%D1%81%D1%82.jpg",
        "http://россия.рф/test.jpg" -> "http://xn--h1alffa9f.xn--p1ai/test.jpg"
      )
      testScenarios(scenarios)
    }

    "correctly support encoded symbols (avoiding double encoding)" in {
      val scenarios = Seq(
        "http://test.ru/%D1%82%D0%B5%D1%81%D1%82%20.jpg" -> "http://test.ru/%D1%82%D0%B5%D1%81%D1%82%20.jpg",
        "http://test.ru/test%20.jpg" -> "http://test.ru/test%20.jpg",
        "http://avtolubitel.su/xxx/foto/NivaChevrolet/Снежная%20Королева%20зад.jpg" -> "http://avtolubitel.su/xxx/foto/NivaChevrolet/%D0%A1%D0%BD%D0%B5%D0%B6%D0%BD%D0%B0%D1%8F%20%D0%9A%D0%BE%D1%80%D0%BE%D0%BB%D0%B5%D0%B2%D0%B0%20%D0%B7%D0%B0%D0%B4.jpg"
      )
      testScenarios(scenarios)
    }

    "ignore anchors" in {
      val scenarios = Seq(
        "http://test.ru/test.png#dsdsd" -> "http://test.ru/test.png",
        "http://test.ru/test.png#dsdsd//" -> "http://test.ru/test.png",
        "http://test.ru/test.png//#dsdsd" -> "http://test.ru/test.png"
      )
      testScenarios(scenarios)
    }

    "handle query params" in {
      val scenarios = Seq(
        "http://test.ru/test.png?dsdsd=sdsd&ssd=fdfd" -> "http://test.ru/test.png?dsdsd=sdsd&ssd=fdfd",
        "http://test.ru/test.png?dsdsd=sdsd&ssd=%20" -> "http://test.ru/test.png?dsdsd=sdsd&ssd=",
        "http://test.ru/test.png?dsdsd=sdsd&ssd= ss " -> "http://test.ru/test.png?dsdsd=sdsd&ssd=%20ss"
      )
      testScenarios(scenarios)
    }

    "handle cyrillic query params" in {
      val scenarios = Seq(
        "http://test.ru/test.png?пар1=знач1" -> "http://test.ru/test.png?%D0%BF%D0%B0%D1%801=%D0%B7%D0%BD%D0%B0%D1%871",
        "http://test.ru/test.png?%D0%BF%D0%B0%D1%801.%D0%B7%D0%BD%D0%B0%D1%871&t=p" -> "http://test.ru/test.png?%D0%BF%D0%B0%D1%801.%D0%B7%D0%BD%D0%B0%D1%871&t=p"
      )
      testScenarios(scenarios)
    }

    "old pica test" in {
      val scenarios = Seq(
        "http://a.b.ru/hello" -> "http://a.b.ru/hello",
        "http://a.b.ru/д д/hello" -> "http://a.b.ru/%D0%B4%20%D0%B4/hello",
        "http://a.b.ru/д д/hello?key=д д" -> "http://a.b.ru/%D0%B4%20%D0%B4/hello?key=%D0%B4%20%D0%B4",
        "http://2rrealty.ru/sites/default/files/03/26/2017 - 17:08/cam9.jpg" -> "http://2rrealty.ru/sites/default/files/03/26/2017%20-%2017:08/cam9.jpg",
        "http://жкленинскиегорки.рф/sites/all/themes/gorki/promo/gorki-photo2.jpg" -> "http://xn--c1adaclackcdezbs4ak.xn--p1ai/sites/all/themes/gorki/promo/gorki-photo2.jpg"
      )
      testScenarios(scenarios)
    }

    "invalid urls" in {
      testInvalid(
        Seq(
          "https:/sanray73.ru/image/cache/data/new/inzh/valfex/mufta-ppr-valfex-500x500.jpg"
        )
      )
    }

    "correctly handler plus sign" in {
      val scenarios = Seq(
        "https://pmaf.ru/content/images/25/kostum-dlya-malyarnykh-rabot-55951496639251_+096fb7a4a5.jpg" -> "https://pmaf.ru/content/images/25/kostum-dlya-malyarnykh-rabot-55951496639251_+096fb7a4a5.jpg",
        "https://test.ru?a+b=a+b" -> "https://test.ru?a+b=a+b",
        "https://test.ru?a%2Bb=a%2Bb" -> "https://test.ru?a+b=a+b",
        "https://test.ru?a b=a b" -> "https://test.ru?a%20b=a%20b"
      )
      testScenarios(scenarios)
    }

  }

  private def testScenarios(scenarios: Seq[(String, String)]): Unit = {
    ioTest {
      IO.foreach(scenarios) { case (url, expected) =>
        UrlUtils.normalize(url).map { u =>
          u shouldBe expected
        }
      }
    }
  }

  private def testInvalid(urls: Seq[String]) =
    intercept[IllegalUrl] {
      ioTest {
        IO.foreach(urls) { url =>
          UrlUtils.normalize(url)
        }
      }
    }

}
