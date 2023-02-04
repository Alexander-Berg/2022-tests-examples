package ru.yandex.vos2.autoru.services

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.TrucksModel.TruckCategory
import ru.yandex.vos2.AutoruModel.AutoruOffer.TruckInfo

/**
  * Created by andrey on 9/5/17.
  */
@RunWith(classOf[JUnitRunner])
class AutoruTrucksCatalogTest extends AnyFunSuite {
  test("cabinKey 6") {
    assert(AutoruTrucksCatalog.CabinByCode(6) == TruckInfo.CabinType.SEAT_6)
    assert(
      AutoruTrucksCatalog
        .CabinCodeByNumber((TruckInfo.CabinType.SEAT_6.getNumber, TruckCategory.TRUCK.getNumber)) == 876
    )
  }
}
