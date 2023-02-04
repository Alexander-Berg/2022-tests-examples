package ru.yandex.vertis.feedprocessor.autoru.scheduler.parser

import java.io.InputStream
import java.time.{LocalTime, YearMonth}
import java.nio.charset.StandardCharsets.UTF_8
import org.apache.commons.io.IOUtils
import org.scalacheck.Gen
import org.scalatest.Inside
import ru.auto.api.ApiOfferModel.Section
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model
import ru.yandex.vertis.feedprocessor.autoru.model.{ExternalOfferError, OfferError, ServiceInfo, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper.CarsUnificator
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer.ModificationString
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.{
  CarExternalOffer,
  Contact,
  Currency,
  Panoramas,
  ServiceAutoApply
}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.parser.car.CarParser
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.ServiceInfo
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.parser.Parser
import ru.yandex.vertis.feedprocessor.services.geocoder.GeocodeResult

import scala.annotation.nowarn

/**
  * @author pnaydenov
  */
@nowarn("msg=deprecated")
class AutoruCarParserSpec extends WordSpecBase with Inside {

  implicit val taskContextUsed: TaskContext =
    model.TaskContext(tasksGen(serviceInfoGen = generateServiceInfo(Section.USED)).next)
  val taskContextNew = model.TaskContext(tasksGen(serviceInfoGen = generateServiceInfo(Section.NEW)).next)

  val parser: Parser[CarExternalOffer] = new Parser[CarExternalOffer] {

    override def parse(
        feed: InputStream
      )(implicit tc: TaskContext): Iterator[Either[ExternalOfferError, CarExternalOffer]] =
      new CarParser(feed)(tc)
  }

  "AutoruCarParser" should {
    "parse feed of new" in {
      val feed = getClass.getResourceAsStream("/cars_feed_new.xml")
      val offersAll = parser.parse(feed)(taskContextNew).toList
      val offers = offersAll.collect { case Right(offer) => offer }

      offersAll.size shouldEqual 2
      offers should have size (2)

      offers(0).mark shouldEqual "Volkswagen"
      offers(0).model shouldEqual "Caravelle, T6"
      offers(0).modification shouldEqual ModificationString("Long 2.0d MT (102 л.с.)")
      offers(0).bodyType shouldEqual "Минивэн"
      offers(0).wheel shouldEqual "Левый"
      offers(0).color shouldEqual "Черный"
      offers(0).metallic.get shouldEqual "Нет"
      offers(0).availability shouldEqual "В наличии"
      offers(0).custom shouldEqual "Растаможен"
      offers(0).ownersNumber shouldEqual Some("Не было владельцев")
      offers(0).year shouldEqual 2017
      offers(0).registryYear shouldEqual Some(2017)
      offers(0).vin shouldEqual Some("WV2ZZZ7HZJH031495")
      offers(0).uniqueId shouldEqual Some("e92701503c0ae1b5d51adf8071c932f7")
      offers(0).price shouldEqual 2404144
      offers(0).originalPrice shouldEqual Some(2504144)
      offers(0).images should have size 1
      offers(0).geocoding shouldEqual
        Some(
          Map(
            "address from location tag" -> GeocodeResult(
              id = 213,
              lat = 152.43,
              lng = 444.169,
              address = "address from location tag",
              city = Some("Gotham")
            )
          )
        )
      offers(0).poiId shouldEqual Some("address from location tag")
      offers(0).badges shouldEqual Seq("Парктроник", "Коврики в подарок", "КАСКО 3% | КРЕДИТ 3%")
      offers(0).equipmentCodes shouldBe empty
      offers(0).onlineViewAvailable shouldBe Some(true)
      offers(0).bookingAllowed shouldBe Some(true)
      offers(0).avitoSaleServices shouldEqual List("first", "second")
      offers(0).dromSaleServices shouldEqual List("third", "fourth")
      offers(0).classifieds shouldEqual Some(List("avito", "drom"))

      offers(1).avitoSaleServices shouldEqual Nil
      offers(1).dromSaleServices shouldEqual Nil
    }

    "parse feed of used" in {
      val feed = getClass.getResourceAsStream("/cars_feed_used.xml")
      val offersAll = parser.parse(feed).toList
      val offers = offersAll.collect { case Right(offer) => offer }
      offersAll should have size (3)
      offers should have size (3)
      offers(0).vin should contain("WF0UXXGAJU7J30004")
      offers(0).year shouldEqual 2009
      offers(0).registryYear shouldEqual Some(2009)
      offers(0).images should have size 3
      offers(0).contactInfo.toList shouldEqual List(
        Contact(Some("Менеджер"), "+74997020609", Some(LocalTime.of(9, 0)), Some(LocalTime.of(20, 0)))
      )
      offers(0).warrantyExpire shouldEqual Some(YearMonth.of(2026, 5))
      offers(0).serviceAutoApply shouldEqual None
      offers(0).complectation shouldEqual Some(Right("Core (1.4 MT)"))
      offers(0).creditDiscount shouldEqual Some(220000)
      offers(0).insuranceDiscount shouldEqual Some(215000)
      offers(0).maxDiscount shouldEqual Some(425000)
      offers(0).tradeinDiscount shouldEqual Some(210000)
      offers(0).uniqueId shouldEqual Some("294698")
      offers(0).geocoding shouldEqual
        Some(
          Map(
            "address from location tag" -> GeocodeResult(
              id = 213,
              lat = 152.43,
              lng = 444.169,
              address = "address from location tag",
              city = Some("Gotham")
            )
          )
        )
      offers(0).poiId shouldEqual Some("г.Москва, м,Юго-Западная, ул.Академика Анохина, д.6, корп.6")
      offers(0).panoramas shouldEqual Some(
        Panoramas(
          Some("spins.spincar.com/autopragayug/01053828"),
          Some("https://st.yandex-team.ru/AUTORUSALES-5573/attachments/3548380?")
        )
      )
      offers(0).equipmentCodes.get shouldEqual Seq("foo", "bar", "baz")
      offers(0).avitoSaleServices shouldEqual List("first", "second")
      offers(0).dromSaleServices shouldEqual List("third", "fourth")
      offers(0).classifieds shouldEqual Some(Nil)
      offers(0).pledgeNumber shouldBe empty
      offers(0).notRegisteredInRussia shouldBe empty

      offers(1).mark shouldEqual "ВАЗ (Lada)"
      offers(1).model shouldEqual "Largus, I"
      offers(1).modification shouldEqual ModificationString("1.6 MT (102 л.с.)")
      offers(1).complectation shouldEqual None
      offers(1).bodyType shouldEqual "Универсал 5 дв."
      offers(1).wheel shouldEqual "Левый"
      offers(1).color shouldEqual "Бежевый"
      offers(1).metallic.get shouldEqual "Да"
      offers(1).availability shouldEqual "В наличии"
      offers(1).custom shouldEqual "Растаможен"
      offers(1).state shouldEqual Some("Не требует ремонта")
      offers(1).ownersNumber shouldEqual Some("Три владельца")
      offers(1).run shouldEqual Some(51000)
      offers(1).year shouldEqual 2016
      offers(1).registryYear shouldEqual Some(2016)
      offers(1).price shouldEqual 465000
      offers(1).originalPrice shouldBe None
      offers(1).currency.currency shouldEqual Currency.from("RUR").currency
      offers(1).vin should contain("XTARS0Y5LE0805533")
      offers(1).description shouldBe empty
      offers(1).extras shouldBe empty
      offers(1).images should have size 3
      offers(1).video shouldBe empty
      offers(1).poiId shouldEqual Some("г.Москва, м,Юго-Западная, ул.Академика Анохина, д.6, корп.6")
      offers(1).saleServices shouldBe empty
      offers(1).contactInfo shouldBe empty
      offers(1).warrantyExpire shouldBe empty
      offers(1).serviceAutoApply shouldEqual None
      offers(1).uniqueId shouldEqual Some("294700")
      offers(1).panoramas shouldEqual None
      offers(1).equipmentCodes shouldBe empty
      offers(1).classifieds shouldBe None
      offers(1).pledgeNumber shouldBe empty
      offers(1).notRegisteredInRussia shouldBe empty

      offers(2).vin should contain("JMZCR19F280223078")
      offers(2).images should have size 3
      offers(2).serviceAutoApply shouldEqual None
      offers(2).uniqueId shouldEqual Some("294701")
      offers(2).panoramas shouldEqual Some(
        Panoramas(None, Some("https://st.yandex-team.ru/AUTORUSALES-5573/attachments/3548380?"))
      )
      offers(2).pledgeNumber should contain("0000-111-222222-333")
      offers(2).notRegisteredInRussia should contain(true)
    }

    "parse empty feed" in {
      val emptyFeed = "<data><cars></cars></data>"
      parser.parse(IOUtils.toInputStream(emptyFeed, UTF_8)).toList shouldBe empty
    }

    "parse feed with incomplete entry" in {
      val emptyFeed = "<data><cars><car></car></cars></data>"
      val offers = parser.parse(IOUtils.toInputStream(emptyFeed, UTF_8)).toList
      offers should have size 1
      // allow emission of ExternalOfferError (in case of unpredictable parsing errors)
      // or subclass of ExternalOffer (if you want to save other useful data: for example, check another fields)
      if (offers.exists(_.isRight)) {
        val rightOffers = offers.collect { case Right(o) => o }
        rightOffers should have size 1
        intercept[OfferError] {
          rightOffers.head.checkFields()
        }
      }
    }

    "fail with exceeded limit (40) of images" in {
      val feed = getClass.getResourceAsStream("/cars_feed_new_exceeded_images.xml")
      val offers = parser.parse(feed).toList

      offers should have size 1

      val offer = offers.head
      if (offer.isRight) {
        intercept[OfferError] {
          offer.right.get.checkFields()
        }
      }
    }

    "fail on incomplete feed" in {
      val incomplete = "<random></random>"
      intercept[Exception] {
        parser.parse(IOUtils.toInputStream(incomplete, UTF_8)).toList
      }
    }

    "fail on feed with corrupted entry" in {
      val corruptedXml = "<data><cars><car></car><car><foo>...<bar></car></cars></data>"
      val iterator = parser.parse(IOUtils.toInputStream(corruptedXml, UTF_8))
      assert(iterator.hasNext)
      val firstEntry = iterator.next()
      info("Handle entry with valid xml as ExternalOfferError")
      assert(firstEntry.isLeft)
      intercept[Exception] {
        iterator.hasNext
        iterator.next()
      }
    }

    "fail on wrong feed" in {
      val unexpectedXml = "<undefined-tag></undefined-tag>"
      val wrongXml = "{}"
      intercept[Exception] {
        parser.parse(IOUtils.toInputStream(unexpectedXml, UTF_8)).toList
      }
      intercept[Exception] {
        parser.parse(IOUtils.toInputStream(wrongXml, UTF_8)).toList
      }
    }

    "don't fail on incomplete entry or offer without vin" in {
      val feed = getClass.getResourceAsStream("/cars_corrupted_feed.xml")
      val offers = parser.parse(feed).toList
      offers should have size (3)
      val offer1 :: offer2 :: offer3 :: Nil = offers
      inside(offers) {
        case Right(offer1) :: Left(offer2) :: Right(offer3) :: Nil =>
          offer1.position shouldEqual 0

          offer2.position shouldEqual 1

          offer3.vin should contain("JMZCR19F280223078")
          offer3.checkFields()
      }
    }

    "don't fail on unescaped ampersand" in {
      val feed = getClass.getResourceAsStream("/feed_with_ampersand.xml")
      val offersAll = parser.parse(feed)(taskContextNew).toList
      val offers = offersAll.collect { case Right(offer) => offer }

      offersAll.size shouldEqual 3
      offers should have size (3)

      offers(0).description shouldEqual Some("«Press & Drive»")
      offers(1).description shouldEqual Some("«Press & Drive»")
      offers(2).description shouldEqual Some("&amp ' &go")
    }

    "return notices" in {
      val feed = getClass.getResourceAsStream("/feed_with_notices.xml")
      val offersAll = parser.parse(feed)(taskContextNew).toList
      val offers = offersAll.collect { case Right(offer) => offer }

      assert(offers.size == offersAll.size)

      assert(offers.head.notices.size == 2)
      assert(offers.head.notices.head.columnName == "contact.time")
      assert(offers.head.notices(1).columnName == "contact.phone")

      assert(offers(1).notices.isEmpty)

      assert(offers(2).notices.size == 2)
      assert(offers(2).notices(0).columnName == "warranty_expire")
      assert(offers(2).notices(1).columnName == "pledge_number")
    }

    "prefer tech_param as modification" in {
      val feed = getClass.getResourceAsStream("/modification_variants_feed.xml")
      val offersAll = parser.parse(feed)(taskContextNew).toList
      val offer = offersAll(1).right.get
      offer.modification shouldEqual CarExternalOffer.TechParam(20071438L)
    }

    "allow empty mark/model tags for offer with tech_param_id" in {
      val feed = getClass.getResourceAsStream("/modification_variants_feed.xml")
      val offersAll = parser.parse(feed)(taskContextNew).toList
      val offer = offersAll(1).right.get
      offer.mark shouldBe empty
      offer.model shouldBe empty
    }

    "prefer complete set of separate tags as modification" in {
      val feed = getClass.getResourceAsStream("/modification_variants_feed.xml")
      val offersAll = parser.parse(feed)(taskContextNew).toList
      val offer = offersAll(2).right.get
      offer.modification shouldEqual
        CarExternalOffer.EngineInfo(Some(3000), 310, "бензин", "AT", "полный", Some("Long"))
    }

    "prefer modification_id if another tags not assigned" in {
      val feed = getClass.getResourceAsStream("/modification_variants_feed.xml")
      val offersAll = parser.parse(feed)(taskContextNew).toList
      val offer = offersAll(0).right.get
      offer.modification shouldEqual CarExternalOffer.ModificationString("Long 3.0 AT (310 л.с.) 4WD")
    }

    "error on non electro car w/o engine volume" in {
      val feed = getClass.getResourceAsStream("/modification_variants_feed.xml")
      val offersAll = parser.parse(feed)(taskContextNew).toList
      val offer = offersAll(3).left.get
      offer.error.getMessage shouldEqual "Укажите объем двигателя"
    }

    "allow electro car w/o engine volume" in {
      val feed = getClass.getResourceAsStream("/modification_variants_feed.xml")
      val offersAll = parser.parse(feed)(taskContextNew).toList
      val offer = offersAll(4).right.get
      offer.modification shouldEqual CarExternalOffer.EngineInfo(
        None,
        CarsUnificator.toHorsePower("126 кВт"),
        "электро",
        "AT",
        "передний",
        None
      )
    }

    "don't touch cache breaker if unnecessary" in {
      val feed = getClass.getResourceAsStream("/cars_feed_new.xml")
      val offer = parser.parse(feed)(taskContextNew).toList.head.right.get
      offer.images.toSet shouldEqual Set("http://aaa24.ru/files/ad/podolskvw-avtorusskomtr/789553_15082537910.jpg")
    }

    "add cache breaker if required" in {
      implicit val tc: TaskContext = model.TaskContext(
        tasksGen(serviceInfoGen =
          serviceInfoGen(
            categoryGen = Gen.const(15),
            sectionGen = Gen.const(Section.NEW.getNumber),
            imagesCacheBreaker = Some(7)
          )
        ).next
      )
      val feed = getClass.getResourceAsStream("/cars_feed_new.xml")
      val offer = parser.parse(feed)(tc).toList.head.right.get
      offer.images.toSet shouldEqual
        Set("http://aaa24.ru/files/ad/podolskvw-avtorusskomtr/789553_15082537910.jpg?autorucb=7")
    }

    "parse service_auto_apply" in {
      val feed = getClass.getResourceAsStream("/service_auto_apply_variants.xml")
      val offer = parser.parse(feed)(taskContextNew).toSeq.apply(0).right.get
      offer.serviceAutoApply.get shouldEqual
        ServiceAutoApply("off", Seq(1, 4, 6), Some(LocalTime.of(12, 5, 12)), Some(6))
    }

    "handle service_auto_apply errors" in {
      val feed = getClass.getResourceAsStream("/service_auto_apply_variants.xml")
      val offersWithWrongServiceAutoApply = parser.parse(feed)(taskContextNew).toList.drop(3)
      assert(offersWithWrongServiceAutoApply.nonEmpty)
      for (offer <- offersWithWrongServiceAutoApply) {
        offer.right.get.serviceAutoApply shouldBe empty
      }
    }

    "don't require another fields if service_auto_apply off" in {
      val feed = getClass.getResourceAsStream("/service_auto_apply_variants.xml")
      val offersWIthSwitchOff = parser.parse(feed)(taskContextNew).toList.take(3).tail
      assert(offersWIthSwitchOff.nonEmpty)
      for (offer <- offersWIthSwitchOff) {
        offer.right.get.serviceAutoApply.get shouldEqual ServiceAutoApply("off", Nil, None, None)
      }
    }
  }

  private def generateServiceInfo(section: Section): Gen[ServiceInfo] =
    serviceInfoGen(categoryGen = Gen.const(15), sectionGen = Gen.const(section.getNumber))
}
