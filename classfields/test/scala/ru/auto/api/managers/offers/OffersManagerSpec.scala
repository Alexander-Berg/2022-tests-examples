package ru.auto.api.managers.offers

import cats.syntax.option._
import org.apache.http.client.utils.URIBuilder
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.{BeforeAndAfter, OptionValues}
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.auto.api.ApiOfferModel._
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.{Actions, PaidReason, PhotoType}
import ru.auto.api.ResponseModel.ResponseStatus
import ru.auto.api.auth.{Application, ApplicationToken}
import ru.auto.api.buyers.BuyersModel.PredictionsList
import ru.auto.api.exceptions._
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.broker.BrokerManager
import ru.auto.api.managers.decay.{DecayManager, DecayOptions}
import ru.auto.api.managers.enrich.{EnrichManager, EnrichOptions}
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.price.UserPriceManager
import ru.auto.api.model.ModelGenerators.{OfferGen, SignResponseGen}
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.services.cabinet.CabinetApiClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.predictbuyer.PredictBuyerClient
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.telepony.TeleponyClient
import ru.auto.api.services.uploader.UploaderClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.util.Tagged._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.uploader.model.SignResponse

import scala.concurrent.Future
import ru.auto.api.managers.enrich.enrichers.DailyCountersEnricher

import java.time.LocalDate
import ru.auto.api.ResponseModel.OfferListingResponse
import com.google.protobuf.BoolValue
import ru.auto.api.managers.passport.PassportManager
import org.scalatest.prop.TableDrivenPropertyChecks
import ru.auto.api.ResponseModel.Filters

import scala.jdk.CollectionConverters._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 05.03.17
  */
class OffersManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with BeforeAndAfter
  with OptionValues
  with TableDrivenPropertyChecks {
  val vosClient: VosClient = mock[VosClient]
  val cabinetApiClient: CabinetApiClient = mock[CabinetApiClient]
  val enrichManager: EnrichManager = mock[EnrichManager]
  val phoneRedirectManager: PhoneRedirectManager = mock[PhoneRedirectManager]
  val decayManager: DecayManager = mock[DecayManager]
  val userPriceManager: UserPriceManager = mock[UserPriceManager]
  val featureManager: FeatureManager = mock[FeatureManager]
  val predictBuyerClient: PredictBuyerClient = mock[PredictBuyerClient]
  val teleponyClient: TeleponyClient = mock[TeleponyClient]
  val brokerManager: BrokerManager = mock[BrokerManager]
  val uploaderClient: UploaderClient = mock[UploaderClient]
  val salesmanClient: SalesmanClient = mock[SalesmanClient]
  val fakeManager: FakeManager = mock[FakeManager]
  val passportManager: PassportManager = mock[PassportManager]

  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(true)
  when(featureManager.newOfferActivationLogic).thenReturn(feature)

  val selfAddress = "http://localhost:2600"
  val publicAddress = selfAddress

  val offersManager: OffersManager = new OffersManager(
    vosClient,
    userPriceManager,
    cabinetApiClient,
    decayManager,
    enrichManager,
    fakeManager,
    predictBuyerClient,
    brokerManager,
    teleponyClient,
    selfAddress,
    publicAddress,
    uploaderClient,
    salesmanClient,
    featureManager,
    passportManager,
    imageTtl = None
  )

  before {
    when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturn(Future(false))
  }

  after {
    verifyNoMoreInteractions(vosClient, enrichManager)
    reset(vosClient, enrichManager)
  }

  implicit val trace: Traced = Traced.empty

  implicit val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setApplication(Application.desktop)
    r.setRequestParams(RequestParams.construct("0.0.0.0", sessionId = Some("fake_session")))
    r
  }

  def generateRequestWithUser: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setApplication(Application.desktop)
    r.setRequestParams(RequestParams.construct("0.0.0.0", sessionId = Some("fake_session")))
    r.setUser(ModelGenerators.RegisteredUserRefGen.next)
    r.setToken(TokenServiceImpl.desktop)
    r
  }

  import org.scalacheck.Shrink
  implicit val noShrink: Shrink[Int] = Shrink.shrinkAny

  "OffersManager.getOffer" should {
    "load offer from vos and enrich it" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.PrivateOfferGen) { (selector, offer) =>
        val user = offer.userRef.asRegistered
        val id = offer.id

        reset(decayManager)
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
        when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
        when(fakeManager.fake(any[Offer]())(?)).thenReturnF(offer)
        val result = offersManager.getOffer(selector, user, id).futureValue

        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffer shouldBe offer

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(decayManager).decay(offer, DecayOptions.empty)
        verify(enrichManager).enrich(offer, EnrichOptions.ForCabinetCard)
      }
    }
    "throw OfferNotFound if not found on vos" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.RegisteredUserRefGen, ModelGenerators.OfferIDGen) {
        (selector, user, id) =>
          when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))

          offersManager.getOffer(selector, user, id).failed.futureValue shouldBe an[OfferNotFoundException]

          verify(vosClient).getUserOffer(
            selector,
            user,
            id,
            includeRemoved = false,
            forceTeleponyInfo = false,
            executeOnMaster = true
          )(request)
      }
    }
  }

  "OffersManager.getOfferOrigPhotoUrlList" should {
    "load offer offer orig photos" in {
      forAll(
        ModelGenerators.SelectorGen,
        ModelGenerators.OfferGen,
        ModelGenerators.OfferOrigPhotoGen
      ) { (selector, offer, photos) =>
        val id = offer.id
        val req = {
          val r = new RequestImpl
          r.setUser(offer.userRef)
          r.setTrace(trace)
          r.setApplication(Application.desktop)
          r.setToken(ApplicationToken("swagger"))
          r.setRequestParams(RequestParams.construct("0.0.0.0", sessionId = Some("fake_session")))
          r
        }

        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(vosClient.getOfferOrigPhotos(?, ?)(?)).thenReturnF(photos)

        val result = offersManager.getOfferOrigPhotoUrlList(selector, id)(req).futureValue

        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getUrlCount shouldBe photos.getPhotoCount
        result.getUrl(0) should include("sign=")
        result.getUrl(0) should include("token=raw-photos")

        verify(vosClient).getOffer(
          selector,
          id,
          includeRemoved = false,
          fromNewDb = false,
          forceTeleponyInfo = false
        )(req)

        verify(vosClient).getOfferOrigPhotos(
          selector,
          id
        )(req)

        reset(vosClient)
      }
    }
    "throw OfferNotFound if not found on vos" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.RegisteredUserRefGen, ModelGenerators.OfferIDGen) {
        (selector, user, id) =>
          when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))
          when(vosClient.getOfferOrigPhotos(?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))

          offersManager.getOfferOrigPhotoUrlList(selector, id).failed.futureValue shouldBe an[OfferNotFoundException]

          verify(vosClient).getOffer(
            selector,
            id,
            includeRemoved = false,
            fromNewDb = false,
            forceTeleponyInfo = false
          )(request)

          reset(vosClient)
      }
    }
  }

  "OffersManager.getSameOffer" should {
    "load same offer from vos and enrich it" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.PrivateOfferGen) { (selector, offer) =>
        val user = offer.userRef.asRegistered
        val id = offer.id

        reset(decayManager)
        when(vosClient.similar(?, ?, ?)(?)).thenReturnF(Some(offer))
        when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
        when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)

        val result = offersManager.getSameOffer(user, selector, id).futureValue.get

        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffer shouldBe offer

        verify(vosClient).similar(selector, user, id)(request)
        verify(decayManager).decay(offer, DecayOptions.empty)
        verify(enrichManager).enrich(offer, EnrichOptions.ForCabinetCard)
      }
    }
    "return empty response when similar not found" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.RegisteredUserRefGen, ModelGenerators.OfferIDGen) {
        (selector, user, id) =>
          when(vosClient.similar(?, ?, ?)(?)).thenReturnF(None)

          offersManager.getSameOffer(user, selector, id).futureValue shouldBe None

          verify(vosClient).similar(selector, user, id)(request)
      }
    }
  }

  "OffersManager.actualize" should {
    "actualize offer in vos and return success response" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.RegisteredUserRefGen, ModelGenerators.OfferIDGen) {
        (selector, user, id) =>
          when(vosClient.actualize(?, ?, ?)(?)).thenReturn(Future.unit)

          val response = offersManager.actualize(selector, user, id).futureValue
          response.getStatus shouldBe ResponseStatus.SUCCESS

          verify(vosClient).actualize(selector, user, id)(request)
      }
    }
    "throw OfferNotFound if offer not found on vos" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.RegisteredUserRefGen, ModelGenerators.OfferIDGen) {
        (selector, user, id) =>
          when(vosClient.actualize(?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))

          offersManager.actualize(selector, user, id).failed.futureValue shouldBe an[OfferNotFoundException]

          verify(vosClient).actualize(selector, user, id)(request)
      }
    }
  }

  "OffersManager.archive" should {
    "archive offer in vos and return success response" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.RegisteredUserRefGen, ModelGenerators.OfferIDGen) {
        (selector, user, id) =>
          when(vosClient.archive(?, ?, ?)(?)).thenReturn(Future.unit)

          val response = offersManager.archive(selector, user, id).futureValue
          response.getStatus shouldBe ResponseStatus.SUCCESS

          verify(vosClient).archive(selector, user, id)(request)
      }
    }
    "throw OfferNotFound if offer not found on vos" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.RegisteredUserRefGen, ModelGenerators.OfferIDGen) {
        (selector, user, id) =>
          when(vosClient.archive(?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))

          offersManager.archive(selector, user, id).failed.futureValue shouldBe an[OfferNotFoundException]

          verify(vosClient).archive(selector, user, id)(request)
      }
    }
    "archive multiposting offer in vos and return success response" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.DealerUserRefGen, ModelGenerators.OfferIDGen) {
        (selector, user, id) =>
          when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturn(Future(true))
          when(vosClient.archive(?, ?, ?)(?)).thenReturn(Future.unit)
          when(vosClient.archiveMultiPosting(?, ?, ?)(?)).thenReturn(Future.unit)

          val response = offersManager.archive(selector, user, id).futureValue
          response.getStatus shouldBe ResponseStatus.SUCCESS

          verify(vosClient).archive(selector, user, id)(request)
          verify(vosClient).archiveMultiPosting(selector, user, id)(request)
      }
    }
  }

  "OffersManager.hide" should {

    val offer = Offer
      .newBuilder()
      .setSeller(Seller.newBuilder().addPhones(Phone.newBuilder().setPhone("+79991234567")))
      .build()

    "hide offer in vos and return success response" in {
      forAll(
        ModelGenerators.SelectorGen,
        ModelGenerators.RegisteredUserRefGen,
        ModelGenerators.OfferIDGen,
        ModelGenerators.OfferHideRequestGen
      ) { (selector, user, id, hideRequest) =>
        when(vosClient.hide(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
        doNothing().when(brokerManager).reportOfferRecall(?, ?, ?, ?)(?, ?)
        when(teleponyClient.removeRedirect(?, ?, ?)(?)).thenReturn(Future.unit)
        when(teleponyClient.addPhoneToWhiteList(?, ?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.hide(selector, user, id, hideRequest).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).hide(selector, user, id, hideRequest)(request)
        verify(vosClient, atMostOnce()).getOffer(eq(selector), eq(id), ?, ?, ?)(?)
        verify(brokerManager, atMostOnce()).reportOfferRecall(eq(selector), eq(user), eq(id), eq(hideRequest))(
          eq(request),
          ?
        )
        verify(teleponyClient, atMostOnce()).removeRedirect(?, ?, ?)(?)
        verify(teleponyClient, atMostOnce()).addPhoneToWhiteList(?, ?, ?, ?)(?)
        reset(vosClient, brokerManager, teleponyClient)
      }
    }
    "hide OfferNotFound if offer not found on vos" in {
      forAll(
        ModelGenerators.SelectorGen,
        ModelGenerators.RegisteredUserRefGen,
        ModelGenerators.OfferIDGen,
        ModelGenerators.OfferHideRequestGen
      ) { (selector, user, id, hideRequest) =>
        when(vosClient.hide(?, ?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
        doNothing().when(brokerManager).reportOfferRecall(?, ?, ?, ?)(?, ?)
        when(teleponyClient.removeRedirect(?, ?, ?)(?)).thenReturn(Future.unit)
        when(teleponyClient.addPhoneToWhiteList(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        offersManager.hide(selector, user, id, hideRequest).failed.futureValue shouldBe an[OfferNotFoundException]

        verify(vosClient).hide(selector, user, id, hideRequest)(request)
        verify(brokerManager, atMostOnce()).reportOfferRecall(eq(selector), eq(user), eq(id), eq(hideRequest))(
          eq(request),
          ?
        )
        verify(vosClient, atMostOnce()).getOffer(eq(selector), eq(id), ?, ?, ?)(?)
        verify(teleponyClient, atMostOnce()).removeRedirect(?, ?, ?)(?)
        verify(teleponyClient, atMostOnce()).addPhoneToWhiteList(?, ?, ?, ?)(?)
        reset(vosClient, brokerManager, teleponyClient)
      }
    }

    "hide multiposting offer and return success response with autoru offer" in {
      forAll(
        ModelGenerators.SelectorGen,
        ModelGenerators.DealerUserRefGen,
        ModelGenerators.OfferIDGen,
        ModelGenerators.OfferHideRequestGen
      ) { (selector, user, id, hideRequest) =>
        val testOffer = offer.toBuilder
          .setActions(Actions.newBuilder().setHide(true))
          .build()

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.hideMultiPosting(?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.hide(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturnF(testOffer)
        doNothing().when(brokerManager).reportOfferRecall(?, ?, ?, ?)(?, ?)
        when(teleponyClient.removeRedirect(?, ?, ?)(?)).thenReturn(Future.unit)
        when(teleponyClient.addPhoneToWhiteList(?, ?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.hide(selector, user, id, hideRequest).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).hide(selector, user, id, hideRequest)(request)
        verify(vosClient).hideMultiPosting(selector, user, id)(request)
        verify(vosClient, atMostOnce()).getOffer(eq(selector), eq(id), ?, ?, ?)(?)
        verify(brokerManager, atMostOnce()).reportOfferRecall(eq(selector), eq(user), eq(id), eq(hideRequest))(
          eq(request),
          ?
        )
        verify(teleponyClient, atMostOnce()).removeRedirect(?, ?, ?)(?)
        verify(teleponyClient, atMostOnce()).addPhoneToWhiteList(?, ?, ?, ?)(?)
        reset(vosClient, brokerManager, teleponyClient)
      }
    }

    "hide multiposting offer and return success response ignore autoru offer" in {
      forAll(
        ModelGenerators.SelectorGen,
        ModelGenerators.DealerUserRefGen,
        ModelGenerators.OfferIDGen,
        ModelGenerators.OfferHideRequestGen
      ) { (selector, user, id, hideRequest) =>
        val testOffer = offer.toBuilder
          .setActions(Actions.newBuilder().setHide(false))
          .build()

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.hideMultiPosting(?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturnF(testOffer)

        val response = offersManager.hide(selector, user, id, hideRequest).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).hideMultiPosting(selector, user, id)(request)
        verify(vosClient, never()).hide(selector, user, id, hideRequest)(request)
        verify(vosClient, atMostOnce()).getOffer(eq(selector), eq(id), ?, ?, ?)(?)
        verify(brokerManager, never()).reportOfferRecall(eq(selector), eq(user), eq(id), eq(hideRequest))(
          eq(request),
          ?
        )
        verify(teleponyClient, never()).removeRedirect(?, ?, ?)(?)
        verify(teleponyClient, never()).addPhoneToWhiteList(?, ?, ?, ?)(?)
        reset(vosClient, brokerManager, teleponyClient)
      }
    }
  }

  "OffersManager.activate" should {
    "load current offer with include_removed = true and throw OfferNotFound if offer not found" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.PrivateOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))

        an[OfferNotFoundException] shouldBe thrownBy {
          offersManager.activate(selector, user, id).await
        }

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
      }
    }
    "activate offer if activation price is zero" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.PrivateCarsOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(userPriceManager.getActivationPrice(?)(?)).thenReturnF(price)
        when(vosClient.similar(?, ?, ?)(?)).thenReturnF(None)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.activate(?, ?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.activate(selector, user, id).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(userPriceManager).getActivationPrice(offer)(request)
        verify(vosClient).similar(selector, user, id)(request)
        verify(vosClient).activate(selector, user, id, needActivation = false)(request)
        verify(vosClient).checkActivationStatus(selector, user, id)(trace)
      }
    }

    "do not load price for dealer, charge instead" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.DealerOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asDealer

        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(salesmanClient.validateActivation(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.activate(?, ?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.activate(selector, user, id).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(salesmanClient).validateActivation(user.asDealer, offer, AutoruProduct.Placement)(request)
        verify(vosClient).activate(selector, user, id, needActivation = false)(request)
        verify(vosClient).checkActivationStatus(selector, user, id)(trace)
      }
    }

    "throw not enough funds if unable to charge dealer's offer" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.DealerOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asDealer

        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(salesmanClient.validateActivation(?, ?, ?)(?)).thenThrowF(new UnableToActivateOfferException)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())

        val response = offersManager.activate(selector, user, id).failed.futureValue
        response shouldBe a[UnableToActivateOfferException]

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(salesmanClient).validateActivation(user.asDealer, offer, AutoruProduct.Placement)(request)
        verify(vosClient).checkActivationStatus(selector, user, id)(trace)
      }
    }

    "throw OfferTooOld if vos rejected activation" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.PrivateCarsOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered

        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenThrowF(new OfferTooOldException)
        an[OfferTooOldException] shouldBe thrownBy {
          offersManager.activate(selector, user, id).await
        }

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(vosClient).checkActivationStatus(selector, user, id)(trace)
      }
    }

    "throw PaymentNeeded with price if activation price > 0 and no similar offers found" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.PrivateCarsOfferGen, Gen.chooseNum(1, 10000)) {
        (selector, offer, price) =>
          assume(price > 0)
          val id = offer.id
          val user = offer.userRef.asRegistered
          val activationPrice = ActivationPrice(price, price, None, None)

          when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
          when(userPriceManager.getActivationPrice(?)(?)).thenReturnF(activationPrice)
          when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
          when(vosClient.similar(?, ?, ?)(?)).thenReturnF(None)

          val ex = the[PaymentNeeded] thrownBy {
            offersManager.activate(selector, user, id).await
          }
          ex.price shouldBe activationPrice

          verify(vosClient).getUserOffer(
            selector,
            user,
            id,
            includeRemoved = true,
            forceTeleponyInfo = false,
            executeOnMaster = true
          )(request)
          verify(userPriceManager).getActivationPrice(offer)(request)
          verify(vosClient).similar(selector, user, id)(request)
          verify(vosClient).checkActivationStatus(selector, user, id)(trace)
      }
    }

    "throw HaveSimilarOffer with price and enriched similar offer" +
      " if activation price > 0, paid_reason = SAME_SALE and similar offers is found" in {
      forAll(
        ModelGenerators.SelectorGen,
        ModelGenerators.PrivateCarsOfferGen,
        ModelGenerators.PrivateCarsOfferGen,
        ModelGenerators.PrivateCarsOfferGen,
        Gen.chooseNum(1, 10000)
      ) { (selector, offer, similarOffer, enrichedOffer, price) =>
        assume(price > 0)
        val id = offer.id
        val user = offer.userRef.asRegistered
        val activationPrice = ActivationPrice(price, price, Some(PaidReason.SAME_SALE), None)

        reset(decayManager)
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(userPriceManager.getActivationPrice(?)(?)).thenReturnF(activationPrice)
        when(vosClient.similar(?, ?, ?)(?)).thenReturnF(Some(similarOffer))
        when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(enrichedOffer)
        when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(enrichedOffer)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())

        val ex = the[HaveSimilarOffer] thrownBy {
          offersManager.activate(selector, user, id).await
        }
        ex.price shouldBe activationPrice
        ex.offer shouldBe enrichedOffer

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(userPriceManager).getActivationPrice(offer)(request)
        verify(vosClient).similar(selector, user, id)(request)
        verify(enrichManager).enrich(similarOffer, EnrichOptions.ForCabinetCard)(request)
        verify(decayManager).decay(enrichedOffer, DecayOptions.empty)
        verify(vosClient).checkActivationStatus(selector, user, id)(trace)
      }
    }

    "activate multiposting offer and activate autoru as well for private user" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.PrivateCarsOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        val enabledAutoRu = Multiposting.Classified
          .newBuilder()
          .setName(ClassifiedName.AUTORU)
          .setEnabled(true)

        val multiPostingOffer =
          Offer.newBuilder(offer).setMultiposting(Multiposting.newBuilder().addClassifieds(enabledAutoRu)).build()

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturn(Future(true))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(multiPostingOffer)
        when(userPriceManager.getActivationPrice(?)(?)).thenReturnF(price)
        when(vosClient.similar(?, ?, ?)(?)).thenReturnF(None)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.activate(?, ?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.activate(selector, user, id).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(userPriceManager).getActivationPrice(multiPostingOffer)(request)
        verify(vosClient).similar(selector, user, id)(request)
        verify(vosClient).activate(selector, user, id, needActivation = false)(request)
        verify(vosClient).checkActivationStatus(selector, user, id)(trace)
      }
    }

    "activate multiposting offer and activate autoru as well for dealer user" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.DealerCarOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        val enabledAutoRu = Multiposting.Classified
          .newBuilder()
          .setName(ClassifiedName.AUTORU)
          .setEnabled(true)

        val multiPostingOffer =
          Offer
            .newBuilder(offer)
            .setMultiposting(Multiposting.newBuilder().addClassifieds(enabledAutoRu))
            .setActions(Actions.newBuilder().setActivate(true))
            .build()

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturn(Future(true))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(multiPostingOffer)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
        when(salesmanClient.validateActivation(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.activate(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.activateMultiPosting(?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.activate(selector, user, id).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(vosClient).activate(selector, user, id, needActivation = false)(request)
        verify(vosClient).checkActivationStatus(selector, user, id)(trace)
        verify(vosClient).activateMultiPosting(selector, user, id)(request)
      }
    }

    "activate multiposting offer and don't activate autoru (action is not allowed)" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.DealerCarOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        val enabledAutoRu = Multiposting.Classified
          .newBuilder()
          .setName(ClassifiedName.AUTORU)
          .setEnabled(true)

        val multiPostingOffer =
          Offer
            .newBuilder(offer)
            .setMultiposting(Multiposting.newBuilder().addClassifieds(enabledAutoRu))
            .setActions(Actions.newBuilder().setActivate(false))
            .build()

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturn(Future(true))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(multiPostingOffer)
        when(vosClient.activateMultiPosting(?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.activate(selector, user, id).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(vosClient, never()).activate(selector, user, id, needActivation = false)(request)
        verify(vosClient, never()).checkActivationStatus(selector, user, id)(trace)
        verify(vosClient).activateMultiPosting(selector, user, id)(request)
      }
    }

    "activate multiposting offer and don't activate autoru (disabled)" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.DealerCarOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        val enabledAutoRu = Multiposting.Classified
          .newBuilder()
          .setName(ClassifiedName.AUTORU)
          .setEnabled(false)

        val multiPostingOffer =
          Offer.newBuilder(offer).setMultiposting(Multiposting.newBuilder().addClassifieds(enabledAutoRu)).build()

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturn(Future(true))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(multiPostingOffer)
        when(userPriceManager.getActivationPrice(?)(?)).thenReturnF(price)
        when(vosClient.similar(?, ?, ?)(?)).thenReturnF(None)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.activate(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.activateMultiPosting(?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.activate(selector, user, id).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(userPriceManager, never).getActivationPrice(multiPostingOffer)(request)
        verify(vosClient, never).similar(selector, user, id)(request)
        verify(vosClient, never).activate(selector, user, id, needActivation = false)(request)
        verify(vosClient, never).checkActivationStatus(selector, user, id)(trace)
        verify(vosClient).activateMultiPosting(selector, user, id)(request)
      }
    }

    "activate multiposting offer and don't activate autoru (is not presented in the classifieds list)" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.DealerCarOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        val enabledAutoRu = Multiposting.Classified
          .newBuilder()
          .setName(ClassifiedName.DROM)
          .setEnabled(true)

        val multiPostingOffer =
          Offer.newBuilder(offer).setMultiposting(Multiposting.newBuilder().addClassifieds(enabledAutoRu)).build()

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturn(Future(true))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(multiPostingOffer)
        when(userPriceManager.getActivationPrice(?)(?)).thenReturnF(price)
        when(vosClient.similar(?, ?, ?)(?)).thenReturnF(None)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.activate(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.activateMultiPosting(?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.activate(selector, user, id).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(userPriceManager, never).getActivationPrice(multiPostingOffer)(request)
        verify(vosClient, never).similar(selector, user, id)(request)
        verify(vosClient, never).activate(selector, user, id, needActivation = false)(request)
        verify(vosClient, never).checkActivationStatus(selector, user, id)(trace)
        verify(vosClient).activateMultiPosting(selector, user, id)(request)
      }
    }

    "activate multiposting offer and don't activate autoru (classifieds are not defined)" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.DealerCarOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturn(Future(true))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(userPriceManager.getActivationPrice(?)(?)).thenReturnF(price)
        when(vosClient.similar(?, ?, ?)(?)).thenReturnF(None)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.activate(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.activateMultiPosting(?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.activate(selector, user, id).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(userPriceManager, never).getActivationPrice(offer)(request)
        verify(vosClient, never).similar(selector, user, id)(request)
        verify(vosClient, never).activate(selector, user, id, needActivation = false)(request)
        verify(vosClient, never).checkActivationStatus(selector, user, id)(trace)
        verify(vosClient).activateMultiPosting(selector, user, id)(request)
      }
    }

    "don't activate multiposting (is already active) offer but activate autoru" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.PrivateCarsOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        val enabledAutoRu = Multiposting.Classified
          .newBuilder()
          .setName(ClassifiedName.AUTORU)
          .setEnabled(true)

        val multiposting = Multiposting
          .newBuilder()
          .addClassifieds(enabledAutoRu)
          .setStatus(OfferStatus.ACTIVE)

        val multiPostingOffer = Offer.newBuilder(offer).setMultiposting(multiposting).build()

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturn(Future(true))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(multiPostingOffer)
        when(userPriceManager.getActivationPrice(?)(?)).thenReturnF(price)
        when(vosClient.similar(?, ?, ?)(?)).thenReturnF(None)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.activate(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.activateMultiPosting(?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.activate(selector, user, id).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(userPriceManager).getActivationPrice(multiPostingOffer)(request)
        verify(vosClient).similar(selector, user, id)(request)
        verify(vosClient).activate(selector, user, id, needActivation = false)(request)
        verify(vosClient).checkActivationStatus(selector, user, id)(trace)
        verify(vosClient, never).activateMultiPosting(selector, user, id)(request)
      }
    }

    "don't activate multiposting offer and don't activate autoru (both are already active)" in {
      forAll(ModelGenerators.SelectorGen, ModelGenerators.DealerCarOfferGen) { (selector, offer) =>
        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        val enabledAutoRu = Multiposting.Classified
          .newBuilder()
          .setName(ClassifiedName.AUTORU)
          .setEnabled(true)

        val multiposting = Multiposting
          .newBuilder()
          .addClassifieds(enabledAutoRu)
          .setStatus(OfferStatus.ACTIVE)

        val multiPostingOffer = Offer
          .newBuilder(offer)
          .setStatus(OfferStatus.ACTIVE)
          .setMultiposting(multiposting)
          .build()

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturn(Future(true))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(multiPostingOffer)
        when(userPriceManager.getActivationPrice(?)(?)).thenReturnF(price)
        when(vosClient.similar(?, ?, ?)(?)).thenReturnF(None)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.activate(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.activateMultiPosting(?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.activate(selector, user, id).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).getUserOffer(
          selector,
          user,
          id,
          includeRemoved = true,
          forceTeleponyInfo = false,
          executeOnMaster = true
        )(request)
        verify(userPriceManager, never).getActivationPrice(offer)(request)
        verify(vosClient, never).similar(selector, user, id)(request)
        verify(vosClient, never).activate(selector, user, id, needActivation = false)(request)
        verify(vosClient, never).checkActivationStatus(selector, user, id)(trace)
        verify(vosClient, never).activateMultiPosting(selector, user, id)(request)
      }
    }
  }

  "OffersManager.checkOwnership" should {

    // need request with user to avoid 401
    implicit val request: Request = {
      val r = new RequestImpl
      r.setTrace(trace)
      r.setApplication(Application.desktop)
      r.setRequestParams(RequestParams.construct("0.0.0.0", sessionId = Some("fake_session")))
      r.setUser(ModelGenerators.RegisteredUserRefGen.next)
      r
    }

    "return success response if all offers belong to current user" in {
      forAll(Gen.nonEmptyListOf(ModelGenerators.OfferIDGen)) { offerIds =>
        when(vosClient.checkBelong(?, ?)(?)).thenReturnF(true)
        //noinspection ScalaUnnecessaryParentheses
        offersManager.checkOwnership(offerIds).futureValue shouldBe (())
        verify(vosClient).checkBelong(request.user.registeredRef, offerIds)
      }
    }

    "throw OfferNotFoundException if some of offers don't belong to current user" in {
      forAll(Gen.nonEmptyListOf(ModelGenerators.OfferIDGen)) { offerIds =>
        when(vosClient.checkBelong(?, ?)(?)).thenReturnF(false)
        offersManager.checkOwnership(offerIds).failed.futureValue shouldBe an[OfferNotFoundException]
        verify(vosClient).checkBelong(request.user.registeredRef, offerIds)
      }
    }
  }

  "OffersManager.getBuyerPrediction" should {

    "return predicted phones for offer owner" in {
      forAll(OfferGen) { offer1 =>
        reset(predictBuyerClient, vosClient)
        // need request with user to avoid 401
        val requestWithUser: Request = generateRequestWithUser

        val offer =
          offer1.updated(_.setSellerType(SellerType.COMMERCIAL).setUserRef(requestWithUser.user.registeredRef.toPlain))

        val category = CategorySelector.Cars
        val id = offer.id
        val predictionsList = PredictionsList.newBuilder().setOfferId(id.toString).build()
        when(predictBuyerClient.getBuyerPredict(eq(id), ?)(?)).thenReturnF(predictionsList)
        when(vosClient.checkBelong(?, eq(List(id)))(?))
          .thenReturnF(result = true)
        val result = offersManager.getBuyerPrediction(category, id)(requestWithUser).await
        result.getOfferId shouldBe offer.getId
        result.getPredictionsList shouldNot be(null)

        verify(vosClient).checkBelong(eq(requestWithUser.user.registeredRef), eq(List(offer.id)))(?)
      }
    }
    "should not return predicted data for non-owners" in {
      forAll(OfferGen) { offer1 =>
        // need request with user to avoid 401
        val requestWithUser: Request = generateRequestWithUser
        reset(predictBuyerClient, vosClient)
        val offer =
          offer1.updated(
            _.setSellerType(SellerType.COMMERCIAL).setUserRef(ModelGenerators.RegisteredUserRefGen.next.toPlain)
          )

        val category = CategorySelector.Cars
        val id = offer.id
        val predictionsList = PredictionsList.newBuilder().setOfferId(id.toString).build()
        when(predictBuyerClient.getBuyerPredict(eq(id), ?)(?)).thenReturnF(predictionsList)
        when(vosClient.checkBelong(?, eq(List(id)))(?))
          .thenReturnF(result = false)
        assertThrows[OfferNotFoundException](offersManager.getBuyerPrediction(category, id)(requestWithUser).await)
        verify(vosClient).checkBelong(eq(requestWithUser.user.registeredRef), eq(List(offer.id)))(?)
      }
    }
  }

  "OffersManager.enableClassified" should {
    "enable classified" in {
      forAll(
        ModelGenerators.ActiveMultipostingOfferGen,
        ModelGenerators.SelectorGen,
        ModelGenerators.ClassifiedNameGen
      ) { (offerTemplate, selector, classified) =>
        val offer = offerTemplate.toBuilder
          .setStatus(OfferStatus.INACTIVE)
          .setActions(Actions.newBuilder().setActivate(true))
          .build()

        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(userPriceManager.getActivationPrice(?)(?)).thenReturnF(price)
        when(salesmanClient.validateActivation(?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.similar(?, ?, ?)(?)).thenReturnF(None)
        when(vosClient.checkActivationStatus(?, ?, ?)(?)).thenReturnF(())
        when(vosClient.activate(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.enableClassified(?, ?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.enableClassified(id, selector, user, classified).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(cabinetApiClient, atLeastOnce).isMultipostingEnabled(?)(?)

        if (classified == ClassifiedName.AUTORU && offer.hasActiveMultiposting) {
          verify(vosClient).getUserOffer(
            selector,
            user,
            id,
            includeRemoved = true,
            forceTeleponyInfo = false,
            executeOnMaster = true
          )(request)
          verify(vosClient).activate(eq(selector), eq(user), eq(id), ?)(?)
        }
        verify(vosClient).enableClassified(eq(selector), eq(user), eq(id), eq(classified))(?)

        reset(vosClient, salesmanClient, cabinetApiClient, userPriceManager)
      }
    }

    "enable classified, ignore auto.ru offer activation" in {
      forAll(
        ModelGenerators.ActiveMultipostingOfferGen,
        ModelGenerators.SelectorGen,
        ModelGenerators.ClassifiedNameGen
      ) { (offerTemplate, selector, classified) =>
        val offer = offerTemplate.toBuilder
          .setStatus(OfferStatus.INACTIVE)
          .setActions(Actions.newBuilder().setActivate(false))
          .build()

        val id = offer.id
        val user = offer.userRef.asRegistered
        val price = ActivationPrice(0, 0, None, None)

        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.enableClassified(?, ?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.enableClassified(id, selector, user, classified).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(cabinetApiClient, atLeastOnce).isMultipostingEnabled(?)(?)

        if (classified == ClassifiedName.AUTORU && offer.hasActiveMultiposting) {
          verify(vosClient).getUserOffer(
            selector,
            user,
            id,
            includeRemoved = true,
            forceTeleponyInfo = false,
            executeOnMaster = true
          )(request)
          verify(vosClient, never()).activate(eq(selector), eq(user), eq(id), ?)(?)
        }
        verify(vosClient).enableClassified(eq(selector), eq(user), eq(id), eq(classified))(?)

        reset(vosClient, salesmanClient, cabinetApiClient, userPriceManager)
      }
    }

    "throw error for not active multiposting offer" in {
      forAll(
        ModelGenerators.DealerOfferGen,
        ModelGenerators.SelectorGen,
        ModelGenerators.ClassifiedNameGen
      ) { (offer, selector, classified) =>
        val id = offer.id
        val user = offer.userRef.asRegistered

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)

        an[StatusConflict] shouldBe thrownBy {
          offersManager.enableClassified(id, selector, user, classified).await
        }

        verify(cabinetApiClient, atLeastOnce).isMultipostingEnabled(?)(?)
        reset(salesmanClient, cabinetApiClient)
      }
    }
  }

  "OffersManager.disableClassified" should {
    "disable classified" in {
      forAll(
        ModelGenerators.ActiveMultipostingOfferGen,
        ModelGenerators.SelectorGen,
        ModelGenerators.ClassifiedNameGen
      ) { (offer, selector, classified) =>
        val id = offer.id
        val user = offer.userRef.asRegistered

        val testOffer = offer.toBuilder
          .setActions(Actions.newBuilder().setHide(true))
          .build()

        when(vosClient.hide(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturnF(testOffer)
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(testOffer)
        when(teleponyClient.removeRedirect(?, ?, ?)(?)).thenReturn(Future.unit)
        when(teleponyClient.addPhoneToWhiteList(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.disableClassified(?, ?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.disableClassified(id, selector, user, classified).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(cabinetApiClient, atLeastOnce).isMultipostingEnabled(?)(?)

        if (classified == ClassifiedName.AUTORU && offer.hasActiveMultiposting) {
          verify(vosClient).getUserOffer(
            selector,
            user,
            id,
            includeRemoved = true,
            forceTeleponyInfo = false,
            executeOnMaster = true
          )(request)
          verify(vosClient).hide(eq(selector), eq(user), eq(id), ?)(?)
        }
        verify(vosClient).disableClassified(eq(selector), eq(user), eq(id), eq(classified))(?)

        reset(vosClient, salesmanClient, cabinetApiClient, teleponyClient)
      }
    }

    "disable classified, ignore auto.ru offer deactivation" in {
      forAll(
        ModelGenerators.ActiveMultipostingOfferGen,
        ModelGenerators.SelectorGen,
        ModelGenerators.ClassifiedNameGen
      ) { (offer, selector, classified) =>
        val id = offer.id
        val user = offer.userRef.asRegistered

        val testOffer = offer.toBuilder
          .setActions(Actions.newBuilder().setHide(false))
          .build()

        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturnF(testOffer)
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(testOffer)
        when(teleponyClient.removeRedirect(?, ?, ?)(?)).thenReturn(Future.unit)
        when(teleponyClient.addPhoneToWhiteList(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.disableClassified(?, ?, ?, ?)(?)).thenReturn(Future.unit)

        val response = offersManager.disableClassified(id, selector, user, classified).futureValue
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(cabinetApiClient, atLeastOnce).isMultipostingEnabled(?)(?)

        if (classified == ClassifiedName.AUTORU && offer.hasActiveMultiposting) {
          verify(vosClient).getUserOffer(
            selector,
            user,
            id,
            includeRemoved = true,
            forceTeleponyInfo = false,
            executeOnMaster = true
          )(request)
          verify(vosClient, never()).hide(eq(selector), eq(user), eq(id), ?)(?)
        }
        verify(vosClient).disableClassified(eq(selector), eq(user), eq(id), eq(classified))(?)

        reset(vosClient, salesmanClient, cabinetApiClient, teleponyClient)
      }
    }

    "throw error for not active multiposting offer" in {
      forAll(
        ModelGenerators.DealerOfferGen,
        ModelGenerators.SelectorGen,
        ModelGenerators.ClassifiedNameGen
      ) { (offer, selector, classified) =>
        val id = offer.id
        val user = offer.userRef.asRegistered

        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)

        an[StatusConflict] shouldBe thrownBy {
          offersManager.disableClassified(id, selector, user, classified).await
        }

        verify(cabinetApiClient, atLeastOnce).isMultipostingEnabled(?)(?)
        reset(salesmanClient, cabinetApiClient)
      }
    }
  }

  "OffersManager.getDocumentPhotoUploadUrls" should {
    "return upload urls" in {
      forAll(ModelGenerators.OfferIDGen) { offerId =>
        val requestWithUser: Request = generateRequestWithUser
        val category = CategorySelector.Cars
        reset(uploaderClient, vosClient)
        val (stsFrontUrl, stsFrontCallback) =
          callBackForType(PhotoType.STS_FRONT, category, offerId)(requestWithUser)
        val (stsBackUrl, stsBackCallback) =
          callBackForType(PhotoType.STS_BACK, category, offerId)(requestWithUser)
        val (driversLicenseUrl, driversLicenseCallback) =
          callBackForType(PhotoType.DRIVING_LICENSE, category, offerId)(requestWithUser)

        when(vosClient.checkBelong(?, eq(List(offerId)))(?))
          .thenReturnF(result = true)
        when(uploaderClient.sign(?, ?, eq(Some(stsFrontCallback)), ?, ?)(?))
          .thenReturn(Future.successful(stsFrontUrl))
        when(uploaderClient.sign(?, ?, eq(Some(stsBackCallback)), ?, ?)(?))
          .thenReturn(Future.successful(stsBackUrl))
        when(uploaderClient.sign(?, ?, eq(Some(driversLicenseCallback)), ?, ?)(?))
          .thenReturn(Future.successful(driversLicenseUrl))

        val response = offersManager.getDocumentPhotoUploadUrls(category, offerId)(requestWithUser).await
        response.getDocumentPhotoUploadUrl.getStsFront shouldBe stsFrontUrl.uploadUrl
        response.getDocumentPhotoUploadUrl.getStsBack shouldBe stsBackUrl.uploadUrl
        response.getDocumentPhotoUploadUrl.getDrivingLicense shouldBe driversLicenseUrl.uploadUrl
        response.getStatus shouldBe ResponseStatus.SUCCESS

        verify(vosClient).checkBelong(eq(requestWithUser.user.registeredRef), eq(List(offerId)))(?)
        val callbacks = List(stsFrontCallback, stsBackCallback, driversLicenseCallback)
        verify(uploaderClient, times(3)).sign(
          ?,
          ?,
          argThat[Option[String]](arg => callbacks.contains(arg.get)),
          ?,
          ?
        )(?)
      }
    }

    def callBackForType(
        photoType: PhotoType,
        category: CategorySelector,
        offerId: OfferID
    )(request: Request): (SignResponse, String) = {
      val callback = new URIBuilder(s"$selfAddress/1.0/user/offers/$category/$offerId/document-photo")
      request.user.optSessionID.foreach(session => callback.addParameter("session_id", session.value))
      request.user.dealerRef.foreach(dealer => callback.addParameter("x_dealer_id", dealer.clientId.toString))
      callback.addParameter("token", request.token.value)
      callback.addParameter("photo_type", photoType.name())
      SignResponseGen.next -> callback.toString
    }
  }

  "OffersManager.getListing" should {
    "process the response as expected" in {
      val category = ModelGenerators.StrictCategoryGen.next
      val user = ModelGenerators.PrivateUserRefGen.next
      val paging = ModelGenerators.PagingGen.next
      val sorting = ModelGenerators.sortingGen.next
      val countersDays = Gen.choose(1, 100).next.tagged[DailyCountersEnricher.DayTag]
      val filters = ModelGenerators.FiltersGen.next
      val decayOptions = ModelGenerators.DecayOptionsGen.next
      val rawResponse = ModelGenerators.ListingResponseGen.next
      val enrichedResponse = ModelGenerators.ListingResponseGen.next
      val decayedResponse = ModelGenerators.ListingResponseGen.next
      val fakedResponse = ModelGenerators.ListingResponseGen.next

      val dailyCountersParams = DailyCountersEnricher.DailyCountersParams.Enabled(countersDays)

      when(vosClient.getListing(?, ?, ?, ?, ?, ?, ?)(?)).thenReturnF(rawResponse)
      when(enrichManager.enrich(? : OfferListingResponse, ?)(?)).thenReturnF(enrichedResponse)
      when(decayManager.decay(? : OfferListingResponse, ?)(?)).thenReturnF(decayedResponse)
      when(fakeManager.fake(? : OfferListingResponse)(?)).thenReturnF(fakedResponse)

      val result =
        offersManager
          .getListing(
            category,
            user,
            paging,
            filters,
            sorting,
            dailyCountersParams,
            decayOptions,
            isFavoritesNeeded = false
          )
          .futureValue
      val expectedStatsFrom = Some(LocalDate.now(OffersManager.StatsTimezone).minusDays(countersDays))
      result shouldBe fakedResponse

      verify(vosClient).getListing(category, user, paging, filters, sorting, false, expectedStatsFrom)(request)
      verify(enrichManager)
        .enrich(rawResponse, EnrichOptions.ForCabinetListing.copy(dailyCounters = dailyCountersParams))(request)
      verify(decayManager).decay(enrichedResponse, decayOptions)(request)
      verify(fakeManager).fake(decayedResponse)(request)
      reset(vosClient)
    }
  }

  "OffersManager.getListingForOtherUser" should {
    "return user's offers when allowed by other user" in {
      val profile =
        ModelGenerators.PassportAutoruProfileGen.next.toBuilder.setAllowOffersShow(BoolValue.of(true)).build()
      val category = ModelGenerators.StrictCategoryGen.next
      val user = ModelGenerators.PrivateUserRefGen.next
      val paging = ModelGenerators.PagingGen.next
      val sorting = ModelGenerators.sortingGen.next
      val response = ModelGenerators.ListingResponseGen.next
      val filtersBase = ModelGenerators.FiltersGen.next.toBuilder().clearStatus().build()

      val filters = filtersBase
        .toBuilder()
        .addStatus(OfferStatus.ACTIVE)
        // This should be excluded by the implementation
        .addStatus(OfferStatus.BANNED)
        .build()

      val effectiveFilters = filtersBase
        .toBuilder()
        .addStatus(OfferStatus.ACTIVE)
        .build()

      when(passportManager.getUserProfile(user)(request)).thenReturnF(profile)
      when(vosClient.getListing(?, ?, ?, ?, ?, ?, ?)(?)).thenReturnF(response)
      when(enrichManager.enrich(? : OfferListingResponse, ?)(?)).thenReturnF(response)
      when(decayManager.decay(? : OfferListingResponse, ?)(?)).thenReturnF(response)
      when(fakeManager.fake(? : OfferListingResponse)(?)).thenReturnF(response)

      val result = offersManager.getListingForOtherUser(category, user, paging, filters, sorting).futureValue
      result shouldBe response

      verify(vosClient).getListing(category, user, paging, effectiveFilters, sorting, false, None)(request)
      verify(enrichManager)
        .enrich(? : OfferListingResponse, eq.apply(EnrichOptions.ForCabinetListing.copy(notesAndFavorites = true)))(?)
      verify(decayManager).decay(? : OfferListingResponse, eq.apply(DecayOptions.full))(?)
      reset(vosClient)
    }

    "raise ActionForbidden when not allowed by other user" in {
      val profile =
        ModelGenerators.PassportAutoruProfileGen.next.toBuilder.clearAllowOffersShow.build()
      val category = ModelGenerators.StrictCategoryGen.next
      val user = ModelGenerators.PrivateUserRefGen.next
      val paging = ModelGenerators.PagingGen.next
      val sorting = ModelGenerators.sortingGen.next
      val filtersBase = ModelGenerators.FiltersGen.next.toBuilder().clearStatus().build()

      val requestedFilters = filtersBase
        .toBuilder()
        .addStatus(OfferStatus.ACTIVE)
        // This should be excluded by the implementation
        .addStatus(OfferStatus.BANNED)
        .build()

      when(passportManager.getUserProfile(user)(request)).thenReturnF(profile)

      val error =
        offersManager.getListingForOtherUser(category, user, paging, requestedFilters, sorting).failed.futureValue
      error shouldBe an[ActionForbidden]
    }

    "raise IllegalArgumentException when only unsupported statuses are specified" in {
      val profile =
        ModelGenerators.PassportAutoruProfileGen.next.toBuilder.setAllowOffersShow(BoolValue.of(true)).build()
      val category = ModelGenerators.StrictCategoryGen.next
      val user = ModelGenerators.PrivateUserRefGen.next
      val paging = ModelGenerators.PagingGen.next
      val sorting = ModelGenerators.sortingGen.next
      val filtersBase = ModelGenerators.FiltersGen.next.toBuilder().clearStatus().build()

      val requestedFilters = filtersBase
        .toBuilder()
        // This should be excluded by the implementation
        .addStatus(OfferStatus.BANNED)
        .build()

      val error =
        offersManager.getListingForOtherUser(category, user, paging, requestedFilters, sorting).failed.futureValue
      error shouldBe a[IllegalArgumentException]
    }
  }

  "OffersManager.offerFiltersForOtherUser" should {
    import OfferStatus._
    val statusExamples = Table[Set[OfferStatus], Option[Set[OfferStatus]]](
      ("in", "expected"),
      // Preserve allowed statuses
      (Set(ACTIVE), Set(ACTIVE).some),
      (Set(INACTIVE), Set(INACTIVE).some),
      (Set(ACTIVE, INACTIVE), Set(ACTIVE, INACTIVE).some),
      // Drop disallowed statuses
      (Set(ACTIVE, BANNED), Set(ACTIVE).some),
      (OfferStatus.values.toSet - UNRECOGNIZED, Set(ACTIVE, INACTIVE).some),
      // Return all allowed statuses if the filter doesn't have specified statuses
      (Set.empty, Set(ACTIVE, INACTIVE).some),
      // Return `None` if the filter only has disallowed statuses
      (Set(BANNED), None)
    )

    "work as expected for known examples" in forAll(statusExamples) {
      (inStatuses: Set[OfferStatus], expectedStatuses: Option[Set[OfferStatus]]) =>
        val in = Filters.newBuilder().addAllStatus(inStatuses.asJava).build
        val expected = expectedStatuses.map(es => Filters.newBuilder().addAllStatus(es.asJava).build)
        OffersManager.offerFiltersForOtherUser(in) shouldBe expected
    }

    "produce filters that only allow specified statuses" in forAll(
      Gen.listOf(Gen.oneOf(OfferStatus.values.toSet - UNRECOGNIZED)).map(_.toSet)
    ) { inStatuses =>
      val in = Filters.newBuilder().addAllStatus(inStatuses.asJava).build
      val outOpt = OffersManager.offerFiltersForOtherUser(in)
      outOpt.foreach { out =>
        val outStatuses = out.getStatusList.asScala.toVector
        // Filter with no conditions on status would allow any statuses!
        outStatuses should not be empty
        Set(ACTIVE, INACTIVE) should contain allElementsOf (outStatuses)
      }
    }
  }
}
