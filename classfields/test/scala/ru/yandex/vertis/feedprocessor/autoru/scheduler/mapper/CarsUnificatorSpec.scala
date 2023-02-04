package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.scalatest.concurrent.ScalaFutures
import ru.auto.api.unification.Unification.{CarsUnificationCollection, CarsUnificationEntry}
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.OfferMessage
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer.{EngineInfo, ModificationString}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClient
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * @author pnaydenov
  */
class CarsUnificatorSpec extends WordSpecBase with MockitoSupport with ScalaFutures {
  "CarsUnificator" should {
    "unify by modification_id or identical set of separate tags" in {
      val client = mock[UnificatorClient]
      when(client.carsUnify(?)).thenReturn(Future.successful(CarsUnificationCollection.newBuilder().build()))

      val unificator = new CarsUnificator(client)

      val offer1 = AutoruGenerators
        .carExternalOfferGen(newTasksGen)
        .next
        .copy(
          modification = ModificationString("GTC 1.6 MT (115 л.с.)"),
          mark = "Opel",
          model = "Astra, H Рестайлинг",
          bodyType = "Хэтчбек 3 дв.",
          year = 2007
        )

      val offer2 = offer1.copy(
        bodyType = "Хэтчбек",
        modification = EngineInfo(Some(1600), 115, "Бензин", "MT", "передний", Some("GTC")),
        doorsCount = Some(3),
        year = 2008
      )

      unificator.unify(Seq(offer1, offer2)).futureValue

      val request: ArgumentCaptor[CarsUnificationCollection] =
        ArgumentCaptor.forClass(classOf[CarsUnificationCollection])

      verify(client).carsUnify(request.capture())

      val entries = request.getValue.getEntriesList.asScala
      entries should have size (2)

      val entry1 = entries.find(_.getRawYear == "2007").get
      val entry2 = entries.find(_.getRawYear == "2008").get

      entry1.getRawMark shouldEqual "Opel"
      entry1.getRawModel shouldEqual "Astra H Рестайлинг"
      entry1.getRawBodyType shouldEqual "Хэтчбек 3 дв."
      entry1.getRawDoorsCount shouldEqual "3"
      entry1.getRawPower shouldEqual "115 л.с."
      entry1.getRawTransmission shouldEqual "MT"
      entry1.getRawDisplacement shouldEqual "1.6"
      entry1.getRawEngineType shouldEqual ""
      entry1.getRawIs4Wd shouldEqual "false"
      entry1.getRawNameplate shouldEqual "GTC"

      entry2.getRawMark shouldEqual "Opel"
      entry2.getRawModel shouldEqual "Astra H Рестайлинг"
      entry2.getRawBodyType shouldEqual "Хэтчбек"
      entry2.getRawDoorsCount shouldEqual "3"
      entry2.getRawPower shouldEqual "115 л.с."
      entry2.getRawTransmission shouldEqual "MT"
      entry2.getRawDisplacement shouldEqual "1.6"
      entry2.getRawEngineType shouldEqual "Бензин"
      entry2.getRawGearType shouldEqual "передний"
      entry2.getRawNameplate shouldEqual "GTC"
    }

    "convert kwt to horse power" in {
      CarsUnificator.toHorsePower("100 kWt") shouldEqual 136
      CarsUnificator.toHorsePower("100 kwT") shouldEqual 136
      CarsUnificator.toHorsePower("100 кВТ") shouldEqual 136
      CarsUnificator.toHorsePower("100 КвТ") shouldEqual 136
      CarsUnificator.toHorsePower("100 л.с.") shouldEqual 100
      CarsUnificator.toHorsePower("100 оло ло") shouldEqual 100
      CarsUnificator.toHorsePower("100") shouldEqual 100
    }

    "extract doors count from appropriate field or from body type" in {
      val client = mock[UnificatorClient]
      when(client.carsUnify(?)).thenReturn(Future.successful(CarsUnificationCollection.newBuilder().build()))
      val unificator = new CarsUnificator(client)
      val offer1 = AutoruGenerators
        .carExternalOfferGen(newTasksGen)
        .next
        .copy(
          modification = ModificationString("GTC 1.6 MT (115 л.с.)"),
          mark = "Opel",
          model = "Astra, H Рестайлинг",
          bodyType = "Хэтчбек 3 дв.",
          year = 2007,
          doorsCount = None
        )

      val offer2 = AutoruGenerators
        .carExternalOfferGen(newTasksGen)
        .next
        .copy(
          modification = ModificationString("GTC 1.6 MT (115 л.с.)"),
          mark = "Opel",
          model = "Astra, H Рестайлинг",
          bodyType = "Хэтчбек 3 дв.",
          year = 2008,
          doorsCount = Some(4)
        )

      unificator.unify(Seq(offer1, offer2)).futureValue

      val request: ArgumentCaptor[CarsUnificationCollection] =
        ArgumentCaptor.forClass(classOf[CarsUnificationCollection])
      verify(client).carsUnify(request.capture())
      val entries = request.getValue.getEntriesList.asScala
      entries should have size (2)
      val entry1 = entries.find(_.getRawYear == "2007").get
      val entry2 = entries.find(_.getRawYear == "2008").get

      entry1.getRawDoorsCount shouldEqual "3"
      entry2.getRawDoorsCount shouldEqual "4"
    }

    "escape commas from mark/model" in {
      val client = mock[UnificatorClient]
      when(client.carsUnify(?)).thenReturn(
        Future.successful(
          CarsUnificationCollection
            .newBuilder()
            .addEntries(
              CarsUnificationEntry
                .newBuilder()
                .setRawMark("Opel SuperEdition&")
                .setRawModel("Astra H Рестайлинг++")
                .setRawBodyType("Хэтчбек 3 дв.")
                .setRawNameplate("GTC")
                .setRawDoorsCount("3")
                .setRawYear("2007")
                .setRawDisplacement("1.6")
                .setRawTransmission("MT")
                .setRawPower("115 л.с.")
                .setRawIs4Wd("false")
                .setMark("OPEL")
                .setTechParamId(1)
                .setModel("ASTRA")
            )
            .build()
        )
      )
      val unificator = new CarsUnificator(client)
      val offer1 = AutoruGenerators
        .carExternalOfferGen(newTasksGen)
        .next
        .copy(
          modification = ModificationString("GTC 1.6 MT (115 л.с.)"),
          mark = " Opel,  SuperEdition& ",
          model = "Astra,   H,  Рестайлинг++ ", // should ignore extra spaces
          bodyType = "Хэтчбек 3 дв.",
          year = 2007,
          doorsCount = None,
          unification = None
        )

      val result = unificator.unify(Seq(offer1)).futureValue
      val request: ArgumentCaptor[CarsUnificationCollection] =
        ArgumentCaptor.forClass(classOf[CarsUnificationCollection])
      verify(client).carsUnify(request.capture())
      val entry = request.getValue.getEntries(0)
      entry.getRawMark shouldEqual "Opel SuperEdition&"
      entry.getRawModel shouldEqual "Astra H Рестайлинг++"

      val offer = result.head.asInstanceOf[OfferMessage[CarExternalOffer]].offer
      offer.unification shouldNot be(empty)
    }
  }
}
