package ru.yandex.auto.vin.decoder.partners.interfax

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.VinReportModel.OwnerType
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.misc.DateTimeUtils.RichProtoTimestamp

import scala.xml.XML

class InterfaxCheckVinRawToPrepConverterTest extends AnyFunSuite {

  private val vinCode = VinCode("SJNFDAJ11U1084830")
  private val converter = new InterfaxCheckVinRawToPreparedConverter
  implicit val t: Traced = Traced.empty

  test("success") {
    val raw = ResourceUtils.getStringFromResources(s"/interfax/checkVIN/success.xml")

    val model = InterfaxCheckVinRawModel(vinCode, raw, XML.loadString(raw))
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.INTERFAX_LEASINGS)

    assert(converted.getLeasingsCount == 2)
    val leasingA = converted.getLeasings(0)
    assert(!leasingA.hasStopDate)
    assert(leasingA.getStartDate.getMillis == 1436734800000L)
    assert(leasingA.getEndDate.getMillis == 1538341200000L)
    assert(leasingA.getContractNumber == "LS-771499/2015")
    assert(leasingA.getLessorsCount == 1)
    assert(leasingA.getLessors(0).getOwnerType == OwnerType.Type.LEGAL)
    assert(leasingA.getLessors(0).getName == "ООО \"М-Лизинг \"")
    assert(leasingA.getLessors(0).getInn == "5024093363")
    assert(leasingA.getLessees(0).getOwnerType == OwnerType.Type.LEGAL)
    assert(leasingA.getLessees(0).getName == "ООО \"М-Профи\"")
    assert(leasingA.getLessees(0).getInn == "5024117159")

    val leasingB = converted.getLeasings(1)
    assert(leasingB.hasIsSubleasing)
    assert(leasingB.getIsSubleasing.getValue)
    assert(leasingB.getContractNumber == "LS-771599/2015")
    assert(leasingB.getStartDate.getMillis == 1436734800000L)
    assert(leasingB.getEndDate.getMillis == 1538341200000L)
    assert(leasingB.getStopReason == "Вернули тачку")
    assert(leasingB.getStopDate.getMillis == 1451595600000L)
    assert(leasingB.getLessors(0).getOwnerType == OwnerType.Type.PERSON)
    assert(leasingB.getLessors(0).getName == "Павел")
  }

  test("not found") {
    val raw = ResourceUtils.getStringFromResources(s"/interfax/checkVIN/not_found.xml")

    val model = InterfaxCheckVinRawModel(vinCode, raw, XML.loadString(raw))
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.INTERFAX_LEASINGS)

    assert(converted.getLeasingsCount == 0)
  }
}
