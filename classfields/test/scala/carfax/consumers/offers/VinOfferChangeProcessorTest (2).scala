package carfax.consumers.offers

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.extdata.region.Tree
import ru.yandex.auto.vin.decoder.manager.orders.OrdersManager
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.vin.RegionInfo
import ru.yandex.auto.vin.decoder.providers.resellers.ResellersWithFreeReportAccess
import ru.yandex.auto.vin.decoder.report.processors.report.ReportManager
import ru.yandex.auto.vin.decoder.storage.orders.ExternalReportDefinitions
import ru.yandex.auto.vin.decoder.storage.orders.impl.InMemoryReportDefinitionDao
import ru.yandex.auto.vin.decoder.ydb.YdbOffersDao
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class VinOfferChangeProcessorTest extends AnyWordSpecLike with Matchers with MockitoSupport {

  val tree: Tree = mock[Tree]
  val reportManager: ReportManager = mock[ReportManager]
  val ordersManager: OrdersManager = mock[OrdersManager]
  val resellersProvider: ResellersWithFreeReportAccess = ResellersWithFreeReportAccess(Set.empty)
  val externalReportDefinitions: ExternalReportDefinitions = mock[ExternalReportDefinitions]
  val ydbOffersDao: YdbOffersDao = mock[YdbOffersDao]
  val reportDefinitionDao = new InMemoryReportDefinitionDao(externalReportDefinitions)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val t: Traced = Traced.empty
  val MOSCOW: Long = 213
  val LUBERCI: Long = 10738
  val MOSCOW_AND_MO: Long = 1
  val KOSTROMA: Long = 513

  val vin = VinCode("ABCDEFGHJKLMNPRST")

  val vinOfferChangeProcessor =
    new VinOfferChangeProcessor(
      resellersProvider,
      ordersManager,
      tree,
      reportDefinitionDao,
      ydbOffersDao,
      ydbOffersDao,
      Feature("", _ => true)
    )

  "needUpdatePaidSourcesOnOfferActive" should {
    "return true" when {
      "feature enabled and activated suitable offer" in {
        when(tree.isInside(?, ?)).thenReturn(false)
        val result = vinOfferChangeProcessor.needUpdatePaidSourcesOnOfferActive(
          vin,
          "BMW",
          Some(RegionInfo(MOSCOW)),
          isOwnerReseller = true
        )
        result shouldBe true
      }
    }
    "return false" when {
      "offer is not suitable because of owner is not reseller" in {
        when(tree.isInside(?, ?)).thenReturn(false)

        val result = vinOfferChangeProcessor.needUpdatePaidSourcesOnOfferActive(
          vin,
          "BMW",
          Some(RegionInfo(MOSCOW)),
          isOwnerReseller = false
        )
        result shouldBe false
      }
      "offer is not suitable because of mark" in {
        when(tree.isInside(?, ?)).thenReturn(false)

        val result = vinOfferChangeProcessor.needUpdatePaidSourcesOnOfferActive(
          vin,
          "VAZ",
          Some(RegionInfo(MOSCOW)),
          isOwnerReseller = true
        )
        result shouldBe false
      }
      "offer is not suitable because of region" in {
        when(tree.isInside(?, ?)).thenReturn(true)

        val result = vinOfferChangeProcessor.needUpdatePaidSourcesOnOfferActive(
          vin,
          "BMW",
          Some(RegionInfo(MOSCOW)),
          isOwnerReseller = true
        )
        result shouldBe false
      }
    }
  }

}
