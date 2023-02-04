package ru.auto.api.managers.personalization

import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.{BeforeAndAfterEach, Ignore}
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.DummyOperationalSupport
import ru.auto.api.ResponseModel.OfferListingResponse
import ru.auto.api.broker_events.BigbEvents.BigbSearcherEvent
import ru.auto.api.features.FeatureManager
import ru.auto.api.geo.Tree
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.decay.{DecayManager, DecayOptions}
import ru.auto.api.managers.enrich.{EnrichManager, EnrichOptions}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.searcher.SearcherRequest
import ru.auto.api.services.bigbrother.DefaultBigBrotherClient._
import ru.auto.api.services.bigbrother.{BigBrotherSearchParams, DefaultBigBrotherClient}
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.heh.HehClient
import ru.auto.api.services.recommender.RecommenderClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.Request
import ru.auto.api.util.search.SearchMappings
import ru.yandex.proto.crypta.Profile
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

@Ignore
class PersonalizationManagerSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest
  with DummyOperationalSupport
  with BeforeAndAfterEach
  with MockitoSupport {

  private val searcher = mock[SearcherClient]

  private val client = new DefaultBigBrotherClient(http)
  implicit override def request: Request = super.request
  override protected def beforeEach(): Unit = reset(searcher)

  private val brokerClient = mock[BrokerClient]
  when(brokerClient.send(any[String](), any[BigbSearcherEvent]())(?)).thenReturn(Future.unit)

  private val recommenderClient = mock[RecommenderClient]
  when(recommenderClient.getTechParams(?)(?)).thenReturn(Future.successful(List()))

  private val hehClient = mock[HehClient]
  when(hehClient.recommend(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(List.empty))

  private val geobaseClient = mock[GeobaseClient]
  when(geobaseClient.regionIdByIp(?)(?)).thenReturn(Future.successful(213L))

  private val tree = mock[Tree]

  private val decayManager = mock[DecayManager]
  stub(decayManager.decay(_: Seq[Offer], _: DecayOptions)(_: Request)) {
    case (offers, _, _) => Future.successful(offers)
  }

  val enrichManager: EnrichManager = mock[EnrichManager]
  stub(enrichManager.enrich(_: Seq[Offer], _: EnrichOptions)(_: Request)) {
    case (offers, _, _) => Future.successful(offers)
  }

  private val searchMappings = mock[SearchMappings]

  private val featureManager = mock[FeatureManager]
  when(featureManager.useNewRecommendations).thenReturn(Feature("", _ => false))

  private val manager =
    new PersonalizationManager(
      searcher,
      client,
      brokerClient,
      enrichManager,
      decayManager,
      recommenderClient,
      searchMappings,
      hehClient,
      geobaseClient,
      tree,
      featureManager
    )
  private val profileYUid = "123456"
  private val profileIdfa = "234567"
  private val profileGaid = "345678"
  private val profileIvf = "456789"
  private val profileMmDeviceId = "5567810"

  private val emptySimilarResponse = OfferListingResponse.newBuilder().addAllOffers(Seq.empty.asJava).build()
  private val maxOffers = 15

  private def bigBrotherReturnProfile(resourcePath: String,
                                      yandexUid: Option[String] = None,
                                      idfa: Option[String] = None,
                                      gaid: Option[String] = None,
                                      ifv: Option[String] = None,
                                      mmDeviceId: Option[String] = None): Unit = {
    var url = s"/bigb?$FormatParam=protobuf"
    yandexUid.foreach(uid => url += s"&$BigBrotherUidParam=$uid")
    idfa.foreach(uid => url += s"&$IdfaParam=$uid")
    gaid.foreach(uid => url += s"&$GaidParam=$uid")
    ifv.foreach(uid => url += s"&$IfvParam=$uid")
    mmDeviceId.foreach(uid => url += s"&$MmDeviceIdParam=$uid")
    url += s"&$ClientParam=autoru-api"
    http.expectUrl(url)
    http.respondWithProtoFrom[Profile](resourcePath)
  }

  private def cars(count: Int, milliage: Int, configurationId: Option[Long] = None): List[Offer] =
    Gen.listOfN(count, OfferGen).next.map { o =>
      val oNew = o.toBuilder
      oNew.getStateBuilder.setMileage(0)
      configurationId.foreach(oNew.getCarInfoBuilder.setConfigurationId)
      oNew.build()
    }

  private val viewedOffers = Gen.listOfN(45, OfferGen).next
  private val similarOffersWuthoutNew = cars(count = 1, milliage = 100)

  private val regularSimilar = Gen.listOfN(5, OfferGen).next ++
    cars(count = 1, milliage = 0, configurationId = Some(100L)) ++
    cars(count = 1, milliage = 0, configurationId = Some(200L))

  private val newSimilar = cars(count = 3, milliage = 0, configurationId = Some(100L)) ++
    cars(count = 2, milliage = 0, configurationId = Some(200L))

  "PersonalizationManager" should {
    "returns recommended offers" in {
      val viewed = viewedOffers.take(5)
      val viewedResponse = OfferListingResponse.newBuilder().addAllOffers(viewed.asJava).build()
      val similarResponse =
        OfferListingResponse.newBuilder().addAllOffers((similarOffersWuthoutNew ++ viewed.take(1)).asJava).build()
      bigBrotherReturnProfile("/bigbrother/profile.json", yandexUid = Some(profileYUid))
      when(searcher.searchOffersByIds(?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(viewedResponse))
        .thenReturn(Future.successful(similarResponse))
        .thenReturn(Future.successful(emptySimilarResponse))

      val result =
        manager
          .getRecommendedOffers(
            BigBrotherSearchParams.apply(yandexUid = Some(profileYUid)),
            Set.empty,
            doNotAddLastViewedParam = false,
            maxOffers,
            None
          )
          .futureValue
          .getOffersList
          .asScala
          .toSeq

      verify(decayManager).decay(MockitoSupport.eq.apply(result), MockitoSupport.eq.apply(DecayOptions.full))(?)

      result shouldBe viewed ++ similarOffersWuthoutNew
    }

    "returns recommended offers without last viewed" in {
      val viewed = viewedOffers.slice(5, 10)
      val viewedResponse = OfferListingResponse.newBuilder().addAllOffers(viewed.asJava).build()
      val similarResponse =
        OfferListingResponse.newBuilder().addAllOffers((similarOffersWuthoutNew ++ viewed.take(1)).asJava).build()
      bigBrotherReturnProfile("/bigbrother/profile.json", yandexUid = Some(profileYUid))
      when(searcher.searchOffersByIds(?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(viewedResponse))
        .thenReturn(Future.successful(similarResponse))
        .thenReturn(Future.successful(emptySimilarResponse))

      val result =
        manager
          .getRecommendedOffers(
            BigBrotherSearchParams.apply(yandexUid = Some(profileYUid)),
            Set.empty,
            doNotAddLastViewedParam = true,
            maxOffers,
            None
          )
          .futureValue
          .getOffersList
          .asScala
          .toSeq

      verify(decayManager).decay(MockitoSupport.eq.apply(result), MockitoSupport.eq.apply(DecayOptions.full))(?)

      result shouldBe similarOffersWuthoutNew
    }

    "returns recommended offers with only one new offer in one configurationId" in {
      val viewed = viewedOffers.slice(5, 10)
      val viewedResponse = OfferListingResponse.newBuilder().addAllOffers(viewed.asJava).build()
      val similarResponse =
        OfferListingResponse.newBuilder().addAllOffers((regularSimilar ++ newSimilar).asJava).build()
      bigBrotherReturnProfile("/bigbrother/profile.json", yandexUid = Some(profileYUid))
      when(searcher.searchOffersByIds(?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(viewedResponse))
        .thenReturn(Future.successful(similarResponse))
        .thenReturn(Future.successful(emptySimilarResponse))

      val result =
        manager
          .getRecommendedOffers(
            BigBrotherSearchParams.apply(yandexUid = Some(profileYUid)),
            Set.empty,
            doNotAddLastViewedParam = true,
            maxOffers,
            None
          )
          .futureValue
          .getOffersList
          .asScala
          .toSeq

      verify(decayManager).decay(MockitoSupport.eq.apply(result), MockitoSupport.eq.apply(DecayOptions.full))(?)

      result.toSet shouldBe regularSimilar.toSet
    }

    "return empty list of offers" in {
      bigBrotherReturnProfile("/bigbrother/empty_items_profile.json", yandexUid = Some(profileYUid))

      val result =
        manager
          .getRecommendedOffers(
            BigBrotherSearchParams.apply(yandexUid = Some(profileYUid)),
            Set.empty,
            doNotAddLastViewedParam = false,
            maxOffers,
            None
          )
          .futureValue
          .getOffersList
          .asScala
          .toSeq

      verify(searcher, times(0)).searchOffersByIds(?, ?, ?, ?, ?, ?)(?, ?)
      verify(decayManager).decay(MockitoSupport.eq.apply(result), MockitoSupport.eq.apply(DecayOptions.full))(?)
      result shouldBe Seq.empty
    }

    "return non empty response for cold start" in {
      val viewed = viewedOffers.slice(10, 15)
      val viewedResponse = OfferListingResponse.newBuilder().addAllOffers(viewed.asJava).build()
      bigBrotherReturnProfile("/bigbrother/cold_start_items_profile.json", yandexUid = Some(profileYUid))
      val coldStartIds = Set(21028057, 21134181).map(_.toString)

      val requestMatcher = argThat[SearcherRequest] { arg =>
        arg.params.getOrElse("configuration_id", Set.empty) == coldStartIds
      }

      when(searcher.searchOffersByIds(requestMatcher, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(viewedResponse))

      val result =
        manager
          .getRecommendedOffers(
            BigBrotherSearchParams.apply(yandexUid = Some(profileYUid)),
            Set.empty,
            doNotAddLastViewedParam = false,
            maxOffers,
            None
          )
          .futureValue
          .getOffersList
          .asScala
          .toSeq

      verify(decayManager).decay(MockitoSupport.eq.apply(result), MockitoSupport.eq.apply(DecayOptions.full))(?)
      result should contain theSameElementsAs viewed
    }

    "returns recommended offers when provided only idfa" in {
      val viewed = viewedOffers.slice(15, 20)
      val viewedResponse = OfferListingResponse.newBuilder().addAllOffers(viewed.asJava).build()
      val similarResponse =
        OfferListingResponse.newBuilder().addAllOffers((similarOffersWuthoutNew ++ viewed.take(1)).asJava).build()
      bigBrotherReturnProfile("/bigbrother/profile.json", idfa = Some(profileIdfa))
      when(searcher.searchOffersByIds(?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(viewedResponse))
        .thenReturn(Future.successful(similarResponse))
        .thenReturn(Future.successful(emptySimilarResponse))

      val result =
        manager
          .getRecommendedOffers(
            BigBrotherSearchParams.apply(idfa = Some(profileIdfa)),
            Set.empty,
            doNotAddLastViewedParam = false,
            maxOffers,
            None
          )
          .futureValue
          .getOffersList
          .asScala
          .toSeq

      verify(decayManager).decay(MockitoSupport.eq.apply(result), MockitoSupport.eq.apply(DecayOptions.full))(?)
      result shouldBe viewed ++ similarOffersWuthoutNew
    }

    "returns recommended offers when provided only gaid" in {
      val viewed = viewedOffers.slice(20, 25)
      val viewedResponse = OfferListingResponse.newBuilder().addAllOffers(viewed.asJava).build()
      val similarResponse =
        OfferListingResponse.newBuilder().addAllOffers((similarOffersWuthoutNew ++ viewed.take(1)).asJava).build()
      bigBrotherReturnProfile("/bigbrother/profile.json", gaid = Some(profileGaid))
      when(searcher.searchOffersByIds(?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(viewedResponse))
        .thenReturn(Future.successful(similarResponse))
        .thenReturn(Future.successful(emptySimilarResponse))

      val result =
        manager
          .getRecommendedOffers(
            BigBrotherSearchParams.apply(gaid = Some(profileGaid)),
            Set.empty,
            doNotAddLastViewedParam = false,
            maxOffers,
            None
          )
          .futureValue
          .getOffersList
          .asScala
          .toSeq

      verify(decayManager).decay(MockitoSupport.eq.apply(result), MockitoSupport.eq.apply(DecayOptions.full))(?)
      result shouldBe viewed ++ similarOffersWuthoutNew
    }

    "returns recommended offers when provided both gaid and yandexuid" in {
      val viewed = viewedOffers.slice(25, 30)
      val viewedResponse = OfferListingResponse.newBuilder().addAllOffers(viewed.asJava).build()
      val similarResponse =
        OfferListingResponse.newBuilder().addAllOffers((similarOffersWuthoutNew ++ viewed.take(1)).asJava).build()
      bigBrotherReturnProfile("/bigbrother/profile.json", yandexUid = Some(profileYUid), idfa = Some(profileIdfa))
      when(searcher.searchOffersByIds(?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(viewedResponse))
        .thenReturn(Future.successful(similarResponse))
        .thenReturn(Future.successful(emptySimilarResponse))

      val result = manager
        .getRecommendedOffers(
          BigBrotherSearchParams.apply(yandexUid = Some(profileYUid), idfa = Some(profileIdfa)),
          Set.empty,
          doNotAddLastViewedParam = false,
          maxOffers,
          None
        )
        .futureValue
        .getOffersList
        .asScala
        .toSeq

      verify(decayManager).decay(MockitoSupport.eq.apply(result), MockitoSupport.eq.apply(DecayOptions.full))(?)
      result shouldBe viewed ++ similarOffersWuthoutNew
    }

    "returns recommended offers when provided only ifv" in {
      val viewed = viewedOffers.slice(35, 40)
      val viewedResponse = OfferListingResponse.newBuilder().addAllOffers(viewed.asJava).build()
      val similarResponse =
        OfferListingResponse.newBuilder().addAllOffers((similarOffersWuthoutNew ++ viewed.take(1)).asJava).build()
      bigBrotherReturnProfile("/bigbrother/profile.json", ifv = Some(profileIvf))
      when(searcher.searchOffersByIds(?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(viewedResponse))
        .thenReturn(Future.successful(similarResponse))
        .thenReturn(Future.successful(emptySimilarResponse))

      val result = manager
        .getRecommendedOffers(
          BigBrotherSearchParams.apply(ifv = Some(profileIvf)),
          Set.empty,
          doNotAddLastViewedParam = false,
          maxOffers,
          None
        )
        .futureValue
        .getOffersList
        .asScala
        .toSeq

      verify(decayManager).decay(MockitoSupport.eq.apply(result), MockitoSupport.eq.apply(DecayOptions.full))(?)
      result shouldBe viewed ++ similarOffersWuthoutNew
    }

    "returns recommended offers when provided only mmDeviceId" in {
      val viewed = viewedOffers.slice(40, 45)
      val viewedResponse = OfferListingResponse.newBuilder().addAllOffers(viewed.asJava).build()
      val similarResponse =
        OfferListingResponse.newBuilder().addAllOffers((similarOffersWuthoutNew ++ viewed.take(1)).asJava).build()
      bigBrotherReturnProfile("/bigbrother/profile.json", mmDeviceId = Some(profileMmDeviceId))
      when(searcher.searchOffersByIds(?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(viewedResponse))
        .thenReturn(Future.successful(similarResponse))
        .thenReturn(Future.successful(emptySimilarResponse))

      val result = manager
        .getRecommendedOffers(
          BigBrotherSearchParams.apply(mmDeviceId = Some(profileMmDeviceId)),
          Set.empty,
          doNotAddLastViewedParam = false,
          maxOffers,
          None
        )
        .futureValue
        .getOffersList
        .asScala
        .toSeq

      verify(decayManager).decay(MockitoSupport.eq.apply(result), MockitoSupport.eq.apply(DecayOptions.full))(?)
      result shouldBe viewed ++ similarOffersWuthoutNew
    }
  }

}
