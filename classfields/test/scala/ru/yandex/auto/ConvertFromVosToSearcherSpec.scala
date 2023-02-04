package ru.yandex.auto

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.auto.actor.cars.Unifier
import ru.yandex.auto.actor.moto.MotoAdConverter
import ru.yandex.auto.actor.trucks.TruckAdConverter
import ru.yandex.auto.core.model.moto.MotoAd
import ru.yandex.auto.core.model.trucks.TruckAd
import ru.yandex.auto.utils.CarAdConverter
import ru.yandex.auto.core.model.{CarAd, MotoOffer, TruckOffer, Offer => DomainOffer}
import ru.yandex.auto.message.AutoSchema
import ru.yandex.auto.message.MotoOffersSchema.MotoOfferMessage
import ru.yandex.auto.message.TrucksOffersSchema.TrucksOfferMessage

@RunWith(classOf[JUnitRunner])
class ConvertFromVosToSearcherSpec extends FunSuite {

  test("converting cars from vos to searcher") {
    // vos - raw model
    val vosOffer = AutoSchema.OfferMessage
      .newBuilder()
      .setVersion(8)
      .setId("123")
      .setMark("AUDI")
      .setModel("A3")
      .setPrice(100000)
      .setCreationDate(1656444197082L)
      .setPageUrl("google.com")
      .addSearchTags("super_auto")
      .build()

    // shard - domain model
    val domainOffer = DomainOffer.fromMessage(vosOffer)

    // shard - unify
    val unifiedOffer = Unifier.offerToUnifiedCarInfo(domainOffer)

    // TODO modifier ???

    // shard - carAd
    val carAd = CarAdConverter.toCarAd(unifiedOffer)(_ => "")

    // shard - send to searcher
    val carAdMessage = carAd.toMessage

    // searcher - extract carAd
    val searcherCarAd = CarAd.fromMessage(carAdMessage)

    assert(searcherCarAd.getId == "123")
    assert(searcherCarAd.getMark == "AUDI")
    assert(searcherCarAd.getModel == "A3")
    assert(searcherCarAd.getPrice == 100000)
    assert(searcherCarAd.getSearchTags.size() == 1)
  }

  test("converting motos from vos to searcher") {
    // vos - raw model
    val vosMotoOffer = MotoOfferMessage
      .newBuilder()
      .setVersion(1)
      .setId("123")
      .setPrice(7329)
      .setCreationDate(1656444197082L)
      .addSearchTags("super_motos")
      .build()

    // shard - domain model
    val domainOffer = MotoOffer.fromMessage(vosMotoOffer)

    // shard - unify
    val unifiedOffer = ru.yandex.auto.actor.moto.Unifier.offerToUnifiedMotoInfo(domainOffer)

    // TODO modifier ???

    // shard - motoAd
    val motoAd = MotoAdConverter.toMotoAd(unifiedOffer)

    // shard - send to searcher
    val motoAdMessage = motoAd.toMessage

    // searcher - extract motoAd
    val searcherMotoAd = MotoAd.fromMessage(motoAdMessage)

    assert(searcherMotoAd.getId == "123")
    assert(searcherMotoAd.getPrice == 7329)
    assert(searcherMotoAd.getSearchTags.size() == 1)
  }

  test("converting trucks from vos to searcher") {
    // vos - raw model
    val vosTruckOffer = TrucksOfferMessage
      .newBuilder()
      .setVersion(1)
      .setId("123")
      .setPrice(99997)
      .setCreationDate(1656444197082L)
      .addSearchTags("super_trucks")
      .build()

    // shard - domain model
    val domainOffer = TruckOffer.fromMessage(vosTruckOffer)

    // shard - unify
    val unifiedOffer = ru.yandex.auto.actor.trucks.Unifier.offerToUnifiedTruckInfo(domainOffer)

    // TODO modifier ???

    // shard - truckAd
    val truckAd = TruckAdConverter.toTruckAd(unifiedOffer)

    // shard - send to searcher
    val truckAdMessage = truckAd.toMessage

    // searcher - extract truckAd
    val searcherTruckAd = TruckAd.fromMessage(truckAdMessage)

    assert(searcherTruckAd.getId == "123")
    assert(searcherTruckAd.getPrice == 99997)
    assert(searcherTruckAd.getSearchTags.size() == 1)
  }
}
