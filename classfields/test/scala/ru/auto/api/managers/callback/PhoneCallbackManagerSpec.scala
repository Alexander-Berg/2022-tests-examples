package ru.auto.api.managers.callback

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.http.HttpStatus
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.{Phone, TeleponyInfo}
import ru.auto.api.auth.Application
import ru.auto.api.callback.CallbackModel.{CallBackProvider, PhoneCallbackRequest, SalonPhoneCallbackRequest}
import ru.auto.api.exceptions.{CallbackDisabledException, InvalidPhoneNumberException}
import ru.auto.api.geo.{Region, Tree}
import ru.auto.api.managers.callback.PhoneCallbackManager.CallKeeperSite
import ru.auto.api.managers.offers.PhoneRedirectManager
import ru.auto.api.model.CategorySelector.StrictCategory
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.{CategorySelector, OfferID, PhoneUtils, RequestParams, UserRef}
import ru.auto.api.services.billing.CabinetClient
import ru.auto.api.services.callkeeper.CallKeeperClient
import ru.auto.api.services.callkeeper.CallKeeperClient.CallKeeperRequest
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.telepony.TeleponyClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.time.TimeService
import ru.auto.api.util.TimeUtils.RichInstant
import ru.auto.api.{ApiOfferModel, AsyncTasksSupport, BaseSpec}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}
import ru.yandex.vertis.telepony.model.proto.{CallPeriods, CallbackOrder, CallbackOrderCreateRequest, CallerIdModeEnum}
import ru.yandex.vertis.tracing.Traced
import PhoneCallbackManager._
import ru.auto.api.services.dealer_pony.DealerPonyClient

import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import org.scalacheck.Gen
import java.time.ZoneOffset
import java.time.LocalTime
import java.time.Instant

class PhoneCallbackManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport
  with Matchers {

  trait mocks {
    val callKeeperClient: CallKeeperClient = mock[CallKeeperClient]
    val teleponyClient: TeleponyClient = mock[TeleponyClient]
    val salesmanClient: SalesmanClient = mock[SalesmanClient]
    val dealerPonyClient: DealerPonyClient = mock[DealerPonyClient]
    val offerLoader: PhoneCallbackOfferLoader = mock[PhoneCallbackOfferLoader]
    val tree: Tree = mock[Tree]
    implicit val trace: Traced = Traced.empty
    lazy val requestParams: RequestParams = RequestParams.empty

    implicit val request: RequestImpl = {
      val req = new RequestImpl
      req.setApplication(Application.desktop)
      req.setTrace(trace)
      req.setRequestParams(requestParams)
      req
    }
    val cabinetClient: CabinetClient = mock[CabinetClient]
    val whash = "test_whash"
    val trucksApiKey = "test_api_key_trucks"
    val motoApiKey = "test_api_key_moto"
    val auctionApiKey = "test_api_key_auction"
    val defaultApiKey = "test_api_key"
    val searcherClient: SearcherClient = mock[SearcherClient]
    val phoneRedirectManager: PhoneRedirectManager = mock[PhoneRedirectManager]
    val vosClient: VosClient = mock[VosClient]
    val config: Config = ConfigFactory.parseString(s"""
        autoru.api.callkeeper.trucks.whash = "$whash"
        autoru.api.callkeeper.trucks.key = "$trucksApiKey"
        autoru.api.callkeeper.cars.key = "$defaultApiKey"
        autoru.api.callkeeper.cars.whash = "$whash"
        autoru.api.callkeeper.moto.key = "$motoApiKey"
        autoru.api.callkeeper.moto.whash = "$whash"
        autoru.api.callkeeper.auction.key = "$auctionApiKey"
        autoru.api.callkeeper.auction.whash = "$whash"
      """)

    val timeService: TimeService = mock[TimeService]

    val phoneCallbackMessageBuilder = mock[PhoneCallbackMessageBuilder]

    val manager = new PhoneCallbackManager(
      callKeeperClient,
      teleponyClient,
      dealerPonyClient,
      offerLoader,
      tree,
      PhoneCallbackManager.readConfig(config),
      phoneRedirectManager,
      searcherClient,
      vosClient,
      phoneCallbackMessageBuilder,
      timeService
    )

    val clientPhone = "79161111111test"
    val normalizedClientPhone = "79161111111"
    val sourcePhone = "79162222222"
    val proxyPhone = "79163333333"
    val testRequest: PhoneCallbackRequest = PhoneCallbackRequest.newBuilder().setPhone(clientPhone).build()
    val testOfferId: OfferID = OfferID.parse("111-111")
    val testCategory: CategorySelector.Cars.type = CategorySelector.Cars
    val userRef: UserRef = UserRef.parse("user:123")
    val dealerRef: UserRef = UserRef.parse("dealer:123")
    val incorrectPhone = "8916123"
    val testSalonId = 12L
    val testRegion: Region = mock[Region]
    var tzOffset = 0
    def tz = ZoneOffset.ofTotalSeconds(tzOffset)
    val tradeIn = arbitrary[Boolean].next

    val text = Gen
      .option(HashGen)
      .next

    when(testRegion.tzOffset).thenReturn(tzOffset)
    when(dealerPonyClient.getTeleponyInfoByOffer(?)(?)).thenReturnF(TeleponyInfo.getDefaultInstance)

    def testForCategory(generatedOffer: ApiOfferModel.Offer,
                        category: StrictCategory,
                        expectedApiKey: String,
                        expectedUtmSource: String): Unit = {
      val now: Instant = java.time.Instant
        .now()
        .atOffset(ZoneOffset.ofHours(3))
        .`with`(LocalTime.of(5, 0))
        .toInstant()
      when(timeService.getNowJavaInstant).thenReturn(now)
      val offer = generatedOffer.updated {
        _.setId(testOfferId.toPlain)
          .setUserRef(dealerRef.toPlain)
          .getSellerBuilder
          .clearTeleponyInfo()
      }
      val phone = Phone
        .newBuilder()
        .setPhone("79162222222")
        .setCallHourStart(9)
        .setCallHourEnd(11)
        .build()
      when(searcherClient.getOffer(?, ?)(?)).thenReturnF(offer)
      when(phoneRedirectManager.getDirectOfferPhones(?)(?)).thenReturnF(Seq(phone))
      val seller = offer.getSeller.toBuilder.setPhones(0, phone).build()
      val offerWithPhone = offer.updated {
        _.setSeller(seller)
          .setSalon(
            SalonGen.next.toBuilder
              .setClientId(offer.dealerUserRef.clientId.toString)
          )
      }
      val teleponyDomain = HashGen.next
      val teleponyTag = HashGen.next
      when(dealerPonyClient.callbackTeleponyInfo(offerWithPhone)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offer.getId())
          .setTag(teleponyTag)
          .build()
      )
      when(phoneCallbackMessageBuilder.buildCallbackMessage(offerWithPhone, tradeIn))
        .thenReturn(text)
      when(offerLoader.getOfferWithOldCategory(category, testOfferId))
        .thenReturn(Future.successful(offerWithPhone))
      when(teleponyClient.createCallBack(?, ?)(?)).thenReturn(Future.successful(CallbackOrder.getDefaultInstance))
      when(tree.region(?)).thenReturn(Some(testRegion))
      manager.registerPhoneCallback(testRequest, testOfferId, category, tradeIn = tradeIn).await
      executeAsyncTasks()
      verifyNoMoreInteractions(callKeeperClient)
      verify(teleponyClient).createCallBack(?, ?)(?)
    }

    def executeAsyncTasks(): Unit = {
      Future.sequence(request.tasks.start(HttpStatus.SC_OK)).await
    }

  }

  private class teleponyMocks(val openHour: Int = 9, val closeHour: Int = 18) extends mocks {
    val sellerRef = DealerUserRefGen.next
    val offerId = OfferIDGen.next
    val category = CategorySelector.Cars

    val sellerPhone = Phone
      .newBuilder()
      .setPhone(PhoneUtils.format(PhoneGen.next, "1:3:7"))
      .setCallHourStart(openHour)
      .setCallHourEnd(closeHour)
      .build()
    val normalizedSellerPhone = "+" + PhoneUtils.normalize(sellerPhone.getPhone)

    val offer = CarsOfferGen.next.updated {
      _.setId(offerId.toPlain)
        .setUserRef(sellerRef.toPlain)
        .setSalon(
          SalonGen.next.toBuilder
            .setClientId(sellerRef.clientId.toString)
        )
        .getSellerBuilder
        .clearTeleponyInfo()
        .setPhones(0, sellerPhone)
    }
    val buyerPhone = PhoneUtils.format(PhoneGen.next, "1:3:7")
    val normalizedBuyerPhone = "+" + PhoneUtils.normalize(buyerPhone)
    val payload: Map[String, String] = Map(HashGen.next -> HashGen.next, HashGen.next -> HashGen.next)
    val utmSource = HashGen.next

    val params = PhoneCallbackRequest
      .newBuilder()
      .setPhone(buyerPhone)
      .setProvider(CallBackProvider.TELEPONY)
      .setUtmSource(utmSource)
      .putAllPayload(payload.asJava)
    val teleponyDomain = HashGen.next
    val teleponyTag = HashGen.next
    when(dealerPonyClient.getTeleponyInfoByOffer(?)(?)).thenReturnF(
      TeleponyInfo
        .newBuilder()
        .setDomain(teleponyDomain)
        .setObjectId(offerId.toPlain)
        .setTag(teleponyTag)
        .build()
    )
    when(phoneCallbackMessageBuilder.buildCallbackMessage(offer, tradeIn))
      .thenReturn(text)
    when(offerLoader.getOfferWithOldCategory(category, offerId))
      .thenReturn(Future.successful(offer))
    when(phoneRedirectManager.getDirectOfferPhones(?)(?)).thenReturnF(Seq(sellerPhone))
    when(searcherClient.getOffer(?, ?)(?)).thenReturnF(offer)
    tzOffset = 18000
    when(tree.region(?)).thenReturn(Some(testRegion))
    when(testRegion.tzOffset).thenReturn(tzOffset)
    var createRequest = CallbackOrderCreateRequest.getDefaultInstance
    stub(teleponyClient.createCallBack(_: String, _: CallbackOrderCreateRequest)(_: Traced)) {
      case (_, request, _) =>
        createRequest = request
        Future.successful(CallbackOrder.getDefaultInstance)
    }
  }

  "PhoneCallbackManager" should {
    "make telepony request before dealer is open" in new teleponyMocks() {
      val now: java.time.Instant = java.time.Instant
        .now()
        .atOffset(ZoneOffset.ofHours(3))
        .`with`(LocalTime.of(5, 0))
        .toInstant
      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )
      manager.registerPhoneCallback(params.build(), offerId, category, tradeIn = tradeIn).await
      executeAsyncTasks()
      verify(teleponyClient).createCallBack(eeq(teleponyDomain), ?)(?)
      val callPeriods: CallPeriods = createRequest.getTargetInfo.getCallPeriods
      createRequest.getTag shouldBe teleponyTag
      createRequest.getPayloadMap.asScala shouldBe (payload ++ Map(
        UtmSourceKey -> utmSource,
        OfferIdKey -> offerId.toPlain,
        ClientIdKey -> sellerRef.asDealer.clientId.toString
      ))
      createRequest.getSourceInfo.getNumber shouldBe normalizedBuyerPhone
      createRequest.getSourceInfo.getCallerIdMode shouldBe CallerIdModeEnum.Mode.SYSTEM_NUMBER
      createRequest.getTargetInfo.getNumber shouldBe normalizedSellerPhone
      createRequest.getTargetInfo.getCallerIdMode shouldBe CallerIdModeEnum.Mode.REAL_NUMBER
      createRequest.getTargetInfo.getAudio.getText shouldBe text.getOrElse("")
      callPeriods.getPeriodsCount shouldBe 3
      callPeriods.getPeriods(0).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 0)
      callPeriods.getPeriods(0).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 0)
      callPeriods.getPeriods(1).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 1)
      callPeriods.getPeriods(1).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 1)
      callPeriods.getPeriods(2).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 2)
      callPeriods.getPeriods(2).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 2)
    }

    "make telepony request in work hours for fulltime dealer" in new teleponyMocks(0, 0) {
      val now: java.time.Instant = java.time.Instant
        .now()
        .atOffset(ZoneOffset.ofHours(3))
        .`with`(LocalTime.of(8, 0))
        .toInstant
      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )
      manager.registerPhoneCallback(params.build(), offerId, category, tradeIn = tradeIn).await
      executeAsyncTasks()
      verify(teleponyClient).createCallBack(eeq(teleponyDomain), ?)(?)
      val callPeriods: CallPeriods = createRequest.getTargetInfo.getCallPeriods
      createRequest.getTag shouldBe teleponyTag
      createRequest.getPayloadMap.asScala shouldBe (payload ++ Map(
        UtmSourceKey -> utmSource,
        OfferIdKey -> offerId.toPlain,
        ClientIdKey -> sellerRef.asDealer.clientId.toString
      ))
      createRequest.getSourceInfo.getNumber shouldBe normalizedBuyerPhone
      createRequest.getSourceInfo.getCallerIdMode shouldBe CallerIdModeEnum.Mode.SYSTEM_NUMBER
      createRequest.getTargetInfo.getNumber shouldBe normalizedSellerPhone
      createRequest.getTargetInfo.getCallerIdMode shouldBe CallerIdModeEnum.Mode.REAL_NUMBER
      createRequest.getTargetInfo.getAudio.getText shouldBe text.getOrElse("")
      callPeriods.getPeriodsCount shouldBe 3
      callPeriods.getPeriods(0).getOpenTime shouldBe now.toProtobufTimestamp
      callPeriods.getPeriods(0).getCloseTime shouldBe getTimestamp(now, tzOffset, 0, 1)
      callPeriods.getPeriods(1).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 1)
      callPeriods.getPeriods(1).getCloseTime shouldBe getTimestamp(now, tzOffset, 0, 2)
      callPeriods.getPeriods(2).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 2)
      callPeriods.getPeriods(2).getCloseTime shouldBe getTimestamp(now, tzOffset, 0, 3)
    }

    "make telepony request in work hours" in new teleponyMocks(openHour = 8) {
      // This is before 10 which is the "sane" opening hour in the implementation.
      val nowHour = 9
      val now: java.time.Instant = java.time.Instant
        .now()
        .atOffset(tz)
        .`with`(LocalTime.of(nowHour, 0))
        .toInstant
      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )
      manager.registerPhoneCallback(params.build(), offerId, category, tradeIn = tradeIn).await
      executeAsyncTasks()
      verify(teleponyClient).createCallBack(eeq(teleponyDomain), ?)(?)
      val callPeriods: CallPeriods = createRequest.getTargetInfo.getCallPeriods
      createRequest.getTag shouldBe teleponyTag
      createRequest.getPayloadMap.asScala shouldBe (payload ++ Map(
        UtmSourceKey -> utmSource,
        OfferIdKey -> offerId.toPlain,
        ClientIdKey -> sellerRef.asDealer.clientId.toString
      ))
      createRequest.getSourceInfo.getNumber shouldBe normalizedBuyerPhone
      createRequest.getSourceInfo.getCallerIdMode shouldBe CallerIdModeEnum.Mode.SYSTEM_NUMBER
      createRequest.getTargetInfo.getNumber shouldBe normalizedSellerPhone
      createRequest.getTargetInfo.getCallerIdMode shouldBe CallerIdModeEnum.Mode.REAL_NUMBER
      createRequest.getTargetInfo.getAudio.getText shouldBe text.getOrElse("")
      callPeriods.getPeriodsCount shouldBe 3
      callPeriods.getPeriods(0).getOpenTime shouldBe getTimestamp(now, tzOffset, nowHour, 0)
      callPeriods.getPeriods(0).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 0)
      callPeriods.getPeriods(1).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 1)
      callPeriods.getPeriods(1).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 1)
      callPeriods.getPeriods(2).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 2)
      callPeriods.getPeriods(2).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 2)
    }

    "make telepony request if domain = auto-dealers" in new teleponyMocks() {
      val now: java.time.Instant = java.time.Instant
        .now()
        .atOffset(ZoneOffset.ofHours(3))
        .`with`(LocalTime.of(8, 0))
        .toInstant

      val params2 = PhoneCallbackRequest
        .newBuilder()
        .setPhone(buyerPhone)
        .setProvider(CallBackProvider.CALLKEEPER)
        .setUtmSource(utmSource)
        .putAllPayload(payload.asJava)

      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain("auto-dealers")
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )
      manager.registerPhoneCallback(params2.build(), offerId, category, tradeIn = tradeIn).await
      executeAsyncTasks()
      verify(teleponyClient).createCallBack(eeq("auto-dealers"), ?)(?)
    }

    "make telepony request in evening" in new teleponyMocks() {
      val now: java.time.Instant = java.time.Instant
        .now()
        .atOffset(ZoneOffset.ofHours(3))
        .`with`(LocalTime.of(17, 0))
        .toInstant
      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )
      manager.registerPhoneCallback(params.build(), offerId, category, tradeIn = tradeIn).await
      executeAsyncTasks()
      verify(teleponyClient).createCallBack(eeq(teleponyDomain), ?)(?)
      val callPeriods: CallPeriods = createRequest.getTargetInfo.getCallPeriods
      createRequest.getTag shouldBe teleponyTag
      createRequest.getPayloadMap.asScala shouldBe (payload ++ Map(
        UtmSourceKey -> utmSource,
        OfferIdKey -> offerId.toPlain,
        ClientIdKey -> sellerRef.asDealer.clientId.toString
      ))
      createRequest.getSourceInfo.getNumber shouldBe normalizedBuyerPhone
      createRequest.getSourceInfo.getCallerIdMode shouldBe CallerIdModeEnum.Mode.SYSTEM_NUMBER
      createRequest.getTargetInfo.getNumber shouldBe normalizedSellerPhone
      createRequest.getTargetInfo.getCallerIdMode shouldBe CallerIdModeEnum.Mode.REAL_NUMBER
      createRequest.getTargetInfo.getAudio.getText shouldBe text.getOrElse("")
      callPeriods.getPeriodsCount shouldBe 3
      callPeriods.getPeriods(0).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 1)
      callPeriods.getPeriods(0).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 1)
      callPeriods.getPeriods(1).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 2)
      callPeriods.getPeriods(1).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 2)
      callPeriods.getPeriods(2).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 3)
      callPeriods.getPeriods(2).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 3)
    }

    "check if offer is dealer offer" in new mocks {
      forAll(OfferGen) { generatedOffer =>
        val offer = generatedOffer.updated(_.setId(testOfferId.toPlain).setUserRef(userRef.toPlain))
        when(offerLoader.getOfferWithOldCategory(testCategory, testOfferId))
          .thenReturn(Future.successful(offer))
        intercept[CallbackDisabledException] {
          manager.registerPhoneCallback(testRequest, testOfferId, testCategory, tradeIn = tradeIn).await
        }
      }
    }

    "check phone number for correctness" in new mocks {
      intercept[InvalidPhoneNumberException] {
        val incorrectRequest = PhoneCallbackRequest.newBuilder().setPhone(incorrectPhone).build()
        manager.registerPhoneCallback(incorrectRequest, testOfferId, testCategory, tradeIn = tradeIn).await
      }
    }

    "send correct api key for trucks" in new mocks {
      testForCategory(OfferGen.next, CategorySelector.Trucks, trucksApiKey, "desktop")
      verifyNoMoreInteractions(cabinetClient)
    }

    "send correct api key for moto" in new mocks {
      testForCategory(OfferGen.next, CategorySelector.Moto, motoApiKey, "desktop")
      verifyNoMoreInteractions(cabinetClient)
    }

    "send correct api key for auction" in new mocks {
      val now: java.time.Instant = java.time.Instant
        .now()
        .atOffset(ZoneOffset.ofHours(3))
        .`with`(LocalTime.of(5, 0))
        .toInstant
      when(timeService.getNowJavaInstant).thenReturn(now)
      val category = CategorySelector.Cars
      val source = OfferGen.next
      val offer = source.updated { x =>
        x.setId(testOfferId.toPlain)
          .setUserRef(dealerRef.toPlain)
          .getSellerBuilder
          .getTeleponyInfoBuilder
          .setDomain("autoru_billing")
          .setObjectId("dealer-1234")
      }
      val phone = Phone
        .newBuilder()
        .setPhone("79162222222")
        .setCallHourStart(9)
        .setCallHourEnd(11)
        .build()
      val teleponyDomain = HashGen.next
      val teleponyTag = HashGen.next

      when(searcherClient.getOffer(?, ?)(?)).thenReturnF(offer)
      when(phoneRedirectManager.getDirectOfferPhones(?)(?)).thenReturnF(Seq(phone))
      val seller = offer.getSeller.toBuilder.setPhones(0, phone).build()
      val offerWithPhone = offer.updated(
        _.setSeller(seller)
          .setSalon(
            SalonGen.next.toBuilder
              .setClientId(offer.dealerUserRef.clientId.toString)
          )
      )
      when(dealerPonyClient.callbackTeleponyInfo(offerWithPhone)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerWithPhone.getId())
          .setTag(teleponyTag)
          .build()
      )
      when(phoneCallbackMessageBuilder.buildCallbackMessage(offerWithPhone, tradeIn))
        .thenReturn(text)
      when(offerLoader.getOfferWithOldCategory(category, testOfferId))
        .thenReturn(Future.successful(offerWithPhone))
      when(teleponyClient.createCallBack(?, ?)(?)).thenReturn(Future.successful(CallbackOrder.getDefaultInstance))
      when(tree.region(?)).thenReturn(Some(testRegion))
      manager.registerPhoneCallback(testRequest, testOfferId, category, tradeIn = tradeIn).futureValue
      executeAsyncTasks()
      verifyNoMoreInteractions(callKeeperClient)
      verify(teleponyClient).createCallBack(?, ?)(?)
    }

    "respect user's preferred hours if those are not over yet" in new teleponyMocks(openHour = 8, closeHour = 20) {
      val preferredStartHour = 15
      val preferredEndHour = 18
      val nowHour = 16
      val now: java.time.Instant = java.time.Instant
        .now()
        .atOffset(tz)
        .`with`(LocalTime.of(nowHour, 0))
        .toInstant
      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )
      params.getPreferredCallHoursBuilder.setStartHour(LocalTime.of(preferredStartHour, 0).atOffset(tz).toString)
      params.getPreferredCallHoursBuilder.setEndHour(LocalTime.of(preferredEndHour, 0).atOffset(tz).toString)

      manager.registerPhoneCallback(params.build(), offerId, category, tradeIn = tradeIn).await
      executeAsyncTasks()

      verify(teleponyClient).createCallBack(eeq(teleponyDomain), ?)(?)
      val callPeriods: CallPeriods = createRequest.getTargetInfo.getCallPeriods
      callPeriods.getPeriodsCount shouldBe 3
      callPeriods.getPeriods(0).getOpenTime shouldBe getTimestamp(now, tzOffset, nowHour, 0)
      callPeriods.getPeriods(0).getCloseTime shouldBe getTimestamp(now, tzOffset, preferredEndHour, 0)
      callPeriods.getPeriods(1).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 1)
      callPeriods.getPeriods(1).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 1)
      callPeriods.getPeriods(2).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 2)
      callPeriods.getPeriods(2).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 2)
    }

    "respect user's preferred hours if those are over" in new teleponyMocks(openHour = 8, closeHour = 21) {
      val preferredStartHour = 12
      val preferredEndHour = 15
      val nowHour = 16
      val now: java.time.Instant = java.time.Instant
        .now()
        .atOffset(tz)
        .`with`(LocalTime.of(nowHour, 0))
        .toInstant
      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )
      params.getPreferredCallHoursBuilder.setStartHour(LocalTime.of(preferredStartHour, 0).atOffset(tz).toString)
      params.getPreferredCallHoursBuilder.setEndHour(LocalTime.of(preferredEndHour, 0).atOffset(tz).toString)

      manager.registerPhoneCallback(params.build(), offerId, category, tradeIn = tradeIn).await
      executeAsyncTasks()

      verify(teleponyClient).createCallBack(eeq(teleponyDomain), ?)(?)
      val callPeriods: CallPeriods = createRequest.getTargetInfo.getCallPeriods
      callPeriods.getPeriodsCount shouldBe 3
      callPeriods.getPeriods(0).getOpenTime shouldBe getTimestamp(now, tzOffset, preferredStartHour, 1)
      callPeriods.getPeriods(0).getCloseTime shouldBe getTimestamp(now, tzOffset, preferredEndHour, 1)
      callPeriods.getPeriods(1).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 2)
      callPeriods.getPeriods(1).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 2)
      callPeriods.getPeriods(2).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 3)
      callPeriods.getPeriods(2).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 3)
    }

    "intersect user's preferred hours with salon's schedule" in new teleponyMocks(openHour = 9) {
      val preferredStartHour = 8
      val preferredEndHour = 15
      val nowHour = 7
      val now: java.time.Instant = java.time.Instant
        .now()
        .atOffset(tz)
        .`with`(LocalTime.of(nowHour, 0))
        .toInstant
      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )
      params.getPreferredCallHoursBuilder.setStartHour(LocalTime.of(preferredStartHour, 0).atOffset(tz).toString)
      params.getPreferredCallHoursBuilder.setEndHour(LocalTime.of(preferredEndHour, 0).atOffset(tz).toString)

      manager.registerPhoneCallback(params.build(), offerId, category, tradeIn = tradeIn).await
      executeAsyncTasks()

      verify(teleponyClient).createCallBack(eeq(teleponyDomain), ?)(?)
      val callPeriods: CallPeriods = createRequest.getTargetInfo.getCallPeriods
      callPeriods.getPeriodsCount shouldBe 3
      callPeriods.getPeriods(0).getOpenTime shouldBe getTimestamp(now, tzOffset, openHour, 0)
      callPeriods.getPeriods(0).getCloseTime shouldBe getTimestamp(now, tzOffset, preferredEndHour, 0)
      callPeriods.getPeriods(1).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 1)
      callPeriods.getPeriods(1).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 1)
      callPeriods.getPeriods(2).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 2)
      callPeriods.getPeriods(2).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 2)
    }

    "interpret user's preferred hours using the provided timezone offset" in new teleponyMocks(
      openHour = 8,
      closeHour = 21
    ) {
      val userTimeZone = ZoneOffset.ofHours(7)

      // User timezone is `GMT+7`, while the salon uses `GMT+5`. This means that in the salon's timezone the preferred
      // interval is 10-13.
      val timeZoneDifferenceHours = 2
      val preferredStartHour = 12
      val preferredEndHour = 15

      val nowHour = 20
      val now: java.time.Instant = java.time.Instant
        .now()
        .atOffset(tz)
        .`with`(LocalTime.of(nowHour, 0))
        .toInstant
      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )
      params.getPreferredCallHoursBuilder.setStartHour(
        LocalTime.of(preferredStartHour, 0).atOffset(userTimeZone).toString
      )
      params.getPreferredCallHoursBuilder.setEndHour(LocalTime.of(preferredEndHour, 0).atOffset(userTimeZone).toString)

      manager.registerPhoneCallback(params.build(), offerId, category, tradeIn = tradeIn).await
      executeAsyncTasks()

      verify(teleponyClient).createCallBack(eeq(teleponyDomain), ?)(?)
      val callPeriods: CallPeriods = createRequest.getTargetInfo.getCallPeriods
      callPeriods.getPeriodsCount shouldBe 3
      callPeriods.getPeriods(0).getOpenTime shouldBe getTimestamp(
        now,
        tzOffset,
        preferredStartHour - timeZoneDifferenceHours,
        1
      )
      callPeriods.getPeriods(0).getCloseTime shouldBe getTimestamp(
        now,
        tzOffset,
        preferredEndHour - timeZoneDifferenceHours,
        1
      )
      callPeriods.getPeriods(1).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 2)
      callPeriods.getPeriods(1).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 2)
      callPeriods.getPeriods(2).getOpenTime shouldBe getTimestamp(now, tzOffset, 10, 3)
      callPeriods.getPeriods(2).getCloseTime shouldBe getTimestamp(now, tzOffset, closeHour, 3)
    }

    "throw IllegalArgumentException for malformed call hours" in new teleponyMocks() {
      val now: java.time.Instant = java.time.Instant.now()
      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )

      params.getPreferredCallHoursBuilder.setStartHour("malformed")
      params.getPreferredCallHoursBuilder.setEndHour(LocalTime.MIN.atOffset(tz).toString)

      intercept[IllegalArgumentException] {
        manager.registerPhoneCallback(params.build(), offerId, category, tradeIn = tradeIn).await
        executeAsyncTasks()
      }
    }

    "throw IllegalArgumentException for missing call hours" in new teleponyMocks() {
      val now: java.time.Instant = java.time.Instant.now()
      when(timeService.getNowJavaInstant).thenReturn(now)
      when(dealerPonyClient.callbackTeleponyInfo(offer)).thenReturnF(
        TeleponyInfo
          .newBuilder()
          .setDomain(teleponyDomain)
          .setObjectId(offerId.toPlain)
          .setTag(teleponyTag)
          .build()
      )

      params.getPreferredCallHoursBuilder()
      intercept[IllegalArgumentException] {
        manager.registerPhoneCallback(params.build(), offerId, category, tradeIn = tradeIn).await
        executeAsyncTasks()
      }
    }
  }

  private def getTimestamp(moment: java.time.Instant, tzOffset: Int, hour: Int, day: Int) =
    moment
      .atOffset(ZoneOffset.ofHours(tzOffset / 3600))
      .`with`(LocalTime.of(hour, 0))
      .plusDays(day)
      .toInstant
      .toProtobufTimestamp

  "validate client phone for salon callback" in new mocks {

    val params = SalonPhoneCallbackRequest
      .newBuilder()
      .setClientPhone(incorrectPhone)
      .setSalonSourcePhone(normalizedClientPhone)
      .setSalonProxyPhone(normalizedClientPhone)
      .build()
    intercept[InvalidPhoneNumberException] {
      manager.registerSalonPhoneCallback(params, testSalonId).await
    }
  }

  "validate salon source phone for salon callback" in new mocks {

    val params = SalonPhoneCallbackRequest
      .newBuilder()
      .setClientPhone(normalizedClientPhone)
      .setSalonSourcePhone(incorrectPhone)
      .setSalonProxyPhone(normalizedClientPhone)
      .build()
    intercept[InvalidPhoneNumberException] {
      manager.registerSalonPhoneCallback(params, testSalonId).await
    }
  }

  "validate salon proxy phone for salon callback" in new mocks {

    val params = SalonPhoneCallbackRequest
      .newBuilder()
      .setClientPhone(normalizedClientPhone)
      .setSalonSourcePhone(normalizedClientPhone)
      .setSalonProxyPhone(incorrectPhone)
      .build()
    intercept[InvalidPhoneNumberException] {
      manager.registerSalonPhoneCallback(params, testSalonId).await
    }
  }

  "get call hours from salon" in new mocks {

    val salonPhone = Phone
      .newBuilder()
      .setCallHourStart(11)
      .setCallHourEnd(12)
      .setOriginal(sourcePhone)
      .setPhone(proxyPhone)
    val salon = SalonGen.next.toBuilder.addPhones(salonPhone).build()

    val params = SalonPhoneCallbackRequest
      .newBuilder()
      .setClientPhone(normalizedClientPhone)
      .setSalonSourcePhone(sourcePhone)
      .setSalonProxyPhone(proxyPhone)
      .setTag("CARS#NEW")
      .setDomain("autoru_billing")
      .setObjectId("dealer-1234")
      .build()

    val expectedRequest = CallKeeperRequest(
      site = CallKeeperSite,
      clientPhone = normalizedClientPhone,
      managerPhone = proxyPhone,
      textToManager = None,
      openingHours = "11001200",
      externalServiceId = salon.getSalonId.toString,
      utmSource = s"desktop",
      utmCampaign = Some("object_id=dealer-1234__salon_id=0"),
      timeZone = "UTC+00:00",
      auctionApiKey,
      whash
    )

    when(testRegion.tzOffset).thenReturn(0)
    when(tree.region(?)).thenReturn(Some(testRegion))
    when(vosClient.getSalon(any())(any())).thenReturnF(salon)
    when(callKeeperClient.getRegistrationCallbackStatus(?)(?)).thenReturnF("success")
    manager.registerSalonPhoneCallback(params, salon.getSalonId).await
    verify(callKeeperClient).getRegistrationCallbackStatus(expectedRequest)
  }
  "get call hours from salon (fullday work)" in new mocks {

    val salonPhone = Phone
      .newBuilder()
      .setCallHourStart(0)
      .setCallHourEnd(0)
      .setOriginal(sourcePhone)
      .setPhone(proxyPhone)
    val salon = SalonGen.next.toBuilder.addPhones(salonPhone).build()

    val params = SalonPhoneCallbackRequest
      .newBuilder()
      .setClientPhone(normalizedClientPhone)
      .setSalonSourcePhone(sourcePhone)
      .setSalonProxyPhone(proxyPhone)
      .setTag("CARS#NEW")
      .setDomain("autoru_billing")
      .setObjectId("dealer-1234")
      .build()

    val expectedRequest = CallKeeperRequest(
      site = CallKeeperSite,
      clientPhone = normalizedClientPhone,
      managerPhone = proxyPhone,
      textToManager = None,
      openingHours = "00002359",
      externalServiceId = salon.getSalonId.toString,
      utmSource = s"desktop",
      utmCampaign = Some("object_id=dealer-1234__salon_id=0"),
      timeZone = "UTC+00:00",
      auctionApiKey,
      whash
    )

    when(testRegion.tzOffset).thenReturn(0)
    when(tree.region(?)).thenReturn(Some(testRegion))
    when(vosClient.getSalon(any())(any())).thenReturnF(salon)
    when(callKeeperClient.getRegistrationCallbackStatus(?)(?)).thenReturnF("success")
    manager.registerSalonPhoneCallback(params, salon.getSalonId).await
    verify(callKeeperClient).getRegistrationCallbackStatus(expectedRequest)
  }

  "set correct utm source from app name" in new mocks {
    implicit override val request: RequestImpl = new RequestImpl
    request.setApplication(Application.androidApp)
    request.setTrace(trace)
    request.setRequestParams(RequestParams.empty)
    testForCategory(OfferGen.next, CategorySelector.Moto, motoApiKey, "app_android")
  }

  "set correct utm source from request" in new mocks {

    override val testRequest: PhoneCallbackRequest =
      PhoneCallbackRequest.newBuilder().setPhone(clientPhone).setUtmSource("app_ios").build()
    testForCategory(OfferGen.next, CategorySelector.Moto, motoApiKey, "app_ios")
  }

}
