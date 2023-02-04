package ru.auto.api.managers.offers

import akka.http.scaladsl.model.StatusCodes
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel._
import ru.auto.api.ResponseModel.{OfferListingResponse, ResponseStatus}
import ru.auto.api.auth.{Application, ApplicationToken, StaticApplication}
import ru.auto.api.broker_events.BigbEvents.BigbSearcherEvent
import ru.auto.api.exceptions._
import ru.auto.api.experiments.RelatedForCardFromRecommendationService
import ru.auto.api.extdata.DataService
import ru.auto.api.features.FeatureManager
import ru.auto.api.geo.Tree
import ru.auto.api.managers.antiparser.AntiParserManager
import ru.auto.api.managers.app2app.App2AppHandleCrypto
import ru.auto.api.managers.counters.CountersManager
import ru.auto.api.managers.decay.{DecayManager, DecayOptions}
import ru.auto.api.managers.enrich.{EnrichManager, EnrichOptions}
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.favorite.WatchManager
import ru.auto.api.managers.offers.PhoneViewNeedAuthManager.{NeedAuthResult, Skip}
import ru.auto.api.managers.searcher.SearcherManager
import ru.auto.api.managers.sync.SyncManager
import ru.auto.api.model.CategorySelector.StrictCategory
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.model.gen.BasicGenerators.readableString
import ru.auto.api.model.searcher.{GroupBy, SearcherRequest}
import ru.auto.api.services.bigbrother.{BigBrotherClient, BigBrotherSearchParams}
import ru.auto.api.services.counter.CounterClient
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.heh.HehClient
import ru.auto.api.services.history.HistoryClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.octopus.OctopusClient
import ru.auto.api.services.recommender.RecommenderClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.util.search.SearchesUtils
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.{ApiOfferModel, AsyncTasksSupport, BaseSpec}
import ru.yandex.proto.crypta.Profile
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 07.03.17
  */
class OfferCardManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with AsyncTasksSupport {

  val searcherClient: SearcherClient = mock[SearcherClient]
  val enrichManager: EnrichManager = mock[EnrichManager]
  val phoneRedirectManager: PhoneRedirectManager = mock[PhoneRedirectManager]
  val offerLoader: EnrichedOfferLoader = mock[EnrichedOfferLoader]
  val antiParserManager: AntiParserManager = mock[AntiParserManager]
  val counterClient: CounterClient = mock[CounterClient]
  val historyClient: HistoryClient = mock[HistoryClient]
  val decayManager: DecayManager = mock[DecayManager]
  val statEventsManager: StatEventsManager = mock[StatEventsManager]
  val tree: Tree = mock[Tree]
  val watchManager: WatchManager = mock[WatchManager]
  val optionsNoTruncate: DecayOptions = DecayOptions.full.copy(truncatePriceHistory = false)
  val octopusClient: OctopusClient = mock[OctopusClient]
  val featureManager: FeatureManager = mock[FeatureManager]
  val recommenderClient: RecommenderClient = mock[RecommenderClient]
  private val bigBrotherClient = mock[BigBrotherClient]
  val fakeManager: FakeManager = mock[FakeManager]
  val countersManager: CountersManager = mock[CountersManager]
  val hehClient: HehClient = mock[HehClient]
  val dataService: DataService = mock[DataService]
  val geobaseClient: GeobaseClient = mock[GeobaseClient]
  val bigBrotherSearchParams: BigBrotherSearchParams = mock[BigBrotherSearchParams]

  when(bigBrotherClient.getProfile(?)(?)).thenReturn(Future.successful(Profile.getDefaultInstance))

  private val brokerClient = mock[BrokerClient]
  when(brokerClient.send(any[String](), any[BigbSearcherEvent]())(?)).thenReturn(Future.unit)

  private val app2appHandleCrypto = mock[App2AppHandleCrypto]

  private val phoneViewNeedAuthManager = mock[PhoneViewNeedAuthManager]
  when(phoneViewNeedAuthManager.check(?)(?)).thenReturn(Future.successful(Skip))

  val offerCardManager = new OfferCardManager(
    offerLoader,
    searcherClient,
    historyClient,
    decayManager,
    enrichManager,
    fakeManager,
    phoneRedirectManager,
    antiParserManager,
    statEventsManager,
    tree,
    watchManager,
    featureManager,
    recommenderClient,
    bigBrotherClient,
    brokerClient,
    app2appHandleCrypto,
    countersManager,
    phoneViewNeedAuthManager,
    hehClient,
    geobaseClient
  )

  implicit private val trace: Traced = Traced.empty

  private def generateReq(requestParams: RequestParams = RequestParams.construct("1.1.1.1"),
                          application: StaticApplication = Application.iosApp,
                          token: ApplicationToken = TokenServiceImpl.iosApp,
                          user: UserRef = PersonalUserRefGen.next): RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(requestParams)
    r.setApplication(application)
    r.setToken(token)
    r.setUser(user)
    r.setTrace(trace)
    r
  }
  implicit private var request: RequestImpl = generateReq()

  before {
    reset(
      phoneRedirectManager,
      historyClient,
      searcherClient,
      decayManager,
      tree,
      antiParserManager,
      watchManager,
      offerLoader,
      app2appHandleCrypto
    )
  }

  when(featureManager.allowAutoruPanoramas).thenReturn {
    new Feature[Boolean] {
      override def name: String = "allow_autoru_panoramas"
      override def value: Boolean = false
    }
  }

  private val maxRecommendedTechParamsFeature: Feature[Int] = mock[Feature[Int]]
  when(featureManager.maxRecommendedTechParamsForCard).thenReturn(maxRecommendedTechParamsFeature)

  "OfferCardManager.getOffer" should {
    "load COMMERCIAL offer" in {
      forAll(OfferGen) { offer1 =>
        request = generateReq()
        reset(historyClient, tree, watchManager, offerLoader)
        val offer = offer1.updated(_.setSellerType(SellerType.COMMERCIAL))
        val historyEntityResponse = HistoryEntityResponseGen.next
        val historyEntityId = historyEntityResponse.entity.entityId

        val category = CategorySelector.Cars
        val id = offer.id
        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(historyClient.addHistory(?, ?, ?)(?)).thenReturnF(historyEntityResponse)
        when(tree.region(?)).thenReturn(None)
        when(watchManager.addWatch(?, ?)(?)).thenReturnF(())

        val result = offerCardManager.getOfferCardResponse(category, id).await
        val futures = request.tasks.start(StatusCodes.OK)
        futures.size shouldBe 1
        futures.foreach(_.await)
        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffer.id shouldBe offer.id

        verify(offerLoader).getOffer(eq(category), eq(id), ?, ?, ?, ?)(eq(request))
        verify(historyClient).addHistory(offer, request.user.personalRef, Some(5.minutes))
        verify(tree).region(offer.getSeller.getLocation.getGeobaseId)

        if (historyEntityResponse.entity.addCount.contains(SyncManager.MinViewsToWatch)) {
          verify(watchManager).addWatch(request.user.personalRef, historyEntityId)
        } else {
          verifyNoMoreInteractions(watchManager)
        }
      }
    }

    "load autoru_expert offer for owner" in {
      forAll(OfferGen) { offer1 =>
        implicit val request: Request = generatePrivateUserRequest
        reset(historyClient, tree, watchManager, offerLoader)
        val offer = offer1.updated(b => {
          b.setSellerType(SellerType.COMMERCIAL)
          b.getAdditionalInfoBuilder.setAutoruExpert(true)
          b.setUserRef(request.user.ref.toPlain)
        })

        val category = CategorySelector.Cars
        val id = offer.id
        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(tree.region(?)).thenReturn(None)

        offerCardManager.getOfferCardResponse(category, id).await
        val futures = request.tasks.start(StatusCodes.OK)
        futures.size shouldBe 0

        verify(offerLoader).getOffer(eq(category), eq(id), ?, ?, ?, ?)(eq(request))
        verify(tree).region(offer.getSeller.getLocation.getGeobaseId)

        verifyNoMoreInteractions(historyClient)
        verifyNoMoreInteractions(watchManager)
      }
    }

    "load autoru_expert offer for moderator" in {
      forAll(OfferGen) { offer1 =>
        implicit val request: Request = PrivateModeratorRequestGen.next
        reset(historyClient, tree, watchManager, offerLoader)
        val offer = offer1.updated(b => {
          b.setSellerType(SellerType.COMMERCIAL)
          b.getAdditionalInfoBuilder.setAutoruExpert(true)
        })
        val historyEntityResponse = HistoryEntityResponseGen.next
        val historyEntityId = historyEntityResponse.entity.entityId

        val category = CategorySelector.Cars
        val id = offer.id
        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(historyClient.addHistory(?, ?, ?)(?)).thenReturnF(historyEntityResponse)
        when(tree.region(?)).thenReturn(None)
        when(watchManager.addWatch(?, ?)(?)).thenReturnF(())

        val result = offerCardManager.getOfferCardResponse(category, id).await
        val futures = request.tasks.start(StatusCodes.OK)
        futures.size shouldBe 1
        futures.foreach(_.await)
        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffer.id shouldBe offer.id

        verify(offerLoader).getOffer(eq(category), eq(id), ?, ?, ?, ?)(eq(request))
        verify(historyClient).addHistory(eq(offer), eq(request.user.personalRef), eq(Some(5.minutes)))(?)
        verify(tree).region(offer.getSeller.getLocation.getGeobaseId)

        if (historyEntityResponse.entity.addCount.contains(SyncManager.MinViewsToWatch)) {
          verify(watchManager).addWatch(request.user.personalRef, historyEntityId)
        } else {
          verifyNoMoreInteractions(watchManager)
        }
      }
    }

    "load autoru_expert offer for autoru_expert aware user" in {
      forAll(OfferGen) { offer1 =>
        implicit val request: Request = addResellerSession(generatePrivateUserRequest)
        reset(historyClient, tree, watchManager, offerLoader)
        val offer = offer1.updated(b => {
          b.setSellerType(SellerType.COMMERCIAL)
          b.getAdditionalInfoBuilder.setAutoruExpert(true)
        })
        val historyEntityResponse = HistoryEntityResponseGen.next
        val historyEntityId = historyEntityResponse.entity.entityId

        val category = CategorySelector.Cars
        val id = offer.id
        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(historyClient.addHistory(?, ?, ?)(?)).thenReturnF(historyEntityResponse)
        when(tree.region(?)).thenReturn(None)
        when(watchManager.addWatch(?, ?)(?)).thenReturnF(())

        val result = offerCardManager.getOfferCardResponse(category, id).await
        val futures = request.tasks.start(StatusCodes.OK)
        futures.size shouldBe 1
        futures.foreach(_.await)
        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffer.id shouldBe offer.id

        verify(offerLoader).getOffer(eq(category), eq(id), ?, ?, ?, ?)(eq(request))
        verify(historyClient).addHistory(offer, request.user.personalRef, Some(5.minutes))
        verify(tree).region(offer.getSeller.getLocation.getGeobaseId)

        if (historyEntityResponse.entity.addCount.contains(SyncManager.MinViewsToWatch)) {
          verify(watchManager).addWatch(request.user.personalRef, historyEntityId)
        } else {
          verifyNoMoreInteractions(watchManager)
        }
      }
    }

    "load COMMERCIAL offer for owner" in {
      forAll(OfferGen) { offer1 =>
        request = generateReq()
        reset(tree, offerLoader)
        val offer = offer1.updated(_.setSellerType(SellerType.COMMERCIAL).setUserRef(request.user.ref.toPlain))

        val category = CategorySelector.Cars
        val id = offer.id
        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(tree.region(?)).thenReturn(None)

        val result = offerCardManager.getOfferCardResponse(category, id).await
        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffer.id shouldBe offer.id

        verify(offerLoader).getOffer(eq(category), eq(id), ?, ?, ?, ?)(eq(request))
        verify(tree).region(offer.getSeller.getLocation.getGeobaseId)
      }
    }

    "load PRIVATE offer" in {
      forAll(OfferGen) { offer1 =>
        request = generateReq()
        reset(historyClient, tree, watchManager, offerLoader)
        val offer = offer1.updated(_.setSellerType(SellerType.PRIVATE))
        val historyEntityResponse = HistoryEntityResponseGen.next
        val historyEntityId = historyEntityResponse.entity.entityId

        val category = CategorySelector.Cars
        val id = offer.id
        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(historyClient.addHistory(?, ?, ?)(?)).thenReturnF(historyEntityResponse)
        when(watchManager.addWatch(?, ?)(?)).thenReturnF(())

        val result = offerCardManager.getOfferCardResponse(category, id).await
        val futures = request.tasks.start(StatusCodes.OK)
        futures.foreach(_.await)
        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffer.id shouldBe offer.id

        verify(offerLoader).getOffer(eq(category), eq(id), ?, ?, ?, ?)(eq(request))
        verify(historyClient).addHistory(offer, request.user.personalRef, Some(5.minutes))
        if (historyEntityResponse.entity.addCount.contains(SyncManager.MinViewsToWatch)) {
          verify(watchManager).addWatch(request.user.personalRef, historyEntityId)
        } else {
          verifyNoMoreInteractions(watchManager)
        }
      }
    }

    "load PRIVATE offer for owner" in {
      forAll(OfferGen) { offer1 =>
        reset(offerLoader)
        val offer = offer1.updated(_.setSellerType(SellerType.PRIVATE).setUserRef(request.user.ref.toPlain))

        val category = CategorySelector.Cars
        val id = offer.id
        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)

        val result = offerCardManager.getOfferCardResponse(category, id).await
        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffer.id shouldBe offer.id

        verify(offerLoader).getOffer(eq(category), eq(id), ?, ?, ?, ?)(eq(request))
      }
    }

    "throw OfferNotFound if offer not found" in {
      forAll(OfferGen) { offer =>
        reset(offerLoader)
        val category = CategorySelector.Cars
        val id = offer.id
        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))

        intercept[OfferNotFoundException] {
          offerCardManager.getOfferCardResponse(category, id).await
        }

        verify(offerLoader).getOffer(eq(category), eq(id), ?, ?, ?, ?)(eq(request))
      }
    }

    "set offer status to active if multiposting.status = ACTIVE" in {
      forAll(MultipostingOfferGen) { offer =>
        request = generateReq()
        reset(historyClient, tree, watchManager, offerLoader)
        val historyEntityResponse = HistoryEntityResponseGen.next

        val category = CategorySelector.Cars
        val id = offer.id

        when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(historyClient.addHistory(?, ?, ?)(?)).thenReturnF(historyEntityResponse)
        when(tree.region(?)).thenReturn(None)
        when(watchManager.addWatch(?, ?)(?)).thenReturnF(())

        val result = offerCardManager.getOfferCardResponse(category, id).await
        val futures = request.tasks.start(StatusCodes.OK)
        futures.foreach(_.await)
        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffer.id shouldBe offer.id

        if (offer.hasMultiposting && offer.getMultiposting.getStatus == OfferStatus.ACTIVE &&
            offer.getStatus == OfferStatus.INACTIVE) {
          // update offer status to active
          result.getOffer.getStatus shouldBe OfferStatus.ACTIVE
        } else {
          // do not change offer status if offer has no active multiposting or offer status is not inactive
          result.getOffer.getStatus shouldBe offer.getStatus
        }
      }
    }
  }

  private def addResellerSession(request: Request): Request = {
    request.asInstanceOf[RequestImpl].setSession(ResellerSessionResultGen.next)
    request
  }

  private def generatePrivateUserRequest: Request = {
    implicit val request: RequestImpl = new RequestImpl
    request.setRequestParams(RequestParams.construct("1.1.1.1"))
    request.setApplication(Application.iosApp)
    request.setUser(PrivateUserRefGen.next)
    request.setTrace(trace)
    request
  }

  "OfferCardManager.getPhones" should {

    "forbid phones for inactive offer" in {
      val category = CategorySelector.Cars
      val offer = OfferWithOnePhoneGen.next.toBuilder.setStatus(OfferStatus.INACTIVE).build()
      val id = offer.id

      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(antiParserManager.canShowPhone(?)).thenReturnF(true)

      intercept[OfferNotActiveException] {
        offerCardManager.getPhones(category, id).await
      }

      verify(antiParserManager).canShowPhone(request)
      verify(offerLoader).findRawOffer(eq(category), eq(id), fromVosOnly = eq(true), eq(false))(eq(request))
    }

    "forbid phones for parsers" in {
      val category = CategorySelector.Cars
      val offer = OfferWithOnePhoneGen.next.toBuilder.setStatus(OfferStatus.INACTIVE).build()
      val id = offer.id

      when(antiParserManager.canShowPhone(?)).thenReturnF(false)

      intercept[PhoneParserException] {
        offerCardManager.getPhones(category, id).await
      }

      verify(antiParserManager).canShowPhone(request)
    }

    "forbid phones for unauthorized users" in {
      val category = CategorySelector.Cars
      val offer = OfferWithOnePhoneGen.next.toBuilder.setStatus(OfferStatus.INACTIVE).build()
      val id = offer.id

      reset(phoneViewNeedAuthManager)
      when(phoneViewNeedAuthManager.check(?)(?))
        .thenReturn(Future.successful(NeedAuthResult(new NeedAuthActionException)))
      when(antiParserManager.canShowPhone(?)).thenReturnF(true)
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)

      intercept[NeedAuthActionException] {
        offerCardManager.getPhones(category, id).await
      }

      verify(phoneViewNeedAuthManager).check(offer)

      reset(phoneViewNeedAuthManager)
      when(phoneViewNeedAuthManager.check(?)(?)).thenReturn(Future.successful(Skip))
    }

    "fill phones from PhoneRedirectManager for seller with app2app=true and without name" in {
      implicit val request: Request = generatePrivateUserRequest
      val category = CategorySelector.Cars
      val offer = OfferWithOnePhoneGen.next.updated { builder =>
        builder.getSellerBuilder.clearName()
      }

      val id = offer.id
      val redirect = PhoneObjectGen.next.updated(_.setApp2AppCallAvailable(true))
      val sellerPic = ImageUrlGenerator.next

      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(antiParserManager.canShowPhone(?)).thenReturnF(true)
      val buyerApp2AppHandle = readableString.next
      val sellerApp2AppHandle = readableString.next
      when(app2appHandleCrypto.encrypt(?[AutoruUser], ?[StrictCategory], ?[OfferID]))
        .thenReturn(buyerApp2AppHandle)
        .thenReturn(sellerApp2AppHandle)
      when(phoneRedirectManager.getOfferPhones(?, ?, ?)(?)).thenReturnF(Seq(redirect))
      when(phoneRedirectManager.fillApp2AppPayload(?, ?, ?)(?)).thenReturn(redirect)
      val enriched = offer.updated { builder =>
        builder.getSellerBuilder.setPhones(0, redirect)
        builder.getSellerBuilder.setUserpic(sellerPic)
      }
      when(
        enrichManager.enrich(
          offer,
          EnrichOptions(
            phoneRedirects = true,
            techParams = true,
            userPic = true,
            required = Set(ApiOfferModel.EnrichFailedFlag.PHONE_REDIRECTS)
          )
        )
      ).thenReturnF(enriched)

      val phoneResponse = offerCardManager.getPhones(category, id).await
      phoneResponse.getSellerAlias shouldBe "Продавец"
    }

    "fill phones from PhoneRedirectManager for seller with app2app=true" in {
      implicit val request: Request = generatePrivateUserRequest
      val category = CategorySelector.Cars
      val offer = OfferWithOnePhoneGen.next

      val id = offer.id
      val redirect = PhoneObjectGen.next.updated(_.setApp2AppCallAvailable(true))
      val sellerPic = ImageUrlGenerator.next

      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(antiParserManager.canShowPhone(?)).thenReturnF(true)
      val buyerApp2AppHandle = readableString.next
      val sellerApp2AppHandle = readableString.next
      when(app2appHandleCrypto.encrypt(?[AutoruUser], ?[StrictCategory], ?[OfferID]))
        .thenReturn(buyerApp2AppHandle)
        .thenReturn(sellerApp2AppHandle)
      when(phoneRedirectManager.getOfferPhones(?, ?, ?)(?)).thenReturnF(Seq(redirect))
      when(phoneRedirectManager.fillApp2AppPayload(?, ?, ?)(?)).thenReturn(redirect)
      val enriched = offer.updated { builder =>
        builder.getSellerBuilder.setPhones(0, redirect)
        builder.getSellerBuilder.setUserpic(sellerPic)
      }
      when(
        enrichManager.enrich(
          offer,
          EnrichOptions(
            phoneRedirects = true,
            techParams = true,
            userPic = true,
            required = Set(ApiOfferModel.EnrichFailedFlag.PHONE_REDIRECTS)
          )
        )
      ).thenReturnF(enriched)

      val phoneResponse = offerCardManager.getPhones(category, id).await
      phoneResponse.getStatus shouldBe ResponseStatus.SUCCESS
      phoneResponse.getPhonesList should have size 1
      phoneResponse.getPhonesList.get(0) shouldEqual redirect
      phoneResponse.getSellerAlias shouldBe offer.getSeller.getName
      phoneResponse.getSellerPic shouldBe sellerPic
      phoneResponse.getApp2AppHandle shouldBe sellerApp2AppHandle

      verify(offerLoader).findRawOffer(eq(category), eq(id), fromVosOnly = eq(true), eq(false))(eq(request))
      verify(antiParserManager).canShowPhone(request)
      verify(app2appHandleCrypto).encrypt(request.user.ref.asPrivate, category, OfferID.parse(offer.getId))
      verify(app2appHandleCrypto).encrypt(
        UserRef.parse(offer.getUserRef).asPrivate,
        category,
        OfferID.parse(offer.getId)
      )
      verify(phoneRedirectManager).fillApp2AppPayload(eq(enriched), eq(redirect), eq(Some(buyerApp2AppHandle)))(?)
      verify(enrichManager).enrich(
        offer,
        EnrichOptions(
          phoneRedirects = true,
          techParams = true,
          userPic = true,
          required = Set(ApiOfferModel.EnrichFailedFlag.PHONE_REDIRECTS)
        )
      )(request)

      // do not increment for owner
      verifyNoMoreInteractions(counterClient)
    }

    "fill phones from PhoneRedirectManager for seller with app2app=false" in {
      implicit val request: RequestImpl = generateReq()
      val category = CategorySelector.Cars
      val offer = OfferWithOnePhoneGen.next

      val id = offer.id
      val redirect = PhoneObjectGen.next
      val sellerPic = ImageUrlGenerator.next

      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(antiParserManager.canShowPhone(?)).thenReturnF(true)
      val app2appHandle = readableString.next
      when(app2appHandleCrypto.encrypt(?[AutoruUser], ?[StrictCategory], ?[OfferID])).thenReturn(app2appHandle)
      when(phoneRedirectManager.getOfferPhones(?, ?, ?)(?)).thenReturnF(Seq(redirect))
      when(phoneRedirectManager.fillApp2AppPayload(?, ?, ?)(?)).thenReturn(redirect)
      val enriched = offer.updated { builder =>
        builder.getSellerBuilder.setPhones(0, redirect)
        builder.getSellerBuilder.setUserpic(sellerPic)
      }
      when(
        enrichManager.enrich(
          offer,
          EnrichOptions(
            phoneRedirects = true,
            techParams = true,
            userPic = true,
            required = Set(ApiOfferModel.EnrichFailedFlag.PHONE_REDIRECTS)
          )
        )
      ).thenReturnF(enriched)

      val phoneResponse = offerCardManager.getPhones(category, id).await
      phoneResponse.getStatus shouldBe ResponseStatus.SUCCESS
      phoneResponse.getPhonesList should have size 1
      phoneResponse.getPhonesList.get(0) shouldEqual redirect
      phoneResponse.getSellerAlias shouldBe offer.getSeller.getName
      phoneResponse.getSellerPic shouldBe sellerPic

      verify(offerLoader).findRawOffer(eq(category), eq(id), fromVosOnly = eq(true), eq(false))(eq(request))
      verify(antiParserManager).canShowPhone(request)
      if (request.user.ref.isPrivate) {
        verify(app2appHandleCrypto).encrypt(request.user.ref.asPrivate, category, OfferID.parse(offer.getId))
        verify(phoneRedirectManager).fillApp2AppPayload(eq(enriched), eq(redirect), eq(Some(app2appHandle)))(?)
      } else {
        verify(phoneRedirectManager).fillApp2AppPayload(eq(enriched), eq(redirect), eq(None))(?)
      }
      verify(enrichManager).enrich(
        offer,
        EnrichOptions(
          phoneRedirects = true,
          techParams = true,
          userPic = true,
          required = Set(ApiOfferModel.EnrichFailedFlag.PHONE_REDIRECTS)
        )
      )(request)

      // do not increment for owner
      verifyNoMoreInteractions(counterClient)
    }

    "fill phones for inactive offer with active multiposting" in {
      forAll(OfferWithOnePhoneGen, MultipostingOfferGen, PhoneObjectGen) {
        case (offerWithOnePhone, multipostingOffer, redirect) =>
          val category = CategorySelector.Cars
          val offer = offerWithOnePhone.updated { b =>
            b.setUserRef(request.user.ref.toPlain)
            b.setStatus(OfferStatus.INACTIVE)
            b.setMultiposting(multipostingOffer.getMultiposting)
          }
          val id = offer.id

          reset(offerLoader, antiParserManager, phoneRedirectManager, enrichManager)

          when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
          when(antiParserManager.canShowPhone(?)).thenReturnF(true)

          if (offer.hasActiveMultiposting) {
            when(phoneRedirectManager.getOfferPhones(?, ?, ?)(?)).thenReturnF(Seq(redirect))
            when(enrichManager.enrich(offer, EnrichOptions(phoneRedirects = true)))
              .thenReturnF(offer.updated(_.getSellerBuilder.setPhones(0, redirect)))

            val phoneResponse = offerCardManager.getPhones(category, id).await
            phoneResponse.getStatus shouldBe ResponseStatus.SUCCESS
            phoneResponse.getPhonesList should have size 1
            phoneResponse.getPhonesList.get(0) shouldEqual redirect
          } else {
            intercept[OfferNotActiveException] {
              offerCardManager.getPhones(category, id).await
            }
          }

          verify(offerLoader).findRawOffer(eq(category), eq(id), fromVosOnly = eq(true), eq(false))(eq(request))
          verify(antiParserManager).canShowPhone(request)

          // do not increment for owner
          verifyNoMoreInteractions(counterClient)
      }
    }
  }

  "OfferCardManager.related" should {
    "load related offers with experiment" in {
      val offer = OfferGen.next
      val relatedOffers = Gen.listOfN(3, OfferGen).next
      val searchRequest = searcherParamsGen.next
      reset(offerLoader, recommenderClient, searcherClient, enrichManager, decayManager)
      val category = CategorySelector.Cars
      implicit val request: RequestImpl = generateReq(
        RequestParams.construct("1.1.1.1", experiments = Set(RelatedForCardFromRecommendationService.desktopExp)),
        Application.web,
        TokenServiceImpl.web
      )
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
      val relatedTechParams = relatedOffers.map(_.getCarInfo.getTechParamId).distinct
      when(recommenderClient.getTechParams(?)(?)).thenReturnF(relatedTechParams)

      val response =
        OfferCardManager.auxSortingByTechParam((offer.getCarInfo.getTechParamId +: relatedTechParams).distinct)(
          OfferListingResponse
            .newBuilder()
            .addAllOffers(relatedOffers.asJava)
            .build()
        )

      when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(response)
      when(fakeManager.fake(any[OfferListingResponse]())(?)).thenReturnF(response)
      val params =
        OfferCardManager.paramsWhenDealer(searchRequest.params, offer) +
          ("tech_param_id" -> (offer.getCarInfo.getTechParamId.toString +: relatedTechParams.map(_.toString)).toSet)
      val (finalResponse, finalParams) = if (OfferCardManager.isDealerRelated(offer)) {
        (
          OfferCardManager.setDealerSpecialFlag(response),
          OfferCardManager.paramsWhenDealerRelated(params, offer)
        )
      } else {
        val preparedParams = OfferCardManager.carSearchParams(params, offer)
        (
          response,
          if (response.getPagination.getTotalOffersCount < 3) {
            OfferCardManager.paramsWhenNotEnoughOffers(preparedParams, offer)
          } else preparedParams
        )
      }

      when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(finalResponse)

      when(enrichManager.enrich(any[OfferListingResponse](), ?)(?)).thenReturnF(finalResponse)
      when(decayManager.decay(any[OfferListingResponse](), ?)(?)).thenReturnF(finalResponse)

      reset(maxRecommendedTechParamsFeature)
      when(maxRecommendedTechParamsFeature.value).thenReturn(10)

      val relatedResponse =
        offerCardManager
          .related(category, offer.id, searchRequest.params, Paging.Default, bigBrotherSearchParams)
          .futureValue

      verify(maxRecommendedTechParamsFeature).value
      reset(maxRecommendedTechParamsFeature)

      val searcherRequest = SearcherRequest(category, finalParams)
      val searchId = SearchesUtils.generateId(searcherRequest)
      relatedResponse.getSearchId shouldBe searchId

      verify(offerLoader).findRawOffer(category, offer.id)
      verify(recommenderClient).getTechParams(offer.getCarInfo.getTechParamId)
      verify(searcherClient).searchOffers(
        eq(searcherRequest),
        eq(Paging.Default),
        eq(SearcherManager.RelevanceSorting),
        eq(GroupBy.NoGrouping),
        eq(Some(searchId))
      )(?, ?)

      println(s"OfferCardManager.isDealerRelated(offer)=${OfferCardManager.isDealerRelated(offer)}")
      verify(enrichManager).enrich(finalResponse, EnrichOptions.ForRelated)(request)
      verify(decayManager).decay(finalResponse, DecayOptions.ForCard)(request)
    }

    "load related offers without experiment" in {
      forAll(OfferGen, Gen.listOfN(3, OfferGen), searcherParamsGen) {
        case (offer, relatedOffers, searchRequest) =>
          reset(offerLoader, recommenderClient, searcherClient, enrichManager, decayManager)
          val category = CategorySelector.Cars

          val response = OfferListingResponse
            .newBuilder()
            .addAllOffers(relatedOffers.asJava)
            .build()

          val paging = Paging.Default
          when(fakeManager.fake(any[OfferListingResponse]())(?)).thenReturnF(response)

          when(searcherClient.related(?, ?, ?, ?)(?)).thenReturnF(response)
          when(enrichManager.enrich(any[OfferListingResponse](), ?)(?)).thenReturnF(response)
          when(decayManager.decay(any[OfferListingResponse](), ?)(?)).thenReturnF(response)

          val relatedResponse =
            offerCardManager
              .related(category, offer.id, searchRequest.params, paging, bigBrotherSearchParams)
              .futureValue
          relatedResponse shouldBe response.toBuilder
            .setSearchId(SearchesUtils.generateId(SearcherRequest(category, searchRequest.params)))
            .build

          verify(searcherClient).related(category, offer.id, searchRequest.params, paging)
          verify(enrichManager).enrich(response, EnrichOptions.ForRelated)(request)
          verify(decayManager).decay(response, DecayOptions.ForCard)(request)
          verifyNoMoreInteractions(offerLoader)
          verifyNoMoreInteractions(recommenderClient)
      }
    }

    "load related offers with experiment but moto category" in {
      forAll(OfferGen, Gen.listOfN(3, OfferGen), searcherParamsGen) {
        case (offer, relatedOffers, searchRequest) =>
          reset(offerLoader, recommenderClient, searcherClient, enrichManager, decayManager)
          val category = CategorySelector.Moto
          implicit val request: RequestImpl = generateReq(
            RequestParams.construct(
              "1.1.1.1",
              experiments = Set(RelatedForCardFromRecommendationService.desktopExp)
            ),
            Application.web,
            TokenServiceImpl.web
          )
          val paging = Paging.Default

          val response = OfferListingResponse
            .newBuilder()
            .addAllOffers(relatedOffers.asJava)
            .build()
          when(fakeManager.fake(any[OfferListingResponse]())(?)).thenReturnF(response)

          when(searcherClient.related(?, ?, ?, ?)(?)).thenReturnF(response)
          when(enrichManager.enrich(any[OfferListingResponse](), ?)(?)).thenReturnF(response)
          when(decayManager.decay(any[OfferListingResponse](), ?)(?)).thenReturnF(response)

          val relatedResponse =
            offerCardManager
              .related(category, offer.id, searchRequest.params, paging, bigBrotherSearchParams)
              .futureValue
          relatedResponse shouldBe response.toBuilder
            .setSearchId(SearchesUtils.generateId(SearcherRequest(category, searchRequest.params)))
            .build

          verify(searcherClient).related(category, offer.id, searchRequest.params, paging)
          verify(enrichManager).enrich(response, EnrichOptions.ForRelated)(request)
          verify(decayManager).decay(response, DecayOptions.ForCard)(request)
          verifyNoMoreInteractions(offerLoader)
          verifyNoMoreInteractions(recommenderClient)
      }
    }

    "load related offers with dealer special flags" in {

      forAll(OfferGen, DealerUserRefGen, SalonGen, SellerGen, Gen.listOfN(3, OfferGen), searcherParamsGen) {
        case (offer, dealer, salon, seller, relatedOffers, searchRequest) =>
          val dealerOffer = offer.toBuilder
            .setUserRef(dealer.toPlain)
            .setSalon(salon)
            .setSellerType(SellerType.COMMERCIAL)
            .setSeller(seller)
            .build()

          reset(offerLoader, recommenderClient, searcherClient, enrichManager, decayManager)
          val category = CategorySelector.Cars
          implicit val request: RequestImpl = generateReq(
            RequestParams.construct(
              "1.1.1.1",
              experiments = Set(RelatedForCardFromRecommendationService.desktopExp)
            ),
            Application.web,
            TokenServiceImpl.web
          )

          val response = OfferListingResponse
            .newBuilder()
            .addAllOffers(relatedOffers.asJava)
            .build()

          val flags = response.toBuilder.getResponseFlagsBuilder.setDealerSpecial(true).build()
          val responseWithFlags = response.toBuilder.setResponseFlags(flags).build()

          when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(dealerOffer)
          when(searcherClient.searchOffers(?, ?, ?, ?, ?)(?, ?)).thenReturnF(responseWithFlags)
          when(recommenderClient.getTechParams(?)(?)).thenReturnF(Seq.empty[Long])
          when(enrichManager.enrich(any[OfferListingResponse](), ?)(?)).thenReturnF(responseWithFlags)
          when(decayManager.decay(any[OfferListingResponse](), ?)(?)).thenReturnF(responseWithFlags)
          when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
          when(fakeManager.fake(any[OfferListingResponse]())(?)).thenReturnF(responseWithFlags)

          reset(maxRecommendedTechParamsFeature)
          when(maxRecommendedTechParamsFeature.value).thenReturn(10)

          val relatedResponse =
            offerCardManager
              .related(category, offer.id, searchRequest.params, Paging.Default, bigBrotherSearchParams)
              .futureValue

          verify(maxRecommendedTechParamsFeature).value
          reset(maxRecommendedTechParamsFeature)

          assert(relatedResponse.getResponseFlags.getDealerSpecial)
      }
    }

  }
}
