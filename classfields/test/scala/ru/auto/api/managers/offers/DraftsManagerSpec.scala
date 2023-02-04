package ru.auto.api.managers.offers

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => eqq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.time.{Millis, Span}
import ru.auto.api.ApiOfferModel.Multiposting.Classified
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CommonModel.{ClientFeature, PaidService}
import ru.auto.api.ResponseModel.{DraftResponse, ResponseStatus}
import ru.auto.api.auth.Application
import ru.auto.api.broker_events.BrokerEvents.LightFormEvent
import ru.auto.api.exceptions.{BannedDomainException, DraftFromGarageCardApiException, ForceUpdateRequired, UserNotFoundException}
import ru.auto.api.features.FeatureManager
import ru.auto.api.features.FeatureManager.DealerVasProductsFeatures
import ru.auto.api.geo.Tree
import ru.auto.api.managers.carfax.CarfaxDraftsManager
import ru.auto.api.managers.decay.{DecayManager, DecayOptions}
import ru.auto.api.managers.enrich.{EnrichManager, EnrichOptions}
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.managers.garage.{CardNotFoundException, GarageManager, UnsupportedUserType}
import ru.auto.api.managers.parsing.{DraftHandleCrypto, ParsingManager}
import ru.auto.api.managers.price.{DealerPriceManager, UserPriceManager}
import ru.auto.api.managers.validation.ValidationManager
import ru.auto.api.model.AutoruProduct._
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.model.salesman.{CallPaymentAvailability, Campaign, PaymentModel, SaleCategory}
import ru.auto.api.services.billing.CabinetClient
import ru.auto.api.services.cabinet.CabinetApiClient
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.salesman.SalesmanClient.GoodsRequest
import ru.auto.api.services.settings.SettingsClient
import ru.auto.api.services.tradein.TradeInNotifierClient
import ru.auto.api.services.uploader.UploaderClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.Protobuf.RichDateTime
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.TimeUtils.TimeProvider
import ru.auto.api.util.concurrency.FutureTimeoutHandler
import ru.auto.api.util.form.FormOptionsMapper
import ru.auto.api.vin.garage.GarageApiModel.ProvenOwnerState
import ru.auto.api.{AsyncTasksSupport, BaseSpec, ResponseModel}
import ru.yandex.passport.model.api.ApiModel.{SessionResult, UserEssentials, UserResult}
import ru.yandex.passport.model.common.CommonModel.{DomainBan, UserModerationStatus}
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Created by mcsim-gr on 17.07.17.
  */
class DraftsManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with BeforeAndAfter
  with AsyncTasksSupport {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(interval = Span(400, Millis))

  import DraftsManagerSpec._

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
  val decayManager: DecayManager = mock[DecayManager]
  val featureManager: FeatureManager = mock[FeatureManager]
  val carfaxDraftsManager: CarfaxDraftsManager = mock[CarfaxDraftsManager]
  val garageManager: GarageManager = mock[GarageManager]
  val draftHandleCrypto: DraftHandleCrypto = mock[DraftHandleCrypto]
  val brokerClient: BrokerClient = mock[BrokerClient]
  val salesmanClient: SalesmanClient = mock[SalesmanClient]
  implicit val system: ActorSystem = ActorSystem("draft-manager-spec-system")
  val futureTimeoutHandler: FutureTimeoutHandler = new FutureTimeoutHandler()

  val optImageTtl = None

  private val enableShowInStories = mock[Feature[Boolean]]
  when(enableShowInStories.value).thenReturn(true)
  when(featureManager.enableShowInStories).thenReturn(enableShowInStories)
  when(draftHandleCrypto.encrypt(?, ?, ?, ?)).thenReturn("testEncrypt")

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
    optImageTtl,
    featureManager,
    carfaxDraftsManager,
    garageManager,
    futureTimeoutHandler,
    draftHandleCrypto,
    brokerClient,
    "https://test.avto.ru",
    salesmanClient
  )

  implicit private val trace = Traced.empty

  implicit private var request: RequestImpl = generateReq()

  def generateReq(xFeatures: Option[Set[String]] = None): RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some("testUid"), xFeatures = xFeatures))
    r.setUser(PrivateUserRefGen.next)
    r.setApplication(Application.iosApp)
    r.setToken(TokenServiceImpl.iosApp)
    r.setTrace(trace)
    r
  }

  val testRegionId = 1L

  before {
    request = generateReq()
    reset(userPriceManager)
    reset(passportClient)
    reset(vosClient)
    reset(carfaxDraftsManager)
  }

  "DraftsManager" should {
    "write tskv update event if draft for creating (isAddForm = true)" in {
      val offer = OfferGen.next
      val draftId = OfferIDGen.next
      val draft = OfferGen.next
      val user = request.user.registeredRef

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(statEventsManager.logDraftUpdateEvent(?, ?, ?)(?)).thenReturn(Future.unit)
      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturn(Future.successful(false))
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)

      draftsManager
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
      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)
      verify(statEventsManager).logDraftUpdateEvent(CategorySelector.Cars, draftId, draft)
    }

    "write broker event" in {
      import org.joda.time.DateTimeUtils
      DateTimeUtils.setCurrentMillisFixed(10L)
      val draftId = OfferIDGen.next

      val session = {
        val b = SessionResult.newBuilder()
        b.getSessionBuilder.setDeviceUid(DeviceUidGen.next)
        val essentials = UserEssentials.newBuilder().addPhones("1234567").build()
        b.setUser(essentials)
        b.build()
      }
      val requestWithSession = request
      requestWithSession.setSession(session)

      val user = requestWithSession.user.registeredRef

      val phone = requestWithSession.user.session
        .flatMap {
          _.getUser.getPhonesList.asScala.headOption
        }
        .getOrElse(throw new IllegalArgumentException("expected registered user with phone"))
      val hash = draftHandleCrypto.encrypt(user.asPrivate, phone, CategorySelector.Cars, draftId)
      val url = s"https://test.avto.ru/sales-parsing/info/$hash/"

      val event = LightFormEvent
        .newBuilder()
        .setDraftId(draftId.toPlain)
        .setUrl(url)
        .setCategory(CategorySelector.Cars.`enum`)
        .setUserId(user.toPlain)
        .setPhone(phone)
        .setTimestamp(DateTime.now().toProtobufTimestamp)
        .build()

      when(brokerClient.send(any[String](), any[LightFormEvent]())(?)).thenReturn(Future.unit)

      draftsManager
        .lightFormRequest(
          CategorySelector.Cars,
          user,
          draftId
        )
        .await
      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)
      verify(brokerClient).send(any[String](), eqq(event))(any[ProtoMarshaller[LightFormEvent]]())

      DateTimeUtils.setCurrentMillisSystem()
    }

    "do not add default hide license plate for high ios versions" in {
      val offer = OfferGen.next.updated(_.getStateBuilder.setHideLicensePlate(false))
      val draftId = OfferIDGen.next
      val draft = OfferGen.next
      val user = request.user.registeredRef

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(statEventsManager.logDraftUpdateEvent(?, ?, ?)(?)).thenReturn(Future.unit)
      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturn(Future.successful(false))
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)

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
      res.getOffer.getState.getHideLicensePlate shouldBe false
    }

    "write tskv update event if draft for creating (isAddForm = false, originalId is set)" in {
      val offer = OfferGen.next
      val draftId = OfferIDGen.next
      val draft = OfferGen.next
      val user = request.user.registeredRef

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(statEventsManager.logDraftUpdateEvent(?, ?, ?)(?)).thenReturn(Future.unit)
      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturn(Future.successful(false))
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)

      draftsManager
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
      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)
      verify(statEventsManager).logDraftUpdateEvent(CategorySelector.Cars, draftId, draft)
    }

    "isNewDraft should be false if there is original_id" in {
      val offer = OfferGen.next
      val draftId = OfferIDGen.next
      val draft = OfferGen.next.toBuilder.setAdditionalInfo(AdditionalInfo.newBuilder.setOriginalId("test").build).build
      val user = request.user.registeredRef

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(statEventsManager.logDraftUpdateEvent(?, ?, ?)(?)).thenReturn(Future.unit)
      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturn(Future.successful(false))
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)

      draftsManager
        .updateDraft(
          CategorySelector.Cars,
          user,
          draftId,
          draft,
          isAddForm = false,
          canChangePanorama = true,
          canDisableChats = true
        )
        .await
      verify(userPriceManager).getPrices(?, ?, ?, ?, ?)(?)
    }

    "updateDraftWithPartnerOptions should update options" in {
      val offer = OfferGen.next
      val draftId = OfferIDGen.next
      val draft = OfferGen.next.toBuilder.clearAdditionalInfo().build
      val user = request.user.registeredRef

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(statEventsManager.logDraftUpdateEvent(?, ?, ?)(?)).thenReturn(Future.unit)
      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturn(Future.successful(false))
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)
      when(carfaxDraftsManager.enrichOptionsIfNeeded(?)(?))
        .thenReturnF(Some(Map("abs" -> true)))
      when(passportClient.getUser(?)(?)).thenReturnF(UserResult.getDefaultInstance)

      val result =
        draftsManager
          .updateDraftWithPartnerOptions(
            CategorySelector.Cars,
            user,
            draftId,
            draft,
            isAddForm = true,
            canChangePanorama = true,
            canDisableChats = true
          )
          .await
      verify(carfaxDraftsManager).enrichOptionsIfNeeded(?)(?)
      result.getPartnerOptions.getIsUpdated shouldBe true
      result.getPartnerOptions.getEquipmentMap.asScala shouldBe Map("abs" -> true)
    }

    "isNewDraft should be true if there isn't original_id" in {
      val offer = OfferGen.next
      val draftId = OfferIDGen.next
      val draft = OfferGen.next.toBuilder.clearAdditionalInfo().build
      val user = request.user.registeredRef

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(statEventsManager.logDraftUpdateEvent(?, ?, ?)(?)).thenReturn(Future.unit)
      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturn(Future.successful(false))
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)

      draftsManager
        .updateDraft(
          CategorySelector.Cars,
          user,
          draftId,
          draft,
          isAddForm = false,
          canChangePanorama = true,
          canDisableChats = true
        )
        .await
      verify(userPriceManager).getPrices(?, ?, ?, ?, ?)(?)
    }

    "don't write tskv update event if draft is not for creating (isAddForm = false)" in {
      val offer = OfferGen.next
      val draftId = OfferIDGen.next
      val draft = OfferGen.next
      val draftBuilder = Offer.newBuilder(draft)
      draftBuilder.getAdditionalInfoBuilder.setOriginalId(OfferIDGen.next.toString)
      val updatedDraft = draftBuilder.build()
      val user = request.user.registeredRef

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(updatedDraft)
      when(statEventsManager.logDraftUpdateEvent(?, ?, ?)(?)).thenReturn(Future.unit)
      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturn(Future.successful(false))
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)

      draftsManager
        .updateDraft(
          CategorySelector.Cars,
          user,
          draftId,
          draft,
          isAddForm = false,
          canChangePanorama = true,
          canDisableChats = true
        )
        .await
      val futures = request.tasks.start(StatusCodes.OK)
      futures.size shouldBe 0
      futures.foreach(_.await)
      verifyNoMoreInteractions(statEventsManager)
    }

    "write tskv publish event" in {
      val offer = OfferGen.next
      val draft = OfferGen.next

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.publishDraft(?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
      when(validationManager.getValidationData(?, ?, ?, ?)(?))
        .thenReturnF(AdditionalValidationData.empty)
      when(statEventsManager.logDraftPublishEvent(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(passportClient.getUserModeration(?)(?))
        .thenReturnF(UserModerationStatus.getDefaultInstance)
      when(settingsClient.updateSettings(?, ?, ?)(?)).thenReturnF(Map.empty)

      draftsManager
        .publishDraft(
          CategorySelector.Cars,
          request.user.registeredRef,
          OfferIDGen.next,
          None,
          canChangePanorama = true,
          canDisableChats = true
        )
        .await

      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)

      verify(statEventsManager).logDraftPublishEvent(offer, draft, isAdd = true, None)
    }

    "publish draft" in {
      val offer = OfferGen.next
      val draft = OfferGen.next
      val enrichedOffer = OfferGen.next
      val decayedOffer = OfferGen.next

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.publishDraft(?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(enrichedOffer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(decayedOffer)
      when(validationManager.getValidationData(?, ?, ?, ?)(?))
        .thenReturnF(AdditionalValidationData.empty)
      when(statEventsManager.logDraftPublishEvent(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(passportClient.getUserModeration(?)(?))
        .thenReturnF(UserModerationStatus.getDefaultInstance)
      when(settingsClient.updateSettings(?, ?, ?)(?)).thenReturnF(Map.empty)

      val response =
        draftsManager
          .publishDraft(
            CategorySelector.Cars,
            request.user.registeredRef,
            OfferIDGen.next,
            None,
            canChangePanorama = true,
            canDisableChats = true
          )
          .await

      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)

      EnrichOptions.AfterPublish.counters shouldBe true
      EnrichOptions.AfterPublish.phoneCounters shouldBe true

      verify(enrichManager).enrich(offer, EnrichOptions.AfterPublish)
      verify(decayManager).decay(enrichedOffer, DecayOptions.ForDraft)
      response.res.getOffer shouldEqual decayedOffer
    }

    "publish draft for multiposting dealer [status = ACTIVE]" in {
      val offer = OfferGen.next
      val enrichedOffer = OfferGen.next
      val decayedOffer = OfferGen.next

      val draft = OfferGen.next.updated(b => b.getAdditionalInfoBuilder.setHidden(false))

      val dealer = DealerUserRefGen.next

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.publishDraft(?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(enrichedOffer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(decayedOffer)
      when(validationManager.getValidationData(?, ?, ?, ?)(?))
        .thenReturnF(AdditionalValidationData.empty)
      when(statEventsManager.logDraftPublishEvent(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(passportClient.getUserModeration(?)(?))
        .thenReturnF(UserModerationStatus.getDefaultInstance)
      when(settingsClient.updateSettings(?, ?, ?)(?)).thenReturnF(Map.empty)
      when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
      when(timeProvider.now()).thenReturn(111L)

      val draftId = OfferIDGen.next
      draftsManager
        .publishDraft(
          CategorySelector.Cars,
          dealer,
          draftId,
          from = None,
          canChangePanorama = true,
          canDisableChats = true
        )
        .await

      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)

      val expectedDraft = draft.updated { b =>
        b.getMultipostingBuilder
          .setStatus(OfferStatus.ACTIVE)
          .addClassifieds {
            Classified
              .newBuilder()
              .setName(Classified.ClassifiedName.AUTORU)
              .setStatus(OfferStatus.ACTIVE)
              .setEnabled(true)
              .setCreateDate(111L)
          }

        b.complementPrices
        FormOptionsMapper.offerFromFormToCatalog(b)
      }

      verify(vosClient).updateDraft(
        eq(CategorySelector.Cars),
        eq(dealer),
        eq(draftId),
        eq(expectedDraft),
        ?,
        ?
      )(?)
    }

    "publish draft for multiposting dealer [status = INACTIVE]" in {
      val offer = OfferGen.next
      val enrichedOffer = OfferGen.next
      val decayedOffer = OfferGen.next

      val draft = OfferGen.next.updated(b => b.getAdditionalInfoBuilder.setHidden(true))

      val dealer = DealerUserRefGen.next

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.publishDraft(?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(enrichedOffer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(decayedOffer)
      when(validationManager.getValidationData(?, ?, ?, ?)(?))
        .thenReturnF(AdditionalValidationData.empty)
      when(statEventsManager.logDraftPublishEvent(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(passportClient.getUserModeration(?)(?))
        .thenReturnF(UserModerationStatus.getDefaultInstance)
      when(settingsClient.updateSettings(?, ?, ?)(?)).thenReturnF(Map.empty)
      when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
      when(timeProvider.now()).thenReturn(222L)

      val draftId = OfferIDGen.next
      draftsManager
        .publishDraft(
          CategorySelector.Cars,
          dealer,
          draftId,
          from = None,
          canChangePanorama = true,
          canDisableChats = true
        )
        .await

      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)

      val expectedDraft = draft.updated { b =>
        b.getMultipostingBuilder
          .setStatus(OfferStatus.INACTIVE)
          .addClassifieds {
            Classified
              .newBuilder()
              .setName(Classified.ClassifiedName.AUTORU)
              .setStatus(OfferStatus.INACTIVE)
              .setEnabled(true)
              .setCreateDate(222L)
          }

        b.complementPrices
        FormOptionsMapper.offerFromFormToCatalog(b)
      }

      verify(vosClient).updateDraft(
        eq(CategorySelector.Cars),
        eq(dealer),
        eq(draftId),
        eq(expectedDraft),
        ?,
        ?
      )(?)
    }

    "publish draft for multiposting dealer [non-empty in draft]" in {
      val offer = OfferGen.next
      val enrichedOffer = OfferGen.next
      val decayedOffer = OfferGen.next

      val draft = OfferGen.next.updated { b =>
        b.getAdditionalInfoBuilder.setHidden(true)
        b.getMultipostingBuilder.setStatus(OfferStatus.NEED_ACTIVATION)
      }

      val dealer = DealerUserRefGen.next

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.publishDraft(?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(enrichedOffer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(decayedOffer)
      when(validationManager.getValidationData(?, ?, ?, ?)(?))
        .thenReturnF(AdditionalValidationData.empty)
      when(statEventsManager.logDraftPublishEvent(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(passportClient.getUserModeration(?)(?))
        .thenReturnF(UserModerationStatus.getDefaultInstance)
      when(settingsClient.updateSettings(?, ?, ?)(?)).thenReturnF(Map.empty)
      when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
      when(timeProvider.now()).thenReturn(222L)

      val draftId = OfferIDGen.next
      draftsManager
        .publishDraft(
          CategorySelector.Cars,
          dealer,
          draftId,
          from = None,
          canChangePanorama = true,
          canDisableChats = true
        )
        .await

      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)

      verify(vosClient, never()).updateDraft(?, ?, ?, ?, ?, ?)(?)
    }

    "publish draft with postTradein = true" in {
      val tradeInInfo = TradeInInfo
        .newBuilder()
        .setTradeInType(TradeInType.FOR_MONEY)
        .setTradeInPriceRange(
          TradeInInfo.PriceRange
            .newBuilder()
            .setFrom(100000)
            .setTo(300000)
            .setCurrency("RUR")
        )
      val offer = OfferGen.next.toBuilder.setTradeInInfo(tradeInInfo).build()

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(tradeInNotifierClient.tradeInValidate(?)(?)).thenReturn(Future.unit)
      when(vosClient.publishDraft(?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
      when(validationManager.getValidationData(?, ?, ?, ?)(?))
        .thenReturnF(AdditionalValidationData.empty)
      when(statEventsManager.logDraftPublishEvent(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(passportClient.getUserModeration(?)(?))
        .thenReturnF(UserModerationStatus.getDefaultInstance)
      when(settingsClient.updateSettings(?, ?, ?)(?)).thenReturnF(Map.empty)

      val response =
        draftsManager
          .publishDraft(
            CategorySelector.Cars,
            request.user.registeredRef,
            OfferIDGen.next,
            None,
            canChangePanorama = true,
            canDisableChats = true,
            postTradeIn = true
          )
          .await

      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)

      response.res.getOffer shouldEqual offer
    }

    "publish draft with postTradein = false" in {
      val tradeInInfo = TradeInInfo
        .newBuilder()
        .setTradeInType(TradeInType.FOR_MONEY)
        .setTradeInPriceRange(
          TradeInInfo.PriceRange
            .newBuilder()
            .setFrom(100000)
            .setTo(300000)
            .setCurrency("RUR")
        )
      val publishedOffer = OfferGen.next
      val offer = publishedOffer.toBuilder.setTradeInInfo(tradeInInfo).build()

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(publishedOffer)
      when(vosClient.publishDraft(?, ?, ?, ?, ?)(?)).thenReturnF(publishedOffer)
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(publishedOffer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(publishedOffer)
      when(validationManager.getValidationData(?, ?, ?, ?)(?))
        .thenReturnF(AdditionalValidationData.empty)
      when(statEventsManager.logDraftPublishEvent(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(passportClient.getUserModeration(?)(?))
        .thenReturnF(UserModerationStatus.getDefaultInstance)
      when(settingsClient.updateSettings(?, ?, ?)(?)).thenReturnF(Map.empty)

      val response =
        draftsManager
          .publishDraft(
            CategorySelector.Cars,
            request.user.registeredRef,
            OfferIDGen.next,
            None,
            canChangePanorama = true,
            canDisableChats = true,
            postTradeIn = false
          )
          .await

      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)

      response.res.getOffer shouldEqual publishedOffer
    }

    "pass remote id and remote url in draft publish request" in {
      val offer = OfferGen.next
      val draft = OfferGen.next

      val remoteId = "avito|cars|1767143438"
      val remoteUrl = "https://www.avito.ru/miass/avtomobili/toyota_land_cruiser_prado_2013_1767143438"

      implicit val request: RequestImpl = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some("testUid")))
        r.setUser(PrivateUserRefGen.next)
        r.setApplication(Application.iosApp)
        r.setToken(TokenServiceImpl.iosApp)
        r.setTrace(trace)
        val sessionResultBuilder = SessionResultGen.next.toBuilder
        sessionResultBuilder.getSessionBuilder.putPayload(ParsingManager.SessionPayload.RemoteId, remoteId)
        sessionResultBuilder.getSessionBuilder.putPayload(ParsingManager.SessionPayload.RemoteUrl, remoteUrl)
        r.setSession(sessionResultBuilder.build())
        r
      }

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(draft)
      when(vosClient.publishDraft(?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
      when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
      when(validationManager.getValidationData(?, ?, ?, ?)(?))
        .thenReturnF(AdditionalValidationData.empty)
      when(statEventsManager.logDraftPublishEvent(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(passportClient.getUserModeration(?)(?))
        .thenReturnF(UserModerationStatus.getDefaultInstance)
      when(settingsClient.updateSettings(?, ?, ?)(?)).thenReturnF(Map.empty)

      draftsManager
        .publishDraft(
          CategorySelector.Cars,
          request.user.registeredRef,
          OfferIDGen.next,
          None,
          canChangePanorama = true,
          canDisableChats = true
        )
        .await

      val futures = request.tasks.start(StatusCodes.OK)
      futures.foreach(_.await)

      verify(vosClient).publishDraft(
        ?,
        ?,
        ?,
        eq(
          AdditionalDraftParams(None, Some(remoteId), Some(remoteUrl), canChangePanorama = true, canDisableChats = true)
        ),
        ?
      )(?)
    }

    "create default draft" in {
      val userResult = PassportUserResultGen.next

      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturnF(true)
      when(passportClient.getUser(?)(?)).thenReturnF(userResult)
      when(geobaseClient.regionIdByIp("1.1.1.1")).thenReturnF(213)
      when(tree.region(213)).thenReturn(None)

      val result = draftsManager.getDefaultDraft(Cars, request.user).futureValue

      result.getDocuments.getCustomCleared shouldBe true
      result.getState.getCondition shouldBe Condition.CONDITION_OK
      result.getSeller.getName shouldBe userResult.getUser.getProfile.getAutoru.getAlias
      userResult.getUser.getEmailsList.asScala.collectFirst {
        case email if email.getConfirmed =>
          result.getSeller.getUnconfirmedEmail shouldBe email.getEmail
      }
      userResult.getUser.getPhonesList.asScala.map(_.getPhone).toSet shouldBe
        result.sellerPhones.map(_.getPhone).toSet

      result.getSeller.getRedirectPhones shouldBe true
    }

    "create default draft while user info is not available" in {
      when(passportClient.getUser(?)(?)).thenReturn(Future.failed(new UserNotFoundException))

      val result = draftsManager.getDefaultDraft(Cars, request.user).futureValue

      result.getDocuments.getCustomCleared shouldBe true
      result.getState.getCondition shouldBe Condition.CONDITION_OK
    }

    "fail to publish draft for banned user" in {
      val offer = OfferGen.next.toBuilder.setCategory(Category.CARS).build()
      val user = PrivateUserRefGen.next
      val moderation = moderationStatus(Seq("CARS")).next
      when(passportClient.getUserModeration(?)(?)).thenReturnF(moderation)
      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer)

      draftsManager
        .publishDraft(CategorySelector.Cars, user, offer.id, None, canChangePanorama = true, canDisableChats = true)
        .failed
        .futureValue shouldBe an[BannedDomainException]

      verify(vosClient).getDraft(eq(CategorySelector.Cars), eq(user), eq(offer.id), eq(true))(?)
      verify(passportClient).getUserModeration(eq(user))(?)
    }

    "read mod status from session if present" in {
      implicit val r: RequestImpl = new RequestImpl
      r.setRequestParams(RequestParams.empty)
      val session = ResellerSessionResultGen.next.toBuilder
      session.getUserBuilder.setModerationStatus(moderationStatus(Seq("CARS")).next)
      r.setSession(session.build())
      r.setApplication(Application.iosApp)
      val user = PrivateUserRefGen.next
      r.setUser(user)
      val offer = OfferGen.next.toBuilder.setCategory(Category.CARS).build()
      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer)

      draftsManager
        .publishDraft(CategorySelector.Cars, user, offer.id, None, canChangePanorama = true, canDisableChats = true)(r)
        .failed
        .futureValue shouldBe an[BannedDomainException]

      verify(vosClient).getDraft(eq(CategorySelector.Cars), eq(user), eq(offer.id), eq(true))(?)
      verifyNoMoreInteractions(passportClient)
    }

    "able to publish draft for mos_ru_validation status" in {
      val offer = OfferGen.next.toBuilder.setCategory(Category.CARS).build()
      val user = PrivateUserRefGen.next
      val moderation = mosRuValidationModStatus(Seq("CARS"))
      when(passportClient.getUserModeration(?)(?)).thenReturnF(moderation)
      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(vosClient.publishDraft(?, ?, ?, ?, ?)(?)).thenReturnF(offer)

      draftsManager
        .publishDraft(CategorySelector.Cars, user, offer.id, None, canChangePanorama = true, canDisableChats = true)
        .await

      verify(vosClient).getDraft(eq(CategorySelector.Cars), eq(user), eq(offer.id), eq(true))(?)
      verify(passportClient).getUserModeration(eq(user))(?)
      verify(vosClient).publishDraft(
        ?,
        eq(user.asRegistered),
        eq(OfferID.parse(offer.getId)),
        eq(AdditionalDraftParams(None, None, None, canChangePanorama = true, canDisableChats = true)),
        ?
      )(?)
    }

    "return current draft without phones not in user profile" in {
      val userResult0 = PassportUserResultGen.next
      val phone1: String = "79991112233"
      val phone2: String = "79994445566"

      val userResult1: UserResult = keepPhones(userResult0, phone1)

      when(passportClient.getUser(?)(?)).thenReturnF(userResult1)

      val offer0: Offer = OfferGen.next
      val offer1 = keepPhones(offer0, phone1, phone2)
      val offer2 = keepPhones(offer0, phone1)

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer1)

      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("draft_cars" -> offer1.getId))

      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturnF(true)
      stub(enrichManager.enrich(_: Offer, _: EnrichOptions)(_: RequestImpl)) {
        case (offer, _, _) => Future.successful(offer)
      }
      stub(decayManager.decay(_: Offer, _: DecayOptions)(_: RequestImpl)) {
        case (offer, _, _) => Future.successful(offer)
      }
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)

      val result = draftsManager
        .getCurrentDraft(
          CategorySelector.Cars,
          request.user.registeredRef,
          isAddForm = false,
          requestCarfaxOptions = false
        )(request)
        .futureValue

      result.getOffer.getSeller.getPhonesCount shouldBe 1
      result.getOffer.getSeller.getPhones(0).getPhone shouldBe phone1
    }

    "get current draft without section successfully" in {
      val userResult = PassportUserResultGen.next
      val offer = OfferGen.next.toBuilder.setSection(Section.SECTION_UNKNOWN).build()
      when(passportClient.getUser(?)(?)).thenReturnF(userResult)
      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("draft_cars" -> offer.getId))
      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturnF(true)
      stub(enrichManager.enrich(_: Offer, _: EnrichOptions)(_: RequestImpl)) {
        case (offerToEnrich, _, _) => Future.successful(offerToEnrich)
      }
      stub(decayManager.decay(_: Offer, _: DecayOptions)(_: RequestImpl)) {
        case (offerToDecay, _, _) => Future.successful(offerToDecay)
      }
      when(userPriceManager.getMultipleOffersPrices(?, ?, ?, ?)(?))
        .thenThrowF(new Exception("Invalid section: SECTION_UNKNOWN"))

      val result = draftsManager
        .getCurrentDraft(
          CategorySelector.Cars,
          request.user.registeredRef,
          isAddForm = false,
          requestCarfaxOptions = false
        )(request)
        .futureValue
      result.getServicePricesList shouldBe empty
    }

    "fail get current draft for request with FORCE_UPDATE feature" in {
      val customRequest = generateReq(Some(Set(ClientFeature.FORCE_UPDATE.name())))

      draftsManager
        .getCurrentDraft(
          CategorySelector.Cars,
          customRequest.user.registeredRef,
          isAddForm = false,
          requestCarfaxOptions = false
        )(customRequest)
        .failed
        .futureValue shouldBe an[ForceUpdateRequired]
    }

    "return draft without phones not in user profile" in {
      val userResult0 = PassportUserResultGen.next
      val phone1: String = "79991112233"
      val phone2: String = "79994445566"

      val userResult1: UserResult = keepPhones(userResult0, phone1)

      when(passportClient.getUser(?)(?)).thenReturnF(userResult1)

      val offer0: Offer = OfferGen.next
      val offerId = OfferID.parse(offer0.getId)
      val offer1 = keepPhones(offer0, phone1, phone2)
      val offer2 = keepPhones(offer0, phone1)

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer1)

      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("draft_cars" -> offer1.getId))

      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturnF(true)
      stub(enrichManager.enrich(_: Offer, _: EnrichOptions)(_: RequestImpl)) {
        case (offer, _, _) => Future.successful(offer)
      }
      stub(decayManager.decay(_: Offer, _: DecayOptions)(_: RequestImpl)) {
        case (offer, _, _) => Future.successful(offer)
      }
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)

      val result = draftsManager
        .getDraft(CategorySelector.Cars, request.user.registeredRef, offerId, isAddForm = false)(request)
        .futureValue

      result.getOffer.getSeller.getPhonesCount shouldBe 1
      result.getOffer.getSeller.getPhones(0).getPhone shouldBe phone1
    }

    "send only inactive filtered services to cabinet" in {
      forAll(RegionGen) { region =>
        val category = CategorySelector.Cars
        val dealer = DealerUserRefGen.next
        val builder = OfferGen.next.toBuilder
        builder.setUserRef(dealer.toPlain)
        builder.clearServices()
        builder.addServices(PaidService.newBuilder().setService("all_sale_fresh").setIsActive(true))
        builder.addServices(PaidService.newBuilder().setService("all_sale_color").setIsActive(false))
        builder.addServices(PaidService.newBuilder().setService("all_sale_special").setIsActive(false))
        val offer = builder.build()

        val expectedPaymentModel = PaymentModel.Single
        val expectedDealerVasProductsFeatures = DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List(testRegionId))
        )

        when(featureManager.manageGoodsInSalesman).thenReturn(new Feature[Boolean] {
          override def name: String = "create_goods_in_salesman"
          override def value: Boolean = false
        })
        when(cabinetClient.requestServices(any[Offer](), ?)(?)).thenReturn(Future.unit)
        when(salesmanClient.getCampaigns(eq(dealer), eq(true))(?))
          .thenReturnF(
            Set(
              Campaign(
                paymentModel = expectedPaymentModel,
                tag = "",
                category = offer.getCategory.toString.toLowerCase,
                subcategory = Nil,
                section = List(offer.getSection.toString.toLowerCase),
                size = 100,
                enabled = true
              )
            )
          )
        when(featureManager.dealerVasProductsFeatures).thenReturn(expectedDealerVasProductsFeatures)
        when(tree.unsafeFederalSubject(offer.getSalon.getPlace.getGeobaseId)).thenReturn(
          region
        )

        val expectedServices: Seq[AutoruProduct] = Seq(
          AutoruProduct.SpecialOffer
        )

        draftsManager.processServices(category, dealer, offer.getServicesList.asScala.toSeq, offer, None).await
        verify(cabinetClient).requestServices(
          offer,
          expectedServices
        )
      }
    }

    "send services to salesman if feature is up" in {
      forAll(RegionGen) { region =>
        val category = CategorySelector.Cars
        val dealer = DealerUserRefGen.next
        val builder = CarsOfferGen.next.toBuilder
        builder.setUserRef(dealer.toPlain)
        builder.clearServices()
        builder.setOldCategoryId(15)
        builder.addServices(PaidService.newBuilder().setService("all_sale_fresh").setIsActive(true))
        builder.addServices(PaidService.newBuilder().setService("all_sale_color").setIsActive(false))
        builder.addServices(PaidService.newBuilder().setService("all_sale_special").setIsActive(false))
        val offer = builder.build()

        val expectedPaymentModel = PaymentModel.Single
        val expectedDealerVasProductsFeatures = DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List(testRegionId))
        )

        when(salesmanClient.createGoods(eq(dealer.clientId), ?, ?)(?)).thenReturn(Future.unit)
        when(featureManager.manageGoodsInSalesman).thenReturn(new Feature[Boolean] {
          override def name: String = "create_goods_in_salesman"
          override def value: Boolean = true
        })
        when(salesmanClient.getCampaigns(eq(dealer), eq(true))(?))
          .thenReturnF(
            Set(
              Campaign(
                paymentModel = expectedPaymentModel,
                tag = "",
                category = offer.getCategory.toString.toLowerCase,
                subcategory = Nil,
                section = List(offer.getSection.toString.toLowerCase),
                size = 100,
                enabled = true
              )
            )
          )
        when(featureManager.dealerVasProductsFeatures).thenReturn(expectedDealerVasProductsFeatures)
        when(tree.unsafeFederalSubject(offer.getSalon.getPlace.getGeobaseId)).thenReturn(
          region
        )

        val expectedGoodsRequest =
          GoodsRequest(
            offer.getId,
            SaleCategory(offer.getOldCategoryId),
            offer.getSection,
            AutoruProduct.SpecialOffer,
            None
          )

        draftsManager.processServices(category, dealer, offer.getServicesList.asScala.toSeq, offer, None).await
        verify(salesmanClient).createGoods(
          dealer.clientId,
          expectedGoodsRequest,
          withMoneyCheck = false
        )
      }
    }

    "send services to cabinet if feature is up and flag is down" in {
      forAll(RegionGen) { region =>
        val category = CategorySelector.Cars
        val dealer = DealerUserRefGen.next
        val builder = CarsOfferGen.next.toBuilder
        builder.setUserRef(dealer.toPlain)
        builder.clearServices()
        builder.setOldCategoryId(15)
        builder.addServices(PaidService.newBuilder().setService("all_sale_fresh").setIsActive(true))
        builder.addServices(PaidService.newBuilder().setService("all_sale_color").setIsActive(false))
        builder.addServices(PaidService.newBuilder().setService("all_sale_special").setIsActive(false))
        val offer = builder.build()

        val expectedPaymentModel = PaymentModel.Single
        val expectedDealerVasProductsFeatures = DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List(testRegionId))
        )
        when(cabinetClient.requestServices(any[Offer](), ?)(?)).thenReturn(Future.unit)

        when(featureManager.manageGoodsInSalesman).thenReturn(new Feature[Boolean] {
          override def name: String = "create_goods_in_salesman"
          override def value: Boolean = true
        })
        when(salesmanClient.getCampaigns(eq(dealer), eq(true))(?))
          .thenReturnF(
            Set(
              Campaign(
                paymentModel = expectedPaymentModel,
                tag = "",
                category = offer.getCategory.toString.toLowerCase,
                subcategory = Nil,
                section = List(offer.getSection.toString.toLowerCase),
                size = 100,
                enabled = true
              )
            )
          )
        when(featureManager.dealerVasProductsFeatures).thenReturn(expectedDealerVasProductsFeatures)
        when(tree.unsafeFederalSubject(offer.getSalon.getPlace.getGeobaseId)).thenReturn(
          region
        )

        val expectedGoodsRequest =
          GoodsRequest(
            offer.getId,
            SaleCategory(offer.getOldCategoryId),
            offer.getSection,
            AutoruProduct.SpecialOffer,
            None
          )

        draftsManager.processServices(category, dealer, offer.getServicesList.asScala.toSeq, offer, Some(true)).await
      }
    }

    "load same offer from vos and enrich it" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.PrivateOfferGen) { (selector, offer) =>
        val user = offer.userRef.asRegistered
        val id = offer.id

        when(vosClient.similarForDraft(?, ?, ?)(?)).thenReturnF(Some(offer))
        when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
        when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)

        val result = draftsManager.getSameOffer(user, selector, id).futureValue.get

        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffer shouldBe offer

        verify(vosClient).similarForDraft(selector, user, id)(request)
        verify(decayManager).decay(offer, DecayOptions.ForDraft)
        verify(enrichManager).enrich(offer, EnrichOptions.ForCabinetCard)
      }
    }
    "return empty response when similar not found" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.RegisteredUserRefGen, ModelGenerators.OfferIDGen) {
        (selector, user, id) =>
          when(vosClient.similarForDraft(?, ?, ?)(?)).thenReturnF(None)

          draftsManager.getSameOffer(user, selector, id).futureValue shouldBe None

          verify(vosClient).similarForDraft(selector, user, id)(request)
      }
    }
  }

  "dealerProducts()" should {
    when(featureManager.dealerVasProductsFeatures)
      .thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List(testRegionId))
        )
      )
    "return for new cars:new draft" in {
      draftsManager.dealerProducts(
        CategorySelector.Cars,
        Section.NEW,
        isNewDraft = true,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge
      )
    }

    "return for non-new cars:new draft" in {
      draftsManager.dealerProducts(
        CategorySelector.Cars,
        Section.NEW,
        isNewDraft = false,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        Boost
      )
    }

    "return for new cars:used draft" in {
      draftsManager.dealerProducts(
        CategorySelector.Cars,
        Section.USED,
        isNewDraft = true,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        PackageTurbo
      )
    }

    "return for non-new cars:used draft" in {
      draftsManager.dealerProducts(
        CategorySelector.Cars,
        Section.USED,
        isNewDraft = false,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        Boost,
        PackageTurbo,
        Reset
      )
    }

    "return for new moto draft" in {
      draftsManager.dealerProducts(
        CategorySelector.Moto,
        Section.NEW,
        isNewDraft = true,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        PackageTurbo
      )
      draftsManager.dealerProducts(
        CategorySelector.Moto,
        Section.USED,
        isNewDraft = true,
        dealerRegionId = 1L,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        PackageTurbo
      )
    }

    "return for non-new moto draft" in {
      draftsManager.dealerProducts(
        CategorySelector.Moto,
        Section.NEW,
        isNewDraft = false,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        Boost,
        PackageTurbo
      )
      draftsManager.dealerProducts(
        CategorySelector.Moto,
        Section.USED,
        isNewDraft = false,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        Boost,
        PackageTurbo
      )
    }

    "return for new trucks draft" in {
      draftsManager.dealerProducts(
        CategorySelector.Trucks,
        Section.NEW,
        isNewDraft = true,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        PackageTurbo
      )
      draftsManager.dealerProducts(
        CategorySelector.Trucks,
        Section.USED,
        isNewDraft = true,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        PackageTurbo
      )
    }

    "return for non-new trucks draft" in {
      draftsManager.dealerProducts(
        CategorySelector.Trucks,
        Section.NEW,
        isNewDraft = false,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        Boost,
        PackageTurbo
      )
      draftsManager.dealerProducts(
        CategorySelector.Trucks,
        Section.USED,
        isNewDraft = false,
        dealerRegionId = testRegionId,
        CallPaymentAvailability.NoCalls
      ) should contain theSameElementsAs Seq(
        Premium,
        SpecialOffer,
        Badge,
        Boost,
        PackageTurbo
      )
    }
  }

  "createDraftFromGarageCard()" should {
    "create draft" in {
      val offerId = OfferIDGen.next
      val user = request.user.registeredRef
      val cardBuilder = GarageCardGen.next.toBuilder
      val category = CategorySelector.Cars
      val userResult = PassportUserResultGen.next
      val now = Timestamps.fromMillis(System.currentTimeMillis())

      cardBuilder.getProvenOwnerStateBuilder
        .setAssignmentDate(now)
        .setStatus(ProvenOwnerState.ProvenOwnerStatus.OK)

      val card = cardBuilder.build()

      val offer = draftsManager.fillOfferFromGarageCard(Offer.newBuilder().setId(offerId.toPlain).build(), card)

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(garageManager.getCard(?, ?, ?)(?)).thenReturnF(card)
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturnF(true)
      when(passportClient.getUser(?)(?)).thenReturnF(userResult)
      when(vosClient.addProvenOwnerState(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(
        draftsManager.getCurrentDraft(
          CategorySelector.Cars,
          user,
          isAddForm = true,
          requestCarfaxOptions = false
        )
      ).thenReturnF(DraftResponse.newBuilder().setOffer(offer).build())

      when(
        draftsManager.updateDraft(
          CategorySelector.Cars,
          user,
          isAddForm = true,
          draftId = offerId,
          draft = offer,
          canChangePanorama = true,
          canDisableChats = true
        )
      ).thenReturnF(DraftResponse.newBuilder().setOffer(offer).build())

      when(vosClient.createDraft(CategorySelector.Cars, user, offer))
        .thenReturn(Future.successful(offer.toBuilder.setId(OfferIDGen.next.toPlain).build()))
      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("draft_cars" -> offer.getId))

      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      stub(enrichManager.enrich(_: Offer, _: EnrichOptions)(_: RequestImpl)) {
        case (offer, _, _) => Future.successful(offer)
      }
      stub(decayManager.decay(_: Offer, _: DecayOptions)(_: RequestImpl)) {
        case (offer, _, _) => Future.successful(offer)
      }
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)
      val draftResponse: ResponseModel.DraftResponse =
        draftsManager
          .createDraftFromGarageCard(category, user, card.getId, canChangePanorama = true, canDisableChats = true)
          .futureValue

      verify(vosClient).addProvenOwnerState(
        CategorySelector.Cars,
        user,
        offerId,
        new DateTime(now.getSeconds * 1000)
      )
      draftResponse.getStatus shouldBe ResponseStatus.SUCCESS
      draftResponse.getOffer.getAdditionalInfo.getGarageId shouldBe card.getId
    }

    "throw DraftFromGarageCardApiException when card not found" in {
      val offerId = OfferIDGen.next
      val user = request.user.registeredRef
      val card = GarageCardGen.next
      val offer = draftsManager.fillOfferFromGarageCard(Offer.newBuilder().setId(offerId.toPlain).build(), card)
      val category = CategorySelector.Cars
      val userResult = PassportUserResultGen.next

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturnF(true)
      when(passportClient.getUser(?)(?)).thenReturnF(userResult)
      when(
        draftsManager.getCurrentDraft(
          CategorySelector.Cars,
          user,
          isAddForm = true,
          requestCarfaxOptions = false
        )
      ).thenReturnF(DraftResponse.newBuilder().setOffer(offer).build())

      when(
        draftsManager.updateDraft(
          CategorySelector.Cars,
          user,
          isAddForm = true,
          draftId = offerId,
          draft = offer,
          canChangePanorama = true,
          canDisableChats = true
        )
      ).thenReturnF(DraftResponse.newBuilder().setOffer(offer).build())

      when(vosClient.createDraft(CategorySelector.Cars, user, offer))
        .thenReturn(Future.successful(offer.toBuilder.setId(OfferIDGen.next.toPlain).build()))
      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("draft_cars" -> offer.getId))

      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      stub(enrichManager.enrich(_: Offer, _: EnrichOptions)(_: RequestImpl)) {
        case (offer, _, _) => Future.successful(offer)
      }
      stub(decayManager.decay(_: Offer, _: DecayOptions)(_: RequestImpl)) {
        case (offer, _, _) => Future.successful(offer)
      }
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)
      when(garageManager.getCard(?, ?, ?)(?))
        .thenReturn(Future.failed(CardNotFoundException("")))
      draftsManager
        .createDraftFromGarageCard(category, user, card.getId, canChangePanorama = true, canDisableChats = true)
        .failed
        .futureValue shouldBe an[DraftFromGarageCardApiException]
    }

    "throw DraftFromGarageCardApiException when user unsupported" in {
      val offerId = OfferIDGen.next
      val user = request.user.registeredRef
      val card = GarageCardGen.next
      val offer = draftsManager.fillOfferFromGarageCard(Offer.newBuilder().setId(offerId.toPlain).build(), card)
      val category = CategorySelector.Cars
      val userResult = PassportUserResultGen.next

      when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturnF(offer)
      when(vosClient.updateDraft(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(redirectManager.canCreateRedirect(?, ?, ?)(?)).thenReturnF(true)
      when(passportClient.getUser(?)(?)).thenReturnF(userResult)
      when(
        draftsManager.getCurrentDraft(
          CategorySelector.Cars,
          user,
          isAddForm = true,
          requestCarfaxOptions = false
        )
      ).thenReturnF(DraftResponse.newBuilder().setOffer(offer).build())

      when(
        draftsManager.updateDraft(
          CategorySelector.Cars,
          user,
          isAddForm = true,
          draftId = offerId,
          draft = offer,
          canChangePanorama = true,
          canDisableChats = true
        )
      ).thenReturnF(DraftResponse.newBuilder().setOffer(offer).build())

      when(vosClient.createDraft(CategorySelector.Cars, user, offer))
        .thenReturn(Future.successful(offer.toBuilder.setId(OfferIDGen.next.toPlain).build()))
      when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("draft_cars" -> offer.getId))

      when(uploaderClient.sign(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(SignResponseGen.next))
      stub(enrichManager.enrich(_: Offer, _: EnrichOptions)(_: RequestImpl)) {
        case (offer, _, _) => Future.successful(offer)
      }
      stub(decayManager.decay(_: Offer, _: DecayOptions)(_: RequestImpl)) {
        case (offer, _, _) => Future.successful(offer)
      }
      when(userPriceManager.getPrices(?, ?, ?, ?, ?)(?)).thenReturnF(Seq.empty)
      when(garageManager.getCard(?, ?, ?)(?))
        .thenReturn(Future.failed(UnsupportedUserType()))
      draftsManager
        .createDraftFromGarageCard(category, user, card.getId, canChangePanorama = true, canDisableChats = true)
        .failed
        .futureValue shouldBe an[DraftFromGarageCardApiException]
    }
  }
}

object DraftsManagerSpec {

  private def keepPhones(userResult: UserResult, phones: String*): UserResult = {
    val builder = userResult.toBuilder
    builder.getUserBuilder.clearPhones()
    phones.foreach(phone => {
      builder.getUserBuilder.addPhonesBuilder().setPhone(phone)
    })
    builder.build()
  }

  private def keepPhones(offer: Offer, phones: String*): Offer = {
    val builder = offer.toBuilder
    builder.getSellerBuilder.clearPhones()
    builder.getPrivateSellerBuilder.clearPhones()
    phones.foreach(phone => {
      builder.getSellerBuilder.addPhonesBuilder().setPhone(phone)
      builder.getPrivateSellerBuilder.addPhonesBuilder().setPhone(phone)
    })
    builder.build()
  }

  private def mosRuValidationModStatus(domains: Seq[String]): UserModerationStatus = {
    val b = UserModerationStatus
      .newBuilder()
      .setReseller(false)
    val banReasons = Set("MOS_RU_VALIDATION")
    domains.foreach { domain =>
      b.putBans(
        domain,
        DomainBan.newBuilder.addAllReasons(banReasons.asJava).build
      )
    }

    b.build()
  }
}
