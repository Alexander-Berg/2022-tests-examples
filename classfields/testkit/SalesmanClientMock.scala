package auto.common.clients.salesman.testkit

import java.time.Instant

import common.geobase.model.RegionIds.RegionId
import auto.common.clients.salesman.{Campaign, ProductCreationResult}
import auto.common.clients.salesman.{ProductDomain, SalesmanClient}
import ru.auto.api.api_offer_model.{Offer, TeleponyInfo}
import ru.auto.salesman.MatchApplication
import ru.auto.salesman.model.cashback.api_model.LoyaltyReportInfo
import ru.auto.salesman.model.telepony.api_model.{CallInfoResponse, TeleponyInfoBatchRequest, TeleponyInfoBatchResponse}
import ru.auto.salesman.model.user.api_model.VinHistoryPurchasesForVin
import ru.auto.salesman.products.products
import ru.auto.salesman.products.products.ProductRequest
import ru.auto.salesman.tariffs.credit_tariffs.DealersWithActiveApplicationCredit
import ru.auto.salesman.calls.calls_tariff_response.CallsTariffsResponse
import ru.yandex.vertis.billing.model.Limits
import zio.test.mock.mockable
import zio.{Task, ZLayer}

@mockable[SalesmanClient.Service]
object SalesmanClientMock

object SalesmanClientEmpty {

  val empty = ZLayer.succeed {
    new SalesmanClient.Service {

      override def getAllActiveProducts(domain: ProductDomain): Task[Seq[products.Product]] = ???

      override def getDealerActiveProducts(dealerId: Long, domain: ProductDomain): Task[Seq[products.Product]] = ???

      override def amoSyncDealers(dealerIds: Seq[Long]): Task[Unit] = ???

      override def createTradeIn(
          createForms: MatchApplication.MatchApplicationCreateForms): Task[MatchApplication.MatchApplicationCreateResponse] =
        ???

      override def createProduct(request: ProductRequest): Task[ProductCreationResult] = ???

      override def getDealersWithActiveCreditApplicationProduct: Task[Seq[DealersWithActiveApplicationCredit.GroupedDealers]] =
        ???

      override def getDealerCampaign(dealerId: Long, includeDisabled: Boolean): Task[List[Campaign]] = ???

      override def getLoyaltyReport(clientId: Long): Task[Option[LoyaltyReportInfo]] = ???

      override def getBatchTeleponyInfo(request: TeleponyInfoBatchRequest): Task[TeleponyInfoBatchResponse] = ???

      override def getTeleponyInfo(offer: Offer): Task[TeleponyInfo] = ???

      override def getAvailableCallTariffsInRegion(clientId: Long, regionId: RegionId): Task[CallsTariffsResponse] = ???

      override def getVinHistoryPurchases(vin: String): Task[VinHistoryPurchasesForVin] = ???

      override def getCallCarsUsedCampaignLimits(dealerId: Long): Task[Option[Limits]] = ???
    }
  }
}
