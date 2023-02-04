package ru.auto.api.managers.offers

import org.apache.commons.io.IOUtils
import org.mockito.ArgumentMatchers.{argThat, eq => eqq}
import org.mockito.Mockito.{reset, times, verify, verifyNoMoreInteractions}
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CommonModel.PaidService
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.CallRecordNotFound
import ru.auto.api.features.FeatureManager
import ru.auto.api.geo.{GeoUtils, Tree}
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.model.bunker.telepony.{MarkRedirectInfo, PhoneRedirectInfo}
import ru.auto.api.model.gen.BasicGenerators.readableString
import ru.auto.api.services.dealer_pony.DealerPonyClient
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.settings.SettingsClient
import ru.auto.api.services.telepony.TeleponyClient.{CallRecord, CreateRequest, Domains, RedirectOptions}
import ru.auto.api.services.telepony.{TeleponyCallsClient, TeleponyClient}
import ru.auto.api.util.{Request, RequestImpl, Resources, UrlBuilder}
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.auto.dealer_pony.proto.ApiModel.{CallInfoResponse, CmeTeleponyInfoRequest, CmeTeleponyInfoResponse}
import ru.yandex.vertis.billing.Model._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.moderation.proto.Model.Reason
import ru.yandex.vertis.tracing.Traced

import java.time.OffsetDateTime
import scala.concurrent.duration._

/**
  * @author pnaydenov
  */
class PhoneRedirectManagerSpec extends BaseSpec with ScalaCheckPropertyChecks with MockitoSupport with OptionValues {
  private val mark = "FORD"

  private val phoneRedirectInfo = PhoneRedirectInfo(
    Map(mark -> MarkRedirectInfo(2010, 500000.0, List(GeoUtils.MoscowFederalSubjectId)))
  )

  abstract class Fixture(needRegistered: Boolean = false, withAlias: Boolean = true, testTag: Option[String] = None) {
    val voxName = readableString(20, 20).next
    val teleponyClient: TeleponyClient = mock[TeleponyClient]
    val teleponyCallsClient: TeleponyCallsClient = mock[TeleponyCallsClient]
    val geobaseClient: GeobaseClient = mock[GeobaseClient]
    val salesmanClient: SalesmanClient = mock[SalesmanClient]
    val dealerPonyClient: DealerPonyClient = mock[DealerPonyClient]
    val featureManager: FeatureManager = mock[FeatureManager]
    val urlBuilder: UrlBuilder = mock[UrlBuilder]
    val fakeManager: FakeManager = mock[FakeManager]

    when(fakeManager.shouldTakeFakeOfferPhone(?)(?)).thenReturn(false)
    when(fakeManager.getTagWithFakeCheck(?, ?)(?)).thenReturn(testTag)
    when(urlBuilder.offerUrl(?, ?, ?)).thenReturn("testUrl")

    val settingsClient = mock[SettingsClient]
    when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map.empty[String, String])

    val tree: Tree = mock[Tree]
    when(tree.isInside(?, ?)).thenReturn(true)
    val allowRedirectFeature: Feature[Boolean] = mock[Feature[Boolean]]
    val fakeManagerSkipHarabaEnabledFeature: Feature[Boolean] = mock[Feature[Boolean]]
    when(allowRedirectFeature.value).thenReturn(true)
    when(fakeManagerSkipHarabaEnabledFeature.value).thenReturn(false)
    when(featureManager.allowRedirectUnsuccessfulEnabled).thenReturn(allowRedirectFeature)
    when(featureManager.fakeManagerSkipHarabaEnabled).thenReturn(fakeManagerSkipHarabaEnabledFeature)
    val voxCheckFeature: Feature[Boolean] = mock[Feature[Boolean]]
    when(voxCheckFeature.value).thenReturn(true)
    when(featureManager.voxCheck).thenReturn(voxCheckFeature)

    implicit val trace: Traced = Traced.empty

    implicit val request: Request = {
      val r = new RequestImpl
      r.setTrace(trace)
      r.setRequestParams(RequestParams.construct("1.1.1.1"))
      if (needRegistered) {
        val sessionResult = SessionResultGen.next.toBuilder
        if (!withAlias) sessionResult.getUserBuilder.getProfileBuilder.clearAlias()
        else sessionResult.getUserBuilder.getProfileBuilder.setAlias(readableString.next)
        r.setSession(sessionResult.build())
        r.setUser(AutoruUser(r.user.session.get.getSession.getUserId.toLong))
      }
      r
    }

    val phoneRedirectManager: PhoneRedirectManager =
      new PhoneRedirectManager(
        teleponyClient,
        teleponyCallsClient,
        geobaseClient,
        dealerPonyClient,
        settingsClient,
        featureManager,
        tree,
        ttl = 1.day,
        minAvailableCount = 3,
        phoneRedirectInfo,
        urlBuilder,
        fakeManager
      )

    val offer: ApiOfferModel.Offer = ModelGenerators.OfferWithOnePhoneGen.next.updated { b =>
      b.getSellerBuilder
        .setRedirectPhones(true)
        .getPhonesBuilderList
        .get(0)
        .setCallHourStart(10)
        .setCallHourEnd(20)
      b.getSalonBuilder.setDealerId("1")
      b.setCategory(Category.CARS)
      b.setSection(Section.NEW)
    }
    lazy val originalPhone: String = offer.getSeller.getPhones(0).getPhone
    lazy val redirectPhone = "+72222222222"

    lazy val redirect =
      TeleponyClient.Redirect(
        "aabbcc",
        offer.getId,
        OffsetDateTime.now(),
        redirectPhone,
        "+" + originalPhone,
        None,
        None,
        None
      )

    lazy val redirectableOffer = PrivateCarsOfferGen.next.updated { builder =>
      builder.setCarInfo(builder.getCarInfo.toBuilder.setMark(mark))
      builder.clearServices()
      builder.addServices(PaidService.newBuilder().setIsActive(false))
      val location = LocationGen.next.toBuilder.setGeobaseId(GeoUtils.MoscowCityId)
      builder.getSellerBuilder.setLocation(location)
      builder.getDocumentsBuilder.setYear(phoneRedirectInfo.marks(mark).fromYear + 2)
      builder.getPriceInfoBuilder.setRurPrice((phoneRedirectInfo.marks(mark).fromPrice + 10).toFloat)
    }
  }

  "PhoneRedirectManager.redirectPhone" should {

    "load redirects" in new Fixture() {
      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)

      val redirects = phoneRedirectManager.getPhonesWithRedirectsForOffer(offer).futureValue

      redirects should have size 1
      redirects.head.getRedirect shouldEqual "72222222222"
      redirects.head.getCallHourStart shouldEqual 10
      redirects.head.getCallHourEnd shouldEqual 20

      verify(teleponyClient).getOrCreate(?, ?, ?)(?)
    }

    "load redirect for offer using phone number (clientId = None)" in new Fixture() {
      val cmeRedirectRequest = CmeRedirectRequest
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setExternalClientId("cme-id")
        .setPhoneNumber("7123456789")
        .setPlatform("platform")
        .build()

      val teleponyInfo = TeleponyInfo
        .newBuilder()
        .setObjectId("objectId")
        .setTag("tag")
        .build()

      val cmeTeleponyInfoRequest = CmeTeleponyInfoRequest
        .newBuilder()
        .setCategory(cmeRedirectRequest.getCategory)
        .setSection(cmeRedirectRequest.getSection)
        .setExternalClientId(cmeRedirectRequest.getExternalClientId)
        .setPhoneNumber(cmeRedirectRequest.getPhoneNumber)
        .setPlatform(cmeRedirectRequest.getPlatform)
        .setVin(cmeRedirectRequest.getVin)
        .setNumberType(cmeRedirectRequest.getNumberType)
        .setTags(cmeRedirectRequest.getTags)
        .build()

      when(dealerPonyClient.getTeleponyInfoForCme(?)(?))
        .thenReturnF(CmeTeleponyInfoResponse.newBuilder().setResponse(teleponyInfo).build())
      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)

      val phoneRedirect =
        phoneRedirectManager.getPhoneWithRedirectForCme(cmeRedirectRequest, Domains.CMExpert, None).futureValue

      phoneRedirect.getRedirect shouldEqual "72222222222"

      verify(dealerPonyClient).getTeleponyInfoForCme(eqq(cmeTeleponyInfoRequest))(?)
      verify(teleponyClient).getOrCreate(eqq(Domains.CMExpert.toString()), eqq("objectId"), ?)(?)
    }

    "load redirect for offer using phone number (clientId = Some(_))" in new Fixture() {
      val cmeRedirectRequest = CmeRedirectRequest
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setExternalClientId("cme-id")
        .setPhoneNumber("7123456789")
        .setPlatform("platform")
        .build()

      val teleponyInfo = TeleponyInfo
        .newBuilder()
        .setObjectId("objectId")
        .setTag("tag")
        .build()

      val cmeTeleponyInfoRequest = CmeTeleponyInfoRequest
        .newBuilder()
        .setCategory(cmeRedirectRequest.getCategory)
        .setSection(cmeRedirectRequest.getSection)
        .setExternalClientId(cmeRedirectRequest.getExternalClientId)
        .setPhoneNumber(cmeRedirectRequest.getPhoneNumber)
        .setPlatform(cmeRedirectRequest.getPlatform)
        .setVin(cmeRedirectRequest.getVin)
        .setNumberType(cmeRedirectRequest.getNumberType)
        .setTags(cmeRedirectRequest.getTags)
        .setClientId(1)
        .build()

      when(dealerPonyClient.getTeleponyInfoForCme(?)(?))
        .thenReturnF(CmeTeleponyInfoResponse.newBuilder().setResponse(teleponyInfo).build())
      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)

      val phoneRedirect =
        phoneRedirectManager.getPhoneWithRedirectForCme(cmeRedirectRequest, Domains.CMExpert, Some(1)).futureValue

      phoneRedirect.getRedirect shouldEqual "72222222222"

      verify(dealerPonyClient).getTeleponyInfoForCme(eqq(cmeTeleponyInfoRequest))(?)
      verify(teleponyClient).getOrCreate(eqq(Domains.CMExpert.toString()), eqq("objectId"), ?)(?)
    }

    "load redirect for CME" in new Fixture() {
      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)

      val phoneNumber = "7123456789"
      val phoneRedirect = phoneRedirectManager.getPhoneWithRedirectForOffer(offer, phoneNumber).futureValue

      phoneRedirect.getRedirect shouldEqual "72222222222"

      verify(teleponyClient).getOrCreate(?, ?, ?)(?)
    }

    "load redirects with telepony info with antifraud = enable for registered user for vox seller but with disabled app2app" in new Fixture(
      needRegistered = true,
      testTag = Some("test-tag")
    ) {
      val offer2 = offer.updated { builder =>
        builder.getSellerBuilder.getTeleponyInfoBuilder
          .setDomain("test-domain")
          .setObjectId("test-object-id")
          .setTtl(12.days.toSeconds)
          .setTag("test-tag")
        builder.getAdditionalInfoBuilder.setTrustedDealerCallsAccepted(true)
        builder.getAdditionalInfoBuilder.setApp2AppCallsDisabled(true)
      }

      val expectedRequest = CreateRequest(
        "+" + offer2.getSeller.getPhones(0).getPhone,
        phoneType = None,
        geoId = None,
        ttl = Some(1036800),
        tag = Some("test-tag"),
        antifraud = Some("enable"),
        options = RedirectOptions(allowRedirectUnsuccessful = Some(false), Some(voxName))
      )

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      reset(settingsClient)
      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("vox_username" -> voxName))

      val redirects = {
        phoneRedirectManager.getPhonesWithRedirectsForOffer(offer2).futureValue.map { phone =>
          phoneRedirectManager.fillApp2AppPayload(offer2, phone, None)
        }
      }

      redirects.head.getApp2AppCallAvailable shouldEqual false
      redirects.head.getApp2AppPayloadCount shouldEqual 0

      val seller = UserRef.unapply(offer2.getUserRef).value.asPrivate
      verify(settingsClient).getSettings(eqq(SettingsClient.SettingsDomain), eqq(seller))(?)
      verify(teleponyClient).getOrCreate(eqq("test-domain"), eqq("test-object-id"), eqq(expectedRequest))(?)
    }

    "load redirects with telepony info with antifraud = enable for registered user for non-vox seller" in new Fixture(
      needRegistered = true,
      testTag = Some("test-tag")
    ) {
      val offer2 = offer.updated { builder =>
        builder.getSellerBuilder.getTeleponyInfoBuilder
          .setDomain("test-domain")
          .setObjectId("test-object-id")
          .setTtl(12.days.toSeconds)
          .setTag("test-tag")
        builder.getAdditionalInfoBuilder.setTrustedDealerCallsAccepted(true)
      }

      val expectedRequest = CreateRequest(
        "+" + offer2.getSeller.getPhones(0).getPhone,
        phoneType = None,
        geoId = None,
        ttl = Some(1036800),
        tag = Some("test-tag"),
        antifraud = Some("enable"),
        options = RedirectOptions(allowRedirectUnsuccessful = Some(false), None)
      )

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)

      val redirects = {
        phoneRedirectManager.getPhonesWithRedirectsForOffer(offer2).futureValue.map { phone =>
          phoneRedirectManager.fillApp2AppPayload(offer2, phone, None)
        }
      }

      redirects.head.getApp2AppCallAvailable shouldEqual false
      redirects.head.getApp2AppPayloadCount shouldEqual 0

      val seller = UserRef.unapply(offer2.getUserRef).value.asPrivate
      verify(settingsClient).getSettings(eqq(SettingsClient.SettingsDomain), eqq(seller))(?)
      verify(teleponyClient).getOrCreate(eqq("test-domain"), eqq("test-object-id"), eqq(expectedRequest))(?)
    }

    "load redirects with telepony info with antifraud = enable for registered user without alias" in new Fixture(
      needRegistered = true,
      withAlias = false
    ) {
      val buyerAlias = request.user.session.get.getUser.getProfile.getAlias
      val markName = readableString.next
      val modelName = readableString.next
      val generationName = readableString.next
      val offer2 = offer.updated { builder =>
        builder.getSellerBuilder.getTeleponyInfoBuilder
          .setDomain("test-domain")
          .setObjectId("test-object-id")
          .setTtl(12.days.toSeconds)
          .setTag("test-tag")
        builder.getAdditionalInfoBuilder.setTrustedDealerCallsAccepted(true)
        builder.getCarInfoBuilder.getMarkInfoBuilder.setName(markName)
        builder.getCarInfoBuilder.getModelInfoBuilder.setName(modelName)
        builder.getCarInfoBuilder.getSuperGenBuilder.setName(generationName)
      }

      val expectedRequest = CreateRequest(
        "+" + offer2.getSeller.getPhones(0).getPhone,
        phoneType = None,
        geoId = None,
        ttl = Some(1036800),
        tag = Some("test-tag"),
        antifraud = Some("enable"),
        options = RedirectOptions(allowRedirectUnsuccessful = Some(false), Some(voxName))
      )

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      reset(settingsClient)
      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("vox_username" -> voxName))

      val sellerApp2AppHandle = readableString.next

      val redirects = {
        phoneRedirectManager.getPhonesWithRedirectsForOffer(offer2).futureValue.map { phone =>
          phoneRedirectManager.fillApp2AppPayload(offer2, phone, Some(sellerApp2AppHandle))
        }
      }

      buyerAlias shouldBe empty

      redirects.head.getApp2AppPayloadMap
        .get("alias_and_subject") shouldEqual s"Покупатель • $markName $modelName $generationName"
    }

    "load redirects with telepony info with antifraud = enable for registered user" in new Fixture(
      needRegistered = true,
      testTag = Some("test-tag")
    ) {
      val buyerAlias = request.user.session.get.getUser.getProfile.getAlias
      val markName = readableString.next
      val modelName = readableString.next
      val generationName = readableString.next
      val offer2 = offer.updated { builder =>
        builder.getSellerBuilder.getTeleponyInfoBuilder
          .setDomain("test-domain")
          .setObjectId("test-object-id")
          .setTtl(12.days.toSeconds)
          .setTag("test-tag")
        builder.getAdditionalInfoBuilder.setTrustedDealerCallsAccepted(true)
        builder.getCarInfoBuilder.getMarkInfoBuilder.setName(markName)
        builder.getCarInfoBuilder.getModelInfoBuilder.setName(modelName)
        builder.getCarInfoBuilder.getSuperGenBuilder.setName(generationName)
      }

      val expectedRequest = CreateRequest(
        "+" + offer2.getSeller.getPhones(0).getPhone,
        phoneType = None,
        geoId = None,
        ttl = Some(1036800),
        tag = Some("test-tag"),
        antifraud = Some("enable"),
        options = RedirectOptions(allowRedirectUnsuccessful = Some(false), Some(voxName))
      )

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      reset(settingsClient)
      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("vox_username" -> voxName))

      val sellerApp2AppHandle = readableString.next

      val redirects = {
        phoneRedirectManager.getPhonesWithRedirectsForOffer(offer2).futureValue.map { phone =>
          phoneRedirectManager.fillApp2AppPayload(offer2, phone, Some(sellerApp2AppHandle))
        }
      }

      redirects.head.getApp2AppCallAvailable shouldEqual true
      redirects.head.getApp2AppPayloadCount shouldEqual 14
      redirects.head.getApp2AppPayloadMap.get("redirect_id") shouldEqual "aabbcc"
      redirects.head.getApp2AppPayloadMap.get("handle") shouldEqual sellerApp2AppHandle
      redirects.head.getApp2AppPayloadMap
        .get("alias_and_subject") shouldEqual s"$buyerAlias • $markName $modelName $generationName"

      val autoruUser = UserRef.unapply(offer2.getUserRef).value.asPrivate
      verify(settingsClient).getSettings(eqq(SettingsClient.SettingsDomain), eqq(autoruUser))(?)
      verify(teleponyClient).getOrCreate(eqq("test-domain"), eqq("test-object-id"), eqq(expectedRequest))(?)
    }

    "load redirects with telepony info with antifraud = enable" in new Fixture() {
      val offer2 = offer.updated { builder =>
        builder.getSellerBuilder.getTeleponyInfoBuilder
          .setDomain("test-domain")
          .setObjectId("test-object-id")
          .setTtl(12.days.toSeconds)
          .setTag("test-tag")
        builder.getAdditionalInfoBuilder.setTrustedDealerCallsAccepted(true)
      }

      val expectedRequest = CreateRequest(
        "+" + offer2.getSeller.getPhones(0).getPhone,
        phoneType = None,
        geoId = None,
        ttl = Some(1036800),
        tag = Some("test-tag"),
        antifraud = Some("enable"),
        options = RedirectOptions(allowRedirectUnsuccessful = Some(false), Some(voxName))
      )

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      reset(settingsClient)
      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("vox_username" -> voxName))

      val redirects = {
        phoneRedirectManager.getPhonesWithRedirectsForOffer(offer2).futureValue.map { phone =>
          phoneRedirectManager.fillApp2AppPayload(offer2, phone, None)
        }
      }

      redirects should have size 1
      redirects.head.getRedirect shouldEqual "72222222222"
      redirects.head.getCallHourStart shouldEqual 10
      redirects.head.getCallHourEnd shouldEqual 20
      redirects.head.getApp2AppCallAvailable shouldEqual false

      redirects.head.getApp2AppPayloadCount shouldEqual 0

      reset(settingsClient)
      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("vox_username" -> voxName))
    }

    "load redirects with telepony info" in new Fixture(needRegistered = true, testTag = Some("test-tag")) {
      val offer2 = offer.updated {
        _.getSellerBuilder.getTeleponyInfoBuilder
          .setDomain("test-domain")
          .setObjectId("test-object-id")
          .setTtl(12.days.toSeconds)
          .setTag("test-tag")
      }

      val expectedRequest = CreateRequest(
        "+" + offer2.getSeller.getPhones(0).getPhone,
        phoneType = None,
        geoId = None,
        ttl = Some(1036800),
        tag = Some("test-tag"),
        antifraud = Some("restricted"),
        options = RedirectOptions(allowRedirectUnsuccessful = Some(false), Some(voxName))
      )

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      reset(settingsClient)
      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("vox_username" -> voxName))

      val redirects = {
        phoneRedirectManager.getPhonesWithRedirectsForOffer(offer2).futureValue.map { phone =>
          phoneRedirectManager.fillApp2AppPayload(offer2, phone, None)
        }
      }

      redirects should have size 1
      redirects.head.getRedirect shouldEqual "72222222222"
      redirects.head.getCallHourStart shouldEqual 10
      redirects.head.getCallHourEnd shouldEqual 20
      redirects.head.getApp2AppCallAvailable shouldEqual true
      redirects.head.getApp2AppPayloadCount shouldEqual 13

      val autoruUser = UserRef.unapply(offer2.getUserRef).value.asPrivate
      verify(settingsClient).getSettings(eqq(SettingsClient.SettingsDomain), eqq(autoruUser))(?)
      verify(teleponyClient).getOrCreate(eqq("test-domain"), eqq("test-object-id"), eqq(expectedRequest))(?)
    }

    "not pass vox_name for users not registered in vox" in new Fixture(testTag = Some("test-tag")) {
      val offer2 = offer.updated {
        _.getSellerBuilder.getTeleponyInfoBuilder
          .setDomain("test-domain")
          .setObjectId("test-object-id")
          .setTtl(12.days.toSeconds)
          .setTag("test-tag")
      }

      val expectedRequest = CreateRequest(
        "+" + offer2.getSeller.getPhones(0).getPhone,
        phoneType = None,
        geoId = None,
        ttl = Some(1036800),
        tag = Some("test-tag"),
        antifraud = Some("restricted"),
        options = RedirectOptions(allowRedirectUnsuccessful = Some(false))
      )

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)

      val redirects = phoneRedirectManager.getPhonesWithRedirectsForOffer(offer2).futureValue

      redirects should have size 1
      redirects.head.getRedirect shouldEqual "72222222222"
      redirects.head.getCallHourStart shouldEqual 10
      redirects.head.getCallHourEnd shouldEqual 20
      redirects.head.getApp2AppCallAvailable shouldEqual false
      redirects.head.getApp2AppPayloadMap.get("redirect_id") shouldEqual null

      val autoruUser = UserRef.unapply(offer2.getUserRef).value.asPrivate
      verifyNoMoreInteractions(settingsClient)
      verify(teleponyClient).getOrCreate(eqq("test-domain"), eqq("test-object-id"), eqq(expectedRequest))(?)
    }

    "not pass vox_name for non-user offer" in new Fixture(testTag = Some("test-tag")) {
      val offer2 = offer.updated { b =>
        b.getSellerBuilder.getTeleponyInfoBuilder
          .setDomain("test-domain")
          .setObjectId("test-object-id")
          .setTtl(12.days.toSeconds)
          .setTag("test-tag")
        b.setUserRef(AutoruDealer(123).toPlain)
      }

      val expectedRequest = CreateRequest(
        "+" + offer2.getSeller.getPhones(0).getPhone,
        phoneType = None,
        geoId = None,
        ttl = Some(1036800),
        tag = Some("test-tag"),
        antifraud = Some("restricted"),
        options = RedirectOptions(allowRedirectUnsuccessful = Some(false))
      )

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)

      val redirects = phoneRedirectManager.getPhonesWithRedirectsForOffer(offer2).futureValue

      verify(teleponyClient).getOrCreate(eqq("test-domain"), eqq("test-object-id"), eqq(expectedRequest))(?)
    }

    "skip redirects with errors" in new Fixture() {
      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenThrowF(new RuntimeException)

      val redirects = phoneRedirectManager.getPhonesWithRedirectsForOffer(offer).futureValue

      redirects should have size 0
      verify(teleponyClient).getOrCreate(?, ?, ?)(?)
    }
  }

  "PhoneRedirectManager.getOfferPhones" should {

    val campaignHeader = CampaignHeader
      .newBuilder()
      .setId("123")
      .setVersion(1)
      .setSettings(
        CampaignSettings
          .newBuilder()
          .setIsEnabled(true)
          .setVersion(1)
      )
      .setProduct(
        Product
          .newBuilder()
          .setVersion(1)
      )
      .setOwner(CustomerHeader.newBuilder().setVersion(1))
      .setOrder(
        Order
          .newBuilder()
          .setVersion(1)
          .setId(1)
          .setText("asd")
          .setCommitAmount(31)
          .setOwner(CustomerId.newBuilder().setVersion(1).setClientId(31))
          .setApproximateAmount(32)
      )
      .build()

    "load redirects and format phones for random user" in new Fixture() {
      assert(request.user.ref != offer.userRef, "should not be owner of offer by test design")

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      when(salesmanClient.getBillingCallCampaign(?)(?)).thenReturnF(Some(campaignHeader))

      val phonesResponse: Seq[ApiOfferModel.Phone] = phoneRedirectManager.getOfferPhones(offer).await

      phonesResponse should have size 1

      phonesResponse.head.getPhone shouldBe PhoneUtils.format(redirectPhone)
      phonesResponse.head.getOriginal shouldBe ""
      phonesResponse.head.getRedirect shouldBe ""
    }

    "load redirects and format phones for owner" in new Fixture() {
      val offer2 = offer.updated(_.setUserRef(request.user.ref.toPlain))

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      when(salesmanClient.getBillingCallCampaign(?)(?)).thenReturnF(Option(campaignHeader))

      val phonesResponse: Seq[ApiOfferModel.Phone] = phoneRedirectManager.getOfferPhones(offer2).await

      phonesResponse should have size 1

      phonesResponse.head.getPhone shouldBe PhoneUtils.format(redirectPhone)
      phonesResponse.head.getOriginal shouldBe PhoneUtils.format(originalPhone)
      phonesResponse.head.getRedirect shouldBe ""
    }

    "skip redirects and format phones for owner" in new Fixture() {
      val offer2 = offer
        .updated(_.setUserRef(request.user.ref.toPlain))
        .updated(_.getSellerBuilder.setRedirectPhones(false))

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      when(salesmanClient.getBillingCallCampaign(?)(?)).thenReturnF(Some(campaignHeader))

      val phonesResponse: Seq[ApiOfferModel.Phone] = phoneRedirectManager.getOfferPhones(offer2).await

      phonesResponse should have size 1

      phonesResponse.head.getPhone shouldBe PhoneUtils.format(originalPhone)
      phonesResponse.head.getOriginal shouldBe PhoneUtils.format(originalPhone)
      phonesResponse.head.getRedirect shouldBe ""
    }

    "skip redirects and format phones for random user" in new Fixture() {
      val offer2 = offer
        .updated(_.getSellerBuilder.setRedirectPhones(false))

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      when(salesmanClient.getBillingCallCampaign(?)(?)).thenReturnF(Some(campaignHeader))

      val phonesResponse: Seq[ApiOfferModel.Phone] = phoneRedirectManager.getOfferPhones(offer2).await

      phonesResponse should have size 1

      phonesResponse.head.getPhone shouldBe PhoneUtils.format(originalPhone)
      phonesResponse.head.getOriginal shouldBe ""
      phonesResponse.head.getRedirect shouldBe ""
    }

    "fill title with seller name if empty" in new Fixture() {
      override val offer = OfferWithOnePhoneGen.next
        .updated { builder =>
          builder.getSellerBuilder.getPhonesBuilder(0).clearTitle
          builder.getSellerBuilder.setName("Custom name")
          builder.getSalonBuilder.setDealerId("1")
        }
      assert(request.user.ref != offer.userRef, "should not be owner of offer by test design")

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      when(salesmanClient.getBillingCallCampaign(?)(?)).thenReturnF(Some(campaignHeader))

      val phonesResponse = phoneRedirectManager.getOfferPhones(offer).await

      phonesResponse should have size 1

      phonesResponse.head.getTitle shouldBe "Custom name"
    }

    "keep title if not empty" in new Fixture() {
      override val offer = OfferWithOnePhoneGen.next
        .updated { builder =>
          builder.getSellerBuilder.getPhonesBuilder(0).setTitle("Original title")
          builder.getSellerBuilder.setName("Custom name")
          builder.getSalonBuilder.setDealerId("1")
        }
      assert(request.user.ref != offer.userRef, "should not be owner of offer by test design")

      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      when(salesmanClient.getBillingCallCampaign(?)(?)).thenReturnF(Some(campaignHeader))

      val phonesResponse = phoneRedirectManager.getOfferPhones(offer).await

      phonesResponse should have size 1

      phonesResponse.head.getTitle shouldBe "Original title"
    }

    "call dealerPonyClient.offerRedirectsAllowed for dealer offer" in new Fixture() {
      override val offer: Offer = OfferWithOnePhoneGen.next
        .updated(_.setUserRef(DealerUserRefGen.next.toPlain))

      when(dealerPonyClient.offerRedirectsAllowed(?)(?)).thenReturnF(true)
      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)
      when(salesmanClient.getBillingCallCampaign(?)(?)).thenReturnF(Some(campaignHeader))

      val phonesResponse: Seq[ApiOfferModel.Phone] = phoneRedirectManager.getOfferPhones(offer).await

      phonesResponse should have size 1

      verify(dealerPonyClient).offerRedirectsAllowed(offer)
    }

    "return fake phones for antirobot degradation and do not call dealer-pony service" in new Fixture() {
      override val offer: Offer = OfferWithOnePhoneGen.next
        .updated(_.setUserRef(DealerUserRefGen.next.toPlain))

      val fakePhone: ApiOfferModel.Phone = ApiOfferModel.Phone.newBuilder().setPhone(PhoneGen.next).build()

      when(fakeManager.shouldTakeFakeOfferPhone(?)(?)).thenReturn(true)
      when(fakeManager.getFakeOfferPhone(offer.getId)).thenReturn(Some(fakePhone))

      val phonesResponse: Seq[ApiOfferModel.Phone] = phoneRedirectManager.getOfferPhones(offer).await

      phonesResponse should have size 1

      phonesResponse.head shouldBe fakePhone

      verifyNoMoreInteractions(dealerPonyClient)
    }
  }

  "PhoneRedirectManager.getDirectOfferPhones" should {

    "return direct phones for random user" in new Fixture() {
      assert(request.user.ref != offer.userRef, "should not be owner of offer by test design")

      val phonesResponse: Seq[ApiOfferModel.Phone] = phoneRedirectManager.getDirectOfferPhones(offer).await

      phonesResponse should have size 1

      phonesResponse.head.getPhone shouldBe PhoneUtils.format(originalPhone)
      phonesResponse.head.getOriginal shouldBe ""
      phonesResponse.head.getRedirect shouldBe ""
    }

    "return direct phones for owner" in new Fixture() {
      val offer2 = offer.updated(_.setUserRef(request.user.ref.toPlain))

      val phonesResponse: Seq[ApiOfferModel.Phone] = phoneRedirectManager.getDirectOfferPhones(offer2).await

      phonesResponse should have size 1

      phonesResponse.head.getPhone shouldBe PhoneUtils.format(originalPhone)
      phonesResponse.head.getOriginal shouldBe PhoneUtils.format(originalPhone)
      phonesResponse.head.getRedirect shouldBe ""
    }
  }

  "PhoneRedirectManager.getCallRecord" should {
    implicit val t: Traced = Traced.empty
    implicit val dealerRequest: Request = {
      val r = new RequestImpl
      r.setUser(AutoruUser(777L))
      r.setDealer(AutoruDealer(555L))
      r.setRequestParams(RequestParams.construct("1.1.1.1"))
      r.setTrace(t)
      r
    }
    val call = TeleponyClient.Call(
      id = "record-id",
      objectId = "dealer-12345",
      createTime = OffsetDateTime.now(),
      time = OffsetDateTime.now(),
      duration = 1,
      talkDuration = 1,
      callResult = TeleponyClient.CallResults.Success,
      recordId = Some("record-id"),
      redirectId = Some("abcd"),
      tag = None,
      source = "+79876543210",
      target = "+79876543210",
      proxy = "+79876543210",
      externalId = "ext:record_id"
    )

    "return call record" in new Fixture() {
      Resources.open("/telepony/call_record.wav") { (content) =>
        val callRecord = new CallRecord("record.wav", IOUtils.toByteArray(content))
        when(teleponyCallsClient.getCallRecord("test-domain", dealerRequest.user.privateRef, "record-id")(t))
          .thenReturnF(callRecord)
        when(teleponyClient.getCall("test-domain", "record-id")(t))
          .thenReturnF(call)
        when(dealerPonyClient.callInfo(?, ?, ?)(?))
          .thenReturnF(CallInfoResponse.newBuilder().setClientId(dealerRequest.user.clientId).build())

        phoneRedirectManager.getCallRecord("test-domain", "record-id")(dealerRequest).futureValue shouldBe callRecord
      }
    }

    "return call record not found" in new Fixture() {
      Resources.open("/telepony/call_record.wav") { (content) =>
        val callRecord = new CallRecord("record.wav", IOUtils.toByteArray(content))
        when(teleponyClient.getCall("test-domain", "record-id")(t))
          .thenReturnF(call)
        when(teleponyCallsClient.getCallRecord("test-domain", dealerRequest.user.privateRef, "record-id")(t))
          .thenReturnF(callRecord)
        when(dealerPonyClient.callInfo(?, ?, ?)(?))
          .thenReturnF(CallInfoResponse.newBuilder().setClientId(1L).build())

        phoneRedirectManager
          .getCallRecord("test-domain", "record-id")(dealerRequest)
          .failed
          .futureValue shouldBe a[CallRecordNotFound]
      }
    }
  }

  "PhoneRedirectManager.dealerTeleponyInfo" should {
    "enrich dealer telepony info" in new Fixture() {
      forAll(Gen.oneOf(Seq("dealer-13202", "dealer-11310", "dealer-36486"))) { objectId =>
        val teleponyInfo = TeleponyInfo
          .newBuilder()
          .setObjectId(objectId)
          .setTag("one=test1#two=test2")
          .build()

        val utm = UtmParams(utmSource = None, utmCampaign = Some("test-campaign-exp-landing-avtoru"), utmContent = None)

        implicit val requestWithUtm: Request = {
          val r = new RequestImpl
          r.setTrace(trace)
          r.setRequestParams(RequestParams.construct("1.1.1.1", xUtm = utm))
          r
        }

        phoneRedirectManager
          .dealerTeleponyInfo(teleponyInfo)(requestWithUtm)
          .getTag shouldBe "one=test1#two=test2#source=direct"
      }
    }

    "dont enrich telepony info for other dealers" in new Fixture() {
      forAll(Gen.oneOf(Seq("dealer-20101", "dealer-16436"))) { objectId =>
        val teleponyInfo = TeleponyInfo
          .newBuilder()
          .setObjectId(objectId)
          .setTag("one=test1#two=test2")
          .build()

        val utm = UtmParams(utmSource = None, utmCampaign = Some("test-campaign-exp-landing-avtoru"), utmContent = None)

        implicit val requestWithUtm: Request = {
          val r = new RequestImpl
          r.setTrace(trace)
          r.setRequestParams(RequestParams.construct("1.1.1.1", xUtm = utm))
          r
        }

        phoneRedirectManager
          .dealerTeleponyInfo(teleponyInfo)(requestWithUtm)
          .getTag shouldBe "one=test1#two=test2"
      }
    }

    "dont enrich tag on unknown utm campaign" in new Fixture() {
      forAll(Gen.oneOf(Seq("dealer-20101", "dealer-16436", "dealer-20101"))) { objectId =>
        val teleponyInfo = TeleponyInfo
          .newBuilder()
          .setObjectId(objectId)
          .setTag("one=test1#two=test2")
          .build()

        val utm =
          UtmParams(utmSource = None, utmCampaign = Some("test-campaign-exp-landing-avtoru-wrong"), utmContent = None)

        implicit val requestWithUtm: Request = {
          val r = new RequestImpl
          r.setTrace(trace)
          r.setRequestParams(RequestParams.construct("1.1.1.1", xUtm = utm))
          r
        }

        phoneRedirectManager
          .dealerTeleponyInfo(teleponyInfo)(requestWithUtm)
          .getTag shouldBe "one=test1#two=test2"
      }
    }

    "dont enrich tag on empty utm campaign" in new Fixture() {
      forAll(Gen.oneOf(Seq("dealer-20101", "dealer-16436", "dealer-20101"))) { objectId =>
        val teleponyInfo = TeleponyInfo
          .newBuilder()
          .setObjectId(objectId)
          .setTag("one=test1#two=test2")
          .build()

        val utm = UtmParams(utmSource = None, utmCampaign = None, utmContent = None)

        implicit val requestWithUtm: Request = {
          val r = new RequestImpl
          r.setTrace(trace)
          r.setRequestParams(RequestParams.construct("1.1.1.1", xUtm = utm))
          r
        }

        phoneRedirectManager
          .dealerTeleponyInfo(teleponyInfo)(requestWithUtm)
          .getTag shouldBe "one=test1#two=test2"
      }
    }

    "enrich dealer telepony info for cannibalism exp" in new Fixture() {
      forAll(Gen.oneOf("AUTORUOFFICE-6072-with-chat", "AUTORUOFFICE-6072-without-chat")) { exp =>
        val teleponyInfo = TeleponyInfo
          .newBuilder()
          .setObjectId("dealer-12345")
          .setTag("one=test1#two=test2")
          .build()

        implicit val requestWithExp: Request = {
          val r = new RequestImpl
          r.setTrace(trace)
          r.setRequestParams(RequestParams.construct("1.1.1.1", experiments = Set(exp)))
          r.setApplication(Application.web)
          r.setToken(TokenServiceImpl.web)
          r
        }

        val tag = phoneRedirectManager
          .dealerTeleponyInfo(teleponyInfo, Some(Category.CARS), Some(Section.NEW), Some("autoru_billing"))(
            requestWithExp
          )
          .getTag
        tag shouldBe s"one=test1#two=test2#exp=$exp"
      }
    }

    "don't enrich dealer telepony withou exp" in new Fixture() {
      private val teleponyInfo = TeleponyInfo
        .newBuilder()
        .setObjectId("dealer-12345")
        .setTag("one=test1#two=test2")
        .build()

      implicit val requestWithUtm: Request = {
        val r = new RequestImpl
        r.setTrace(trace)
        r.setRequestParams(RequestParams.construct("1.1.1.1", experiments = Set()))
        r
      }

      phoneRedirectManager
        .dealerTeleponyInfo(teleponyInfo, Some(Category.CARS), Some(Section.NEW))(requestWithUtm)
        .getTag shouldBe s"one=test1#two=test2"
    }

  }

  "PhoneRedirectManager.allowRedirectUnsuccessful" should {

    "return true on redirectableOffer" in new Fixture() {
      phoneRedirectManager.getAllowRedirectUnsuccessful(redirectableOffer) shouldBe true
    }

    "return false on category != CARS" in new Fixture() {
      val newOffer = copy(redirectableOffer)(_.setCategory(Category.MOTO))
      phoneRedirectManager.getAllowRedirectUnsuccessful(newOffer) shouldBe false
    }

    "return false mark not from list" in new Fixture() {
      val newOffer = copy(redirectableOffer)(o => o.getCarInfoBuilder.setMark("DAEWOO"))
      phoneRedirectManager.getAllowRedirectUnsuccessful(newOffer) shouldBe false
    }

    "return false on dealer user" in new Fixture() {
      val newOffer = copy(redirectableOffer)(_.setUserRef(DealerUserRefGen.next.toPlain))
      phoneRedirectManager.getAllowRedirectUnsuccessful(newOffer) shouldBe false
    }

    "return false on user reseller" in new Fixture() {
      val newOffer = copy(redirectableOffer)(_.addReasonsBan(Reason.USER_RESELLER.name()))
      phoneRedirectManager.getAllowRedirectUnsuccessful(newOffer) shouldBe false
    }

    "return false on offer with paid services" in new Fixture() {
      val newOffer = copy(redirectableOffer)(_.addServices(PaidService.newBuilder().setIsActive(true)))
      phoneRedirectManager.getAllowRedirectUnsuccessful(newOffer) shouldBe false
    }

    "return false on wrong location" in new Fixture() {
      when(tree.isInside(?, ?)).thenReturn(false)
      phoneRedirectManager.getAllowRedirectUnsuccessful(redirectableOffer) shouldBe false
      when(tree.isInside(?, ?)).thenReturn(true)
    }

    "return false on wrong year from" in new Fixture() {
      val newOffer = copy(redirectableOffer)(_.getDocumentsBuilder.setYear(phoneRedirectInfo.marks(mark).fromYear - 1))
      phoneRedirectManager.getAllowRedirectUnsuccessful(newOffer) shouldBe false
    }

    "return false on wrong price" in new Fixture() {
      val newOffer = copy(redirectableOffer)(
        _.getPriceInfoBuilder.setRurPrice((phoneRedirectInfo.marks(mark).fromPrice - 1).toFloat)
      )
      phoneRedirectManager.getAllowRedirectUnsuccessful(newOffer) shouldBe false
    }

    "return false when feature is disabled" in new Fixture() {
      when(allowRedirectFeature.value).thenReturn(false)
      phoneRedirectManager.getAllowRedirectUnsuccessful(redirectableOffer) shouldBe false
    }

    "verify create request sent with allowRedirectUnsuccessful=true" in new Fixture() {
      when(allowRedirectFeature.value).thenReturn(true)
      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)

      val currOffer = offer.toBuilder.mergeFrom(redirectableOffer).clearServices().build()

      phoneRedirectManager.getPhonesWithRedirectsForOffer(currOffer)

      val size = currOffer.getSeller.getPhonesList.size()
      verify(teleponyClient, times(size))
        .getOrCreate(?, ?, argThat[CreateRequest](_.options.allowRedirectUnsuccessful.value))(?)
    }

    "verify create request sent with allowRedirectUnsuccessful=false on category MOTO" in new Fixture() {
      when(allowRedirectFeature.value).thenReturn(true)
      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)

      val currOffer = offer.toBuilder.mergeFrom(redirectableOffer).clearServices().setCategory(Category.MOTO).build()
      phoneRedirectManager.getPhonesWithRedirectsForOffer(currOffer)

      val size = currOffer.getSeller.getPhonesList.size()
      verify(teleponyClient, times(size))
        .getOrCreate(?, ?, argThat[CreateRequest](!_.options.allowRedirectUnsuccessful.value))(?)
    }

    "verify create request sent with allowRedirectUnsuccessful=false when feature disabled" in new Fixture() {
      when(allowRedirectFeature.value).thenReturn(false)
      when(teleponyClient.getOrCreate(?, ?, ?)(?)).thenReturnF(redirect)

      val currOffer = offer.toBuilder.mergeFrom(redirectableOffer).clearServices().build()
      phoneRedirectManager.getPhonesWithRedirectsForOffer(currOffer)

      val size = currOffer.getSeller.getPhonesList.size()
      verify(teleponyClient, times(size))
        .getOrCreate(?, ?, argThat[CreateRequest](!_.options.allowRedirectUnsuccessful.value))(?)
    }
  }

  private def copy(offer: Offer)(f: Offer.Builder => Unit): Offer = {
    Offer.newBuilder(offer).updated(f)
  }
}
