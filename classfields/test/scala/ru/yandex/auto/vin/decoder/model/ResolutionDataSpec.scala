package ru.yandex.auto.vin.decoder.model

import cats.implicits.catsSyntaxOptionId
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.proto.VinHistory._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared

class ResolutionDataSpec extends AnyWordSpecLike {
  private val vin: VinCode = CommonVinCode("SALWA2FK7HA135034")

  "ResolutionData" should {
    "return correct offers" in {
      val reg = VinInfoHistory
        .newBuilder()
        .setRegistration(
          Registration
            .newBuilder()
            .setMark("AUDI")
            .setModel("A7")
        )
        .build()
      val offers = VinInfo
        .newBuilder()
        .setMark("BMW")
        .setModel("X5")
        .build()
      val data = ResolutionData.empty(vin).copy(registration = Prepared(0, 0, 0, reg, "").some, offers = List(offers))
      assert(data.correctOffers.isEmpty)
    }
  }
}
