package ru.auto.api.managers.events

import akka.http.scaladsl.model.StatusCodes
import com.google.protobuf.util.Timestamps
import org.apache.commons.lang3.StringUtils
import org.mockito.Mockito.verify
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.StatEvents._
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.TooManyEventsToLogException
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.offers.OfferLoader
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.model.comments.TopicGroup._
import ru.auto.api.model.events.TskvEvent
import ru.auto.api.model.events.reviews._
import ru.auto.api.model.reviews.AutoReviewsFilter
import ru.auto.api.model.reviews.ReviewModelGenerators._
import ru.auto.api.model.reviews.ReviewModelUtils._
import ru.auto.api.model.uaas.UaasResponse
import ru.auto.api.reviews.ReviewModel.Review.{Comment, Opinion, Status}
import ru.auto.api.reviews.ReviewsResponseModel.ReviewResponse
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.review.{ReviewsSearcherClient, VosReviewClient}
import ru.auto.api.services.settings.RemoteConfigManager
import ru.auto.api.services.settings.RemoteConfigManager.RemoteConfig
import ru.auto.api.util.crypt.Crypto
import ru.auto.api.util.{RequestImpl, TskvLogWriter}
import ru.auto.api.{AsyncTasksSupport, BaseSpec, DummyOperationalSupport}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

/**
  * Created by mcsim-gr on 25.07.17.
  */
class StatEventsManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport
  with DummyOperationalSupport {

  class TestableTskvLogWriter extends TskvLogWriter {
    override def writeTskv(tskv: String): Unit = super.writeTskv(tskv)
  }

  trait Ctx {
    val offerLoader: OfferLoader = mock[OfferLoader]
    val geobaseClient: GeobaseClient = mock[GeobaseClient]
    when(geobaseClient.regionIdByIp(?)(?)).thenReturnF(213)
    val tskvLogWriter: TestableTskvLogWriter = mock[TestableTskvLogWriter]
    val reviewsSearcherClient: ReviewsSearcherClient = mock[ReviewsSearcherClient]
    val vosReviewClient: VosReviewClient = mock[VosReviewClient]
    val remoteConfigManager: RemoteConfigManager = mock[RemoteConfigManager]
    val featureManager: FeatureManager = mock[FeatureManager]
    val crypto: Crypto = mock[Crypto]
    val offset: Option[Long] = None

    val feature: Feature[Boolean] = mock[Feature[Boolean]]
    when(feature.value).thenReturn(false)
    when(featureManager.reviewsNewDeliveryEnabled).thenReturn(feature)

    val statEventsManager = new StatEventsManager(
      offerLoader,
      reviewsSearcherClient,
      vosReviewClient,
      geobaseClient,
      tskvLogWriter,
      remoteConfigManager,
      crypto,
      prometheusRegistryDummy,
      featureManager
    )

    implicit protected val trace = Traced.empty
    protected val userLocation = UserLocation(108, -83.7f, 200)
    implicit protected val request = generateRequest()

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

  "Stat events manager" should {
    "write expboxes from event in tskv log" in new Ctx {
      implicit protected val r1 = generateRequest(uaasResponse = UaasResponse.empty.copy(expBoxes = Some("1,2,3")))
      val offer = OfferGen.next
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      when(tskvLogWriter.logEvent(?, ?)).thenCallRealMethod()
      Mockito.doNothing().when(tskvLogWriter).writeTskv(?)
      val remoteConfig = RemoteConfig(Map.empty)
      private val statEvent: ReportedStatEvent =
        phoneCallEventGen(offer.category).next.toBuilder.setExpBoxes("4,5,6").build()
      statEventsManager.logReportedEvent(statEvent, remoteConfig, offset)
      val asyncTasks = r1.tasks.start(StatusCodes.OK)
      asyncTasks.foreach(_.await)

      val eventCaptor: ArgumentCaptor[TskvEvent] = ArgumentCaptor.forClass(classOf[TskvEvent])
      val paramsCaptor: ArgumentCaptor[Map[String, String]] = ArgumentCaptor.forClass(classOf[Map[String, String]])
      val tskvCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      verify(tskvLogWriter).logEvent(eventCaptor.capture(), paramsCaptor.capture())
      verify(tskvLogWriter).writeTskv(tskvCaptor.capture())

      val event = eventCaptor.getValue
      val params = paramsCaptor.getValue
      val tskv = tskvCaptor.getValue

      params.getOrElse("uaas_boxes", "") shouldEqual "1,2,3"
      event.props.getOrElse("uaas_boxes", "") shouldEqual "4,5,6"
      tskv should include("uaas_boxes=4,5,6")
    }

    "write expboxes from request in tskv log" in new Ctx {
      implicit protected val r1 = generateRequest(uaasResponse = UaasResponse.empty.copy(expBoxes = Some("1,2,3")))
      val offer = OfferGen.next
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      when(tskvLogWriter.logEvent(?, ?)).thenCallRealMethod()
      Mockito.doNothing().when(tskvLogWriter).writeTskv(?)
      val remoteConfig = RemoteConfig(Map.empty)
      private val statEvent: ReportedStatEvent = phoneCallEventGen(offer.category).next
      statEventsManager.logReportedEvent(statEvent, remoteConfig, offset)
      val asyncTasks = r1.tasks.start(StatusCodes.OK)
      asyncTasks.foreach(_.await)

      val eventCaptor: ArgumentCaptor[TskvEvent] = ArgumentCaptor.forClass(classOf[TskvEvent])
      val paramsCaptor: ArgumentCaptor[Map[String, String]] = ArgumentCaptor.forClass(classOf[Map[String, String]])
      val tskvCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      verify(tskvLogWriter).logEvent(eventCaptor.capture(), paramsCaptor.capture())
      verify(tskvLogWriter).writeTskv(tskvCaptor.capture())

      val event = eventCaptor.getValue
      val params = paramsCaptor.getValue
      val tskv = tskvCaptor.getValue

      params.getOrElse("uaas_boxes", "") shouldEqual "1,2,3"
      event.props.getOrElse("uaas_boxes", "") shouldEqual ""
      tskv should include("uaas_boxes=1,2,3")
    }

    "write phone_call event in tskv log" in new Ctx {

      val offer = OfferGen.next
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      val remoteConfig = RemoteConfig(Map.empty)
      statEventsManager.logReportedEvent(phoneCallEventGen(offer.category).next, remoteConfig, offset)
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)

      verify(tskvLogWriter).logEvent(event, params)

      event.component shouldEqual "phone_call"

      event.props.getOrElse("card_id", "") shouldEqual offer.id.id.toString

      params.getOrElse("app", "") shouldEqual "ios"
      params.getOrElse("user_uuid", "") shouldEqual request.requestParams.deviceUid.get
      params.getOrElse("portal_rid", "") shouldEqual "213"
      params.getOrElse("user_lat", "") shouldEqual "108.0"
    }

    "write card_view event in tskv log" in new Ctx {
      val offer = OfferGen.next
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      statEventsManager.logReportedEvents(cardViewEventGen(offer.category).next :: Nil, offset).await
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)

      verify(tskvLogWriter).logEvent(event, params)

      event.component shouldEqual "card_view"
      event.props.getOrElse("card_id", "") shouldEqual offer.id.id.toString
      event.props.getOrElse("trade_in_allowed", "") shouldEqual "true"

      params.getOrElse("app", "") shouldEqual "ios"
      params.getOrElse("user_uuid", "") shouldEqual request.requestParams.deviceUid.get
      params.getOrElse("portal_rid", "") shouldEqual "213"
      params.getOrElse("user_lon", "") shouldEqual "-83.7"
    }

    "write trade_in_send event in tskv log" in new Ctx {
      val offer = OfferGen.next
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      statEventsManager.logReportedEvents(tradeInRequestSendEventGen().next :: Nil, offset).await
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)

      verify(tskvLogWriter).logEvent(event, params)

      event.component shouldEqual "trade_in_request_send"

      params.getOrElse("app", "") shouldEqual "ios"
      params.getOrElse("user_uuid", "") shouldEqual request.requestParams.deviceUid.get
      params.getOrElse("portal_rid", "") shouldEqual "213"
      params.getOrElse("user_lon", "") shouldEqual "-83.7"
    }

    "write trade_in_click event in tskv log" in new Ctx {
      val offer = OfferGen.next
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      statEventsManager.logReportedEvents(tradeInRequestClickEventGen().next :: Nil, offset).await
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)

      verify(tskvLogWriter).logEvent(event, params)

      event.component shouldEqual "trade_in_request_click"

      params.getOrElse("app", "") shouldEqual "ios"
      params.getOrElse("user_uuid", "") shouldEqual request.requestParams.deviceUid.get
      params.getOrElse("portal_rid", "") shouldEqual "213"
      params.getOrElse("user_lon", "") shouldEqual "-83.7"
    }

    "write review_click event in tskv log" in new Ctx {
      val review = ReviewGen.next
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      val clickRequest = ReviewClickEvent
        .newBuilder()
        .setReviewId(review.getId)
        .setEventPlace(EventPlace.REVIEW_POPULAR)
        .setSearchPosition(1)
        .setSearchQueryId("1")
        .build()
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      val reportedEvent = ReportedStatEvent.newBuilder().setReviewClickEvent(clickRequest).build()
      val remoteConfig = RemoteConfig(Map.empty)
      statEventsManager.logReportedEvent(reportedEvent, remoteConfig, offset)
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)
      verify(tskvLogWriter).logEvent(event, params)
      event.component shouldEqual "autoru.review.click"
      event.props.getOrElse("review_id", "") shouldEqual review.getId
      event.props.getOrElse("review_category", "") shouldEqual
        review.getCategory.getOrElse(Category.CATEGORY_UNKNOWN).name().toLowerCase
      event.props.getOrElse("review_date_created", "") shouldEqual Timestamps.toString(review.getPublished)
      event.props.getOrElse("review_date_updated", "") shouldEqual Timestamps.toString(review.getUpdated)
      event.props.getOrElse("review_mark", "") shouldEqual review.getMark.getOrElse("unknown_mark")
      event.props.getOrElse("review_model", "") shouldEqual review.getModel.getOrElse("unknown_model")
      event.props.getOrElse("review_super_gen", "") shouldEqual review.getSuperGenId.getOrElse(0).toString

      event.props.getOrElse("event_place", "") shouldEqual clickRequest.getEventPlace.name().toLowerCase
      event.props.getOrElse("search_query_id", "") shouldEqual clickRequest.getSearchQueryId
      event.props.getOrElse("search_position", "") shouldEqual clickRequest.getSearchPosition.toString
    }

    "write review_show event in tskv log" in new Ctx {
      val review = ReviewGen.next
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      val showRequest = ReviewShowEvent
        .newBuilder()
        .setReviewId(review.getId)
        .setEventPlace(EventPlace.REVIEW_POPULAR)
        .setSearchPosition(1)
        .setSearchQueryId("1")
        .build()
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      val reportedEvent = ReportedStatEvent.newBuilder().setReviewShowEvent(showRequest).build()
      val remoteConfig = RemoteConfig(Map.empty)
      statEventsManager.logReportedEvent(reportedEvent, remoteConfig, offset)
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)
      verify(tskvLogWriter).logEvent(event, params)

      event.component shouldEqual "autoru.review.show"
      event.props.getOrElse("event_place", "") shouldEqual showRequest.getEventPlace.name().toLowerCase
      event.props.getOrElse("search_query_id", "") shouldEqual showRequest.getSearchQueryId
      event.props.getOrElse("search_position", "") shouldEqual showRequest.getSearchPosition.toString
    }

    "write review_comment event in tskv log" in new Ctx {
      val comment = Comment.newBuilder().setId("1").setMessage("test").build()
      val review = ReviewGen.next
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      statEventsManager.logCommentEvent(REVIEWS, review.getId, comment, 0)

      verify(tskvLogWriter).logEvent(event, params)

      event shouldBe an[ReviewCommentTskvEvent]
      event.component shouldEqual "autoru.review.comment_add"

      event.props.getOrElse("comment_id", "") shouldEqual "1"
    }

    "write review_view event in tskv log" in new Ctx {
      val review = ReviewGen.next
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      statEventsManager.logReviewViewEvent(review)
      verify(tskvLogWriter).logEvent(event, params)

      event shouldBe an[ReviewViewTskvEvent]
      event.component shouldEqual "autoru.review.view"
    }

    "write review_opinion event in tskv log" in new Ctx {
      val review = ReviewGen.next
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      statEventsManager.logReviewOpinionEvent(review.getId, Opinion.LIKE)
      verify(tskvLogWriter).logEvent(event, params)

      event shouldBe an[ReviewOpinionTskvEvent]
      event.component shouldEqual "autoru.review.opinion"
    }

    "write review_create event in tskv log" in new Ctx {
      val review = ReviewGen.next.toBuilder.setStatus(Status.ENABLED).build()
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      statEventsManager.logReviewCreateUpdateEvent(review, ReviewCreateUpdateTskvEvent.Create)
      verify(tskvLogWriter).logEvent(event, params)

      event shouldBe an[ReviewCreateUpdateTskvEvent]
      event.component shouldEqual "autoru.review.create"
    }

    "write review_update event in tskv log" in new Ctx {
      val review = ReviewGen.next.toBuilder.setStatus(Status.ENABLED).build()
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      statEventsManager.logReviewCreateUpdateEvent(review, ReviewCreateUpdateTskvEvent.Update)
      verify(tskvLogWriter).logEvent(event, params)

      event shouldBe an[ReviewCreateUpdateTskvEvent]
      event.component shouldEqual "autoru.review.update"
    }

    "write review_draft_update event in tskv log" in new Ctx {
      val review = ReviewGen.next.toBuilder.setStatus(Status.DRAFT).build()
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      statEventsManager.logReviewCreateUpdateEvent(review, ReviewCreateUpdateTskvEvent.Update)
      verify(tskvLogWriter).logEvent(event, params)

      event shouldBe an[ReviewCreateUpdateTskvEvent]
      event.component shouldEqual "autoru.review.draft.update"
    }

    "write review_draft_create event in tskv log" in new Ctx {
      val review = ReviewGen.next.toBuilder.setStatus(Status.DRAFT).build()
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      statEventsManager.logReviewCreateUpdateEvent(review, ReviewCreateUpdateTskvEvent.Create)
      verify(tskvLogWriter).logEvent(event, params)

      event shouldBe an[ReviewCreateUpdateTskvEvent]
      event.component shouldEqual "autoru.review.draft.create"
    }

    "write review_listing event in tskv log" in new Ctx {
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      val filter = AutoReviewsFilter(
        mark = Some("mark"),
        model = Some("model"),
        category = Some(Category.TRUCKS),
        subCategory = Some("subCategory"),
        superGenId = Some(Seq(1)),
        techParamId = Some(Seq(2)),
        photo = Some(true),
        userId = Some("user")
      )
      val paging = Paging(1, 10)
      val sorting = SortingByField("test_field", true)
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      statEventsManager.logReviewListingEvent("test_id", filter, paging, sorting)
      verify(tskvLogWriter).logEvent(event, params)

      event shouldBe an[ReviewsListingTskvEvent]
      event.component shouldEqual "autoru.review.listing"

      event.props.getOrElse("search_query_id", "") shouldEqual "test_id"
      event.props.getOrElse("category", "") shouldEqual filter.category.getOrElse(Category.CATEGORY_UNKNOWN).name()
      event.props.getOrElse("sub_category", "") shouldEqual filter.subCategory.getOrElse("")
      event.props.getOrElse("mark", "") shouldEqual filter.mark.getOrElse("")
      event.props.getOrElse("model", "") shouldEqual filter.model.getOrElse("")
      event.props.getOrElse("super_gen_id", "") shouldEqual filter.superGenId.getOrElse(0).toString
      event.props.getOrElse("tech_param_id", "") shouldEqual filter.techParamId.getOrElse(0).toString
      event.props.getOrElse("page", "") shouldEqual paging.page.toString
      event.props.getOrElse("page_size", "") shouldEqual paging.pageSize.toString
      event.props.getOrElse("sort_field", "") shouldEqual "test_field"
      event.props.getOrElse("sort_direction", "") shouldEqual "desc"
      event.props.getOrElse("has_photo", "") shouldEqual "true"
      event.props.getOrElse("user_id", "") shouldEqual "user"
    }

    "write utm-related fields in tskv log" in new Ctx {
      val review = ReviewGen.next
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      val clickRequest = ReviewClickEvent
        .newBuilder()
        .setReviewId(review.getId)
        .setEventPlace(EventPlace.REVIEW_POPULAR)
        .setSearchPosition(1)
        .setSearchQueryId("1")
        .build()
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      val reportedEvent = ReportedStatEvent
        .newBuilder()
        .setReviewClickEvent(clickRequest)
        .setUtmSource("test-utm-source")
        .setUtmCampaign("test-utm-campaign")
        .setUtmContent("test-utm-content")
        .setUtmMedium("test-utm-medium")
        .setFrom("test-from")
        .build()
      val remoteConfig = RemoteConfig(Map.empty)
      statEventsManager.logReportedEvent(reportedEvent, remoteConfig, offset)
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)
      verify(tskvLogWriter).logEvent(event, params)
      event.component shouldEqual "autoru.review.click"
      event.props("utm_source") shouldEqual "test-utm-source"
      event.props("utm_campaign") shouldEqual "test-utm-campaign"
      event.props("utm_content") shouldEqual "test-utm-content"
      event.props("utm_medium") shouldEqual "test-utm-medium"
      event.props("from") shouldEqual "test-from"
    }

    "write remote config in tskv log" in new Ctx {
      val review = ReviewGen.next
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      val clickRequest = ReviewClickEvent
        .newBuilder()
        .setReviewId(review.getId)
        .setEventPlace(EventPlace.REVIEW_POPULAR)
        .setSearchPosition(1)
        .setSearchQueryId("1")
        .build()
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      val remoteConfig = RemoteConfig(Map("test-key" -> "test-value"))
      when(remoteConfigManager.getRemoteConfig).thenReturnF(remoteConfig)
      val reportedEvent = ReportedStatEvent
        .newBuilder()
        .setReviewClickEvent(clickRequest)
        .build()
      statEventsManager.logReportedEvent(reportedEvent, remoteConfig, offset)
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)
      verify(tskvLogWriter).logEvent(event, params)
      event.component shouldEqual "autoru.review.click"
      params("remote_config_test-key") shouldEqual "test-value"
    }

    "write experiments in tskv log" in new Ctx {
      implicit override val request: RequestImpl = {
        val r = new RequestImpl
        r.setApplication(Application.iosApp)
        r.setVersion(Version.V1_0)
        r.setRequestParams(
          RequestParams.construct(
            "1.1.1.1",
            deviceUid = Some("testUid"),
            userLocation = Some(userLocation),
            experimentBucket = Some("1"),
            experiments = Set("test-experiment")
          )
        )
        r.setUser(PrivateUserRefGen.next)
        r.setTrace(trace)
        r
      }

      val review = ReviewGen.next
      val reviewResponse = ReviewResponse.newBuilder().setReview(review).build()
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      val clickRequest = ReviewClickEvent
        .newBuilder()
        .setReviewId(review.getId)
        .setEventPlace(EventPlace.REVIEW_POPULAR)
        .setSearchPosition(1)
        .setSearchQueryId("1")
        .build()
      val remoteConfig = RemoteConfig(Map("test-key" -> "test-value"))
      when(remoteConfigManager.getRemoteConfig).thenReturnF(remoteConfig)
      when(reviewsSearcherClient.getReview(?)(?)).thenReturn(Future.successful(reviewResponse))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      val reportedEvent = ReportedStatEvent
        .newBuilder()
        .setReviewClickEvent(clickRequest)
        .build()
      statEventsManager.logReportedEvent(reportedEvent, remoteConfig, offset)
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)
      verify(tskvLogWriter).logEvent(event, params)
      event.component shouldEqual "autoru.review.click"
      params("remote_config_test-key") shouldEqual "test-value"
      params("experiments") shouldBe "test-experiment"
      params("testing_group") shouldBe "1"
    }

    "throw TooManyEventsToLogException if pass more then MaxEventsCount" in new Ctx {
      val events = Gen.listOfN(201, PhoneCallEventGen).next
      intercept[TooManyEventsToLogException] {
        statEventsManager.logReportedEvents(events, offset).await
      }
    }

    "write app and view_type based on x-application-id" in new Ctx {
      implicit override val request: RequestImpl = generateRequest(
        application = Some(Application.web),
        xApplicationId = Some("af-desktop-lk")
      )

      val offer = OfferGen.next
      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }

      val remoteConfig = RemoteConfig(Map.empty)
      statEventsManager.logReportedEvent(phoneCallEventGen(offer.category).next, remoteConfig, offset)
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)

      verify(tskvLogWriter).logEvent(event, params)

      event.component shouldEqual "phone_call"
      params.getOrElse("app", "") shouldEqual "desktop"
      params.getOrElse("view_type", "") shouldEqual "desktop"
    }

    "prefer custom view_type if it is provided" in new Ctx {
      implicit override val request: RequestImpl = generateRequest(
        application = Some(Application.web)
      )

      val offer = OfferGen.next
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))
      when(tskvLogWriter.logEvent(?, ?)).thenCallRealMethod()
      Mockito.doNothing().when(tskvLogWriter).writeTskv(?)

      val remoteConfig = RemoteConfig(Map.empty)
      val reportedEvent = phoneCallEventGen(offer.category).next.toBuilder.setViewType(ViewType.VIEW_TYPE_MOBILE).build
      statEventsManager.logReportedEvent(reportedEvent, remoteConfig, offset)
      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)

      val eventCaptor: ArgumentCaptor[TskvEvent] = ArgumentCaptor.forClass(classOf[TskvEvent])
      val paramsCaptor: ArgumentCaptor[Map[String, String]] = ArgumentCaptor.forClass(classOf[Map[String, String]])
      val tskvCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      verify(tskvLogWriter).logEvent(eventCaptor.capture(), paramsCaptor.capture())
      verify(tskvLogWriter).writeTskv(tskvCaptor.capture())

      val event = eventCaptor.getValue
      val params = paramsCaptor.getValue
      val tskv = tskvCaptor.getValue

      params.getOrElse("app", "") shouldEqual "frontend"
      params.getOrElse("view_type", "") shouldEqual "frontend"
      event.props.getOrElse("view_type", "") shouldEqual "mobile"
      tskv should include("view_type=mobile")
      StringUtils.countMatches(tskv, "view_type") shouldBe 1
    }

    "write vas_cancel_event event in tskv log" in new Ctx {
      implicit override val request: RequestImpl = generateRequest()

      var event: TskvEvent = TskvEvent.empty
      var params: Map[String, String] = Map.empty

      val offer = OfferGen.next
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      stub(tskvLogWriter.logEvent _) { case (v, p) => event = v; params = p }
      when(remoteConfigManager.getRemoteConfig).thenReturnF(RemoteConfig(Map.empty))

      val reportedEvent = vasEventGen.next
      val remoteConfig = RemoteConfig(Map.empty)

      statEventsManager.logReportedEvent(reportedEvent, remoteConfig, offset)

      val asyncTasks = request.tasks.start(StatusCodes.OK)
      asyncTasks.size shouldBe 1
      asyncTasks.foreach(_.await)

      verify(tskvLogWriter).logEvent(event, params)

      event.component shouldEqual "vas_cancel_event"
      event.props.getOrElse("app_version", "") shouldEqual reportedEvent.getVasCancelEvent.getAppVersion
      event.props.getOrElse("base_price", "") shouldEqual reportedEvent.getVasCancelEvent.getBasePrice.toString
      event.props.getOrElse("offer_id", "") shouldEqual offer.id.toString
      event.props.getOrElse("card_rid", "") shouldEqual offer.getSeller.getLocation.getGeobaseId.toString
      event.props.getOrElse("card_year", "") shouldEqual offer.getDocuments.getYear.toString
      params.getOrElse("user_uuid", "") shouldEqual request.requestParams.deviceUid.get
      params.getOrElse("app", "") shouldEqual "ios"
    }

  }
}
