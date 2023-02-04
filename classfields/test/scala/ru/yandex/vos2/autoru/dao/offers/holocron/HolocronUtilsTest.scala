package ru.yandex.vos2.autoru.dao.offers.holocron

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.vertis.holocron.common.HoloOffer
import ru.yandex.vos2.util.RandomUtil

@RunWith(classOf[JUnitRunner])
class HolocronUtilsTest extends AnyFunSuite {
  test("hash should change on AutoruExclusiveVerified flag changed") {
    val fullHoloOffer = randomCarsFullHoloOffer.toBuilder
    fullHoloOffer.getVosCarBuilder.getAutoruExclusiveVerifiedBuilder.setValue(true)
    val hash1 = HolocronUtils.getHash(fullHoloOffer.build())
    fullHoloOffer.getVosCarBuilder.getAutoruExclusiveVerifiedBuilder.setValue(false)
    val hash2 = HolocronUtils.getHash(fullHoloOffer.build())
    assert(hash1 != hash2)
  }

  test("topics") {
    assert(HolocronUtils.getTopic(randomCarsHoloOffer) == "holocron-auto-cars")
    assert(HolocronUtils.getTopic(randomTrucksHoloOffer) == "holocron-auto-trucks")
    assert(HolocronUtils.getTopic(randomMotoHoloOffer) == "holocron-auto-moto")
    assert(HolocronUtils.getTopic(randomCarsFullHoloOffer) == "holocron-full-auto-cars")
    assert(HolocronUtils.getTopic(randomTrucksFullHoloOffer) == "holocron-full-auto-trucks")
    assert(HolocronUtils.getTopic(randomMotoFullHoloOffer) == "holocron-full-auto-moto")
  }

  test("message keys") {
    val carsHoloOffer = randomCarsHoloOffer
    assert(HolocronUtils.getKey(carsHoloOffer) == carsHoloOffer.getCar.getId)
    assert(HolocronUtils.getKey(carsHoloOffer) != carsHoloOffer.getTruck.getId)
    assert(HolocronUtils.getKey(carsHoloOffer) != carsHoloOffer.getMoto.getId)
    assert(HolocronUtils.getKey(carsHoloOffer) != carsHoloOffer.getVosCar.getId)
    assert(HolocronUtils.getKey(carsHoloOffer) != carsHoloOffer.getVosTruck.getId)
    assert(HolocronUtils.getKey(carsHoloOffer) != carsHoloOffer.getVosMoto.getId)

    val trucksHoloOffer = randomTrucksHoloOffer
    assert(HolocronUtils.getKey(trucksHoloOffer) != trucksHoloOffer.getCar.getId)
    assert(HolocronUtils.getKey(trucksHoloOffer) == trucksHoloOffer.getTruck.getId)
    assert(HolocronUtils.getKey(trucksHoloOffer) != trucksHoloOffer.getMoto.getId)
    assert(HolocronUtils.getKey(trucksHoloOffer) != trucksHoloOffer.getVosCar.getId)
    assert(HolocronUtils.getKey(trucksHoloOffer) != trucksHoloOffer.getVosTruck.getId)
    assert(HolocronUtils.getKey(trucksHoloOffer) != trucksHoloOffer.getVosMoto.getId)

    val motoHoloOffer = randomMotoHoloOffer
    assert(HolocronUtils.getKey(motoHoloOffer) != motoHoloOffer.getCar.getId)
    assert(HolocronUtils.getKey(motoHoloOffer) != motoHoloOffer.getTruck.getId)
    assert(HolocronUtils.getKey(motoHoloOffer) == motoHoloOffer.getMoto.getId)
    assert(HolocronUtils.getKey(motoHoloOffer) != motoHoloOffer.getVosCar.getId)
    assert(HolocronUtils.getKey(motoHoloOffer) != motoHoloOffer.getVosTruck.getId)
    assert(HolocronUtils.getKey(motoHoloOffer) != motoHoloOffer.getVosMoto.getId)

    val carsFullHoloOffer = randomCarsFullHoloOffer
    assert(HolocronUtils.getKey(carsFullHoloOffer) != carsFullHoloOffer.getCar.getId)
    assert(HolocronUtils.getKey(carsFullHoloOffer) != carsFullHoloOffer.getTruck.getId)
    assert(HolocronUtils.getKey(carsFullHoloOffer) != carsFullHoloOffer.getMoto.getId)
    assert(HolocronUtils.getKey(carsFullHoloOffer) == carsFullHoloOffer.getVosCar.getId)
    assert(HolocronUtils.getKey(carsFullHoloOffer) != carsFullHoloOffer.getVosTruck.getId)
    assert(HolocronUtils.getKey(carsFullHoloOffer) != carsFullHoloOffer.getVosMoto.getId)

    val trucksFullHoloOffer = randomTrucksFullHoloOffer
    assert(HolocronUtils.getKey(trucksFullHoloOffer) != trucksFullHoloOffer.getCar.getId)
    assert(HolocronUtils.getKey(trucksFullHoloOffer) != trucksFullHoloOffer.getTruck.getId)
    assert(HolocronUtils.getKey(trucksFullHoloOffer) != trucksFullHoloOffer.getMoto.getId)
    assert(HolocronUtils.getKey(trucksFullHoloOffer) != trucksFullHoloOffer.getVosCar.getId)
    assert(HolocronUtils.getKey(trucksFullHoloOffer) == trucksFullHoloOffer.getVosTruck.getId)
    assert(HolocronUtils.getKey(trucksFullHoloOffer) != trucksFullHoloOffer.getVosMoto.getId)

    val motoFullHoloOffer = randomMotoFullHoloOffer
    assert(HolocronUtils.getKey(motoFullHoloOffer) != motoFullHoloOffer.getCar.getId)
    assert(HolocronUtils.getKey(motoFullHoloOffer) != motoFullHoloOffer.getTruck.getId)
    assert(HolocronUtils.getKey(motoFullHoloOffer) != motoFullHoloOffer.getMoto.getId)
    assert(HolocronUtils.getKey(motoFullHoloOffer) != motoFullHoloOffer.getVosCar.getId)
    assert(HolocronUtils.getKey(motoFullHoloOffer) != motoFullHoloOffer.getVosTruck.getId)
    assert(HolocronUtils.getKey(motoFullHoloOffer) == motoFullHoloOffer.getVosMoto.getId)
  }

  private def randomCarsHoloOffer = {
    val b = HoloOffer.newBuilder()
    b.getCarBuilder.setId(RandomUtil.nextSymbols(8))
    b.build()
  }

  private def randomTrucksHoloOffer = {
    val b = HoloOffer.newBuilder()
    b.getTruckBuilder.setId(RandomUtil.nextSymbols(8))
    b.build()
  }

  private def randomMotoHoloOffer = {
    val b = HoloOffer.newBuilder()
    b.getMotoBuilder.setId(RandomUtil.nextSymbols(8))
    b.build()
  }

  private def randomCarsFullHoloOffer = {
    val b = HoloOffer.newBuilder()
    b.getVosCarBuilder.setId(RandomUtil.nextSymbols(8))
    b.build()
  }

  private def randomTrucksFullHoloOffer = {
    val b = HoloOffer.newBuilder()
    b.getVosTruckBuilder.setId(RandomUtil.nextSymbols(8))
    b.build()
  }

  private def randomMotoFullHoloOffer = {
    val b = HoloOffer.newBuilder()
    b.getVosMotoBuilder.setId(RandomUtil.nextSymbols(8))
    b.build()
  }
}
