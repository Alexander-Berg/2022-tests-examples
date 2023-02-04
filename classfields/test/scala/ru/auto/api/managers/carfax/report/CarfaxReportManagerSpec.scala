package ru.auto.api.managers.carfax.report

import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.ReportNotBought
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.carfax.CarfaxManager.VinResolveResult
import ru.auto.api.managers.carfax.report.ReportLoader.{OfferReportLoadParams, ReportLoadParams}
import ru.auto.api.managers.carfax.report.model.ReportModel
import ru.auto.api.managers.carfax.{CarfaxManager, CarfaxWalletManager, PurchaseInfo, ReportOfferInfo}
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.vin.{HistoryReportPriceManager, VinResolutionWalletManager}
import ru.auto.api.model.ModelGenerators.PrivateUserRefGen
import ru.auto.api.model.RequestParams
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.vin.Common.IdentifierType
import ru.auto.api.vin.VinApiModel
import ru.auto.api.vin.VinApiModel.ReportParams
import ru.auto.api.vin.VinResolutionModel.ResolutionBilling
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

class CarfaxReportManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport {

  private val carfaxManager = mock[CarfaxManager]
  private val decayManager = mock[CarfaxDecayManager]
  private val walletManager = mock[CarfaxWalletManager]
  private val vinResolutionWalletManager = mock[VinResolutionWalletManager]
  private val historyReportPriceManager = mock[HistoryReportPriceManager]
  private val featureManager = mock[FeatureManager]
  private val fakeManager = mock[FakeManager]
  private val PriceString = "enriched with price"

  implicit val previewWithData: ReportModel[String] = new ReportModel[String] {

    override def enrichWithPrice(preview: String, optBilling: Option[ResolutionBilling]): String = {
      s"$preview $PriceString"
    }

    override def allowToBuyForOffer(data: String): Boolean = ???

    override def extractReportParams(preview: String): ReportParams = params

    override def allowToBuy(preview: String): Boolean = true

    override def enrichWithReloadParams(data: String, params: VinApiModel.ReloadParams): String = ???

    override def isBadLegalStatus(data: String): Boolean = ???

    override def extractQuality(data: String): Int = ???

    override def enrichWithPurchaseInfo(data: String, params: PurchaseInfo): String = data

    override def enrichWithOfferInfo(data: String, params: ReportOfferInfo): String = data

    override def enrichWithRequestInfo(data: String, isModerator: Boolean): String = data
  }

  private val params = ReportParams.newBuilder().build()

  val FreePreview = "free preview"
  val FakePreview = "fake preview"
  val PaidPreview = "paid preview"
  val PaidReport = "paid report"
  val FreePreviewWithPrice = s"$FreePreview $PriceString"
  val FakePreviewWithPrice = s"$FakePreview $PriceString"

  val reportLoader: ReportLoader[String] = new ReportLoader[String] {

    override def getReport(params: ReportLoadParams)(implicit request: Request): Future[String] = {
      if (params.purchaseInfo.isBought) Future.successful(PaidReport) else throw new ReportNotBought
    }

    override def getReportForOffer(
        params: ReportLoader.OfferReportLoadParams
    )(implicit request: Request): Future[String] = ???

    override def getFakeReport(vinOrLp: String)(implicit request: Request): Future[String] =
      Future.successful(FakePreview)

    override def getFakeReportForOffer(params: OfferReportLoadParams)(implicit request: Request): Future[String] = ???
  }

  before {
    when(carfaxManager.translateToVin(eq(licensePlate))(?))
      .thenReturn(Future.successful(VinResolveResult(vin, IdentifierType.VIN, Some(licensePlate))))
    when(carfaxManager.translateToVin(eq(vin))(?))
      .thenReturn(Future.successful(VinResolveResult(vin, IdentifierType.VIN, None)))
    when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(Feature("", _ => false))
  }

  after {
    reset(carfaxManager, walletManager, vinResolutionWalletManager, historyReportPriceManager, featureManager)
  }

  val vin = "Z8T4C5S19BM005269"
  val maskedVin = "Z8T4C5S1*BM*****9"
  val licensePlate = "A777AA777"

  val reportManager = new CarfaxReportManager[String](
    reportLoader,
    carfaxManager,
    decayManager,
    walletManager,
    vinResolutionWalletManager,
    historyReportPriceManager,
    featureManager,
    fakeManager
  )

  implicit private val trace: Traced = Traced.empty

  private def generatePrivateReq: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", antirobotDegradation = true))
    r.setApplication(Application.iosApp)
    r.setUser(PrivateUserRefGen.next)
    r.setTrace(trace)
    r
  }

  "CarfaxReportManager.getReport" should {
    "return fake report" when {
      "it's a robot request" in {
        when(fakeManager.shouldFakeRequest(?)).thenReturn(true)
        when(featureManager.fakeManagerEnabled).thenReturn(new Feature[Boolean] {
          override def name: String = "allow_carfax_reports_honey_pot_for_robots"
          override def value: Boolean = true
        })
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(new Feature[Boolean] {
          override def name: String = "allow_carfax_reports_honey_pot_for_robots"
          override def value: Boolean = true
        })
        when(historyReportPriceManager.getPrices(?)(?)).thenReturn(Future.successful(Nil))
        when(walletManager.getQuota(?)).thenReturn(Future.successful(1))

        val req = generatePrivateReq
        val response = reportManager.getReport(vin, decrementQuota = false, false)(req).await

        verify(vinResolutionWalletManager, never()).buy(?, ?, ?, ?)(?)
        verify(carfaxManager, never()).updateAll(eq(vin), ?, ?, ?)(?)
        verify(decayManager, never()).getReportPurchaseInfo(eq(vin))(?)
        response shouldBe FakePreviewWithPrice
      }
    }
    "throw ReportNotBought" when {
      "try to get not bought full report" in {
        when(decayManager.getReportPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))
        val req = generatePrivateReq
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
        intercept[ReportNotBought] {
          reportManager.getReport(licensePlate, decrementQuota = false, false)(req).await
        }
      }
      "purchase is failed" in {
        when(decayManager.getReportPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))
        when(vinResolutionWalletManager.buy(?, ?, ?, ?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))
        when(walletManager.getQuota(?)).thenReturn(Future.successful(1))
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

        val req = generatePrivateReq
        intercept[ReportNotBought] {
          reportManager.getReport(licensePlate, decrementQuota = true, false)(req).await
        }
      }
    }
    "return full report" when {
      "can show full" in {
        when(decayManager.getReportPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo(true, None)))
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

        val req = generatePrivateReq
        reportManager.getReport(licensePlate, decrementQuota = false, false)(req).await

        verify(vinResolutionWalletManager, never()).buy(?, ?, ?, ?)(?)
        verify(carfaxManager, never()).updateAll(eq(vin), ?, ?, ?)(?)
      }
      "successfully buy" in {
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

        when(decayManager.getReportPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))
        when(vinResolutionWalletManager.buy(?, ?, ?, ?)(?)).thenReturn(Future.successful(PurchaseInfo(true, None)))
        when(carfaxManager.updateAll(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(walletManager.getQuota(?)).thenReturn(Future.successful(1))

        val req = generatePrivateReq
        reportManager.getReport(licensePlate, decrementQuota = true, false)(req).await

        verify(vinResolutionWalletManager, times(1)).buy(?, ?, ?, ?)(?)
        verify(carfaxManager, times(1)).updateAll(eq(vin), ?, ?, ?)(?)
      }
    }

  }
}
