package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import org.scalamock.scalatest.MockFactory
import ru.auto.api.ApiOfferModel.Section
import ru.yandex.vertis.feedprocessor.ScalamockCallHandlers
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.StreamEndMessage
import ru.yandex.vertis.feedprocessor.autoru.scheduler.converter.Catalog7YandexDao
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClient
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators._
import ru.yandex.vertis.feedprocessor.dao.KVClient
import ru.yandex.vertis.feedprocessor.util.{DummyOpsSupport, StreamTestBase}

import scala.concurrent.duration._

/**
  * @author pnaydenov
  */
class OptionsMapperSpec
  extends StreamTestBase
  with ScalamockCallHandlers
  with MockFactory
  with DummyOpsSupport
  with TestApplication {
  implicit val meters = new mapper.Mapper.Meters(prometheusRegistry)

  val config = environment.config.getConfig("feedprocessor.autoru")

  val kvClient = mock[KVClient]
  (kvClient.bulkGet _).expects(*).returningF(Map.empty[String, String]).anyNumberOfTimes()
  (kvClient.bulkSet _).expects(*).returningF(()).anyNumberOfTimes()
  val catalog7YandexDao = mock[Catalog7YandexDao]

  val trucksTaskGen = tasksGen(serviceInfoGen = truckServiceInfoGen())
  val carsTaskGen = tasksGen(serviceInfoGen = carServiceInfoGen(Section.USED))
  val trucksOfferGen = truckExternalOfferGen(trucksTaskGen)

  val carsOfferGen = carExternalOfferGen(carsTaskGen).map {
    _.copy(
      optionIds = None,
      extras = Some(Seq("Штатная сигнализация", "датчик ДОЖДЯ", "Неизвестная позиция")),
      mark = "my_mark",
      interiorCode = Some("my_interior_code"),
      interiorOptionCode = None,
      equipmentCodes = Some(Seq("my_equipment_code1", "my_equipment_code2")),
      equipmentOptionCodes = None
    )
  }

  "OptionsMapper" should {
    "handle trucks w/o change" in {
      val offer = trucksOfferGen.next.copy(
        optionIds = None,
        extras = Some(Seq("Аэродефлектор", "багажное отделение", "Неизвестная позиция"))
      )
      assert(offer.extras.nonEmpty)
      val unificatorClient = mock[UnificatorClient]
      val mapper = new OptionsMapper(unificatorClient, config, kvClient, catalog7YandexDao)
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(5)
      pub.sendNext(Messages.OfferMessage(offer))
      pub.sendNext(StreamEndMessage(offer.taskContext))
      val offerResponse = sub.expectNextPF {
        case Messages.OfferMessage(o) => o
      }
      sub.expectNext(StreamEndMessage(offer.taskContext))
      sub.expectNoMessage(100.millis)
      offerResponse shouldEqual offer
    }

    "unify cars options" in {
      val offer = carsOfferGen.next
      (kvClient.bulkGet _).expects(*).returningF(Map.empty[String, String]).anyNumberOfTimes()
      (kvClient.bulkSet _).expects(*).returningF(()).anyNumberOfTimes()
      val unificatorClient = mock[UnificatorClient]
      (unificatorClient.unifyOptions _)
        .expects(*)
        .returningF(Map("Штатная сигнализация" -> "alarm", "датчик ДОЖДЯ" -> "rain-sensor"))

      val catalog7YandexDao = mock[Catalog7YandexDao]
      (catalog7YandexDao.findOptionCodeByInteriorCode _)
        .expects("my_interior_code", "my_mark")
        .returning(Some("verba_interior_code"))
      val equipmentResult =
        Map("my_equipment_code1" -> "verba_equipment_code1", "my_equipment_code2" -> "verba_equipment_code2")
      (catalog7YandexDao.findOptionCodesByEquipmentCodes _)
        .expects(
          Seq("my_equipment_code1", "my_equipment_code2"),
          "my_mark"
        )
        .returning(equipmentResult)

      val mapper = new OptionsMapper(unificatorClient, config, kvClient, catalog7YandexDao)
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(5)
      pub.sendNext(Messages.OfferMessage(offer))
      pub.sendNext(StreamEndMessage(offer.taskContext))
      val offerResponse = sub.expectNextPF {
        case Messages.OfferMessage(o: CarExternalOffer) => o
      }
      sub.expectNext(StreamEndMessage(offer.taskContext))
      sub.expectNoMessage(100.millis)
      offerResponse.extras shouldEqual offer.extras
      offerResponse.interiorCode shouldEqual offer.interiorCode
      offerResponse.equipmentCodes shouldEqual offer.equipmentCodes
      offerResponse.optionIds.get.toSet shouldEqual Set("alarm", "rain-sensor")
      offerResponse.interiorOptionCode.get shouldEqual "verba_interior_code"
      offerResponse.equipmentOptionCodes.get shouldEqual CarExternalOffer.EquipmentCodes(equipmentResult)
    }

    "handle cars w/o options" in {
      val offer = carsOfferGen.next.copy(
        optionIds = None,
        extras = None,
        interiorCode = None,
        interiorOptionCode = None,
        equipmentCodes = None,
        equipmentOptionCodes = None
      )

      val mapper = new OptionsMapper(mock[UnificatorClient], config, kvClient, mock[Catalog7YandexDao])
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(5)
      pub.sendNext(Messages.OfferMessage(offer))
      pub.sendNext(StreamEndMessage(offer.taskContext))
      val offerResponse = sub.expectNextPF {
        case Messages.OfferMessage(o: CarExternalOffer) => o
      }
      sub.expectNext(StreamEndMessage(offer.taskContext))
      sub.expectNoMessage(100.millis)
      offerResponse.extras shouldBe empty
      offerResponse.optionIds shouldBe empty
      offerResponse.interiorCode shouldBe empty
      offerResponse.interiorOptionCode shouldBe empty
      offerResponse.equipmentCodes shouldBe empty
      offerResponse.equipmentOptionCodes shouldBe empty
    }

    "correctly cache car options" in {
      val kvClient = mock[KVClient]
      (kvClient.bulkGet _)
        .expects(
          Seq("options/Штатная сигнализация", "options/датчик ДОЖДЯ", "options/Неизвестная позиция")
        )
        .returningF(Map("options/датчик ДОЖДЯ" -> "rain-sensor"))
      (kvClient.bulkGet _)
        .expects(Seq("interior/my_mark/my_interior_code"))
        .returningF(Map.empty[String, String])
      (kvClient.bulkGet _)
        .expects(Seq("equipment/my_mark/my_equipment_code1", "equipment/my_mark/my_equipment_code2"))
        .returningF(
          Map("equipment/my_mark/my_equipment_code1" -> "my_equipment_code1/verba_equipment_code1")
        )
      (kvClient.bulkSet _).expects(*).returningF(()).anyNumberOfTimes()

      val offer = carsOfferGen.next
      val unificatorClient = mock[UnificatorClient]
      (unificatorClient.unifyOptions _)
        .expects(*)
        .returningF(Map("Штатная сигнализация" -> "alarm", "датчик ДОЖДЯ" -> "rain-sensor"))

      val catalog7YandexDao = mock[Catalog7YandexDao]
      (catalog7YandexDao.findOptionCodeByInteriorCode _)
        .expects("my_interior_code", "my_mark")
        .returning(Some("verba_interior_code"))
      (catalog7YandexDao.findOptionCodesByEquipmentCodes _)
        .expects(Seq("my_equipment_code2"), "my_mark")
        .returning(Map("my_equipment_code2" -> "verba_equipment_code2"))

      val mapper = new OptionsMapper(unificatorClient, config, kvClient, catalog7YandexDao)
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(5)
      pub.sendNext(Messages.OfferMessage(offer))
      pub.sendNext(StreamEndMessage(offer.taskContext))
      val offerResponse = sub.expectNextPF {
        case Messages.OfferMessage(o: CarExternalOffer) => o
      }
      sub.expectNext(StreamEndMessage(offer.taskContext))
      sub.expectNoMessage(100.millis)
      offerResponse.optionIds.get.toSet shouldEqual Set("alarm", "rain-sensor")
      offerResponse.interiorOptionCode.get shouldEqual "verba_interior_code"
      offerResponse.equipmentOptionCodes.get.vendorCodeToOptionId shouldEqual
        Map("my_equipment_code1" -> "verba_equipment_code1", "my_equipment_code2" -> "verba_equipment_code2")
    }

    "overcome cache server unavailability" in {
      val kvClient = mock[KVClient]
      (kvClient.bulkGet _).expects(*).throwingF(new RuntimeException("Service unavailable")).anyNumberOfTimes()
      (kvClient.bulkSet _).expects(*).throwingF(new RuntimeException("Service unavailable")).anyNumberOfTimes()

      val offer = carsOfferGen.next
      val unificatorClient = mock[UnificatorClient]
      (unificatorClient.unifyOptions _)
        .expects(*)
        .returningF(Map("Штатная сигнализация" -> "alarm", "датчик ДОЖДЯ" -> "rain-sensor"))

      val catalog7YandexDao = mock[Catalog7YandexDao]
      (catalog7YandexDao.findOptionCodeByInteriorCode _)
        .expects("my_interior_code", "my_mark")
        .returning(Some("verba_interior_code"))
      (catalog7YandexDao.findOptionCodesByEquipmentCodes _)
        .expects(Seq("my_equipment_code1", "my_equipment_code2"), "my_mark")
        .returning(
          Map("my_equipment_code1" -> "verba_equipment_code1", "my_equipment_code2" -> "verba_equipment_code2")
        )

      val mapper = new OptionsMapper(unificatorClient, config, kvClient, catalog7YandexDao)
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(5)
      pub.sendNext(Messages.OfferMessage(offer))
      pub.sendNext(StreamEndMessage(offer.taskContext))
      val offerResponse = sub.expectNextPF {
        case Messages.OfferMessage(o: CarExternalOffer) => o
      }
      sub.expectNext(StreamEndMessage(offer.taskContext))
      sub.expectNoMessage(100.millis)
      offerResponse.optionIds.get.toSet shouldEqual Set("alarm", "rain-sensor")
      offerResponse.interiorOptionCode.get shouldEqual "verba_interior_code"
      offerResponse.equipmentOptionCodes.get.vendorCodeToOptionId shouldEqual
        Map("my_equipment_code1" -> "verba_equipment_code1", "my_equipment_code2" -> "verba_equipment_code2")
    }
  }
}
