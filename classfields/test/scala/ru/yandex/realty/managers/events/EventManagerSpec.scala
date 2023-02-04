package ru.yandex.realty.managers.events

import akka.http.scaladsl.model.headers.{ProductVersion, RawHeader}
import com.google.protobuf.StringValue
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.Json
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.auth.AuthInfo
import ru.yandex.realty.clients.abram.AbramClient
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.clients.seller.SellerClient
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.event.RealtyEventModelGen
import ru.yandex.realty.events.{AdditionalPayload, Event, EventBatch, TrafficSourceInfo, UserInfo, WebUserInfo}
import ru.yandex.realty.features.Feature
import ru.yandex.realty.managers.events.BaseEventManager._
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.model.user
import ru.yandex.realty.persistence.OfferId
import ru.yandex.realty.platform.PlatformInfo
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.request.{Request, UserAgent}
import ru.yandex.realty.searcher.api.SearcherApi.{OfferForEventLog, OffersForEventLogResponse}
import ru.yandex.realty.seller.api.SellerApi
import ru.yandex.realty.seller.proto.api.products.ProductSearchResponse
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.sites.campaign.CampaignStorage
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.crypto.Crypto
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller
import ru.yandex.vertis.broker.client.simple.BrokerClient

import java.util
import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
  * Specs for [[EventManager]]
  *
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class EventManagerSpec extends AsyncSpecBase with RealtyEventModelGen with MockFactory {

  private val crypto = new Crypto("^tz+nmyi3(crf$8k")

  private val brokerClient = mock[BrokerClient]
  private val searcherClient = mock[SearcherClient]
  private val abramClient = mock[AbramClient]
  private val sellerClient = mock[SellerClient]
  private val paymentLogFeature = mock[Feature]
  private val sitesService = mock[SitesGroupingService]
  private val campaignStorage = new CampaignStorage(Seq.empty.asJava)
  private val eventManager = new EventManager(
    crypto,
    brokerClient,
    paymentLogFeature,
    new CallPricesEventManagerImpl(abramClient, sellerClient),
    searcherClient,
    sitesService,
    ProviderAdapter.create(campaignStorage)
  )
  implicit private val request = mock[Request]

  val utmLink = "https://realty.yandex.ru/moskva_i_moskovskaya_oblast/kupit/kvartira/" +
    "?utm_source=yandex_direct" +
    "&utm_medium=direct_sell" +
    "&utm_campaign=460_56887073_poisk_tgo_msk_sell_obshie" +
    "&utm_content=idgr-4367574770_cat-kupit_kvartiru_newbuilding_obshie_msk" +
    "&ad_source=yandex_direct" +
    "&utm_term=6079091005771274462"

  private def prepareRequest(
    userAgent: Option[UserAgent] = None,
    platformInfo: Option[PlatformInfo] = None
  ) = {
    (request.trace _)
      .expects()
      .anyNumberOfTimes()
      .returning(Traced.empty)

    (request.userAgent _)
      .expects()
      .anyNumberOfTimes()
      .returning(userAgent)

    (request.antirobotDegradation _)
      .expects()
      .anyNumberOfTimes()
      .returning(false)

    (request.platformInfo _)
      .expects()
      .anyNumberOfTimes()
      .returning(platformInfo)
  }

  "EventManager" should {

    "successfully log empty batch" in {
      prepareRequest()
      (paymentLogFeature.isEnabled _: () => Boolean).expects().returning(false)

      val eventBatch = EventBatch.getDefaultInstance
      eventManager.logEvents(eventBatch).futureValue
    }

    "successfully log single event" in {
      prepareRequest()
      (request.user _).expects().once().returning(user.UserInfo(ip = "", port = 1, AuthInfo()))
      (paymentLogFeature.isEnabled _: () => Boolean).expects().returning(false)

      val eventBatch = EventBatch.newBuilder()
      val event = eventGen.next.toBuilder
      val dump = Json.obj("billing.header.id" -> "1234567890", "billing.call.revenue" -> "100500")
      val key = crypto.encrypt(dump)
      event.setPayload(AdditionalPayload.newBuilder().setKey(key).build())
      eventBatch.addEvents(event.build())

      (sitesService
        .getSiteById(_: Long))
        .expects(*)
        .anyNumberOfTimes()
        .returns(new Site(1L))

      (brokerClient
        .send[Event](_: Option[String], _: Event, _: Option[String])(_: ProtoMarshaller[Event]))
        .expects(*, *, *, *)
        .once()
        .returning(Future.unit)

      eventManager.logEvents(eventBatch.build()).futureValue
    }

    "successfully log multiple events" in {
      prepareRequest()
      (request.user _).expects().anyNumberOfTimes().returning(user.UserInfo(ip = "", port = 1, AuthInfo()))
      (paymentLogFeature.isEnabled _: () => Boolean).expects().returning(false)

      val eventBatch = EventBatch.newBuilder()
      eventBatch.addEvents(eventGen.next)
      eventBatch.addEvents(eventGen.next)

      (sitesService
        .getSiteById(_: Long))
        .expects(*)
        .anyNumberOfTimes()
        .returns(new Site(1L))

      (brokerClient
        .send[Event](_: Option[String], _: Event, _: Option[String])(_: ProtoMarshaller[Event]))
        .expects(*, *, *, *)
        .twice()
        .returning(Future.unit)

      eventManager.logEvents(eventBatch.build()).futureValue
    }

    "enrich empty fields in traffic_source " in {
      val trafficSource = TrafficSourceInfo.newBuilder()
      trafficSource.setUtmLink(
        StringValue.newBuilder().setValue(utmLink)
      )
      BaseEventManager.enrichTrafficSource(trafficSource)
      trafficSource.getUtmCampaign shouldBe "460_56887073_poisk_tgo_msk_sell_obshie"
      trafficSource.getUtmSource shouldBe "yandex_direct"
      trafficSource.getUtmMedium shouldBe "direct_sell"
      trafficSource.getUtmTerm shouldBe "6079091005771274462"
      trafficSource.getUtmContent shouldBe "idgr-4367574770_cat-kupit_kvartiru_newbuilding_obshie_msk"
    }

    "don't touch filled utm_source fields " in {
      val trafficSource = TrafficSourceInfo.newBuilder()
      trafficSource
        .setUtmLink(
          StringValue.newBuilder().setValue(utmLink)
        )
        .setUtmCampaign("UtmCampaign")
        .setUtmSource("UtmSource")
        .setUtmMedium("UtmMedium")
        .setUtmTerm("UtmTerm")
        .setUtmContent("UtmContent")
      BaseEventManager.enrichTrafficSource(trafficSource)
      trafficSource.getUtmCampaign shouldBe "UtmCampaign"
      trafficSource.getUtmSource shouldBe "UtmSource"
      trafficSource.getUtmMedium shouldBe "UtmMedium"
      trafficSource.getUtmTerm shouldBe "UtmTerm"
      trafficSource.getUtmContent shouldBe "UtmContent"
    }

    "Enrich user info when user agent is passed in request" in {
      prepareRequest(Some(UserAgent(Seq(ProductVersion("1"), ProductVersion("2")))))
      (request.user _).expects().once().returning(user.UserInfo(ip = "", port = 1, AuthInfo()))
      val event = Event.newBuilder()
      event.setUserInfo(
        UserInfo
          .newBuilder()
      )
      BaseEventManager.enrichUserInfo(event.getUserInfoBuilder, Seq(RawHeader(HttpHeaderJA3, "ja3test")))
      event.getUserInfo.getJa3 shouldBe "ja3test"
      event.getUserInfo.getUserAgent shouldBe request.userAgent.get.toString
    }

    "Enrich user info when user agent is passed in WebUserInfo" in {
      prepareRequest()
      (request.user _).expects().once().returning(user.UserInfo(ip = "", port = 1, AuthInfo()))

      val event = Event.newBuilder()
      event.setUserInfo(
        UserInfo
          .newBuilder()
          .setWebUserInfo(
            WebUserInfo
              .newBuilder()
              .setUserAgent("ua1")
          )
      )
      BaseEventManager.enrichUserInfo(event.getUserInfoBuilder, Seq(RawHeader(HttpHeaderJA3, "ja3test")))
      event.getUserInfo.getJa3 shouldBe "ja3test"
      event.getUserInfo.getUserAgent shouldBe "ua1"
    }

    "enrich user info ip and uid on desktop" in {
      val authInfo = AuthInfo(uidOpt = Some("authInfoUid"))
      prepareRequest(None, Some(PlatformInfo("desktop", "1.0")))
      (request.user _)
        .expects()
        .once()
        .returning(user.UserInfo(ip = "225.231.208.88", port = 1011, authInfo))

      (request.hasAuthInfo _)
        .expects()
        .never()
        .returning(true)

      (request.authInfo _)
        .expects()
        .never()
        .returning(authInfo)

      val event = Event.newBuilder()
      event.setUserInfo(
        UserInfo
          .newBuilder()
          .setWebUserInfo(WebUserInfo.newBuilder().setUserAgent("ua1"))
          .setUserIp("136.120.90.12")
          .setUserUid("somUid")
      )
      BaseEventManager.enrichUserInfo(event.getUserInfoBuilder, Seq.empty)
      event.getUserInfo.getUserIp shouldBe "136.120.90.12"
      event.getUserInfo.getUserPort shouldBe 1011
      event.getUserInfo.getUserUid shouldBe "somUid"
    }

    "enrich user info ip and uid on android" in {
      val authInfo = AuthInfo(uidOpt = Some("authInfoUid"))
      prepareRequest(None, Some(PlatformInfo("android", "2.1")))

      (request.user _)
        .expects()
        .twice()
        .returning(user.UserInfo(ip = "225.231.208.88", port = 2022, authInfo))

      (request.hasAuthInfo _)
        .expects()
        .once()
        .returning(true)

      (request.authInfo _)
        .expects()
        .twice()
        .returning(authInfo)

      val event = Event.newBuilder()
      event.setUserInfo(
        UserInfo
          .newBuilder()
          .setWebUserInfo(WebUserInfo.newBuilder().setUserAgent("ua1"))
          .setUserIp("136.120.90.12")
          .setUserUid("somUid")
      )
      BaseEventManager.enrichUserInfo(event.getUserInfoBuilder, Seq.empty)
      event.getUserInfo.getUserIp shouldBe "225.231.208.88"
      event.getUserInfo.getUserPort shouldBe 2022
      event.getUserInfo.getUserUid shouldBe "authInfoUid"
    }

    "enrich site info when offer category is unknown" in {
      prepareRequest()
      (request.user _).expects().once().returning(user.UserInfo(ip = "", port = 1, AuthInfo()))
      (paymentLogFeature.isEnabled _: () => Boolean).expects().returning(true)

      val siteId = 123L
      val offerId = "1"
      val eventBuilder = eventGen.next.toBuilder
      val offerInfoBuilder = eventBuilder.getObjectInfo.getOfferInfo.toBuilder
      offerInfoBuilder.setOfferId(offerId)
      val siteInfoBuilder = eventBuilder.getObjectInfo.getSiteInfo.toBuilder
      siteInfoBuilder.setSiteId(siteId.toString).clearDeveloperId()
      val objectInfoBuilder = eventBuilder.getObjectInfo.toBuilder
      objectInfoBuilder.setOfferInfo(offerInfoBuilder).setSiteInfo(siteInfoBuilder)
      eventBuilder.setObjectInfo(objectInfoBuilder)
      val eventBatch = EventBatch.newBuilder()
      eventBatch.addEvents(eventBuilder.build())

      val offers = Future.successful(
        OffersForEventLogResponse
          .newBuilder()
          .addOffers(OfferForEventLog.newBuilder().setOffer(UnifiedOffer.newBuilder().setOfferId(offerId).build()))
          .build()
      )

      (searcherClient
        .getOffersForEventLog(_: Iterable[OfferId])(_: Traced))
        .expects(*, *)
        .once()
        .returning(offers)

      (sellerClient
        .getProductsList(_: SellerApi.ProductSearchRequest)(_: Traced))
        .expects(*, *)
        .anyNumberOfTimes()
        .returning(Future.successful(ProductSearchResponse.getDefaultInstance))

      val site = new Site(siteId)
      site.setBuilders(util.List.of(3L, 4L))
      (sitesService
        .getSiteById(_: Long))
        .expects(siteId)
        .returns(site)

      (brokerClient
        .send[Event](_: Option[String], _: Event, _: Option[String])(_: ProtoMarshaller[Event]))
        .expects(where { (_, e: Event, _, _) =>
          e.getObjectInfo.getSiteInfo.getDeveloperIdList.containsAll(java.util.Set.of("3", "4"))
        })
        .once()
        .returning(Future.unit)

      eventManager.logEvents(eventBatch.build()).futureValue
    }

    "enrich site info when where is no offer" in {
      prepareRequest()
      (request.user _).expects().once().returning(user.UserInfo(ip = "", port = 1, AuthInfo()))
      (paymentLogFeature.isEnabled _: () => Boolean).expects().returning(true)

      val siteId = 321L
      val eventBuilder = eventGen.next.toBuilder
      val siteInfoBuilder = eventBuilder.getObjectInfo.getSiteInfo.toBuilder
      siteInfoBuilder.setSiteId(siteId.toString).clearDeveloperId()
      val objectInfoBuilder = eventBuilder.getObjectInfo.toBuilder
      objectInfoBuilder
        .setSiteInfo(siteInfoBuilder)
        .clearOfferInfo()
      eventBuilder.setObjectInfo(objectInfoBuilder)
      val eventBatch = EventBatch.newBuilder()
      eventBatch.addEvents(eventBuilder.build())

      val site = new Site(siteId)
      site.setBuilders(util.List.of(5L, 6L))
      (sitesService
        .getSiteById(_: Long))
        .expects(siteId)
        .returns(site)

      (brokerClient
        .send[Event](_: Option[String], _: Event, _: Option[String])(_: ProtoMarshaller[Event]))
        .expects(where { (_, e: Event, _, _) =>
          e.getObjectInfo.getSiteInfo.getDeveloperIdList.containsAll(java.util.Set.of("6", "5"))
        })
        .once()
        .returning(Future.unit)

      eventManager.logEvents(eventBatch.build()).futureValue
    }

    "enrich user info when p0f is passed in request" in {
      prepareRequest()
      (request.user _).expects().once().returning(user.UserInfo(ip = "", port = 1, AuthInfo()))

      val event = Event.newBuilder()
      event.setUserInfo(UserInfo.newBuilder())

      BaseEventManager.enrichUserInfo(event.getUserInfoBuilder, Seq(RawHeader(HttpHeaderP0f, "p0f")))
      event.getUserInfo.getP0F shouldBe "p0f"
    }
  }
}
