package ru.auto.api.managers.fake

import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.OfferListingResponse
import ru.auto.api.auth.{Application, ApplicationToken}
import ru.auto.api.features.FeatureManager
import ru.auto.api.model.ModelGenerators.{DeviceUidGen, PrivateUserRefGen, SessionResultGen, TestTokenGen}
import ru.auto.api.model.bunker.fake.FakePhonesList
import ru.auto.api.model.honeypot.fake_manager.{FakeMileageConfig, HarabaConfig}
import ru.auto.api.model.{AutoruUser, OfferID, RequestParams, UserRef}
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.util.Protobuf.RichDateTime
import ru.auto.api.util.crypt.TypedCrypto
import ru.auto.api.util.{RequestImpl, UrlBuilder}
import ru.yandex.passport.model.api.ApiModel.{Session, SessionResult}
import ru.yandex.passport.model.common.CommonModel.{DomainBan, UserModerationStatus}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._

class FakeManagerTest extends BaseSpec with MockitoSupport with OptionValues with ScalaCheckPropertyChecks {

  val testOfferId = "1106456162-05e9b5dd"

  val fakePhones = FakePhonesList(Set("+71231234567", "+71231234568", "+71231234569"))
  val featureManager: FeatureManager = mock[FeatureManager]

  val stsValue = "123*** 2312**"
  val offerBuilder = Offer.newBuilder()
  offerBuilder.setId(testOfferId)

  private val cryptoUserId = mock[TypedCrypto[AutoruUser]]

  private val urlBuilder = new UrlBuilder("desktopHost", "mobileHost", "partsHost", cryptoUserId)
  offerBuilder.getDocumentsBuilder.setSts(stsValue)

  private trait Fixture {
    val fakeManager = new FakeManager(featureManager, null, TestOperationalSupport, fakePhones, urlBuilder, "testSalt")

    val enabledFuture = mock[Feature[Boolean]]
    when(enabledFuture.value).thenReturn(true)
    when(featureManager.fakeManagerSkipHarabaEnabled).thenReturn(enabledFuture)
    when(featureManager.fakeManagerDocsEnabled).thenReturn(enabledFuture)
    when(featureManager.fakeManagerTechparamEnabled).thenReturn(enabledFuture)
    when(featureManager.fakeManagerEnabled).thenReturn(enabledFuture)
    when(featureManager.fakeManagerOfferIdsEnabled).thenReturn(enabledFuture)
    when(featureManager.paymentRestrictionsForBanned).thenReturn(enabledFuture)

    val configFeature = mock[Feature[FakeMileageConfig]]
    when(configFeature.value).thenReturn(FakeMileageConfig.default)

    when(featureManager.fakeManagerMileageConfig).thenReturn(configFeature)
  }

  var prevResult = ""
  "fake sts test" in new Fixture {
    FakeUtils.fakeDocuments(offerBuilder)
    prevResult = offerBuilder.getDocuments.getSts
    println(prevResult)
    assert(prevResult != stsValue)
  }

  "idempodency test" in new Fixture {
    offerBuilder.getDocumentsBuilder.setSts(stsValue)
    FakeUtils.fakeDocuments(offerBuilder)
    val newResult = offerBuilder.getDocuments.getSts
    println(newResult)
    assert(prevResult == newResult)
  }

  "currency test" in new Fixture {
    offerBuilder.getPriceInfoBuilder.setPrice(1235000)

    val historyElement = offerBuilder.build().getPriceInfo.toBuilder
    historyElement.setPrice(1236000)
    historyElement.setCreateTimestamp(historyElement.getCreateTimestamp - 1000)
    offerBuilder.addPriceHistory(historyElement)
    offerBuilder.addPriceHistory(offerBuilder.getPriceInfoBuilder)
    FakeUtils.fakePrice(offerBuilder)
    val newResult = offerBuilder.getPriceInfo.getPrice
    println(newResult)
    assert(!offerBuilder.getPriceHistoryList.asScala.exists(_.getPrice == 1235000))
    assert(offerBuilder.getPriceHistoryList.asScala.exists(_.getPrice == 1236000))
    assert(newResult == 1410000)
  }
  "miles test" in new Fixture {
    offerBuilder.getStateBuilder.setMileage(17500)
    FakeUtils.fakeMileage(offerBuilder)
    val newResult = offerBuilder.getState.getMileage
    println(newResult)
    assert(newResult == 16000)
  }

  "fake offer phones test" in new Fixture {
    val fakePhone = fakeManager.getFakeOfferPhone(offerBuilder.build().getId)
    fakePhones.phones.contains(fakePhone.get.getPhone)
  }

  "fake dealer phones test" in new Fixture {
    val fakePhone = fakeManager.getFakeDealerPhone("test_dealer_code")
    fakePhones.phones.contains(fakePhone.get.getPhone)
  }

  "skip haraba with ja3 and old offer test" in new Fixture {
    val createdAfter = new DateTime().minusDays(2)

    val testConfig = HarabaConfig(Set("123"), Some(createdAfter))

    val mockedConfig = mock[Feature[HarabaConfig]]
    when(mockedConfig.value).thenReturn(testConfig)
    when(featureManager.fakeManagerHarabaConfig).thenReturn(mockedConfig)

    val harabaRequest = {
      val r = new RequestImpl
      r.setTrace(Traced.empty)
      r.setToken(ApplicationToken("ios-123"))
      r.setRequestParams(RequestParams.construct("1.1.1.1", ja3 = Some("123"), antirobotDegradation = true))
      r.setApplication(Application.iosApp)
      r.setUser(UserRef.user(1))
      r
    }
    val originalOffer = offerBuilder.setCreated(createdAfter.minusDays(1).toProtobufTimestamp).build()

    val processedOffer = fakeManager.fake(originalOffer)(harabaRequest).value.get.get
    assert(processedOffer == originalOffer)
  }

  "dont skip haraba test with new offer" in new Fixture {
    val createdAfter = new DateTime().minusDays(2)

    val testConfig = HarabaConfig(Set("123"), Some(createdAfter))

    val mockedConfig = mock[Feature[HarabaConfig]]
    when(mockedConfig.value).thenReturn(testConfig)
    when(featureManager.fakeManagerHarabaConfig).thenReturn(mockedConfig)

    val harabaRequest = {
      val r = new RequestImpl
      r.setTrace(Traced.empty)

      r.setRequestParams(RequestParams.construct("1.1.1.1", ja3 = Some("123"), antirobotDegradation = true))
      r.setApplication(Application.iosApp)
      r.setToken(ApplicationToken("ios-123"))
      r.setUser(UserRef.user(1))
      r
    }
    val originalOffer = offerBuilder.setCreated(createdAfter.plusDays(1).toProtobufTimestamp).build()

    val processedOffer = fakeManager.fake(originalOffer)(harabaRequest).value.get.get
    assert(processedOffer != originalOffer)
  }

  "dont skip without ja3" in new Fixture {

    val testConfig = HarabaConfig(Set("123"), Some(new DateTime().minusDays(2)))

    val mockedConfig = mock[Feature[HarabaConfig]]
    when(mockedConfig.value).thenReturn(testConfig)
    when(featureManager.fakeManagerHarabaConfig).thenReturn(mockedConfig)

    val harabaRequest = {
      val r = new RequestImpl
      r.setTrace(Traced.empty)
      r.setRequestParams(RequestParams.construct("1.1.1.1", antirobotDegradation = true))
      r.setApplication(Application.iosApp)
      r.setToken(ApplicationToken("ios-123"))
      r.setUser(UserRef.user(1))
      r
    }
    val originalOffer = offerBuilder.build()

    when(featureManager.fakeManagerHarabaConfig.value).thenReturn(testConfig)
    val processedOffer = fakeManager.fake(originalOffer)(harabaRequest).value.get.get
    assert(processedOffer.hashCode() != originalOffer.hashCode())
  }

  "fake ids" in new Fixture {

    val testConfig = HarabaConfig(Set.empty[String], None)
    val mockedConfig = mock[Feature[HarabaConfig]]
    when(mockedConfig.value).thenReturn(testConfig)
    when(featureManager.fakeManagerHarabaConfig).thenReturn(mockedConfig)

    val harabaRequest = {
      val r = new RequestImpl
      r.setTrace(Traced.empty)
      r.setRequestParams(RequestParams.construct("1.1.1.1", antirobotDegradation = true))
      r.setApplication(Application.iosApp)
      r.setToken(ApplicationToken("ios-123"))
      r.setUser(UserRef.user(1))
      r
    }

    val originalOffer = offerBuilder.build()

    val listing = OfferListingResponse.newBuilder().addOffers(originalOffer).build()

    when(featureManager.fakeManagerHarabaConfig.value).thenReturn(testConfig)
    val processedOffer = fakeManager.fake(listing)(harabaRequest).value.get.get.getOffersList.asScala.head
    assert(processedOffer.hashCode() != originalOffer.hashCode())
    assert(processedOffer.getId != originalOffer.getId)

    assert(OfferID.parse(processedOffer.getId).hash.get != OfferID.parse(originalOffer.getId).hash.get)
    assert(OfferID.parse(processedOffer.getId).id != OfferID.parse(originalOffer.getId).id)

    val fakedOffer = processedOffer.toBuilder.setId("1101236162-05e9b522").build()
    assert(fakeManager.fakeId(fakedOffer).getId == "1101235346-ff93fae1")
    assert(fakeManager.fakeId(fakedOffer).getUrl == "desktopHost/cars/used/sale/1101235346-ff93fae1/")
    assert(fakeManager.fakeId(fakedOffer).getMobileUrl == "mobileHost/cars/used/sale/1101235346-ff93fae1/")
  }

  "fake mileage no changes when not in list" in new Fixture {

    val request = {
      val r = new RequestImpl
      r.setTrace(Traced.empty)
      r.setRequestParams(RequestParams.construct("1.1.1.1", antirobotDegradation = false))
      r.setApplication(Application.iosApp)
      r.setToken(ApplicationToken("ios-123"))
      r.setUser(UserRef.user(1))
      r
    }
    val originalOffer = offerBuilder.build()
    val listing = OfferListingResponse.newBuilder().addOffers(originalOffer).build()
    val processedOfferListing = fakeManager.fake(listing)(request).value.get.get.getOffersList.asScala.head
    val processedOffer = fakeManager.fake(originalOffer)(request).value.get.get
    assert(processedOffer.hashCode() == originalOffer.hashCode())
    assert(processedOffer.getId == originalOffer.getId)
    assert(processedOfferListing.hashCode() == originalOffer.hashCode())
    assert(processedOfferListing.getId == originalOffer.getId)
    assert(!request.fakeOfferIdFound)
  }

  "fake mileage with changes when  in list" in new Fixture {

    val currentTime = DateTime.now()

    val dateTimeStart = currentTime.minusDays(1)

    when(configFeature.value).thenReturn(FakeMileageConfig(enabled = true, Set(testOfferId), Some(dateTimeStart), None))
    DateTimeUtils.setCurrentMillisFixed(10000L)

    val request = {
      val r = new RequestImpl
      r.setTrace(Traced.empty)
      r.setRequestParams(RequestParams.construct("1.1.1.1", antirobotDegradation = false))
      r.setApplication(Application.iosApp)
      r.setToken(ApplicationToken("ios-123"))
      r.setUser(UserRef.user(1))
      r
    }
    val originalOffer = offerBuilder.build()
    val listing = OfferListingResponse.newBuilder().addOffers(originalOffer).build()
    val processedOfferListing = fakeManager.fake(listing)(request).value.get.get.getOffersList.asScala.head
    val processedOffer = fakeManager.fake(originalOffer)(request).value.get.get
    assert(processedOffer.hashCode() != originalOffer.hashCode())
    assert(processedOffer.getId == originalOffer.getId)
    assert(processedOffer.getState.getMileage != originalOffer.getState.getMileage)
    assert(processedOffer.getState.getMileage == 579796)

    assert(processedOfferListing.hashCode() != originalOffer.hashCode())
    assert(processedOfferListing.getId == originalOffer.getId)
    assert(processedOfferListing.getState.getMileage != originalOffer.getState.getMileage)
    assert(processedOfferListing.getState.getMileage == 579796)
    DateTimeUtils.setCurrentMillisSystem()

  }

  "fake passport result" in new Fixture {

    val request = {
      val r = new RequestImpl
      r.setTrace(Traced.empty)
      r.setRequestParams(RequestParams.construct("1.1.1.1", antirobotDegradation = false))
      r.setApplication(Application.iosApp)
      r.setToken(ApplicationToken("ios-123"))
      r.setUser(UserRef.user(1))
      r.setSession(SessionResult.newBuilder().setSession(Session.newBuilder().setId("1").build()).build())
      r
    }
    val res = fakeManager.getFakeYandexAuthResult(request)
    assert(res.getMatchedCredentials.getIdentityList.asScala.nonEmpty)
    assert(
      res.getMatchedCredentials.getIdentityList.asScala
        .map(_.getEmail)
        .toSet == Set("mta***************", "hib****************")
    )
  }
  "should restrict banned non robot request" in new Fixture {
    val user = PrivateUserRefGen.next
    val token = TestTokenGen.next

    val session = {
      val builder =
        SessionResultGen.next.toBuilder
      val b = UserModerationStatus
        .newBuilder()
        .putAllBans((Map("someDomain" -> DomainBan.newBuilder.addReasons("USER_HACKED").build)).asJava)
      builder.getUserBuilder.setModerationStatus(b)
      builder.build()
    }
    val request = new RequestImpl
    request.setTrace(Traced.empty)
    request.setRequestParams(RequestParams.construct("1.1.1.1"))
    request.setToken(token)
    request.setNewDeviceUid(DeviceUidGen.next)
    request.setUser(user)
    request.setSession(session)
    TokenServiceImpl.getStaticApplication(token).foreach(request.setApplication)

    val res = fakeManager.shouldSkipPaymentRequestForBannedUsers(request)
    assert(res)
  }

  implicit val trace = Traced.empty

}
