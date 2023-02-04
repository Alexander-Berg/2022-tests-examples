package ru.auto.api.managers.vin

import org.mockito.Mockito.{reset, verify}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel._
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.geo.Tree
import ru.auto.api.managers.carfax.{CarfaxWalletManager, PurchaseInfo}
import ru.auto.api.managers.decay.DecayManager
import ru.auto.api.managers.enrich.EnrichManager
import ru.auto.api.managers.features.AppsFeaturesManager
import ru.auto.api.managers.offers.OfferLoader
import ru.auto.api.managers.vin.VinResolutionManager._
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.services.carfax.CarfaxClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.TimeUtils.TimeProvider
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.vin.VinResolutionModel.VinIndexResolution
import ru.auto.api.vin.{ResponseModel, VinResolutionEnums}
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import java.time.{Instant, ZoneId}

class VinResolutionManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport {

  val offerLoader: OfferLoader = mock[OfferLoader]
  val carfaxClient: CarfaxClient = mock[CarfaxClient]
  val appsFeaturesManager: AppsFeaturesManager = mock[AppsFeaturesManager]
  val enrichManager: EnrichManager = mock[EnrichManager]
  val vosClient: VosClient = mock[VosClient]
  val timeProvider: TimeProvider = mock[TimeProvider]
  val vinResolutionWalletManager: VinResolutionWalletManager = mock[VinResolutionWalletManager]
  val carfaxWalletManager: CarfaxWalletManager = mock[CarfaxWalletManager]
  val historyReportPriceManager: HistoryReportPriceManager = mock[HistoryReportPriceManager]
  val featureManager: FeatureManager = mock[FeatureManager]
  val reloadTime = 1538394827565L
  when(timeProvider.now()).thenReturn(reloadTime)
  when(timeProvider.currentLocalDate()).thenReturn {
    Instant.ofEpochMilli(reloadTime).atZone(ZoneId.of("Europe/Moscow")).toLocalDate
  }
  when(timeProvider.currentLocalDateTime()).thenReturn {
    Instant.ofEpochMilli(reloadTime).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime
  }

  val decayManager: DecayManager =
    new DecayManager(mock[PassportClient], vinResolutionWalletManager, mock[Tree], featureManager)

  val vinResolutionManager = new VinResolutionManager(
    appsFeaturesManager,
    offerLoader,
    carfaxClient,
    vosClient,
    timeProvider
  )

  implicit private val trace: Traced = Traced.empty

  private def generateReq: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.web)
    r.setUser(PersonalUserRefGen.next)
    r.setTrace(trace)
    r
  }

  private def generateUserReq(u: UserRef): RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setUser(u)
    r.setApplication(Application.web)
    r.setTrace(trace)
    r
  }

  val dealerWithUserRequestGen: Gen[Request] = for {
    token <- TestTokenGen
    deviceUid <- DeviceUidGen
    sessionResult <- DealerSessionResultGen
  } yield {
    val request = new RequestImpl
    request.setTrace(Traced.empty)
    request.setRequestParams(RequestParams.construct("1.1.1.1"))
    request.setToken(token)
    request.setNewDeviceUid(deviceUid)
    val clientId = sessionResult.getUser.getProfile.getClientId.toLong
    val userId = sessionResult.getUser.getId.toLong
    request.setUser(UserRef.user(userId))
    request.setDealer(AutoruDealer(clientId))
    TokenServiceImpl.getStaticApplication(token).foreach(request.setApplication)
    request
  }

  implicit private val request: RequestImpl = generateReq

  before {
    reset(offerLoader, carfaxClient, appsFeaturesManager)
    when(historyReportPriceManager.getPrices(?, ?)(?)).thenReturnF(Nil)
    when(vinResolutionWalletManager.buy(?, ?, ?, ?)(?)).thenReturnF(PurchaseInfo.NotBought)
  }

  "test by_user passed if owner request resolution" in {
    implicit val req = generateUserReq(PrivateUserRefGen.next)
    val offer = OfferGen.next.updated {
      _.setUserRef(req.user.userRef.toPlain)
    }
    when(appsFeaturesManager.isExtendedLegalPurityGroupSupport(?)).thenReturnF(false)
    val formats = vinResolutionManager.getLegalPurityGroupFormats(offer, Set())(req).await
    List(BY_OWNER, LEGACY_LEGAL_PURITY_GROUP).forall(formats.contains) shouldBe true
  }

  "test by_user passed if owner request resolution with extended legal purity group" in {
    implicit val req = generateUserReq(PrivateUserRefGen.next)
    val offer = OfferGen.next.updated {
      _.setUserRef(req.user.userRef.toPlain)
    }
    when(appsFeaturesManager.isExtendedLegalPurityGroupSupport(?)).thenReturnF(true)
    val formats = vinResolutionManager.getLegalPurityGroupFormats(offer, Set())(req).await
    List(BY_OWNER).forall(formats.contains) shouldBe true
  }

  "test nothing passed if no owner request resolution " in {
    implicit val req = generateUserReq(PrivateUserRefGen.next)
    when(appsFeaturesManager.isExtendedLegalPurityGroupSupport(?)).thenReturnF(true)
    val formats =
      vinResolutionManager.getLegalPurityGroupFormats(OfferGen.next, Set(BY_OWNER))(req).await
    formats shouldBe Set()
  }

  "should reload vin resolution by vin" in {
    implicit val req = generateUserReq(PrivateUserRefGen.next)
    val owner = req.user.userRef
    val vin = "Z8T4C5S19BM005269"

    val offer = CarsOfferGen.next.updated {
      _.setSection(Section.USED)
        .setStatus(OfferStatus.ACTIVE)
        .setUserRef(owner.toPlain)
        .getDocumentsBuilder
        .setVin(vin)

    }

    val category = offer.category
    val builder = VinIndexResolution.newBuilder()
    builder.setVin(vin)
    builder
      .addEntriesBuilder()
      .setPart(VinResolutionEnums.ResolutionPart.RP_LEGAL_GROUP)
      .setStatus(VinResolutionEnums.Status.ERROR)
    val resp = builder.build()
    when(carfaxClient.getResolutionByVin(?, ?)(?)).thenReturnF(resp)
    when(vosClient.getVinResolutionReloadTime(?, ?, ?)(?)).thenReturnF(None)
    when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
    when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)

    when(vinResolutionWalletManager.isVinResolutionAlreadyBought(?)(?)).thenReturnF(PurchaseInfo.NotBought)
    when(appsFeaturesManager.isExtendedLegalPurityGroupSupport(?)).thenReturnF(false)
    when(carfaxWalletManager.getQuota(?)).thenReturnF(0)
    when(vosClient.updateVinResolutionReloadTime(?, ?, ?)(?)).thenReturnF(())
    when(carfaxClient.reloadVinResolution(?, ?, ?)(?)).thenReturnF(())

    vinResolutionManager.reloadVinResolution(category, offer.id)(req).await

    verify(vosClient).updateVinResolutionReloadTime(category, owner.asPrivate, offer.id)(req)
  }

  "should reload vin resolution by license plate" in {
    implicit val req = generateUserReq(PrivateUserRefGen.next)
    val owner = req.user.userRef
    val lp = "A777AA111"

    val offer = CarsOfferGen.next.updated {
      _.setSection(Section.USED)
        .setStatus(OfferStatus.ACTIVE)
        .setUserRef(owner.toPlain)
        .getDocumentsBuilder
        .setLicensePlate(lp)

    }

    val category = offer.category
    val builder = VinIndexResolution.newBuilder()
    builder
      .addEntriesBuilder()
      .setPart(VinResolutionEnums.ResolutionPart.RP_LEGAL_GROUP)
      .setStatus(VinResolutionEnums.Status.ERROR)
    val resp = builder.build()
    val resolveVinResponse = ResponseModel.VinResponse
      .newBuilder()
      .setVin(VinGenerator.next)
      .build()
    when(carfaxClient.getResolutionByLicensePlate(?, ?)(?)).thenReturnF(resp)
    when(vosClient.getVinResolutionReloadTime(?, ?, ?)(?)).thenReturnF(None)
    when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
    when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)

    when(vinResolutionWalletManager.isVinResolutionAlreadyBought(?)(?)).thenReturnF(PurchaseInfo.NotBought)
    when(appsFeaturesManager.isExtendedLegalPurityGroupSupport(?)).thenReturnF(false)
    when(carfaxWalletManager.getQuota(?)).thenReturnF(0)
    when(vosClient.updateVinResolutionReloadTime(?, ?, ?)(?)).thenReturnF(())
    when(carfaxClient.resolveVin(?)(?)).thenReturnF(resolveVinResponse)
    when(carfaxClient.reloadVinResolution(?, ?, ?)(?)).thenReturnF(())

    vinResolutionManager.reloadVinResolution(category, offer.id)(req).await

    verify(vosClient).updateVinResolutionReloadTime(category, owner.asPrivate, offer.id)(req)
  }

}
