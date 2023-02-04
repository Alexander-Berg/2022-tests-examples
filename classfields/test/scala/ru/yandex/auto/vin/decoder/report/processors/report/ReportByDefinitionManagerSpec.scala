package ru.yandex.auto.vin.decoder.report.processors.report

import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.report.converters.raw.PreparedDataConverterImpl
import ru.yandex.auto.vin.decoder.storage.orders.ExternalReportDefinitions
import ru.yandex.auto.vin.decoder.storage.orders.impl.InMemoryReportDefinitionDao
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global

class ReportByDefinitionManagerSpec extends AnyWordSpec with MockitoSupport {
  private val reportDefinitions = mock[ExternalReportDefinitions]
  private val service = new InMemoryReportDefinitionDao(reportDefinitions)

  private val manager =
    new ReportDefinitionManager(service)
//  private val jdm = VinCode("01234567")

  "ReportByDefinitionManager" should {
    "trigger autocode update" in {
      manager
    }
  }
}
