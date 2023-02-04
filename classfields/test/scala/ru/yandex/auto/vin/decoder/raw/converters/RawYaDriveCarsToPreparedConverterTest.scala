package ru.yandex.auto.vin.decoder.raw.converters

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.raw.yadrive.internal.{RawYaDriveCarsToPreparedConverter, YaDriveCarsRawModelManager}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class RawYaDriveCarsToPreparedConverterTest extends AnyFunSuite {

  private val converter = new RawYaDriveCarsToPreparedConverter
  val manager = new YaDriveCarsRawModelManager()
  implicit val t: Traced = Traced.empty

  test("correct convert") {
    val raw = ResourceUtils.getStringFromResources("/ya_drive/ya_drive_cars.json")

    val result = Await.result(converter.convert(manager.parse(raw, "", "").toOption.get), 1.second)

    val mileage = result.getMileage(0)
    val carsharing = result.getCarsharing(0)

    assert(result.getMileageCount === 1)
    assert(result.getCarsharingCount === 1)

    assert(mileage.getBeaten.getValue === true)
    assert(mileage.getValue === 4991)
    assert(mileage.getCity === "Казань")
    assert(mileage.getDate === 1583971200000L)

    assert(carsharing.getDate === 1583971200000L)
    assert(carsharing.getLicensePlate === "Е201СК799")
    assert(carsharing.getOperator === "Яндекс.Драйв")
  }

}
