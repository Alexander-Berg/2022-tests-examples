package ru.auto.api.managers.carfax.offer

import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.{Offer, OfferStatus, PaidServicePrice, Section}
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.{OfferNotFoundException, VinResolutionNotFound}
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.carfax.CarfaxManager.VinResolveResult
import ru.auto.api.managers.carfax._
import ru.auto.api.managers.carfax.report.ReportLoader.{OfferReportLoadParams, ReportLoadParams}
import ru.auto.api.managers.carfax.report.model.ReportModel
import ru.auto.api.managers.carfax.report.{CarfaxDecayManager, ReportLoader}
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.vin.{HistoryReportPriceManager, VinResolutionWalletManager}
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.ModelGenerators.{CarsOfferGen, NonCarsOfferGen, PrivateUserRefGen}
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.{AutoruUser, CategorySelector, OfferID, RequestParams}
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.TimeUtils.DefaultTimeProvider
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.vin.Common.IdentifierType
import ru.auto.api.vin.VinApiModel.{ReloadParams, ReportParams}
import ru.auto.api.vin.VinResolutionModel.ResolutionBilling
import ru.auto.api.vin.{VinApiModel, VinResolutionEnums}
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

class CarfaxOfferReportManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport {

  case class TestReportFormat(isPaid: Boolean,
                              hasPrice: Boolean,
                              allowToBuy: Boolean,
                              badLegalStatus: Boolean,
                              reloadParams: ReloadParams,
                              purchaseTs: Option[Long],
                              isFake: Boolean = false,
                              isFavorite: Boolean = false)

  val sellAllReports: Feature[Boolean] = mock[Feature[Boolean]]
  when(sellAllReports.value).thenReturn(false)
  private val trueFeature = Feature("", _ => true)

  private val carfaxManager = mock[CarfaxManager]
  private val decayManager = mock[CarfaxDecayManager]
  private val walletManager = mock[CarfaxWalletManager]
  private val vinResolutionWalletManager = mock[VinResolutionWalletManager]
  private val historyReportPriceManager = mock[HistoryReportPriceManager]
  private val featureManager = mock[FeatureManager]
  private val fakeManager = mock[FakeManager]
  private val favoritesManager = mock[FavoriteManagerWrapper]

  val carfaxOfferLoader: CarfaxOfferLoader = mock[CarfaxOfferLoader]
  val vosClient: VosClient = mock[VosClient]

  implicit val previewWithData: ReportModel[TestReportFormat] = new ReportModel[TestReportFormat] {

    override def enrichWithPrice(preview: TestReportFormat, optBilling: Option[ResolutionBilling]): TestReportFormat = {
      optBilling match {
        case Some(_) => preview.copy(hasPrice = true)
        case _ => preview
      }
    }

    override def extractReportParams(preview: TestReportFormat): ReportParams = params

    override def allowToBuy(preview: TestReportFormat): Boolean = ???

    override def enrichWithReloadParams(data: TestReportFormat, params: VinApiModel.ReloadParams): TestReportFormat =
      data.copy(reloadParams = params)

    override def enrichWithPurchaseInfo(data: TestReportFormat, params: PurchaseInfo): TestReportFormat = {
      data.copy(purchaseTs = params.optPurchaseTimestamp)
    }

    override def isBadLegalStatus(data: TestReportFormat): Boolean = data.badLegalStatus

    override def extractQuality(data: TestReportFormat): Int = 1

    override def allowToBuyForOffer(data: TestReportFormat): Boolean = data.allowToBuy

    override def enrichWithOfferInfo(data: TestReportFormat, params: ReportOfferInfo): TestReportFormat = {
      data.copy(isFavorite = params.isFavourite)
    }

    override def enrichWithRequestInfo(data: TestReportFormat, isModerator: Boolean): TestReportFormat = data
  }

  private val params = ReportParams.newBuilder().build()
  private val price = PaidServicePrice.newBuilder().build()

  var FreePreview = TestReportFormat(
    isPaid = false,
    hasPrice = false,
    allowToBuy = true,
    badLegalStatus = false,
    reloadParams = ReloadParams.newBuilder().build(),
    None
  )

  var PaidPreview = TestReportFormat(
    isPaid = true,
    hasPrice = false,
    allowToBuy = false,
    badLegalStatus = false,
    reloadParams = ReloadParams.newBuilder().build(),
    Some(123L)
  )

  var PaidReport = TestReportFormat(
    isPaid = true,
    hasPrice = false,
    allowToBuy = false,
    badLegalStatus = false,
    reloadParams = ReloadParams.newBuilder().build(),
    Some(123L)
  )

  var FreeReport = TestReportFormat(
    isPaid = false,
    hasPrice = false,
    allowToBuy = true,
    badLegalStatus = false,
    reloadParams = ReloadParams.newBuilder().build(),
    None
  )

  val FakeReport = TestReportFormat(
    isPaid = false,
    hasPrice = false,
    allowToBuy = false,
    badLegalStatus = false,
    reloadParams = ReloadParams.newBuilder.build,
    purchaseTs = None,
    isFake = true
  )

  val vin = "Z8T4C5S19BM005269"
  val maskedVin = "Z8T4C5S1*BM*****9"
  val licensePlate = "A777AA777"
  val offerID = OfferID.parse("123-abc")

  val reportLoader: ReportLoader[TestReportFormat] =
    new ReportLoader[TestReportFormat] {
      override def getReport(params: ReportLoadParams)(implicit request: Request): Future[TestReportFormat] = ???

      override def getReportForOffer(
          params: ReportLoader.OfferReportLoadParams
      )(implicit request: Request): Future[TestReportFormat] = {
        if (params.purchaseInfo.isBought) Future.successful(PaidReport) else Future.successful(FreeReport)
      }

      override def getFakeReport(vinOrLp: String)(implicit request: Request): Future[TestReportFormat] = ???

      override def getFakeReportForOffer(params: OfferReportLoadParams)(
          implicit request: Request
      ): Future[TestReportFormat] = Future.successful(FakeReport)
    }

  before {
    when(carfaxManager.translateToVin(eq(licensePlate))(?))
      .thenReturn(Future.successful(VinResolveResult(vin, IdentifierType.LICENSE_PLATE, Some(licensePlate))))
    when(carfaxManager.translateToVin(eq(vin))(?))
      .thenReturn(Future.successful(VinResolveResult(vin, IdentifierType.VIN, None)))
    when(carfaxManager.updateAll(?, ?, ?, ?)(?)).thenReturnF(())

    FreePreview = TestReportFormat(
      isPaid = false,
      hasPrice = false,
      allowToBuy = true,
      badLegalStatus = false,
      reloadParams = ReloadParams.newBuilder().build(),
      None
    )
    PaidPreview = TestReportFormat(
      isPaid = true,
      hasPrice = false,
      allowToBuy = false,
      badLegalStatus = false,
      reloadParams = ReloadParams.newBuilder().build(),
      Some(123L)
    )
    PaidReport = TestReportFormat(
      isPaid = true,
      hasPrice = false,
      allowToBuy = false,
      badLegalStatus = false,
      reloadParams = ReloadParams.newBuilder().build(),
      Some(123L)
    )
    FreeReport = TestReportFormat(
      isPaid = false,
      hasPrice = false,
      allowToBuy = true,
      badLegalStatus = false,
      reloadParams = ReloadParams.newBuilder().build(),
      None
    )
  }

  after {
    reset(carfaxManager, walletManager, carfaxOfferLoader)
  }

  implicit private val trace: Traced = Traced.empty

  private def generatePrivateReq(user: AutoruUser = PrivateUserRefGen.next): RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r.setUser(user)
    r.setTrace(trace)
    r
  }

  val offerReportManager = new CarfaxOfferReportManager[TestReportFormat](
    reportLoader,
    carfaxManager,
    walletManager,
    vinResolutionWalletManager,
    historyReportPriceManager,
    decayManager,
    carfaxOfferLoader,
    vosClient,
    DefaultTimeProvider,
    featureManager,
    fakeManager,
    favoritesManager
  )

  "get report" should {
    "throw Offer not found" when {
      "offer does not exist" in {
        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenThrow(new OfferNotFoundException)
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
        implicit val req = generatePrivateReq()

        intercept[OfferNotFoundException] {
          offerReportManager.getReport(Cars, offerID, false, false).await
        }
        assert(1 + 1 == 2)
      }
    }
    "throw ResolutionNotFound" when {
      "category is not cars" in {
        implicit val req = generatePrivateReq()
        val offer = NonCarsOfferGen.next.updated {
          _.setSection(Section.USED)
            .setStatus(OfferStatus.ACTIVE)
        }.toBuilder
        val category = CategorySelector.from(offer.getCategory)
        offer.getDocumentsBuilder.setVin(vin).build()

        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer.build())
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

        intercept[VinResolutionNotFound] {
          offerReportManager.getReport(category, offerID, false, false).await
        }
      }
      "section is not used" in {
        implicit val req = generatePrivateReq()
        val offer = generateOfferWithAvailableResolution().updated {
          _.setSection(Section.NEW)
        }
        val category = CategorySelector.from(offer.getCategory)

        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer)
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

        intercept[VinResolutionNotFound] {
          offerReportManager.getReport(category, offerID, decrementQuota = false, false).await
        }
      }
      "offer without vin and status is ok" in {
        implicit val req = generatePrivateReq()
        val offerBuilder = generateOfferWithAvailableResolution()
        offerBuilder.getDocumentsBuilder.clearVin()
        val offer = offerBuilder.build()

        val category = CategorySelector.from(offer.getCategory)

        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer)
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

        intercept[VinResolutionNotFound] {
          offerReportManager.getReport(category, offerID, decrementQuota = false, false).await
        }
      }
    }
    "return paid report" when {
      "can show paid" in {
        implicit val req = generatePrivateReq()
        val offer = generateOfferWithAvailableResolution().build()
        val category = CategorySelector.from(offer.getCategory)

        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer)
        when(decayManager.getReportPurchaseInfoForOffer(?)(?)).thenReturnF(PurchaseInfo(true, None))
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
        when(favoritesManager.checkIfOffersAreInFavorite(?, ?, ?)(?))
          .thenReturn(Future.successful(Map(offer.id -> false)))

        val result = offerReportManager.getReport(category, offerID, decrementQuota = false, false).await
        result.isPaid shouldBe true
      }
      "successfully decrement quota" in {
        implicit val req = generatePrivateReq()
        val offer = generateOfferWithAvailableResolution().build()
        val category = CategorySelector.from(offer.getCategory)

        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer)
        when(decayManager.getReportPurchaseInfoForOffer(?)(?)).thenReturnF(PurchaseInfo.NotBought)
        when(walletManager.getQuota).thenReturnF(1)
        when(vinResolutionWalletManager.buy(?, ?, ?, ?)(?)).thenReturnF(PurchaseInfo(true, Some(123L)))
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

        val result = offerReportManager.getReport(category, offerID, decrementQuota = true, false).await
        result shouldBe PaidReport
        verify(carfaxManager, times(1)).updateAll(eq(offer.getDocuments.getVin), ?, ?, eq(Some(offer.getId)))(?)
      }
    }
    "return free report" when {
      "decrement quota failed" in {
        implicit val req = generatePrivateReq()
        val offer = generateOfferWithAvailableResolution().build()
        val category = CategorySelector.from(offer.getCategory)

        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer)
        when(decayManager.getReportPurchaseInfoForOffer(?)(?)).thenReturnF(PurchaseInfo.NotBought)
        when(walletManager.getQuota).thenReturnF(1)
        when(vinResolutionWalletManager.buy(?, ?, ?, ?)(?)).thenReturnF(PurchaseInfo.NotBought)
        when(historyReportPriceManager.getPrices(?, ?)(?)).thenReturnF(List(price))
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
        when(favoritesManager.checkIfOffersAreInFavorite(?, ?, ?)(?))
          .thenReturn(Future.successful(Map(offer.id -> false)))

        val result = offerReportManager.getReport(category, offerID, decrementQuota = true, false).await
        result.isPaid shouldBe false
        result.hasPrice shouldBe true
        verify(carfaxManager, never()).updateAll(eq(offer.getDocuments.getVin), ?, ?, ?)(?)
      }
      "zero quota" in {
        implicit val req = generatePrivateReq()
        val offer = generateOfferWithAvailableResolution().build()
        val category = CategorySelector.from(offer.getCategory)

        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer)
        when(decayManager.getReportPurchaseInfoForOffer(?)(?)).thenReturnF(PurchaseInfo.NotBought)
        when(walletManager.getQuota).thenReturnF(0)
        when(vinResolutionWalletManager.buy(?, ?, ?, ?)(?)).thenReturnF(PurchaseInfo.NotBought)
        when(historyReportPriceManager.getPrices(?, ?)(?)).thenReturnF(List(price))
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

        val result = offerReportManager.getReport(category, offerID, decrementQuota = true, false).await
        result.isPaid shouldBe false
        result.hasPrice shouldBe true
        verify(carfaxManager, never()).updateAll(eq(offer.getDocuments.getVin), ?, ?, ?)(?)
      }
      "decrement quota = false" in {
        implicit val req = generatePrivateReq()
        val offer = generateOfferWithAvailableResolution().build()
        val category = CategorySelector.from(offer.getCategory)

        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer)
        when(decayManager.getReportPurchaseInfoForOffer(?)(?)).thenReturnF(PurchaseInfo.NotBought)
        when(walletManager.getQuota).thenReturnF(1)
        when(vinResolutionWalletManager.buy(?, ?, eq(1), ?)(?)).thenReturnF(PurchaseInfo(true, None))
        when(historyReportPriceManager.getPrices(?, ?)(?)).thenReturnF(List(price))
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
        when(favoritesManager.checkIfOffersAreInFavorite(?, ?, ?)(?))
          .thenReturn(Future.successful(Map(offer.id -> false)))

        val result = offerReportManager.getReport(category, offerID, decrementQuota = false, false).await
        result.isPaid shouldBe false
        result.hasPrice shouldBe true
        verify(carfaxManager, never()).updateAll(eq(offer.getDocuments.getVin), ?, ?, ?)(?)
      }
    }
    "enrich with reload params" when {
      "owned by user and bad legal status" in {
        implicit val req = generatePrivateReq()
        val offer = generateOfferWithAvailableResolution().updated {
          _.setUserRef(req.user.ref.toString)
        }
        val category = CategorySelector.from(offer.getCategory)
        FreeReport = FreeReport.copy(badLegalStatus = true)

        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer)
        when(decayManager.getReportPurchaseInfoForOffer(?)(?)).thenReturnF(PurchaseInfo.NotBought)
        when(walletManager.getQuota).thenReturnF(1)
        when(vinResolutionWalletManager.buy(?, ?, eq(1), ?)(?)).thenReturnF(PurchaseInfo(true, None))
        when(historyReportPriceManager.getPrices(?, ?)(?)).thenReturnF(List(price))
        when(vosClient.getVinResolutionReloadTime(?, ?, ?)(?)).thenReturnF(Some(0))
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

        val result = offerReportManager.getReport(category, offerID, decrementQuota = false, false).await
        result.isPaid shouldBe false
        result.hasPrice shouldBe true
        result.reloadParams.getAllowReload shouldBe true
        verify(carfaxManager, never()).updateAll(eq(offer.getDocuments.getVin), ?, ?, ?)(?)
      }
    }

    "return fake report" when {
      "it's a robot request" in {
        implicit val req = generatePrivateReq()
        val offer = generateOfferWithAvailableResolution().build
        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer)
        when(fakeManager.fake(any[Offer]())(?)).thenReturnF(offer)
        when(walletManager.getQuota).thenReturnF(1)
        when(historyReportPriceManager.getPrices(?, ?)(?)).thenReturnF(List(price))
        when(decayManager.getReportPurchaseInfoForOffer(?)(?)).thenReturnF(PurchaseInfo.NotBought)
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(true)
        when(favoritesManager.checkIfOffersAreInFavorite(?, ?, ?)(?))
          .thenReturn(Future.successful(Map(offer.id -> false)))

        val report = offerReportManager.getReport(Cars, offerID, false, false).await

        verify(fakeManager, times(1)).fake(any[Offer]())(?)
        assert(report.isFake)
      }
    }

    "return is_favorite = true" when {
      "offer is added in user favorites" in {
        implicit val req = generatePrivateReq()
        val offer = generateOfferWithAvailableResolution().build
        val category = CategorySelector.from(offer.getCategory)

        when(carfaxOfferLoader.loadOffer(?, ?)(?)).thenReturnF(offer)
        when(decayManager.getReportPurchaseInfoForOffer(?)(?)).thenReturnF(PurchaseInfo(true, None))
        when(featureManager.allowCarfaxReportsHoneyPotForRobots).thenReturn(trueFeature)
        when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
        when(favoritesManager.checkIfOffersAreInFavorite(?, ?, ?)(?))
          .thenReturn(Future.successful(Map(offer.id -> true)))

        val result = offerReportManager.getReport(category, offerID, decrementQuota = false, false).await
        result.isFavorite shouldBe true
      }
    }
  }

  private def generateOfferWithAvailableResolution(): Offer.Builder = {
    val offer = CarsOfferGen.next.updated {
      _.setSection(Section.USED)
        .setStatus(OfferStatus.ACTIVE)
    }.toBuilder
    offer.getDocumentsBuilder.setVin(vin).build()
    offer.getDocumentsBuilder.setVinResolution(VinResolutionEnums.Status.OK)

    offer
  }

}
