package ru.yandex.vertis.feedprocessor.autoru.scheduler.parser

import org.apache.commons.io.IOUtils
import ru.auto.api.ApiOfferModel.Section
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.AutoruExternalOfferError
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._

import java.nio.charset.StandardCharsets.UTF_8
import scala.annotation.nowarn

/**
  * @author pnaydenov
  */
@nowarn("msg=deprecated")
class AutoruParserSpec extends WordSpecBase {
  "AutoruParser car format autoselect" should {
    val task = tasksGen(serviceInfoGen = carServiceInfoGen(Section.NEW)).next
    implicit val tc = model.TaskContext(task)

    "automatically select autoru parser" in {
      val feed = getClass.getResourceAsStream("/cars_feed_new.xml")
      val parser = new AutoruParser
      val results = parser.parse(feed).toList
      results should have size (2)
      results(0).right.get.vin.get shouldEqual "WV2ZZZ7HZJH031495"
    }

    "automatically select avito parser" in {
      val feed = getClass.getResourceAsStream("/avito_feed_example.xml")
      val parser = new AutoruParser
      val results = parser.parse(feed).toList
      results should have size (2)
      val offer = results.head.right.get
      val error = results.last.left.get
      offer.vin.get shouldEqual "1FTWR72P1LVA41777"
      error.error.getMessage should include("Не указано значение обязательного поля")
    }

    "don't fall on very short feed" in {
      val feed = IOUtils.toInputStream("<Ads><Ad><Vin>1</Vin></Ad></Ads>", UTF_8)
      val parser = new AutoruParser
      val results = parser.parse(feed).toList
      results should have size (1)
      val error = results.head.left.get
      error.asInstanceOf[AutoruExternalOfferError].vin.get shouldEqual "1"
    }

    "don't fall on empty feed" in {
      val feed1 = IOUtils.toInputStream("<Ads></Ads>", UTF_8)
      val feed2 = IOUtils.toInputStream("<data><cars></cars></data>", UTF_8)
      val parser = new AutoruParser
      parser.parse(feed1).toList shouldBe empty
      parser.parse(feed2).toList shouldBe empty
    }

    "fall on unexpected format" in {
      val feed1 = IOUtils.toInputStream("<Adddds></Adddds>", UTF_8)
      val feed2 = IOUtils.toInputStream("<data></data>", UTF_8)
      val parser = new AutoruParser
      intercept[RuntimeException] {
        parser.parse(feed1).toList
      }
      intercept[RuntimeException] {
        parser.parse(feed2).toList
      }
    }

    "don't fall on very long feed header" in {
      val header = "\n" * 2000 // должен быть больше используемого буфера
      val xml = header + "<data><cars><car><vin>1aabb</vin></car></cars></data>"
      val feed1 = IOUtils.toInputStream(xml, UTF_8)
      val parser = new AutoruParser
      val results = parser.parse(feed1).toList
      results should have size (1)
      val error = results.head.left.get
      error.asInstanceOf[AutoruExternalOfferError].vin.get shouldEqual "1AABB"
    }

    "apply autoru parser if very long feed header" in {
      val header = "\n" * 2000 // должен быть больше используемого буфера
      val feed1 = IOUtils.toInputStream(header + "<Ads><Ad><Vin>1</Vin><Ad></Ads>", UTF_8)
      val parser = new AutoruParser
      // если заполнили весь буфер, но так и не обнаружили root-тег, то по умолчанию выбирается autoru парсер
      // в результате для данного примера можно было бы ожидать успешный обработки avito парсером, но вместо этого
      // получаем ошибку формата от autoru парсера
      intercept[RuntimeException] {
        parser.parse(feed1).toList
      }
    }
  }
}
