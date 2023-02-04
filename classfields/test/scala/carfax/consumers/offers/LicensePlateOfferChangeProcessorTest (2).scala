package carfax.consumers.offers

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AsyncFunSuite
import ru.auto.api.DiffLogModel.OfferChangeEvent
import ru.yandex.auto.vin.decoder.manager.IdentifiersManager
import ru.yandex.auto.vin.decoder.manager.licenseplate.LicensePlateOffersManager
import ru.yandex.auto.vin.decoder.manager.orders.OrdersManager
import ru.yandex.auto.vin.decoder.storage.orders.ExternalReportDefinitions
import ru.yandex.auto.vin.decoder.storage.orders.impl.InMemoryReportDefinitionDao
import ru.yandex.vertis.mockito.MockitoSupport

class LicensePlateOfferChangeProcessorTest extends AsyncFunSuite with MockitoSupport {
  implicit val t: Traced = Traced.empty

  val lpOffersManager = mock[LicensePlateOffersManager]
  val identifiersManager = mock[IdentifiersManager]
  val ordersManager = mock[OrdersManager]
  val externalReportDefinitions: ExternalReportDefinitions = mock[ExternalReportDefinitions]
  val reportDao = new InMemoryReportDefinitionDao(externalReportDefinitions)

  val lpOffersProcessor =
    new LicensePlateOfferChangeProcessor(lpOffersManager, identifiersManager, ordersManager, reportDao)

  test("process") {
    val event = OfferChangeEvent.newBuilder().build()
    lpOffersProcessor.process(event).map { _ =>
      succeed
    }
  }
}
