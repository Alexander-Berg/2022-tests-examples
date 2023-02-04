package ru.yandex.auto.vin.decoder.raw.yadrive

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Carsharing, VinInfoHistory}
import ru.yandex.auto.vin.decoder.raw.yadrive.external.DriveExtenalCarsharingRawModelManager
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class DriveExtenalCarsharingRawModelManagerTest extends AnyFunSuite {

  val manager = new DriveExtenalCarsharingRawModelManager

  test("correct parse") {
    val vh = VinInfoHistory
      .newBuilder()
      .setLicensePlate("E588OP799")
      .setEventType(EventType.YANDEX_DRIVE_EXTERNAL_CARSHARING)
      .setStatus(VinInfoHistory.Status.OK)
      .addCarsharing(
        Carsharing
          .newBuilder()
          .setCity("Москва")
          .setOperator("Делимобиль")
          .setDate(1583971200000L)
          .setDateFinish(1586563200000L)
          .setLicensePlate("E588OP799")
      )
      .build()

    val raw = ResourceUtils.getStringFromResources("/ya_drive/ya_drive_external_cars.json")
    val model = manager.parse(raw, "", "").toOption.get
    val converted = manager.convert(model).await

    assert(converted == vh)
  }

}
