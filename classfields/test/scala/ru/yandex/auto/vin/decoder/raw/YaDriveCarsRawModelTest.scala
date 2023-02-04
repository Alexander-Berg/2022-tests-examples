package ru.yandex.auto.vin.decoder.raw

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.raw.yadrive.internal.YaDriveCarsRawModelManager

import scala.concurrent.ExecutionContext.Implicits.global

class YaDriveCarsRawModelTest extends AnyFunSuite {

  val manager = new YaDriveCarsRawModelManager()

  test("correct parse") {
    val raw = ResourceUtils.getStringFromResources("/ya_drive/ya_drive_cars.json")

    val result = manager.parse(raw, "", "").toOption.get

    assert(result.identifier.toString == "XW8AN2NE8LH006788")
//    assert(result.hash === "73d962c9c1cf9f832473c5206cd4f0f1")
    assert(result.groupId === "2020-03-12")

    assert(result.data.tableName === "2020-03-12")
    assert(result.data.timestamp === 1583971200000L)

    assert(result.data.info.operator == "Яндекс.Драйв")
    assert(result.data.info.isTotal === true)
    assert(result.data.info.mileage === Some(4991))
    assert(result.data.info.city === "Казань")
    assert(result.data.info.cityCode === "KZN")

  }

}
