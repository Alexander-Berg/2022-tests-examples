package ru.auto.api.managers.events.broker

import auto.events.common.Common.{ComplainReason, SellerLink, SocialNetwork}
import com.google.protobuf.util.Timestamps
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.State
import ru.auto.api.CommonModel.{Photo, Video}
import ru.auto.api.ResponseModel.VideoListingResponse
import ru.auto.api.StatEvents.SelfType.TYPE_SINGLE
import ru.auto.api.StatEvents.{PollGroup, ReportedStatEvent}
import ru.auto.api.auth.Application
import ru.auto.api.managers.offers.OfferLoader
import ru.auto.api.model.ModelGenerators.{CategoryGen, ImageUrlGenerator, OfferIDGen, PrivateUserRefGen}
import ru.auto.api.model._
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.api.model.journal.{Article, Image}
import ru.auto.api.model.pushnoy.{ClientOS, DeviceInfo}
import ru.auto.api.model.uaas.UaasResponse
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewsResponseModel.ReviewResponse
import ru.auto.api.services.dealer_pony.DealerPonyClient
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.geobase.GeobaseClient.IpInfo
import ru.auto.api.services.journal.JournalClient
import ru.auto.api.services.pushnoy.PushnoyClient
import ru.auto.api.services.review.ReviewClient
import ru.auto.api.services.video.VideoClient
import ru.auto.api.testkit.TestData
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.{AsyncTasksSupport, BaseSpec, DummyOperationalSupport, StatEvents}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters.{IterableHasAsJava, MapHasAsScala}

class ModelEventBuilderTest
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport
  with DummyOperationalSupport {

  private trait Fixture {
    val offerLoader: OfferLoader = mock[OfferLoader]
    val geobaseClient: GeobaseClient = mock[GeobaseClient]
    val pushnoyClient: PushnoyClient = mock[PushnoyClient]
    val dealerPonyClient: DealerPonyClient = mock[DealerPonyClient]
    val journalClient: JournalClient = mock[JournalClient]
    val reviewClient: ReviewClient = mock[ReviewClient]
    val videoClient: VideoClient = mock[VideoClient]

    val brokerEventBuilder =
      new ModelEventBuilder(
        offerLoader,
        reviewClient,
        geobaseClient,
        pushnoyClient,
        dealerPonyClient,
        TestData.tree,
        journalClient,
        videoClient
      )

    implicit protected val trace: Traced = Traced.empty
    protected val userLocation: UserLocation = UserLocation(108, -83.7f, 200)
    implicit protected val request: Request = generateRequest()

    protected def generateRequest(application: Option[Application] = None,
                                  xApplicationId: Option[String] = None,
                                  uaasResponse: UaasResponse = UaasResponse.empty): RequestImpl = {
      val r = new RequestImpl
      r.setApplication(application.getOrElse(Application.iosApp))
      r.setVersion(Version.V1_0)
      r.setRequestParams(
        RequestParams.construct(
          "1.1.1.1",
          deviceUid = Some("testUid"),
          userLocation = Some(userLocation),
          xApplicationId = xApplicationId,
          uaasResponse = uaasResponse
        )
      )
      r.setUser(PrivateUserRefGen.next)
      r.setTrace(trace)
      r
    }
  }

  "BrokerEventBuilder" should {
    "keep device model and device brand from pushnoy" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val brand = BasicGenerators.readableString.next
      val model = BasicGenerators.readableString.next
      val deviceInfo = DeviceInfo("", "", brand, model, "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)
      val event = ReportedStatEvent.newBuilder()
      val offerId = ModelGenerators.OfferIDGen.next
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      event.getCardShowEventBuilder.setCardId(offerId.toPlain)
      val brokerEvents = brokerEventBuilder.build(Seq(event.build()), None).futureValue
      val brokerEvent = brokerEvents.head
      brokerEvent.getUserInfo.getAppUserInfo.getDeviceBrand shouldBe brand
      brokerEvent.getUserInfo.getAppUserInfo.getDeviceModel shouldBe model
    }

    "story show event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val brand = BasicGenerators.readableString.next
      val model = BasicGenerators.readableString.next
      val deviceInfo = DeviceInfo("", "", brand, model, "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)
      val event = ReportedStatEvent.newBuilder()
      val offerId = ModelGenerators.OfferIDGen.next
      val category = ModelGenerators.CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      val storyId = BasicGenerators.readableString.next
      val slideId = Gen.choose(1, 8).next
      val offer = ModelGenerators.offerGen(strictCategory, offerId.toPlain, ModelGenerators.PrivateUserRefGen).next
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      event.getStoryShowEventBuilder
        .setCardId(offerId.toPlain)
        .setCardCategory(category)
        .setStoryId(storyId)
        .setSlideId(slideId)

      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(offerId), eeq(false), eeq(false))(?)).thenReturnF(offer)
      val brokerEvents = brokerEventBuilder.build(Seq(event.build()), None).futureValue
      val brokerEvent = brokerEvents.head
      brokerEvent.getOffer.getOfferId shouldBe offer.getId
      brokerEvent.getOriginalEvent shouldBe event.build()
    }

    "callback request click event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val phone = ModelGenerators.PhoneGen.next
      event.getCallbackRequestClickEventBuilder().setPhoneNumber(phone)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType() shouldBe auto.events.Model.EventType.CALLBACK_REQUEST_CLICK
      brokerEvent.getUserInfo().getCallbackPhoneNumber() shouldBe phone
    }

    "callback request send event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val phone = ModelGenerators.PhoneGen.next
      event.getCallbackRequestSendEventBuilder().setPhoneNumber(phone)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType() shouldBe auto.events.Model.EventType.CALLBACK_REQUEST_SEND
      brokerEvent.getUserInfo().getCallbackPhoneNumber() shouldBe phone
    }

    "NPS close event" in new Fixture {
      val question = BasicGenerators.readableString.next

      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      event.getNpsCloseBuilder().setQuestion(question)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType() shouldBe auto.events.Model.EventType.POPUP_CLOSE
      brokerEvent.getPopup().getType() shouldBe auto.events.Model.PopupType.NPS
      brokerEvent.getPopup().getNps().getQuestion() shouldBe question
    }

    "NPS show event" in new Fixture {
      val question = BasicGenerators.readableString.next

      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      event.getNpsShowBuilder().setQuestion(question)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType() shouldBe auto.events.Model.EventType.POPUP_SHOW
      brokerEvent.getPopup().getType() shouldBe auto.events.Model.PopupType.NPS
      brokerEvent.getPopup().getNps().getQuestion() shouldBe question
    }

    "NPS submit event" in new Fixture {
      val question = BasicGenerators.readableString.next
      val feedback = BasicGenerators.readableString.next
      val stars = Gen.choose(1, 5).next

      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      event.getNpsSubmitBuilder().setQuestion(question).setFeedback(feedback).setStars(stars)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType() shouldBe auto.events.Model.EventType.POPUP_SUBMIT
      brokerEvent.getPopup().getType() shouldBe auto.events.Model.PopupType.NPS
      brokerEvent.getPopup().getNps().getQuestion() shouldBe question
      brokerEvent.getPopup().getNps().getFeedback() shouldBe feedback
      brokerEvent.getPopup().getNps().getStars() shouldBe stars
    }

    "share event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val socialNetwork = SocialNetwork.VKONTAKTE
      event.getShareClickBuilder.setSocialNetwork(SocialNetwork.VKONTAKTE)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.SHARE_CLICK
      brokerEvent.getShare.getSocialNetwork shouldBe socialNetwork
    }

    "comment event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val text = "comment text"
      event.getCommentSubmitBuilder.setText(text)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.COMMENT_ADD
      brokerEvent.getComment.getText shouldBe text
    }

    "compare event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getCompareRemoveBuilder.setCardId(cardId.toPlain)
      event.getCompareRemoveBuilder.setCategory(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.COMPARE_REMOVE
      brokerEvent.getCardId shouldBe cardId.id
    }

    "catalog event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getCatalogClickBuilder.setCardId(cardId.toPlain)
      event.getCatalogClickBuilder.setCategory(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.CATALOG_CLICK
      brokerEvent.getCardId shouldBe cardId.id
    }

    "free report event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getFreeReportClickBuilder.setCardId(cardId.toPlain)
      event.getFreeReportClickBuilder.setCategory(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.FREE_REPORT_CLICK
      brokerEvent.getCardId shouldBe cardId.id
    }

    "full description event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getFullDescriptionClickBuilder.setCardId(cardId.toPlain)
      event.getFullDescriptionClickBuilder.setCategory(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.FULL_DESCRIPTION_CLICK
      brokerEvent.getCardId shouldBe cardId.id
    }

    "complain event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getComplainClickBuilder.setCardId(cardId.toPlain)
      event.getComplainClickBuilder.setCategory(category)
      val reason = StatEvents.ComplainReason.newBuilder()
      val text = "some complain text"
      reason.setReason(ComplainReason.FAKE_PHOTO)
      reason.setText(text)
      event.getComplainClickBuilder.addReasons(reason.build())
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.COMPLAIN_CLICK
      brokerEvent.getCardId shouldBe cardId.id
      brokerEvent.getComplain.getReasonsList.size() shouldBe 1
      brokerEvent.getComplain.getReasonsList.get(0).getReason shouldBe reason.getReason
      brokerEvent.getComplain.getReasonsList.get(0).getText shouldBe reason.getText
    }

    "review all click event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getReviewAllClickBuilder.setCardId(cardId.toPlain)
      event.getReviewAllClickBuilder.setCategory(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.REVIEW_ALL_CLICK
      brokerEvent.getCardId shouldBe cardId.id
    }

    "journal all click event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getJournalAllClickBuilder.setCardId(cardId.toPlain)
      event.getJournalAllClickBuilder.setCategory(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.JOURNAL_ALL_CLICK
      brokerEvent.getCardId shouldBe cardId.id
    }

    "tutorial all click event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getTutorialAllClickBuilder.setCardId(cardId.toPlain)
      event.getTutorialAllClickBuilder.setCategory(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.TUTORIAL_ALL_CLICK
      brokerEvent.getCardId shouldBe cardId.id
    }

    "video all click event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getVideoAllClickBuilder.setCardId(cardId.toPlain)
      event.getVideoAllClickBuilder.setCategory(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.VIDEO_ALL_CLICK
      brokerEvent.getCardId shouldBe cardId.id
    }

    "video play event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      event.getVideoPlayBuilder.setVideoId(OfferIDGen.next.toString)
      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.VIDEO_PLAY
    }

    "complectation compare event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getComplectationCompareClickBuilder.setCardId(cardId.toPlain)
      event.getComplectationCompareClickBuilder.setCategory(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.COMPLECTATION_COMPARE_CLICK
      brokerEvent.getCardId shouldBe cardId.id
    }

    "best price event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      event.getBestPriceClickBuilder.setCardId(cardId.toPlain)
      event.getBestPriceClickBuilder.setCategory(category)
      event.getBestPriceClickBuilder.setTradein(true)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.BEST_PRICE_CLICK
      brokerEvent.getCardId shouldBe cardId.id
      brokerEvent.getBestPrice.getTradein shouldBe true
    }

    "seller event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val link = SellerLink.AMOUNT_IN_STOCK
      event.getSellerClickBuilder.setLink(link)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.SELLER_CLICK
      brokerEvent.getSellerClick.getLink shouldBe link
    }

    "poll event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val event = ReportedStatEvent.newBuilder()
      event.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      val poll = PollGroup.newBuilder().addQuestion("question").addAnswer("answer").build()
      event.getPollSubmitBuilder.setPoll(poll)

      val brokerEvent = brokerEventBuilder.build(Seq(event.build()), None).futureValue.head
      brokerEvent.getOriginalEvent shouldBe event.build()
      brokerEvent.getEventType shouldBe auto.events.Model.EventType.POLL_SUBMIT
      brokerEvent.getPoll.getPoll shouldBe poll
    }

    "review card event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val reviewId = Gen.alphaNumStr.next
      val title = Gen.alphaNumStr.next
      val reviewResponse =
        ReviewResponse.newBuilder().setReview(Review.newBuilder().setId(reviewId).setTitle(title).build()).build()
      when(reviewClient.getReview(reviewId)).thenReturnF(reviewResponse)

      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val showEvent = ReportedStatEvent.newBuilder()
      showEvent.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      showEvent.getReviewCardShowBuilder
        .setSelfType(TYPE_SINGLE)
        .setCardId(cardId.toPlain)
        .setCategory(category)
        .setReviewId(reviewId)

      val brokerShowEvent = brokerEventBuilder.build(Seq(showEvent.build()), None).futureValue.head
      brokerShowEvent.getOriginalEvent shouldBe showEvent.build()
      brokerShowEvent.getEventType shouldBe auto.events.Model.EventType.SNIPPET_SHOW
      brokerShowEvent.getReview.getId shouldBe reviewId
      brokerShowEvent.getReview.getTitle shouldBe title

      val clickEvent = ReportedStatEvent.newBuilder()
      clickEvent.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      clickEvent.getReviewCardClickBuilder
        .setSelfType(TYPE_SINGLE)
        .setCardId(cardId.toPlain)
        .setCategory(category)
        .setReviewId(reviewId)

      val brokerClickEvent = brokerEventBuilder.build(Seq(clickEvent.build()), None).futureValue.head
      brokerClickEvent.getOriginalEvent shouldBe clickEvent.build()
      brokerClickEvent.getEventType shouldBe auto.events.Model.EventType.SNIPPET_CLICK
      brokerClickEvent.getReview.getId shouldBe reviewId
      brokerClickEvent.getReview.getTitle shouldBe title
    }

    "video card event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val videoId = Gen.alphaNumStr.next
      val title = Gen.alphaNumStr.next
      val videoListingResponse =
        VideoListingResponse.newBuilder().addVideos(Video.newBuilder().setYoutubeId(videoId).setTitle(title)).build()
      when(videoClient.search(videoId)).thenReturnF(videoListingResponse)

      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val showEvent = ReportedStatEvent.newBuilder()
      showEvent.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      showEvent.getVideoCardShowBuilder
        .setSelfType(TYPE_SINGLE)
        .setCardId(cardId.toPlain)
        .setCategory(category)
        .setVideoId(videoId)

      val brokerShowEvent = brokerEventBuilder.build(Seq(showEvent.build()), None).futureValue.head
      brokerShowEvent.getOriginalEvent shouldBe showEvent.build()
      brokerShowEvent.getEventType shouldBe auto.events.Model.EventType.SNIPPET_SHOW
      brokerShowEvent.getVideo.getYoutubeId shouldBe videoId
      brokerShowEvent.getVideo.getTitle shouldBe title

      val clickEvent = ReportedStatEvent.newBuilder()
      clickEvent.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      clickEvent.getVideoCardClickBuilder
        .setSelfType(TYPE_SINGLE)
        .setCardId(cardId.toPlain)
        .setCategory(category)
        .setVideoId(videoId)

      val brokerClickEvent = brokerEventBuilder.build(Seq(clickEvent.build()), None).futureValue.head
      brokerClickEvent.getOriginalEvent shouldBe clickEvent.build()
      brokerClickEvent.getEventType shouldBe auto.events.Model.EventType.SNIPPET_CLICK
      brokerClickEvent.getVideo.getYoutubeId shouldBe videoId
      brokerClickEvent.getVideo.getTitle shouldBe title
    }

    "tutorial card event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val urlPart = Gen.alphaNumStr.next
      val title = Gen.alphaNumStr.next
      val article = Article(123, urlPart, title, Gen.alphaNumStr.next, Image(Map.empty))
      when(journalClient.getPost(urlPart)).thenReturnF(article)

      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val showEvent = ReportedStatEvent.newBuilder()
      showEvent.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      showEvent.getTutorialCardShowBuilder
        .setSelfType(TYPE_SINGLE)
        .setCardId(cardId.toPlain)
        .setCategory(category)
        .setTutorialArticleId(urlPart)

      val brokerShowEvent = brokerEventBuilder.build(Seq(showEvent.build()), None).futureValue.head
      brokerShowEvent.getOriginalEvent shouldBe showEvent.build()
      brokerShowEvent.getEventType shouldBe auto.events.Model.EventType.SNIPPET_SHOW
      brokerShowEvent.getPost.getUrlPart shouldBe urlPart
      brokerShowEvent.getPost.getTitle shouldBe title

      val clickEvent = ReportedStatEvent.newBuilder()
      clickEvent.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      clickEvent.getTutorialCardClickBuilder
        .setSelfType(TYPE_SINGLE)
        .setCardId(cardId.toPlain)
        .setCategory(category)
        .setTutorialArticleId(urlPart)

      val brokerClickEvent = brokerEventBuilder.build(Seq(clickEvent.build()), None).futureValue.head
      brokerClickEvent.getOriginalEvent shouldBe clickEvent.build()
      brokerClickEvent.getEventType shouldBe auto.events.Model.EventType.SNIPPET_CLICK
      brokerShowEvent.getPost.getUrlPart shouldBe urlPart
      brokerShowEvent.getPost.getTitle shouldBe title
    }

    "journal card event" in new Fixture {
      val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
      when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
      val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
      when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

      val urlPart = Gen.alphaNumStr.next
      val title = Gen.alphaNumStr.next
      val article = Article(123, urlPart, title, Gen.alphaNumStr.next, Image(Map.empty))
      when(journalClient.getPost(urlPart)).thenReturnF(article)

      val cardId = OfferIDGen.next
      val category = CategoryGen.next
      val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)
      val offer = ModelGenerators.offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen).next
      when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

      val showEvent = ReportedStatEvent.newBuilder()
      showEvent.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      showEvent.getJournalCardShowBuilder
        .setSelfType(TYPE_SINGLE)
        .setCardId(cardId.toPlain)
        .setCategory(category)
        .setJournalArticleId(urlPart)

      val brokerShowEvent = brokerEventBuilder.build(Seq(showEvent.build()), None).futureValue.head
      brokerShowEvent.getOriginalEvent shouldBe showEvent.build()
      brokerShowEvent.getEventType shouldBe auto.events.Model.EventType.SNIPPET_SHOW
      brokerShowEvent.getPost.getUrlPart shouldBe urlPart
      brokerShowEvent.getPost.getTitle shouldBe title

      val clickEvent = ReportedStatEvent.newBuilder()
      clickEvent.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
      clickEvent.getJournalCardClickBuilder
        .setSelfType(TYPE_SINGLE)
        .setCardId(cardId.toPlain)
        .setCategory(category)
        .setJournalArticleId(urlPart)

      val brokerClickEvent = brokerEventBuilder.build(Seq(clickEvent.build()), None).futureValue.head
      brokerClickEvent.getOriginalEvent shouldBe clickEvent.build()
      brokerClickEvent.getEventType shouldBe auto.events.Model.EventType.SNIPPET_CLICK
      brokerShowEvent.getPost.getUrlPart shouldBe urlPart
      brokerShowEvent.getPost.getTitle shouldBe title
    }
  }

  "photo card event" in new Fixture {
    val ipInfo = IpInfo(0, false, false, false, false, false, false, false, false, false, "", "", "")
    when(geobaseClient.ipInfo(?)(?)).thenReturnF(ipInfo)
    val deviceInfo = DeviceInfo("", "", "", "", "", "", ClientOS.IOS, "", "")
    when(pushnoyClient.getDeviceInfo(?)(?)).thenReturnF(deviceInfo)

    val photoUrl = ImageUrlGenerator.next
    val photoParts = photoUrl.getSizesMap.asScala.toSeq.head._2.split("/")
    val photoId = s"${photoParts(4)}-${photoParts(5)}"
    val photoUrls = List(photoUrl, ImageUrlGenerator.next)

    val cardId = OfferIDGen.next
    val category = CategoryGen.next
    val strictCategory: CategorySelector.StrictCategory = CategorySelector.from(category)

    val offer = ModelGenerators
      .offerGen(strictCategory, cardId.toPlain, ModelGenerators.PrivateUserRefGen)
      .next
      .toBuilder
      .setState(
        State
          .newBuilder()
          .addAllImageUrls(photoUrls.map(url => Photo.newBuilder().putAllSizes(url.getSizesMap).build()).asJava)
          .build()
      )
      .build
    when(offerLoader.findRawOffer(eeq(strictCategory), eeq(cardId), eeq(false), eeq(false))(?)).thenReturnF(offer)

    val showEvent = ReportedStatEvent.newBuilder()
    showEvent.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
    showEvent.getPhotoCardShowBuilder
      .setSelfType(TYPE_SINGLE)
      .setCardId(cardId.toPlain)
      .setCategory(category)
      .setPhotoId(photoId)

    val brokerShowEvent = brokerEventBuilder.build(Seq(showEvent.build()), None).futureValue.head
    brokerShowEvent.getOriginalEvent shouldBe showEvent.build()
    brokerShowEvent.getEventType shouldBe auto.events.Model.EventType.SNIPPET_SHOW
    val resultShowUrlParts = brokerShowEvent.getPhoto.getSizesMap.asScala.values.head.split("/")
    s"${resultShowUrlParts(4)}-${resultShowUrlParts(5)}" shouldBe photoId

    val clickEvent = ReportedStatEvent.newBuilder()
    clickEvent.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
    clickEvent.getPhotoCardClickBuilder
      .setSelfType(TYPE_SINGLE)
      .setCardId(cardId.toPlain)
      .setCategory(category)
      .setPhotoId(photoId)

    val brokerClickEvent = brokerEventBuilder.build(Seq(clickEvent.build()), None).futureValue.head
    brokerClickEvent.getOriginalEvent shouldBe clickEvent.build()
    brokerClickEvent.getEventType shouldBe auto.events.Model.EventType.SNIPPET_CLICK
    val resultClickUrlParts = brokerShowEvent.getPhoto.getSizesMap.asScala.values.head.split("/")
    s"${resultClickUrlParts(4)}-${resultClickUrlParts(5)}" shouldBe photoId
  }
}
