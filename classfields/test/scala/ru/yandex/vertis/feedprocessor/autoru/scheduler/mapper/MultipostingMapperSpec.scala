package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import org.scalacheck.Gen
import org.scalatest.OptionValues
import ru.auto.api.ApiOfferModel.Section
import ru.yandex.vertis.feedprocessor.PropSpecBase
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.dao.MainOffice7Dao
import ru.yandex.vertis.feedprocessor.autoru.model.Generators.{carServiceInfoGen, tasksGen, truckServiceInfoGen, _}
import ru.yandex.vertis.feedprocessor.autoru.model.{Messages, OfferNotice}
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.OfferMessage
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.AutoruExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators.{
  carExternalOfferGen,
  truckExternalOfferGen
}
import ru.yandex.vertis.feedprocessor.util.{DummyOpsSupport, StreamTestBase}
import ru.yandex.vertis.mockito.MockitoSupport

class MultipostingMapperSpec
  extends StreamTestBase
  with MockitoSupport
  with DummyOpsSupport
  with TestApplication
  with OptionValues
  with PropSpecBase {
  implicit val meters = new mapper.Mapper.Meters(prometheusRegistry)

  val config = environment.config.getConfig("feedprocessor.autoru")

  val trucksTaskGen = tasksGen(serviceInfoGen = truckServiceInfoGen())
  val carsTaskGen = tasksGen(serviceInfoGen = carServiceInfoGen(Section.NEW))
  val trucksOfferGen = truckExternalOfferGen(trucksTaskGen)
  val carsOfferGen = carExternalOfferGen(carsTaskGen)

  "MultipostingMapper" should {
    "handle cars" in {
      forAll(carsOfferGen, Gen.option(Gen.oneOf(false, true))) { (offer, flag) =>
        val mainOffice = mock[MainOffice7Dao]
        when(mainOffice.isMultipostingEnabled(any())).thenReturn(flag)

        val mapper = new MultipostingMapper(mainOffice, config)
        val (pub, sub) = createPubSub(mapper.flow())

        sub.request(1)
        pub.sendNext(Messages.OfferMessage(offer.copy(multipostingEnabled = None)))

        val offerResponse = sub.expectNextPF {
          case OfferMessage(offer: AutoruExternalOffer) => offer
        }

        offerResponse.multipostingEnabled shouldBe flag
      }
    }

    "handle trucks" in {
      forAll(trucksOfferGen, Gen.option(Gen.oneOf(false, true))) { (offer, flag) =>
        val mainOffice = mock[MainOffice7Dao]
        when(mainOffice.isMultipostingEnabled(any())).thenReturn(flag)

        val mapper = new MultipostingMapper(mainOffice, config)
        val (pub, sub) = createPubSub(mapper.flow())

        sub.request(1)
        pub.sendNext(Messages.OfferMessage(offer))

        val offerResponse = sub.expectNextPF {
          case OfferMessage(offer: AutoruExternalOffer) => offer
        }

        offerResponse.multipostingEnabled shouldBe flag
      }
    }

    "recover from error" in {
      val offer = carsOfferGen.next
      val mainOffice = mock[MainOffice7Dao]
      when(mainOffice.isMultipostingEnabled(any())).thenThrow(new RuntimeException("Some unexpected error"))

      val mapper = new MultipostingMapper(mainOffice, config)
      val (pub, sub) = createPubSub(mapper.flow())

      sub.request(1)
      pub.sendNext(Messages.OfferMessage(offer.copy(multipostingEnabled = None)))

      val offerResponse = sub.expectNextPF {
        case OfferMessage(offer: AutoruExternalOffer) => offer
      }

      offerResponse.multipostingEnabled shouldBe None
      offerResponse.notices shouldBe List(
        OfferNotice(
          "Не удалось получить флаг включенности мультипостинга",
          "multiposting:multiposting_enabled",
          "None",
          "multiposting"
        )
      )
    }
  }
}
