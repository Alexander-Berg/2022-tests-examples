package ru.yandex.auto.vin.decoder.partners.mazda

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode

class MazdaRawModelTest extends AnyFunSuite {

  private val vinCode = VinCode("SJNFDAJ11U1084830")

  test("success") {
    val raw = ResourceUtils.getStringFromResources(s"/mazda/success.xml")

    val model = MazdaRawModel(raw, 200, vinCode)

    assert(model.rawStatus == "200")
    assert(model.data.Error_spcCode == "0")
    assert(model.data.ListOfMmrVehicleServiceData.AutoVehicle.size == 1)

    val vehicle = model.data.ListOfMmrVehicleServiceData.AutoVehicle.head
    assert(vehicle.SerialNumber.contains("JMZGG14F681721279"))
    assert(vehicle.ListOfMmEautoServiceHistory.get.MmEautoServiceHistory.size == 12)
  }

  test("not found") {
    val raw = ResourceUtils.getStringFromResources(s"/mazda/not_found.xml")

    val model = MazdaRawModel(raw, 200, vinCode)

    assert(model.data.Error_spcCode == "1")
    assert(model.data.Error_spcMessage == "Vehicle Not Found")
  }

  test("fault") {
    val raw = ResourceUtils.getStringFromResources(s"/mazda/fault.xml")

    intercept[RuntimeException] {
      MazdaRawModel(raw, 200, vinCode)
    }
  }

}
