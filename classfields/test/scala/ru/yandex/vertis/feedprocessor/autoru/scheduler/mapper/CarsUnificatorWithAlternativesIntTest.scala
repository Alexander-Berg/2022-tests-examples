package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import akka.actor.ActorSystem
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.model.Generators
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer.ModificationString
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClientImpl
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.{AutoruGenerators, HttpClientSuite}
import ru.yandex.vertis.feedprocessor.http.HttpClientConfig
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.OfferMessage
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper.Mapper
import ru.yandex.vertis.feedprocessor.util.DummyOpsSupport

/**
  * @author pnaydenov
  */
class CarsUnificatorWithAlternativesIntTest
  extends WordSpecBase
  with HttpClientSuite
  with DummyOpsSupport
  with TestApplication {

  implicit lazy val actorSystem: ActorSystem = ActorSystem(environment.serviceName, environment.config)
  implicit val meter: Mapper.Meters = new Mapper.Meters(prometheusRegistry)

  val client = new UnificatorClientImpl(http)

  override protected def config: HttpClientConfig =
    HttpClientConfig("auto2-searcher-api.vrts-slb.test.vertis.yandex.net", 80)

  val carsUnificator =
    new CarsUnificatorWithAlternatives(client) with CarsUnificatorWithAlternatives.AutomaticTransmissionFix

  "CarsUnificatorWithAlternativesSpec" should {
    "unify AT and AMT transmission" in {
      val offer1 = AutoruGenerators
        .carExternalOfferGen(Generators.newTasksGen)
        .next
        .copy(
          modification = ModificationString("GTC 1.6 AT (115 л.с.)"),
          mark = "Opel",
          model = "Astra, H Рестайлинг",
          bodyType = "Хэтчбек 3 дв.",
          year = 2007
        )

      val offer2 = AutoruGenerators
        .carExternalOfferGen(Generators.newTasksGen)
        .next
        .copy(
          modification = ModificationString("1.4 MT (107 л.с.)"),
          mark = "Hyundai",
          model = "Solaris, I Рестайлинг",
          bodyType = "Седан",
          year = 2015
        )

      val offer3 = AutoruGenerators
        .carExternalOfferGen(Generators.newTasksGen)
        .next
        .copy(
          modification = ModificationString("6-speed 1.6 AT (123 л.с.)"),
          mark = "Kia",
          model = "Rio, III",
          bodyType = "Седан",
          year = 2014
        )

      val offer4 = AutoruGenerators
        .carExternalOfferGen(Generators.newTasksGen)
        .next
        .copy(
          modification = ModificationString("1.2 AT (105 л.с.)"),
          mark = "Skoda",
          model = "Fabia, II Рестайлинг",
          bodyType = "Хэтчбек 5 дв.",
          year = 2012
        )

      val offers = Seq(offer1, offer2, offer3, offer4)
      val results = carsUnificator.unify(offers).futureValue

      val res1 = results(0).asInstanceOf[OfferMessage[CarExternalOffer]].offer
      assert(res1.unification.get.transmission.contains("ROBOT_1CLUTCH"))

      val res2 = results(1).asInstanceOf[OfferMessage[CarExternalOffer]].offer
      assert(res2.unification.get.transmission.contains("MECHANICAL"))

      val res3 = results(2).asInstanceOf[OfferMessage[CarExternalOffer]].offer
      assert(res3.unification.get.transmission.contains("AUTOMATIC"))

      val res4 = results(3).asInstanceOf[OfferMessage[CarExternalOffer]].offer
      assert(res4.unification.get.transmission.contains("ROBOT_2CLUTCH"))
    }

    "unify non specified CVT transmission" in {
      val offer1 = AutoruGenerators
        .carExternalOfferGen(Generators.newTasksGen)
        .next
        .copy(
          modification = ModificationString("2.0 AT (148 л.с.) 4WD"),
          mark = "Toyota",
          model = "RAV 4",
          bodyType = "Хэтчбек 5 дв.",
          year = 2011
        )

      val offers = Seq(offer1)
      val results = carsUnificator.unify(offers).futureValue

      results should have size (1)

      val res0 = results(0).asInstanceOf[OfferMessage[CarExternalOffer]].offer
      res0.unification.get.horsePower.get shouldEqual 148 +- 2
      res0.unification.get.displacement.get shouldEqual 2000 +- 100
    }
  }
}
