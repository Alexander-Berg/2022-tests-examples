package ru.auto.api.managers.decay

import akka.actor.ActorSystem
import com.google.protobuf.util.Timestamps
import org.scalacheck.Gen
import org.scalatest.matchers.{MatchResult, Matcher}
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CommonModel.{PaidService, Photo}
import ru.auto.api.auth.{Application, Grant, Grants, NonStaticApplication}
import ru.auto.api.cert.CertModel.{BrandCertStatus, CertStatus}
import ru.auto.api.features.FeatureManager
import ru.auto.api.geo.Tree
import ru.auto.api.managers.carfax.{CarfaxDraftsManager, PurchaseInfo}
import ru.auto.api.managers.enrich.EnrichManager
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.managers.garage.GarageManager
import ru.auto.api.managers.offers.{DraftsManager, PhoneRedirectManager}
import ru.auto.api.managers.parsing.DraftHandleCrypto
import ru.auto.api.managers.price.{DealerPriceManager, UserPriceManager}
import ru.auto.api.managers.validation.ValidationManager
import ru.auto.api.managers.vin.VinResolutionWalletManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.services.billing.CabinetClient
import ru.auto.api.services.cabinet.CabinetApiClient
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.pushnoy.PushnoyClient
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.settings.SettingsClient
import ru.auto.api.services.uploader.UploaderClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.services.tradein.TradeInNotifierClient
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.TimeUtils.TimeProvider
import ru.auto.api.util.concurrency.FutureTimeoutHandler
import ru.auto.api.vin.VinResolutionModel._
import ru.auto.api.vin.{VinResolutionEnums, VinResolutionModel}
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.auto.panoramas.PanoramasModel.Panorama
import ru.yandex.passport.model.api.ApiModel.ApiToken
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.shark.proto.EcreditPrecondition
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Created by mcsim-gr on 21.09.17.
  */
class DecayManagerSpec extends BaseSpec with MockitoSupport {
  private val passportClient: PassportClient = mock[PassportClient]
  private val walletManger: VinResolutionWalletManager = mock[VinResolutionWalletManager]
  private val tree: Tree = mock[Tree]
  private val featureManager: FeatureManager = mock[FeatureManager]
  private val decayManager = new DecayManager(passportClient, walletManger, tree, featureManager)

  private val Ip: String = "0.0.0.0"

  implicit private val trace: Traced = Traced.empty

  implicit private val request: RequestImpl = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setApplication(Application.iosApp)
    r.setRequestParams(RequestParams.construct(Ip))
    r.setUser(ModelGenerators.PrivateUserRefGen.next)
    r
  }

  private val nonStaticRequest: RequestImpl = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setApplication(NonStaticApplication(ApiToken.newBuilder.setId("test").build()))
    r.setRequestParams(RequestParams.construct(Ip))
    r.setUser(ModelGenerators.PrivateUserRefGen.next)
    r
  }

  private val maxPoster = Application.external(
    "maxposter",
    RateLimit.PerApplication(300),
    Grants.Breadcrumbs,
    Grants.Catalog,
    Grants.PassportLogin,
    Grants.Search
  )

  private val comebackUser = Application.external(
    "comeback",
    RateLimit.PerApplication(300),
    Grants.Comeback
  )

  "DecayManager (for regular user)" should {

    when(walletManger.isVinResolutionAlreadyBought(?)(?)).thenReturnF(PurchaseInfo.NotBought)

    val featureShowTransportTax: Feature[Boolean] = mock[Feature[Boolean]]
    when(featureShowTransportTax.value).thenReturn(false)
    when(featureManager.showTransportTax).thenReturn(featureShowTransportTax)

    val featureHideExteriorPanoramas: Feature[Boolean] = mock[Feature[Boolean]]
    when(featureHideExteriorPanoramas.value).thenReturn(false)
    when(featureManager.hideExteriorPanoramas).thenReturn(featureHideExteriorPanoramas)

    val showScoringToAll: Feature[Boolean] = mock[Feature[Boolean]]
    when(showScoringToAll.value).thenReturn(false)
    when(featureManager.showScoringToAll).thenReturn(showScoringToAll)

    val enableShowInStories: Feature[Boolean] = mock[Feature[Boolean]]
    when(enableShowInStories.value).thenReturn(false)
    when(featureManager.enableShowInStories).thenReturn(enableShowInStories)

    mockHidePricesForDealersFeature(true)
    mockHideEcreditPrecondition(false)

    "clear recall info for active offer" in {
      val recallInfo = ModelGenerators.RecallInfoGen.next
      val offer = ModelGenerators.OfferGen.next.toBuilder
        .setStatus(OfferStatus.ACTIVE)
        .setRecallInfo(recallInfo)
        .build()

      val result = decayManager.decay(offer, DecayOptions.empty).futureValue
      result shouldNot haveRecallInfo
    }

    "keep recall info for inactive offer" in {
      val recallInfo = ModelGenerators.RecallInfoGen.next
      val offer = ModelGenerators.OfferGen.next.toBuilder
        .setStatus(OfferStatus.EXPIRED)
        .setRecallInfo(recallInfo)
        .build()

      val result = decayManager.decay(offer, DecayOptions.empty).futureValue
      result should haveRecallInfo
      result.getRecallInfo shouldBe recallInfo
    }

    "hide sensitive data from offer" in {
      val offer = ModelGenerators.OfferWithOnePhoneGen.next

      val result = decayManager.decay(offer, DecayOptions(sensitiveData = true)).futureValue

      result shouldNot haveRealPhone
    }

    "hide empty health in " in {
      val offer = ModelGenerators.OfferWithEmptyScoringGen.next
      val result = decayManager.decay(offer, DecayOptions(sensitiveData = true)).futureValue
      result.hasScore shouldBe false
    }

    "hide price history from offer with truncation flag" in {
      val offer = ModelGenerators.ActiveOfferGen.next

      val result = decayManager.decay(offer, DecayOptions(priceHistory = true, truncatePriceHistory = true)).futureValue

      result.getPriceHistoryCount shouldBe 1
    }

    "keep 2 elems of price history on offer" in {
      val tag = Gen.oneOf(ModelUtils.Tags.PRICE_CHANGE_TAGS.toSeq).next
      val priceHistory = ModelGenerators.PriceInfoGen.listOf(20)
      val offer = ModelGenerators.ActiveOfferGen.next.updated { b =>
        b.addTags(tag)
          .clearPriceHistory()
          .addAllPriceHistory(priceHistory.asJava)
      }

      val result = decayManager.decay(offer, DecayOptions(priceHistory = true, truncatePriceHistory = true)).futureValue

      result.getPriceHistoryCount shouldBe 2
    }

    "keep full price history on offer with no truncation flag" in {
      val tag = Gen.oneOf(ModelUtils.Tags.PRICE_CHANGE_TAGS.toSeq).next
      val offer = ModelGenerators.ActiveOfferGen.next.updated(b => b.addTags(tag))
      val result = decayManager.decay(offer, DecayOptions(priceHistory = true)).futureValue

      result.getPriceHistoryCount shouldBe offer.getPriceHistoryCount
    }

    "hide price history from offer with no truncation flag" in {
      val offer = ModelGenerators.ActiveOfferGen.next

      val result = decayManager.decay(offer, DecayOptions(priceHistory = true)).futureValue

      result.getPriceHistoryCount shouldBe 1
    }

    "keep full price history on offer for owner" in {
      val priceHistoryLength = Gen.choose(3, 10).next
      val priceHistory = Gen.listOfN(priceHistoryLength, ModelGenerators.PriceInfoGen).next
      val offer = ModelGenerators.OfferGen.next.updated { b =>
        b.setUserRef(request.user.ref.toPlain)
        b.clearPriceHistory()
        b.addAllPriceHistory(priceHistory.asJava)
      }

      val result = decayManager.decay(offer, DecayOptions(priceHistory = true)).futureValue

      result.getPriceHistoryCount shouldBe priceHistoryLength
    }

    "keep full price history for private user" in {
      val priceHistoryLength = Gen.choose(1, 10).next
      val priceHistory = Gen.listOfN(priceHistoryLength, ModelGenerators.PriceInfoGen).next
      val last = priceHistory.map(_.getCreateTimestamp).max
      val additionalInfo: ApiOfferModel.AdditionalInfo = AdditionalInfo.newBuilder
        .setFreshDate(Gen.choose(last, 100 * last).next)
        .build()
      val privateUser = PrivateUserRefGen.next
      val tag = Gen.oneOf(ModelUtils.Tags.PRICE_CHANGE_TAGS.toSeq).next
      val offer: Offer = ModelGenerators.ActiveOfferGen.next.updated { b =>
        b.addTags(tag)
        b.setUserRef(privateUser.toPlain)
        b.clearPriceHistory()
        b.addAllPriceHistory(priceHistory.asJava)
        b.setAdditionalInfo(additionalInfo)
      }
      val result = decayManager.decay(offer, DecayOptions(priceHistory = true)).futureValue
      offer.isDealer shouldBe false
      result.getPriceHistoryCount shouldBe priceHistoryLength
    }

    "clip price history before the fresh date for dealers (and keep the last price)" in {
      val priceHistoryLength = Gen.choose(1, 10).next
      val priceHistory = Gen.listOfN(priceHistoryLength, ModelGenerators.PriceInfoGen).next
      val first = priceHistory.map(_.getCreateTimestamp).min
      val last = priceHistory.map(_.getCreateTimestamp).max
      val freshDate = Gen.choose(first, last).next
      val additionalInfo: ApiOfferModel.AdditionalInfo = AdditionalInfo.newBuilder
        .setFreshDate(freshDate)
        .build()
      val dealer = DealerUserRefGen.next
      val tag = Gen.oneOf(ModelUtils.Tags.PRICE_CHANGE_TAGS.toSeq).next
      val offer: Offer = ModelGenerators.ActiveOfferGen.next.updated { b =>
        b.addTags(tag)
        b.setUserRef(dealer.toPlain)
        b.clearPriceHistory()
        b.addAllPriceHistory(priceHistory.asJava)
        b.setAdditionalInfo(additionalInfo)
      }
      val result = decayManager.decay(offer, DecayOptions(priceHistory = true)).futureValue
      offer.isDealer shouldBe true
      result.getPriceHistoryCount should be <= priceHistoryLength
      if (result.getPriceHistoryCount > 1) {
        result.getPriceHistoryCount shouldBe priceHistory.count(_.getCreateTimestamp >= freshDate)
      }
      result.getPriceHistoryList.asScala.map(_.getCreateTimestamp).contains(last) shouldBe true
    }

    "keep the last element of price history and clear search tags" in {
      val priceHistoryLength: Int = Gen.choose(1, 10).next
      val priceHistory = Gen.listOfN(priceHistoryLength, ModelGenerators.PriceInfoGen).next
      val last = priceHistory.map(_.getCreateTimestamp).max
      val additionalInfo: ApiOfferModel.AdditionalInfo = AdditionalInfo.newBuilder
        .setFreshDate(Gen.choose(last, 100 * last).next)
        .build()
      val dealer = DealerUserRefGen.next
      val tag = Gen.oneOf(ModelUtils.Tags.PRICE_CHANGE_TAGS.toSeq).next
      val offer: Offer = ModelGenerators.ActiveOfferGen.next.updated { b =>
        b.addTags(tag)
        b.setUserRef(dealer.toPlain)
        b.clearPriceHistory()
        b.addAllPriceHistory(priceHistory.asJava)
        b.setAdditionalInfo(additionalInfo)
      }
      val result = decayManager.decay(offer, DecayOptions(priceHistory = true)).futureValue
      offer.isDealer shouldBe true
      result.getPriceHistoryCount shouldBe 1
      result.hasPriceHistoryTags shouldBe false
    }

    for {
      shouldMaskVinAndLicensePlate <- Seq(None, Some(true))
    } s"hide Vin on offer if shouldMaskVinAndLicensePlate=$shouldMaskVinAndLicensePlate" in {
      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      val vin = "JN1BDAV36U0500708"
      offerBuilder.getDocumentsBuilder.setVin(vin)
      offerBuilder.getCertInfoBuilder.setVin(vin)
      offerBuilder.getCertInfoBuilder.setStatus(CertStatus.ACTIVE)
      offerBuilder.getBrandCertInfoBuilder.setVin(vin)
      offerBuilder.getBrandCertInfoBuilder.setCertStatus(BrandCertStatus.BRAND_CERT_ACTIVE)

      val result = decayManager
        .decay(
          offerBuilder.build(),
          DecayOptions.empty.copy(shouldMaskVinAndLicensePlate = shouldMaskVinAndLicensePlate)
        )
        .futureValue

      result.getDocuments.getVin shouldBe "JN1**************"
      result.getCertInfo.getVin shouldBe "JN1**************"
      result.getBrandCertInfo.getVin shouldBe "JN1**************"
    }

    for {
      (shouldMaskVinAndLicensePlate, hasGrant) <- Seq((None, true), (Some(true), true), (Some(false), false))
    } s"keep Vin on offer if shouldMaskVinAndLicensePlate=$shouldMaskVinAndLicensePlate and has grant KeepVinAndLp = $hasGrant" in {
      implicit val request: RequestImpl = {
        val grants = if (hasGrant) Application.iosApp.grants :+ Grants.KeepVinAndLp else Application.iosApp.grants
        createRequest(grants)
      }

      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      val vin = "JN1BDAV36U0500708"
      offerBuilder.getDocumentsBuilder.setVin(vin)
      offerBuilder.getCertInfoBuilder.setVin(vin)
      offerBuilder.getCertInfoBuilder.setStatus(CertStatus.ACTIVE)
      offerBuilder.getBrandCertInfoBuilder.setVin(vin)
      offerBuilder.getBrandCertInfoBuilder.setCertStatus(BrandCertStatus.BRAND_CERT_ACTIVE)

      val result = decayManager
        .decay(
          offerBuilder.build(),
          DecayOptions.empty.copy(shouldMaskVinAndLicensePlate = shouldMaskVinAndLicensePlate)
        )
        .futureValue

      result.getDocuments.getVin shouldBe vin
      result.getCertInfo.getVin shouldBe vin
      result.getBrandCertInfo.getVin shouldBe vin
    }

    for {
      shouldMaskVinAndLicensePlate <- Seq(None, Some(true))
    } s"hide short Vin on offer if shouldMaskVinAndLicensePlate=$shouldMaskVinAndLicensePlate" in {
      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      val vin = "E11603148"
      offerBuilder.getDocumentsBuilder.setVin(vin)
      offerBuilder.getCertInfoBuilder.setVin(vin)
      offerBuilder.getCertInfoBuilder.setStatus(CertStatus.ACTIVE)
      offerBuilder.getBrandCertInfoBuilder.setVin(vin)
      offerBuilder.getBrandCertInfoBuilder.setCertStatus(BrandCertStatus.BRAND_CERT_ACTIVE)

      val result = decayManager
        .decay(
          offerBuilder.build(),
          DecayOptions.empty.copy(shouldMaskVinAndLicensePlate = shouldMaskVinAndLicensePlate)
        )
        .futureValue

      result.getDocuments.getVin shouldBe "E11***********"
      result.getCertInfo.getVin shouldBe "E11***********"
      result.getBrandCertInfo.getVin shouldBe "E11***********"
    }

    for {
      (shouldMaskVinAndLicensePlate, hasGrant) <- Seq((None, true), (Some(true), true), (Some(false), false))
    } s"keep short Vin on offer if shouldMaskVinAndLicensePlate=$shouldMaskVinAndLicensePlate and has grant KeepVinAndLp = $hasGrant" in {
      implicit val request: RequestImpl = {
        val grants = if (hasGrant) Application.iosApp.grants :+ Grants.KeepVinAndLp else Application.iosApp.grants
        createRequest(grants)
      }

      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      val vin = "E11603148"
      offerBuilder.getDocumentsBuilder.setVin(vin)
      offerBuilder.getCertInfoBuilder.setVin(vin)
      offerBuilder.getCertInfoBuilder.setStatus(CertStatus.ACTIVE)
      offerBuilder.getBrandCertInfoBuilder.setVin(vin)
      offerBuilder.getBrandCertInfoBuilder.setCertStatus(BrandCertStatus.BRAND_CERT_ACTIVE)

      val result = decayManager
        .decay(
          offerBuilder.build(),
          DecayOptions.empty.copy(shouldMaskVinAndLicensePlate = shouldMaskVinAndLicensePlate)
        )
        .futureValue

      result.getDocuments.getVin shouldBe vin
      result.getCertInfo.getVin shouldBe vin
      result.getBrandCertInfo.getVin shouldBe vin
    }

    for {
      shouldMaskVinAndLicensePlate <- Seq(None, Some(true))
    } s"hide sts on offer if shouldMaskVinAndLicensePlate=$shouldMaskVinAndLicensePlate" in {
      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      offerBuilder.getDocumentsBuilder.setSts("xxx")

      val result = decayManager
        .decay(
          offerBuilder.build(),
          DecayOptions.empty.copy(shouldMaskVinAndLicensePlate = shouldMaskVinAndLicensePlate)
        )
        .futureValue

      result.getDocuments.getSts shouldBe ""
    }

    for {
      (shouldMaskVinAndLicensePlate, hasGrant) <- Seq((None, true), (Some(true), true), (Some(false), false))
    } s"keep sts on offer if shouldMaskVinAndLicensePlate=$shouldMaskVinAndLicensePlate and has grant KeepVinAndLp = $hasGrant" in {
      implicit val request: RequestImpl = {
        val grants = if (hasGrant) Application.iosApp.grants :+ Grants.KeepVinAndLp else Application.iosApp.grants
        createRequest(grants)
      }

      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      val sts = "xxx"
      offerBuilder.getDocumentsBuilder.setSts(sts)

      val result = decayManager
        .decay(
          offerBuilder.build(),
          DecayOptions.empty.copy(shouldMaskVinAndLicensePlate = shouldMaskVinAndLicensePlate)
        )
        .futureValue

      result.getDocuments.getSts shouldBe sts
    }

    "build some addresses" in {
      DecayManager.buildAddress("ул. Пушкина, д. Колотушкина", "Москва") shouldBe "ул. Пушкина, д. Колотушкина"
      DecayManager.buildAddress("Москва ул. Пушкина, д. Колотушкина", "Москва") shouldBe "ул. Пушкина, д. Колотушкина"
      DecayManager.buildAddress("Москва,ул. Пушкина, д. Колотушкина", "Москва") shouldBe "ул. Пушкина, д. Колотушкина"
      DecayManager.buildAddress("Москва, ул. Пушкина, д. Колотушкина", "Москва") shouldBe "ул. Пушкина, д. Колотушкина"
      DecayManager.buildAddress("Россия, Москва, ул. Пушкина, д. Колотушкина", "Москва") shouldBe "ул. Пушкина, д. Колотушкина"
      DecayManager.buildAddress("Москва, Москва, ул. Пушкина, д. Колотушкина", "Москва") shouldBe "ул. Пушкина, д. Колотушкина"
      DecayManager.buildAddress("Москва, Москва-Каланчёвская", "Москва") shouldBe "Москва-Каланчёвская"
    }

    "build address with only region" in {
      DecayManager.buildAddress("Москва", "Москва") shouldBe ""
    }

    "build address ignore case" in {
      DecayManager.buildAddress("МОСКВА ул. Пушкина, д. Колотушкина", "Москва") shouldBe "ул. Пушкина, д. Колотушкина"
      DecayManager.buildAddress("москва", "Москва") shouldBe ""
    }

    "hide owner info for maxposter" in {
      val maxPosterReq = new RequestImpl
      maxPosterReq.setApplication(maxPoster)
      maxPosterReq.setRequestParams(RequestParams.empty)
      maxPosterReq.setUser(ModelGenerators.PrivateUserRefGen.next)

      val builder = OfferGen.next.toBuilder
      builder.setUserRef("user:11111")
      builder.clearServices()
      builder.addServices(PaidService.newBuilder().setService("all_sale_fresh"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_color"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_toplist"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_special"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_premium"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_activate"))
      val offer = builder.build()

      val result = decayManager.decay(offer, DecayOptions(sensitiveData = true))(maxPosterReq).futureValue
      result.getUserRef shouldBe ""
      result.getPrivateSeller.getName shouldBe ""
      result.getPrivateSeller.getPhonesCount shouldBe 0
      result.getSalon.getPhonesCount shouldBe 0
      result.getSeller.getName shouldBe ""
      result.getSeller.getPhonesList.asScala.count(_.getPhone.nonEmpty) shouldBe 0
      result.getSeller.getPhonesList.asScala.count(_.getOriginal.nonEmpty) shouldBe 0
      result.getServicesCount shouldBe 5
      result.getServicesList.asScala.find(_.getService == "all_sale_activate") shouldBe None
    }

    "do not hide vin number in owner info for comeback listing requests" in {
      val comebackRequest = new RequestImpl
      comebackRequest.setApplication(comebackUser)
      comebackRequest.setRequestParams(RequestParams.empty)
      comebackRequest.setUser(ModelGenerators.PrivateUserRefGen.next)

      val builder = OfferGen.next.toBuilder
      builder.setUserRef("user:11111")
      builder.getDocumentsBuilder.setVin("VIN")
      val offer = builder.build()

      val result = decayManager.decay(offer, DecayOptions.ForComebackListing)(comebackRequest).futureValue
      result.getDocuments.getVin shouldBe "VIN"
    }

    "do not hide phones for comeback listing requests" in {
      val phone = "79999999999"

      val comebackRequest = new RequestImpl
      comebackRequest.setApplication(comebackUser)
      comebackRequest.setRequestParams(RequestParams.empty)
      comebackRequest.setUser(ModelGenerators.PrivateUserRefGen.next)

      val builder = ActiveOfferGen.next.toBuilder
      builder.setUserRef("user:11111")
      builder.getDocumentsBuilder.setVin("VIN")
      builder.getSellerBuilder.clearPhones().addPhones(Phone.newBuilder().setPhone(phone))
      val offer = builder.build()

      val result = decayManager.decay(offer, DecayOptions.ForComebackListing)(comebackRequest).futureValue
      result.getSeller.getPhones(0).getPhone shouldBe phone
    }

    "hide dealer fresh services for user" in {
      val req = new RequestImpl
      req.setApplication(maxPoster)
      req.setRequestParams(RequestParams.empty)
      req.setUser(ModelGenerators.PrivateUserRefGen.next)
      val builder = OfferGen.next.toBuilder
      builder.clearServices()
      builder.setUserRef("dealer:123")
      builder.addServices(PaidService.newBuilder().setService("all_sale_fresh"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_color"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_toplist"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_activate"))
      val offer = builder.build()

      val result = decayManager.decay(offer, DecayOptions(sensitiveData = true))(req).futureValue
      result.getServicesCount shouldBe 2
      result.getServicesList.asScala.find(_.getService == "all_sale_activate") shouldBe None
      result.getServicesList.asScala.find(_.getService == "all_sale_fresh") shouldBe None
    }

    "don't hide dealer fresh services for dealer" in {
      val req = new RequestImpl
      req.setApplication(maxPoster)
      req.setRequestParams(RequestParams.empty)
      req.setUser(ModelGenerators.DealerUserRefGen.next)
      req.setDealer(ModelGenerators.DealerUserRefGen.next)
      val builder = OfferGen.next.toBuilder
      builder.clearServices()
      builder.setUserRef("dealer:123")
      builder.addServices(PaidService.newBuilder().setService("all_sale_fresh"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_color"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_toplist"))
      builder.addServices(PaidService.newBuilder().setService("all_sale_activate"))
      val offer = builder.build()

      val result = decayManager.decay(offer, DecayOptions(sensitiveData = true))(req).futureValue
      result.getServicesCount shouldBe 3
      result.getServicesList.asScala.find(_.getService == "all_sale_activate") shouldBe None
    }

    "hide info in vin offers history" in {
      val resolution = VinResolutionModel.Resolution.newBuilder()
      val history = resolution.getOfferHistoryBuilder
      history.addOffers {
        VinResolutionModel.OldOffer
          .newBuilder()
          .setId("1134-1412")
          .setDateOfPlacement(Timestamps.fromMillis(141))
          .setDateOfPlacement(Timestamps.fromMillis(144))
          .setKmAge(1000)
          .setPriceRur(44)
          .setCondition(Condition.CONDITION_BROKEN)
          .setText("15 may 2018")
          .setSoldDateText("15 jule 2018")
          .setRegion(RegionInfoGen.next)
          .setSellerType("Частное лицо")
      }

      DecayManager.decayVinHistory(resolution)

      val decayed = resolution.getOfferHistory.getOffersList.get(0)
      decayed.getId.isEmpty shouldBe true
      decayed.getDateOfPlacement.getSeconds shouldBe 0
      decayed.getDateOfRemoval.getSeconds shouldBe 0
      decayed.getKmAge shouldBe 0
      decayed.getPriceRur shouldBe 0
      decayed.getText shouldBe "15 may 20**"
      decayed.getSoldDateText shouldBe "15 jule 20**"
      decayed.getSellerType shouldBe "*****"
      decayed.getRegion.getName shouldBe "*****"
      decayed.getRegion.getId shouldBe 0
    }

    "decay vin history block, hide status block" in {
      val b = VinResolutionModel.Block.newBuilder
        .setType(BlockType.TEXT_WITH_STATUS)
        .setStatus(VinResolutionEnums.Status.ERROR)

      val res = DecayManager.decayKmAgeBlock(b)
      res.isDefined shouldBe false
    }

    "decay vin history block , decay partner block" in {
      val b = VinResolutionModel.Block.newBuilder
        .setType(BlockType.PARTNER_BLOCK)
        .setValue("132423")
        .setName("21 апреля 2051")

      val res = DecayManager.decayKmAgeBlock(b)
      res.isDefined shouldBe true
      res.get.getValue shouldBe "**** км"
      res.get.getName shouldBe "21 апреля ****"
    }

    "decay vin history block with taxi" in {
      val b = VinResolutionModel.Block.newBuilder
        .setType(BlockType.PARTNER_BLOCK)
        .setValue(DecayManager.TaxiEmoji)
        .setName("21 апреля 2051")

      val res = DecayManager.decayKmAgeBlock(b)
      res.isDefined shouldBe true
      res.get.getValue shouldBe DecayManager.Dash
      res.get.getName shouldBe "21 апреля ****"
    }

    "decay vin history block , decay partner block with dash" in {
      val b = VinResolutionModel.Block.newBuilder
        .setType(BlockType.PARTNER_BLOCK)
        .setValue(DecayManager.Dash)
        .setName("апрель 2051")

      val res = DecayManager.decayKmAgeBlock(b)
      res.isDefined shouldBe true
      res.get.getValue shouldBe DecayManager.Dash
      res.get.getName shouldBe "апрель ****"
    }

    "decay vin history block, not decay partner block if current" in {
      val b = VinResolutionModel.Block.newBuilder
        .setType(BlockType.PARTNER_BLOCK)
        .setValue("132423")
        .setName("21 апреля 2051")
        .setValueHighlightedRed(true)

      b.getPartnerBlockBuilder.setIsCurrentOffer(true)

      val res = DecayManager.decayKmAgeBlock(b)
      res.isDefined shouldBe true
      res.get.getValue shouldBe "132423"
      res.get.getName shouldBe "21 апреля 2051"
      res.get.getValueHighlightedRed shouldBe false
    }

    "decay vin history block, decay vin in partner block if current" in {
      val b = VinResolutionModel.Block.newBuilder
        .setType(BlockType.PARTNER_BLOCK)
        .setValue("132423")
        .setName("21 апреля 2051")
        .setValueHighlightedRed(true)

      val vin = "3N1CN7AP9FL801107"

      b.getPartnerBlockBuilder
        .setIsCurrentOffer(true)
        .addItems {
          val b = VinResolutionModel.BlockItem.newBuilder
            .setType(BlockItemType.KEY_VALUE_ITEM)
          b.getKeyValueBlockItemBuilder.setKey("VIN").setValue(vin)
          b.build()
        }

      val res = DecayManager.decayKmAgeBlock(b)
      res.isDefined shouldBe true
      res.get.getValue shouldBe "132423"
      res.get.getName shouldBe "21 апреля 2051"
      res.get.getPartnerBlock.getItems(0).getKeyValueBlockItem.getValue shouldBe DecayManager.maskVin(vin)
    }

    "decay vin history block, decay licenseplate in partner block if current" in {
      val b = VinResolutionModel.Block.newBuilder
        .setType(BlockType.PARTNER_BLOCK)
        .setValue("132423")
        .setName("21 апреля 2051")
        .setValueHighlightedRed(true)

      val licensePlate = "А777АА777"

      b.getPartnerBlockBuilder
        .setIsCurrentOffer(true)
        .addItems {
          val b = VinResolutionModel.BlockItem.newBuilder
            .setType(BlockItemType.KEY_VALUE_ITEM)
          b.getKeyValueBlockItemBuilder.setKey("Госномер").setValue(licensePlate)
          b.build()
        }

      val res = DecayManager.decayKmAgeBlock(b)
      res.isDefined shouldBe true
      res.get.getValue shouldBe "132423"
      res.get.getName shouldBe "21 апреля 2051"
      res.get.getPartnerBlock.getItems(0).getKeyValueBlockItem.getValue shouldBe DecayManager.maskLicensePlate(
        licensePlate
      )
    }

    "decay vin history item , decay string item" in {
      val b = VinResolutionModel.BlockItem.newBuilder
        .setType(BlockItemType.STRING_ITEM)
      b.getStringBlockItemBuilder.setValue("132423").setIsBold(true)

      val res = DecayManager.decayItem(b)
      res.isDefined shouldBe false
    }

    "decay vin history item , decay key value item" in {
      val b = VinResolutionModel.BlockItem.newBuilder
        .setType(BlockItemType.KEY_VALUE_ITEM)
      b.getKeyValueBlockItemBuilder.setKey("132423").setValue("2134").setValueHighlightedRed(true)

      val res = DecayManager.decayItem(b)
      res.isDefined shouldBe true
      res.get.getKeyValueBlockItem.getValue shouldBe "****"
      res.get.getKeyValueBlockItem.getKey shouldBe "132423"
      res.get.getKeyValueBlockItem.getValueHighlightedRed shouldBe false
    }

    "decay vin history item , decay autoru offer item" in {
      val b = VinResolutionModel.BlockItem.newBuilder
        .setType(BlockItemType.AUTORU_OFFER_LINK_ITEM)
      b.getAutoruOfferLinkItemBuilder.setTitle("132423").setUrl("2134")

      val res = DecayManager.decayItem(b)
      res.isDefined shouldBe true
      res.get.getAutoruOfferLinkItem.getTitle shouldBe "132423"
      res.get.getAutoruOfferLinkItem.getUrl shouldBe ""
    }

    "decay vin history item , decay key value hidden item" in {
      val b = VinResolutionModel.BlockItem.newBuilder
        .setType(BlockItemType.KEY_VALUE_ITEM)
      b.getKeyValueBlockItemBuilder
        .setKey("132423")
        .setValue("2134")
        .setHideItem(true)

      val res = DecayManager.decayItem(b)
      res.isDefined shouldBe false
    }

    "mask licence plate" in {
      DecayManager.maskLicensePlate("А567ТТ89") shouldBe "******|89"
      DecayManager.maskLicensePlate("А567ТТ189") shouldBe "******|189"
      DecayManager.maskLicensePlate("о567ах189") shouldBe "******|189"
      DecayManager.maskLicensePlate("АА56789") shouldBe "*****|89"
      DecayManager.maskLicensePlate("АА567189") shouldBe "*****|189"
      DecayManager.maskLicensePlate("кк567189") shouldBe "*****|189"
      DecayManager.maskLicensePlate("ММ55АА") shouldBe ""
    }

    "not decay vin in a draft if requested by anonym (not the owner)" in {
      request.setToken(TokenServiceImpl.iosApp)

      val timeProvider: TimeProvider = mock[TimeProvider]
      val vosClient: VosClient = mock[VosClient]
      val geobaseClient: GeobaseClient = mock[GeobaseClient]
      val dealerPriceManager: DealerPriceManager = mock[DealerPriceManager]
      val userPriceManager: UserPriceManager = mock[UserPriceManager]
      val uploaderClient: UploaderClient = mock[UploaderClient]
      val cabinetClient: CabinetClient = mock[CabinetClient]
      val cabinetApiClient: CabinetApiClient = mock[CabinetApiClient]
      val settingsClient: SettingsClient = mock[SettingsClient]
      val passportClient: PassportClient = mock[PassportClient]
      val tradeInNotifierClient: TradeInNotifierClient = mock[TradeInNotifierClient]
      val tree: Tree = mock[Tree]
      val enrichManager: EnrichManager = mock[EnrichManager]
      val statEventsManager: StatEventsManager = mock[StatEventsManager]
      val redirectManager: PhoneRedirectManager = mock[PhoneRedirectManager]
      val validationManager: ValidationManager = mock[ValidationManager]
      val pushnoyClient: PushnoyClient = mock[PushnoyClient]
      val carfaxDraftsManager: CarfaxDraftsManager = mock[CarfaxDraftsManager]
      val garageManager: GarageManager = mock[GarageManager]
      val draftHandleCrypto: DraftHandleCrypto = mock[DraftHandleCrypto]
      val brokerClient: BrokerClient = mock[BrokerClient]
      val salesmanClient: SalesmanClient = mock[SalesmanClient]
      implicit val system: ActorSystem = ActorSystem("decay-manager-spec-system")
      val futureTimeoutHandler: FutureTimeoutHandler = new FutureTimeoutHandler()

      val draftsManager = new DraftsManager(
        timeProvider,
        vosClient,
        dealerPriceManager,
        userPriceManager,
        uploaderClient,
        cabinetClient,
        cabinetApiClient,
        settingsClient,
        passportClient,
        tradeInNotifierClient,
        statEventsManager,
        redirectManager,
        validationManager,
        geobaseClient,
        tree,
        "http://localhost:2600",
        decayManager,
        enrichManager,
        None,
        featureManager,
        carfaxDraftsManager,
        garageManager,
        futureTimeoutHandler,
        draftHandleCrypto,
        brokerClient,
        "https://test.avto.ru",
        salesmanClient
      )

      val vin = "JN1BDAV36U0500708"
      val draft = OfferGen.next
      val offer = draft.updated(_.getDocumentsBuilder.setVin(vin))
      val draftId = OfferIDGen.next
      val user = request.user.registeredRef
      val deviceInfo = DeviceInfoGen.next.copy(appVersion = "8.6.0.0000")

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(statEventsManager.logDraftUpdateEvent(?, ?, ?)(?)).thenReturn(Future.unit)
      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturn(Future.successful(false))
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val res = draftsManager
        .updateDraft(
          CategorySelector.Cars,
          user,
          draftId,
          draft,
          isAddForm = true,
          canChangePanorama = true,
          canDisableChats = true
        )
        .await
      res.getOffer.getDocuments.getVin shouldBe vin
    }

    "decay panorama" in {
      when(featureHideExteriorPanoramas.value).thenReturn(true)

      val p = Panorama
        .newBuilder()
        .setId("xxx-yyy-zzz")
        .build()

      val offer = {
        val builder = OfferGen.next.toBuilder

        builder.getStateBuilder
          .setPanoramaAutoru(p)

        builder.build()
      }

      val decayOffer = decayManager.decay(offer, DecayOptions.full).futureValue

      decayOffer.getState.hasPanoramaAutoru shouldBe false

    }

    "not decay panorama" in {
      when(featureHideExteriorPanoramas.value).thenReturn(false)

      val p = Panorama
        .newBuilder()
        .setId("xxx-yyy-zzz")
        .build()
      val offer = {
        val builder = OfferGen.next.toBuilder
        builder.getStateBuilder
          .setPanoramaAutoru(p)

        builder.build()
      }

      val decayOffer = decayManager.decay(offer, DecayOptions.full).futureValue

      decayOffer.getState.hasPanoramaAutoru shouldBe true
      decayOffer.getState.getPanoramaAutoru.getId shouldBe p.getId
    }

    "booking for owner" in {
      val req: RequestImpl = {
        val r = new RequestImpl
        r.setApplication(Application.desktop)
        r.setRequestParams(RequestParams.construct(Ip))
        r.setUser(ModelGenerators.PrivateUserRefGen.next)
        r
      }
      val offer = ModelGenerators.OfferGen.next.updated { builder =>
        builder.getAdditionalInfoBuilder.getBookingBuilder.getStateBuilder.getBookedBuilder
          .setUserRef(req.user.ref.toPlain)
          .setBookingId(ReadableStringGen.next)
      }
      val actual = decayManager.decay(offer, DecayOptions.ForFavorites)(req).futureValue
      val actualBooked = actual.getAdditionalInfo.getBooking.getState.getBooked
      actualBooked.getItsMe shouldBe true
      actualBooked.getUserRef.isEmpty shouldBe true
      actualBooked.getBookingId.nonEmpty shouldBe true
    }

    "booking for not owner" in {
      val req: RequestImpl = {
        val r = new RequestImpl
        r.setApplication(Application.desktop)
        r.setRequestParams(RequestParams.construct(Ip))
        r
      }
      val offer = ModelGenerators.OfferGen.next.updated { builder =>
        builder.getAdditionalInfoBuilder.getBookingBuilder.getStateBuilder.getBookedBuilder
          .setUserRef(ModelGenerators.PrivateUserRefGen.next.toPlain)
          .setBookingId(ReadableStringGen.next)
      }
      val actual = decayManager.decay(offer, DecayOptions.ForFavorites)(req).futureValue
      val actualBooked = actual.getAdditionalInfo.getBooking.getState.getBooked
      actualBooked.getItsMe shouldBe false
      actualBooked.getUserRef.isEmpty shouldBe true
      actualBooked.getBookingId.isEmpty shouldBe true
    }

    "hide orig photo size and add hd flag" in {
      val origSizes = Map("orig" -> "test", "full" -> "test").asJava

      val offerB = Offer
        .newBuilder()

      val stateB = offerB.offer.getState.toBuilder
        .addAllImageUrls(
          List(
            Photo
              .newBuilder()
              .setOrigWidth(1920)
              .setOrigHeight(1080)
              .putAllSizes(origSizes)
              .build(),
            Photo
              .newBuilder()
              .setOrigWidth(1920)
              .setOrigHeight(720)
              .putAllSizes(origSizes)
              .build()
          ).asJava
        )

      offerB.setState(stateB)

      val result = decayManager.decay(offerB.build(), DecayOptions.empty)(nonStaticRequest).futureValue

      result.getState.getImageUrlsList.get(0).getSizesMap.containsKey("orig") shouldBe false
      result.getState.getImageUrlsList.get(0).getIsHd shouldBe true

      result.getState.getImageUrlsList.get(1).getSizesMap.containsKey("orig") shouldBe false
      result.getState.getImageUrlsList.get(1).getIsHd shouldBe false
    }
  }

  private def createRequest(grants: Seq[Grant]) = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setApplication(Application.iosApp.copy(grants = grants))
    r.setRequestParams(RequestParams.construct(Ip))
    r.setUser(ModelGenerators.PrivateUserRefGen.next)
    r
  }

  "DecayManager (in special cases)" should {

    "keep Vin in offer if user is moderator" in {
      val req = PrivateModeratorRequestGen.next
      when(walletManger.isVinResolutionAlreadyBought(?)(?)).thenReturnF(PurchaseInfo.NotBought)
      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      offerBuilder.getDocumentsBuilder.setVin("JN1BDAV36U0500708")
      val result = decayManager.decay(offerBuilder.build(), DecayOptions.empty)(req).futureValue
      result.getDocuments.getVin shouldBe "JN1BDAV36U0500708"
    }

    "keep short Vin in offer if user is moderator" in {
      val req = PrivateModeratorRequestGen.next
      when(walletManger.isVinResolutionAlreadyBought(?)(?)).thenReturnF(PurchaseInfo.NotBought)
      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      offerBuilder.getDocumentsBuilder.setVin("E11603148")
      val result = decayManager.decay(offerBuilder.build(), DecayOptions.empty)(req).futureValue
      result.getDocuments.getVin shouldBe "E11603148"
    }

    "keep Vin in offer if regular user has already bought vin resolution" in {
      when(walletManger.isVinResolutionAlreadyBought(?)(?)).thenReturnF(PurchaseInfo(true, None))
      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      offerBuilder.getDocumentsBuilder.setVin("JN1BDAV36U0500708")
      val result = decayManager.decay(offerBuilder.build(), DecayOptions.empty).futureValue
      result.getDocuments.getVin shouldBe "JN1BDAV36U0500708"
    }

    "keep short Vin in offer if regular user has already bought vin resolution" in {
      when(walletManger.isVinResolutionAlreadyBought(?)(?)).thenReturnF(PurchaseInfo(true, None))
      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      offerBuilder.getDocumentsBuilder.setVin("E11603148")
      val result = decayManager.decay(offerBuilder.build(), DecayOptions.empty).futureValue
      result.getDocuments.getVin shouldBe "E11603148"
    }

    "do not send available_for_checkup tag to dealers" in {
      val offer = ModelGenerators.OfferGen.next.updated(b => b.addTags(Tags.AVAILABLE_FOR_CHECKUP))
      implicit val request: RequestImpl = {
        val r = new RequestImpl
        r.setTrace(trace)
        r.setApplication(Application.iosApp)
        r.setRequestParams(RequestParams.construct(Ip))
        r.setUser(ModelGenerators.DealerUserRefGen.next)
        r
      }
      val result = decayManager.decay(offer, DecayOptions.empty).futureValue
      offer.getTagsList.asScala should contain(Tags.AVAILABLE_FOR_CHECKUP)
      result.getTagsList.asScala should not contain Tags.AVAILABLE_FOR_CHECKUP
    }

    "send available_for_checkup tag to private users" in {
      val offer = ModelGenerators.OfferGen.next.updated(b => b.addTags(Tags.AVAILABLE_FOR_CHECKUP))
      implicit val request: RequestImpl = {
        val r = new RequestImpl
        r.setTrace(trace)
        r.setApplication(Application.iosApp)
        r.setRequestParams(RequestParams.construct(Ip))
        r.setUser(ModelGenerators.PrivateUserRefGen.next)
        r
      }
      val result = decayManager.decay(offer, DecayOptions.empty).futureValue
      offer.getTagsList.asScala should contain(Tags.AVAILABLE_FOR_CHECKUP)
      result.getTagsList.asScala should contain(Tags.AVAILABLE_FOR_CHECKUP)
    }

    "hide price for non active offers" in {
      val offer = ModelGenerators.OfferGen.next.updated(_.setStatus(OfferStatus.INACTIVE))
      val result = decayManager.decay(offer, DecayOptions.empty).futureValue
      result.hasPriceInfo shouldBe false
      result.getPriceHistoryCount shouldBe 0
    }

    "dont hide price for non active offers" in {
      val offer = ModelGenerators.OfferGen.next.updated(_.setStatus(OfferStatus.INACTIVE))
      val result = decayManager.decay(offer, DecayOptions.ForFavorites).futureValue
      result.hasPriceInfo shouldBe true
      result.getPriceHistoryCount shouldBe 0
    }

    "don't hide price for StaticApplication" in {
      mockHidePricesForDealersFeature(true)
      val offer = ModelGenerators.OfferGen.next.updated(_.setSection(Section.USED))
      val result = decayManager.decay(offer, DecayOptions.ForFavorites).futureValue
      result.hasPriceInfo shouldBe true
      result.getPriceHistoryCount shouldBe 0
      result.getState.getMileage shouldNot equal(0)
    }

    "hide price for NonStaticApplication" in {
      mockHidePricesForDealersFeature(true)
      val offer = ModelGenerators.OfferGen.next.updated(_.setSection(Section.USED))
      val result = decayManager.decay(offer, DecayOptions.ForFavorites)(nonStaticRequest).futureValue
      result.hasPriceInfo shouldBe false
      result.getPriceHistoryCount shouldBe 0
      result.getState.getMileage shouldBe 0
    }

    "don't hide price for NonStaticApplication" in {
      mockHidePricesForDealersFeature(false)
      val offer = ModelGenerators.OfferGen.next.updated(_.setSection(Section.USED))
      val result = decayManager.decay(offer, DecayOptions.ForFavorites)(nonStaticRequest).futureValue
      result.hasPriceInfo shouldBe true
      result.getPriceHistoryCount shouldBe 0
      result.getState.getMileage shouldNot equal(0)
    }

    "hide EcreditPrecondition" in {
      mockHideEcreditPrecondition(true)
      val precondition = EcreditPrecondition.getDefaultInstance.toBuilder.setEcreditProductId("product-1").build
      val offer = ModelGenerators.OfferGen.next.updated(_.setEcreditPrecondition(precondition))
      val result = decayManager.decay(offer, DecayOptions.empty)(nonStaticRequest).futureValue
      result.hasEcreditPrecondition shouldBe false
    }

    "don't hide EcreditPrecondition" in {
      mockHideEcreditPrecondition(false)
      val precondition = EcreditPrecondition.getDefaultInstance.toBuilder.setEcreditProductId("product-2").build
      val offer = ModelGenerators.OfferGen.next.updated(_.setEcreditPrecondition(precondition))
      val result = decayManager.decay(offer, DecayOptions.empty)(nonStaticRequest).futureValue
      result.hasEcreditPrecondition shouldBe true
      result.getEcreditPrecondition shouldBe precondition
    }
  }

  private def haveRecallInfo: Matcher[Offer] =
    Matcher(offer => MatchResult(offer.hasRecallInfo, "Offer don't have RecallInfo", "Offer have RecallInfo"))

  private def haveRealPhone: Matcher[Offer] =
    Matcher(offer =>
      MatchResult(
        offer.sellerPhones.exists(p => p.getPhone != "" || p.getOriginal != ""),
        "Offer don't have real phones",
        "Offer have real phones"
      )
    )

  private def mockHidePricesForDealersFeature(value: Boolean): Unit = {
    val hidePricesForDealers: Feature[Boolean] = mock[Feature[Boolean]]
    when(hidePricesForDealers.value).thenReturn(value)
    when(featureManager.hidePricesForDealers).thenReturn(hidePricesForDealers)
  }

  private def mockHideEcreditPrecondition(value: Boolean): Unit = {
    val hideEcreditPrecondition: Feature[Boolean] = mock[Feature[Boolean]]
    when(hideEcreditPrecondition.value).thenReturn(value)
    when(featureManager.hideEcreditPrecondition).thenReturn(hideEcreditPrecondition)
  }
}
