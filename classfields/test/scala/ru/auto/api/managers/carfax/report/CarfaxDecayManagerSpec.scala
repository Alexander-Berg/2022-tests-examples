package ru.auto.api.managers.carfax.report

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.carfax.{CarfaxWalletManager, PurchaseInfo}
import ru.auto.api.managers.vin.VinResolutionWalletManager
import ru.auto.api.model.ModelGenerators.{AnonymousUserRefGen, CarsOfferGen, ModeratorUserGrantsGen, PrivateUserRefGen}
import ru.auto.api.model.bunker.carfax.resellers.ResellersWithFreeReportAccess
import ru.auto.api.model.{RequestParams, UserRef}
import ru.auto.api.testkit.TestData
import ru.auto.api.util.RequestImpl
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.passport.model.api.ApiModel.SessionResult
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

class CarfaxDecayManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with AsyncTasksSupport {
  private val walletManager = mock[CarfaxWalletManager]
  private val vinResolutionWalletManager = mock[VinResolutionWalletManager]
  private val freeReportsFeature = mock[Feature[Boolean]]
  private val appsFeaturesManager = mock[FeatureManager]
  when(appsFeaturesManager.showReportForResellersForFree).thenReturn(freeReportsFeature)
  private val tree = TestData.tree
  private val user = PrivateUserRefGen.next
  private val resellers = ResellersWithFreeReportAccess(Set(user.uid))

  private val decayManager =
    new CarfaxDecayManager(walletManager, vinResolutionWalletManager, appsFeaturesManager, tree, resellers)

  val vin = "Z8T4C5S19BM005269"

  implicit private val trace: Traced = Traced.empty

  private def generateOffer(mark: String, geoId: Long, user: String) = {
    val builder = CarsOfferGen.next.toBuilder
    builder.setUserRef(user)
    builder.getSellerBuilder.getLocationBuilder.setGeobaseId(geoId)
    builder.getCarInfoBuilder.setMark(mark)
    builder.getDocumentsBuilder.setVin(vin)
    builder.build()
  }

  private def generateAnonymReq: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.desktop)
    r.setUser(AnonymousUserRefGen.next)
    r.setTrace(trace)
    r
  }

  private def generatePrivateReq(isModerator: Boolean = true,
                                 isReseller: Boolean,
                                 user: UserRef = user): RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r.setUser(user)
    r.setTrace(trace)
    if (isModerator) {
      r.setSession(moderatorSession)
    }
    if (isReseller) {
      r.setSession(resellerSession)
    }
    r
  }

  private def moderatorSession: SessionResult = {
    SessionResult.newBuilder().setGrants(ModeratorUserGrantsGen.next).build()
  }

  private def resellerSession: SessionResult = {
    val builder = SessionResult.newBuilder()
    builder.getUserBuilder.getModerationStatusBuilder.setReseller(true)
    builder.build()
  }

  "CarfaxDecayManager.canShowFullReport" should {
    "return true" when {
      "user is moderator" in {
        val req = generatePrivateReq(isModerator = true, isReseller = false)
        when(walletManager.getPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))

        val result = decayManager.getReportPurchaseInfo(vin)(req).await
        result shouldBe PurchaseInfo(true, None)
      }
      "already paid" in {
        val req = generatePrivateReq(isModerator = true, isReseller = false)
        when(walletManager.getPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo(true, None)))

        val result = decayManager.getReportPurchaseInfo(vin)(req).await
        result shouldBe PurchaseInfo(true, None)
      }
      "example free vin" in {
        val req = generatePrivateReq(isModerator = true, isReseller = false)

        val result = decayManager.getReportPurchaseInfo(CarfaxDecayManager.ExampleVin)(req).await
        result shouldBe PurchaseInfo(true, None)
      }
      "can show for free" in {
        val req = generatePrivateReq(isModerator = false, isReseller = true)
        val offer = generateOffer("BMW", 213L, req.user.userRef.toString)
        when(freeReportsFeature.value).thenReturn(true)
        when(walletManager.getPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))

        val result = decayManager.getReportPurchaseInfoForOffer(offer)(req).await
        result shouldBe PurchaseInfo(true, None)
      }
    }
    "return false" when {
      "not registered user" in {
        val req = generateAnonymReq

        val result = decayManager.getReportPurchaseInfo(vin)(req).await
        result shouldBe PurchaseInfo.NotBought
      }
      "not bought report" in {
        val req = generatePrivateReq(false, isReseller = false)
        when(walletManager.getPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))

        val result = decayManager.getReportPurchaseInfo(vin)(req).await
        result shouldBe PurchaseInfo.NotBought
      }
      "can't show for free because of free reports feature disabled" in {
        val req = generatePrivateReq(isModerator = false, isReseller = true)
        val offer = generateOffer("BMW", 213L, req.user.userRef.toString)
        when(freeReportsFeature.value).thenReturn(false)
        when(vinResolutionWalletManager.isVinResolutionAlreadyBought(?)(?))
          .thenReturn(Future.successful(PurchaseInfo.NotBought))
        when(walletManager.getPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))

        val result = decayManager.getReportPurchaseInfoForOffer(offer)(req).await
        result shouldBe PurchaseInfo.NotBought
      }
      "can't show for free because of user is not reseller" in {
        val req = generatePrivateReq(isModerator = false, isReseller = false, PrivateUserRefGen.next)
        val offer = generateOffer("BMW", 213L, req.user.userRef.toString)
        when(freeReportsFeature.value).thenReturn(true)
        when(vinResolutionWalletManager.isVinResolutionAlreadyBought(?)(?))
          .thenReturn(Future.successful(PurchaseInfo.NotBought))
        when(walletManager.getPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))

        val result = decayManager.getReportPurchaseInfoForOffer(offer)(req).await
        result shouldBe PurchaseInfo.NotBought
      }
      "can't show for free because of user is not offer owner" in {
        val req = generatePrivateReq(isModerator = false, isReseller = true)
        val offer = generateOffer("BMW", 213L, "user:123")
        when(freeReportsFeature.value).thenReturn(true)
        when(vinResolutionWalletManager.isVinResolutionAlreadyBought(?)(?))
          .thenReturn(Future.successful(PurchaseInfo.NotBought))
        when(walletManager.getPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))

        val result = decayManager.getReportPurchaseInfoForOffer(offer)(req).await
        result shouldBe PurchaseInfo.NotBought
      }
      "can't show for free because of mark is not suitable" in {
        val req = generatePrivateReq(isModerator = false, isReseller = true)
        val offer = generateOffer("VAZ", 213L, req.user.userRef.toString)
        when(freeReportsFeature.value).thenReturn(true)
        when(vinResolutionWalletManager.isVinResolutionAlreadyBought(?)(?))
          .thenReturn(Future.successful(PurchaseInfo.NotBought))
        when(walletManager.getPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))

        val result = decayManager.getReportPurchaseInfoForOffer(offer)(req).await
        result shouldBe PurchaseInfo.NotBought
      }
      "can't show for free because of region is not suitable" in {
        val req = generatePrivateReq(isModerator = false, isReseller = true)
        val offer = generateOffer("BMW", 977L, req.user.userRef.toString)
        when(freeReportsFeature.value).thenReturn(true)
        when(vinResolutionWalletManager.isVinResolutionAlreadyBought(?)(?))
          .thenReturn(Future.successful(PurchaseInfo.NotBought))
        when(walletManager.getPurchaseInfo(?)(?)).thenReturn(Future.successful(PurchaseInfo.NotBought))

        val result = decayManager.getReportPurchaseInfoForOffer(offer)(req).await
        result shouldBe PurchaseInfo.NotBought
      }
    }
  }
}
