package ru.yandex.vertis.feedprocessor.autoru.scheduler.parser.avito

import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model
import ru.yandex.vertis.feedprocessor.autoru.model.TaskContext
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.{CarExternalOffer, Contact, Currency}
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._

import scala.annotation.nowarn

/**
  * @author pnaydenov
  */
@nowarn("msg=deprecated")
class AvitoCarParserSpec extends WordSpecBase {

  val task = newTasksGen.next

  "AvitoCarParser" should {
    "parse feed example" in {
      implicit val tc = model.TaskContext(task)
      val feed = getClass.getResourceAsStream("/avito_feed_example.xml")
      val parser = new AvitoCarParser(feed)
      val result = parser.toList
      result should have size (2)
      val offer = result.head.right.get
      val error = result.last.left.get
      error.error.getMessage should include("Не указано значение обязательного поля")

      offer.position shouldEqual 0
      offer.category shouldEqual Category.CARS
      offer.section shouldEqual Section.NEW
      offer.uniqueId shouldEqual Some("xjfdge4735404")
      offer.mark shouldEqual "Toyota"
      offer.model shouldEqual "Highlander"
      offer.modification shouldEqual CarExternalOffer.EngineInfo(Some(3500), 249, "Бензин", "AT", "Полный", None)
      offer.complectation shouldEqual None
      offer.bodyType shouldEqual "Внедорожник"
      offer.wheel shouldEqual "Левый"
      offer.color shouldEqual "Серый"
      offer.metallic shouldEqual None
      offer.availability shouldEqual "В наличии"
      offer.custom shouldEqual "Растаможен"
      offer.state shouldEqual Some("Отличное")
      offer.ownersNumber shouldEqual Some("Один владелец")
      offer.run shouldEqual Some(53000)
      offer.year shouldEqual 2013
      offer.registryYear shouldEqual None
      offer.price shouldEqual 250000
      offer.creditDiscount shouldEqual None
      offer.tradeinDiscount shouldEqual None
      offer.insuranceDiscount shouldEqual None
      offer.maxDiscount shouldEqual None
      offer.currency.currency shouldEqual Currency
        .from(
          "RUR"
        )
        .currency // для совместимости с нашим текущим форматом
      offer.vin shouldEqual Some("1FTWR72P1LVA41777")
      offer.description shouldEqual Some(
        """Автомобиль покупался новым в мае 2013 года, все ТО пройдены по регламенту в Тойота Центр Петровка. Машина на гарантии до мая 2016. Отличное внешнее и техническое состояние.
          |
          |            Возможен обмен на Ваш авто на выгодных условиях. Гарантия юридической чистоты.""".stripMargin
      )
      offer.extras.get.toSet shouldEqual Set(
        "Усилитель руля Электро-",
        "Управление климатом Климат-контроль многозонный",
        "Управление климатом Атермальное остекление",
        "Управление климатом Управление на руле",
        "Салон Кожа",
        "Салон Люк",
        "Салон Кожаный руль",
        "Обогрев Передних сидений",
        "Обогрев Зеркал",
        "Обогрев Руля",
        "Электростеклоподъемники Только передние",
        "Электропривод Передних сидений",
        "Электропривод Зеркал",
        "Электропривод Складывания зеркал",
        "Память настроек Передних сидений",
        "Память настроек Зеркал",
        "Помощь при вождении Датчик дождя",
        "Помощь при вождении Датчик света",
        "Помощь при вождении Парктроник задний",
        "Противоугонная система Сигнализация",
        "Противоугонная система Иммобилайзер",
        "Подушки безопасности Фронтальные",
        "Подушки безопасности Боковые передние",
        "Активная безопасность Антиблокировка тормозов",
        "Активная безопасность Антипробуксовка",
        "Активная безопасность Курсовая устойчивость",
        "Мультимедиа и навигация CD/DVD/Blu-ray",
        "Мультимедиа и навигация MP3",
        "Мультимедиа и навигация Управление на руле",
        "Аудиосистема 8+ колонок",
        "Аудиосистема Сабвуфер",
        "Фары Светодиодные",
        "Фары Омыватели фар",
        "Фары Противотуманные",
        "Шины и диски 19",
        "Шины и диски Зимние шины в комплекте",
        "Данные о ТО Есть сервисная книжка",
        "Данные о ТО На гарантии"
      )
      offer.images shouldEqual Seq(
        "http://img.test.ru/8F7B-4A4F3A0F2BA1.jpg",
        "http://img.test.ru/8F7B-4A4F3A0F2XA3.jpg?foo=bar",
        "http://img.test.ru/8F7B-4A4F3A0F2XA3.jpg?foo=baz&"
      )
      offer.video shouldEqual Some("http://www.youtube.com/watch?v=YKmDXNrDdBI")
      offer.poiId shouldEqual Some("Москва")
      offer.saleServices shouldEqual Seq()
      offer.serviceAutoApply shouldEqual None
      offer.contactInfo shouldEqual Seq(Contact(Some("Иван Петров-Водкин"), "+7 916 683-78-22"))
      offer.warrantyExpire shouldEqual None
      offer.pts shouldEqual None
      offer.sts shouldEqual Some("77РМ193777")
      offer.armored shouldEqual None
      offer.modificationCode shouldEqual None
      offer.colorCode shouldEqual None
      offer.interiorCode shouldEqual None
      offer.equipmentCodes shouldEqual None
      offer.action shouldEqual None
      offer.exchange shouldEqual None
      offer.discountPrice shouldEqual None
      offer.taskContext shouldEqual tc
      offer.badges shouldEqual Nil
      offer.panoramas shouldBe empty
      offer.doorsCount shouldEqual Some(5)
    }

    "parse description with CDATA" in {
      implicit val tc = model.TaskContext(task)
      val feed = getClass.getResourceAsStream("/avito_feed_for_tests.xml")
      val parser = new AvitoCarParser(feed)
      val result = parser.toList
      result should have size (2)
      val offer = result.head.right.get
      offer.description.get shouldEqual
        """
          |<p>Автомобиль не требует вложений и полностью готов к продаже.
          |Произведена предпродажная диагностика двигателя и ходовой,
          |в результате которой отклонений не выявлено.
          |<br />
          |Комфортабельный и надежный автомобиль в своем классе.</p>
          |<p><strong>Так же вы можете приобрести данный автомобиль, воспользовавшись услугами:</strong></p>
          |<ul>
          |<li>обмен авто по системе Trade-In,
          |<li>программы <em>автокредитования</em>.
          |</ul>
        """.stripMargin.trim.replace("<", "&lt;").replace(">", "&gt;")
    }

    "create offer error for unexpected Category tag" in {
      implicit val tc = model.TaskContext(task)
      val feed = getClass.getResourceAsStream("/avito_feed_for_tests.xml")
      val parser = new AvitoCarParser(feed)
      val result = parser.toList
      result should have size (2)
      val error = result.last.left.get
      error.error.getMessage should include("Неподдерживаемая категория")
    }

    "not allow VIN duplicate" in {
      implicit val tc = model.TaskContext(task)
      val feed = getClass.getResourceAsStream("/avito_vin_duplicate.xml")
      val parser = new AvitoCarParser(feed)
      val result = parser.toList
      result should have size (3)
      result(0).right.get.vin.get shouldEqual "1FTWR72P1LVA41777"
      result(1).left.get.error.getMessage should include("Повторяющийся уникальный идентификатор")
      result(2).right.get.vin.get shouldEqual "2FTWR72P1LVA41777"

      // uniqueId не проверяется на корректность (и в дальнейшем игнорируется), если известен VIN
      result(0).right.get.uniqueId.get shouldEqual result(2).right.get.uniqueId.get
    }

    "not allow ID duplicate" in {
      implicit val tc = model.TaskContext(task)
      val feed = getClass.getResourceAsStream("/avito_id_duplicate.xml")
      val parser = new AvitoCarParser(feed)
      val result = parser.toList
      result should have size (3)
      result(0).right.get.uniqueId.get shouldEqual "111"
      result(1).left.get.error.getMessage should include("Повторяющийся уникальный идентификатор")
      result(2).right.get.uniqueId.get shouldEqual "222"
    }

    "don't touch cache breaker if unnecessary" in {
      implicit val tc = TaskContext(
        task.copy(serviceInfo = task.serviceInfo.copy(autoru = task.serviceInfo.autoru.copy(imagesCacheBreaker = None)))
      )
      val feed = getClass.getResourceAsStream("/avito_feed_example.xml")
      val parser = new AvitoCarParser(feed)
      val result = parser.toList
      val offer = result.head.right.get
      offer.images.toSet shouldEqual
        Set(
          "http://img.test.ru/8F7B-4A4F3A0F2BA1.jpg",
          "http://img.test.ru/8F7B-4A4F3A0F2XA3.jpg?foo=bar",
          "http://img.test.ru/8F7B-4A4F3A0F2XA3.jpg?foo=baz&"
        )
    }

    "add cache breaker if required" in {
      implicit val tc = TaskContext(
        task.copy(serviceInfo =
          task.serviceInfo.copy(autoru = task.serviceInfo.autoru.copy(imagesCacheBreaker = Some(7)))
        )
      )
      val feed = getClass.getResourceAsStream("/avito_feed_example.xml")
      val parser = new AvitoCarParser(feed)
      val result = parser.toList
      val offer = result.head.right.get
      offer.images.toSet shouldEqual
        Set(
          "http://img.test.ru/8F7B-4A4F3A0F2BA1.jpg?autorucb=7",
          "http://img.test.ru/8F7B-4A4F3A0F2XA3.jpg?foo=bar&autorucb=7",
          "http://img.test.ru/8F7B-4A4F3A0F2XA3.jpg?foo=baz&autorucb=7"
        )
    }
  }
}
