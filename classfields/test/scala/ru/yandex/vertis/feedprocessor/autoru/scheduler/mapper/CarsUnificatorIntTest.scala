package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import akka.actor.ActorSystem
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.model.Generators
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.{FailureMessage, OfferMessage}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer.{
  EngineInfo,
  ModificationString,
  TechParam
}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper.Mapper
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClientImpl
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.{AutoruGenerators, HttpClientSuite}
import ru.yandex.vertis.feedprocessor.http.HttpClientConfig
import ru.yandex.vertis.feedprocessor.util.DummyOpsSupport

class CarsUnificatorIntTest extends WordSpecBase with HttpClientSuite with DummyOpsSupport with TestApplication {
  implicit lazy val actorSystem: ActorSystem = ActorSystem(environment.serviceName, environment.config)
  implicit val meter: Mapper.Meters = new Mapper.Meters(prometheusRegistry)

  val client = new UnificatorClientImpl(http)

  val carsUnificator = new CarsUnificator(client)
  val carsByTechParamUnificator = new CarsByTechParamUnificator(client)

  "generation with same name and different years" in {
    val offer1 = AutoruGenerators
      .carExternalOfferGen(Generators.newTasksGen)
      .next
      .copy(
        modification = ModificationString("2.0 MT (128 л.с.)"),
        mark = "Honda",
        model = "CR-V, I",
        bodyType = "Внедорожник 5 дв.",
        year = 1998
      )

    val offer2 = AutoruGenerators
      .carExternalOfferGen(Generators.newTasksGen)
      .next
      .copy(
        modification = ModificationString("2.0 MT (147 л.с.)"),
        mark = "Honda",
        model = "CR-V, I Рестайлинг",
        bodyType = "Внедорожник 5 дв.",
        year = 2001
      )

    val offers = Seq(offer1, offer2)
    val results = carsUnificator.unify(offers).futureValue

    val res1 = results(0).asInstanceOf[OfferMessage[CarExternalOffer]].offer
    assert(res1.unification.get.techParamId.contains(20501222))

    val res2 = results(1).asInstanceOf[OfferMessage[CarExternalOffer]].offer
    assert(res2.unification.get.techParamId.contains(20501278))
  }

  "use nameplate" in {
    val withNameplate = AutoruGenerators
      .carExternalOfferGen(Generators.newTasksGen)
      .next
      .copy(
        modification = ModificationString("Long 3.0 AT (310 л.с.) 4WD"),
        mark = "Audi",
        model = "A8, III (D4) Рестайлинг",
        bodyType = "Седан",
        year = 2015
      )

    val withoutNameplate = AutoruGenerators
      .carExternalOfferGen(Generators.newTasksGen)
      .next
      .copy(
        modification = ModificationString("3.0 AT (310 л.с.) 4WD"),
        mark = "Audi",
        model = "A8, III (D4) Рестайлинг",
        bodyType = "Седан",
        year = 2015
      )

    val offers = Seq(withNameplate, withoutNameplate)
    val results = carsUnificator.unify(offers).futureValue

    val res1 = results(0).asInstanceOf[OfferMessage[CarExternalOffer]].offer
    assert(res1.unification.get.techParamId.contains(20071438))

    val res2 = results(1).asInstanceOf[OfferMessage[CarExternalOffer]].offer
    assert(res2.unification.get.techParamId.contains(20071468))
  }

  "horse power approximation" in {
    val testOffer = AutoruGenerators
      .carExternalOfferGen(Generators.newTasksGen)
      .next
      .copy(
        modification = ModificationString("Sport 1.6 MT (118 л.с.)"),
        mark = "Lada (ВАЗ)",
        model = "Kalina, II",
        bodyType = "Хэтчбек 5 дв.",
        year = 2017
      )

    val offers = Seq(testOffer)
    val results = carsUnificator.unify(offers).futureValue

    assert(results(0) match {
      case _: FailureMessage[_] => true
      case _ => false
    })
  }

  "pick two cabins" in {
    val testOffer = AutoruGenerators
      .carExternalOfferGen(Generators.newTasksGen)
      .next
      .copy(
        modification = ModificationString("full-time 2.0d AT (180 л.с.) 4WD"),
        mark = "Volkswagen",
        model = "Amarok, I",
        bodyType = "Пикап Двойная кабина",
        year = 2015
      )

    val offers = Seq(testOffer)
    val results = carsUnificator.unify(offers).futureValue

    val res1 = results(0).asInstanceOf[OfferMessage[CarExternalOffer]].offer
    assert(res1.unification.get.techParamId.contains(9263553))
  }

  "allow 2 horse difference" in {
    val offer1 = AutoruGenerators
      .carExternalOfferGen(Generators.newTasksGen)
      .next
      .copy(
        modification = ModificationString("2.0 MT (130 л.с.)"),
        mark = "Honda",
        model = "CR-V, I",
        bodyType = "Внедорожник 5 дв.",
        year = 1998
      )
    val offers = Seq(offer1)
    val results = carsUnificator.unify(offers).futureValue

    val res1 = results(0).asInstanceOf[OfferMessage[CarExternalOffer]].offer
    assert(res1.unification.get.techParamId.contains(20501222))
  }

  "allow kWT" in {
    val testOffer = AutoruGenerators
      .carExternalOfferGen(Generators.newTasksGen)
      .next
      .copy(
        modification = ModificationString("75D Electro AT (246 кВт) 4WD"),
        mark = "Tesla",
        model = "Model S, I Рестайлинг",
        bodyType = "Лифтбек",
        year = 2018
      )
    val offers = Seq(testOffer)
    val results = carsUnificator.unify(offers).futureValue

    val res1 = results(0).asInstanceOf[OfferMessage[CarExternalOffer]].offer
    assert(res1.unification.get.techParamId.contains(20859163))
  }

  "allow offer with modification as separate tags" in {
    val offer1 = AutoruGenerators
      .carExternalOfferGen(Generators.newTasksGen)
      .next
      .copy(
        modification = ModificationString("Long 3.0 AT (310 л.с.) 4WD"),
        mark = "Audi",
        model = "A8, III (D4) Рестайлинг",
        bodyType = "Седан",
        year = 2015
      )
    val offer2 = offer1.copy(modification = EngineInfo(Some(3000), 310, "бензин", "AT", "полный", Some("Long")))

    val results = carsUnificator.unify(Seq(offer1, offer2)).futureValue
    results should have size (2)
    results(0).asInstanceOf[OfferMessage[CarExternalOffer]].offer.unification.get.techParamId.get shouldEqual 20071438
    results(1).asInstanceOf[OfferMessage[CarExternalOffer]].offer.unification.get.techParamId.get shouldEqual 20071438
  }

  "allow unification using tech_param_id" in {
    val offer1 = AutoruGenerators
      .carExternalOfferGen(Generators.newTasksGen)
      .next
      .copy(modification = TechParam(20071438))
    val offer2 = offer1.copy(
      modification = EngineInfo(Some(3000), 310, "бензин", "AT", "полный", Some("Long")),
      mark = "Audi",
      model = "A8, III (D4) Рестайлинг",
      bodyType = "Седан",
      year = 2015
    )

    val result1 = carsByTechParamUnificator.unify(Seq(offer1)).futureValue
    val result2 = carsUnificator.unify(Seq(offer2)).futureValue

    val unification1 = result1.head.asInstanceOf[OfferMessage[CarExternalOffer]].offer.unification.get
    val unification2 = result2.head.asInstanceOf[OfferMessage[CarExternalOffer]].offer.unification.get

    unification1.copy(gearType = None) shouldEqual unification2.copy(gearType = None)
    // TODO: gearType разных ручек унификатора не совпадает
  }

  override protected def config: HttpClientConfig =
    HttpClientConfig("auto2-searcher-api.vrts-slb.test.vertis.yandex.net", 80)
}
