package ru.yandex.auto.vin.decoder.partners.audatex

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.audatex.model.AudatexAudaHistoryRawModel

class AudatexAudaHistoryRawModelTest extends AnyFunSuite {

  private val vinCode = VinCode("SJNFDAJ11U1084830")

  test("success") {
    val raw = ResourceUtils.getStringFromResources(s"/audatex/auda_history/success.xml")

    val model = AudatexAudaHistoryRawModel(raw, 200, vinCode)

    assert(model.data.loginId == "RU434494")
    assert(model.data.message.head.messageCode == "AudaHistoryService.OK")

    val reports = model.optReports.get

    assert(reports.size == 2)
    val report = reports.audaHistoryHeader.head
    assert(report.countryCode == "RU")
    assert(report.manufacturer.contains("SUZUKI [R] [50]"))
    assert(report.model.contains("Swift 09/10- [M] [S] [E] [24]"))
    assert(report.subModel.contains("Swift"))
    assert(report.manufacturerCodeAX == "50")
    assert(report.subModelCodeAX == "01")
    assert(report.calculationDateTime.toString == "2013-09-02T23:00")
    assert(report.calculationNumber == "0008132368")
    assert(!report.totalLoss)
    assert(report.labourCost.contains(1.0))
    assert(report.partsCost.contains(19997.0))
    assert(report.paintCost.contains(0.0))
    assert(report.totalCost.contains(19998.0))
    assert(report.mileage.isEmpty)
    assert(report.audaHistoryDetails.parts.audaHistoryPartDetail.size == 6)
    assert(report.audaHistoryDetails.parts.audaHistoryPartDetail.head.description.contains("БАМПЕР П - С/У"))
    assert(report.audaHistoryDetails.parts.audaHistoryPartDetail.head.repairMethod.contains("Замена"))

  }

  test("not found") {
    val raw = ResourceUtils.getStringFromResources(s"/audatex/auda_history/not_found.xml")
    val model = AudatexAudaHistoryRawModel(raw, 200, vinCode)

    assert(model.data.message.head.messageCode == "AudaHistoryService.OK")
    assert(model.optReports.isEmpty)
  }

  test("invalid credentials") {
    val raw = ResourceUtils.getStringFromResources(s"/audatex/invalid_creds.xml")

    intercept[AudatexError] {
      AudatexAudaHistoryRawModel(raw, 200, vinCode)
    }
  }
}
