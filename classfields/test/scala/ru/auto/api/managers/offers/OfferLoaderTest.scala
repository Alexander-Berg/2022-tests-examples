package ru.auto.api.managers.offers

import org.mockito.Mockito
import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel._
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.OfferNotFoundException
import ru.auto.api.managers.decay.{DecayManager, DecayOptions}
import ru.auto.api.managers.enrich.{EnrichManager, EnrichOptions}
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.{CategorySelector, RequestParams}
import ru.auto.api.services.octopus.PhoneAccessDataResponse
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.settings.SettingsClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 22.06.18
  */
class OfferLoaderTest extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  val searcherClient: SearcherClient = mock[SearcherClient]
  val vosClient: VosClient = mock[VosClient]
  val enrichManager: EnrichManager = mock[EnrichManager]
  val decayManager: DecayManager = mock[DecayManager]
  val settingsClient: SettingsClient = mock[SettingsClient]
  val fakeManager = mock[FakeManager]

  when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
  when(fakeManager.frontSalt).thenReturn("salt")
  Mockito.doNothing().when(fakeManager).checkFakeOfferId(?, ?)(?)

  val optionsNoTruncate: DecayOptions = DecayOptions(
    sensitiveData = true,
    hotData = true,
    priceHistory = true,
    address = true,
    truncatePriceHistory = true,
    shouldMaskVinAndLicensePlate = Some(true)
  )

  val offerLoader =
    new EnrichedOfferLoader(vosClient, searcherClient, enrichManager, decayManager, fakeManager, settingsClient)

  implicit private val trace: Traced = Traced.empty

  implicit private val request: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionIdGen.next)))
    r.setApplication(Application.iosApp)
    r.setUser(PersonalUserRefGen.next)
    r.setTrace(trace)
    r
  }

  val phoneAccessDeny = PhoneAccessDataResponse(
    viewAllowed = false,
    incrementAllowed = false
  )

  val phoneAccessAllowed = PhoneAccessDataResponse(
    viewAllowed = true,
    incrementAllowed = true
  )

  before {
    reset(vosClient, searcherClient, decayManager)
  }

  "OfferLoader.getOffer" should {
    "pass force_telepony_info param to vos" in {
      forAll(OfferGen, OfferGen) { (offer1, enriched1) =>
        reset(searcherClient, vosClient, enrichManager, decayManager)
        val offer = offer1.updated(_.setSellerType(SellerType.COMMERCIAL).setUserRef(request.user.ref.toPlain))

        val category = CategorySelector.Cars
        val id = offer.id
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturnF(offer)

        val result = offerLoader.findRawOffer(category, id, fromVosOnly = true, forceTeleponyInfo = true).await
        result.id shouldBe offer.id

        verify(vosClient).getOffer(
          category,
          offer.id,
          fromNewDb = true,
          includeRemoved = true,
          forceTeleponyInfo = true
        )(request)
      }
    }

    "default load without explicit enrich options" in {
      forAll(OfferGen, OfferGen) { (offer1, enriched1) =>
        reset(searcherClient, vosClient, enrichManager, decayManager)
        val offer = offer1.updated(_.setSellerType(SellerType.COMMERCIAL))
        val enriched = enriched1.updated(_.setSellerType(SellerType.COMMERCIAL))

        val category = CategorySelector.Cars
        val id = offer.id

        when(searcherClient.getOffer(?, ?)(?)).thenReturnF(offer)
        when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(enriched)
        when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(enriched)
        when(fakeManager.fake(any[Offer]())(?)).thenReturnF(enriched)

        val result = offerLoader.getOffer(category, id).await
        result.id shouldBe enriched.id

        verify(searcherClient).getOffer(eq(category), eq(id))(eq(trace))
        verify(enrichManager)
          .enrich(offer, EnrichOptions.ForRandomUser.copy(counters = false, sharkInfo = false))
        verify(decayManager).decay(enriched, optionsNoTruncate)
      }
    }

    "load COMMERCIAL offer, enrich & decay" in {
      forAll(OfferGen, OfferGen) { (offer1, enriched1) =>
        reset(searcherClient, vosClient, enrichManager, decayManager)
        val offer = offer1.updated(_.setSellerType(SellerType.COMMERCIAL))
        val enriched = enriched1.updated(_.setSellerType(SellerType.COMMERCIAL))

        val category = CategorySelector.Cars
        val id = offer.id

        when(searcherClient.getOffer(?, ?)(?)).thenReturnF(offer)
        when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(enriched)
        when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(enriched)
        when(fakeManager.fake(any[Offer]())(?)).thenReturnF(enriched)

        val result = offerLoader.getOffer(category, id, OfferCardManager.defaultEnrichOptions).await
        result.id shouldBe enriched.id

        verify(searcherClient).getOffer(eq(category), eq(id))(eq(trace))
        verify(enrichManager).enrich(offer, EnrichOptions.ForRandomUser)
        verify(decayManager).decay(enriched, optionsNoTruncate)
      }
    }

    "load COMMERCIAL offer, enrich & decay for owner" in {
      forAll(OfferGen, OfferGen) { (offer1, enriched1) =>
        reset(searcherClient, vosClient, enrichManager, decayManager)
        val offer = offer1.updated(_.setSellerType(SellerType.COMMERCIAL).setUserRef(request.user.ref.toPlain))
        val enriched = enriched1.updated(_.setSellerType(SellerType.COMMERCIAL).setUserRef(request.user.ref.toPlain))

        val category = CategorySelector.Cars
        val id = offer.id
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(searcherClient.getOffer(?, ?)(?)).thenReturnF(offer)
        when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(enriched)
        when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(enriched)
        when(fakeManager.fake(any[Offer]())(?)).thenReturnF(enriched)

        val result = offerLoader.getOffer(category, id, OfferCardManager.defaultEnrichOptions).await
        result.id shouldBe enriched.id

        verify(vosClient).getOffer(
          category,
          offer.id,
          fromNewDb = true,
          includeRemoved = true,
          forceTeleponyInfo = false
        )(request)
        verify(searcherClient).getOffer(eq(category), eq(id))(eq(trace))
        verify(enrichManager).enrich(offer, EnrichOptions.ForOwnerCard)
        verify(decayManager).decay(enriched, optionsNoTruncate)
      }
    }

    "load PRIVATE offer, enrich & decay" in {
      forAll(OfferGen, OfferGen) { (offer1, enriched1) =>
        reset(searcherClient, vosClient, enrichManager, decayManager)
        val offer = offer1.updated(_.setSellerType(SellerType.PRIVATE))
        val enriched = enriched1.updated(_.setSellerType(SellerType.PRIVATE))

        val category = CategorySelector.Cars
        val id = offer.id
        when(searcherClient.getOffer(?, ?)(?)).thenReturnF(offer)
        when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(enriched)
        when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(enriched)
        when(fakeManager.fake(any[Offer]())(?)).thenReturnF(enriched)

        val result = offerLoader.getOffer(category, id, OfferCardManager.defaultEnrichOptions).await
        result.id shouldBe enriched.id

        verify(searcherClient).getOffer(eq(category), eq(id))(eq(trace))
        verify(enrichManager).enrich(offer, EnrichOptions.ForRandomUser)
        verify(decayManager).decay(enriched, optionsNoTruncate)
      }
    }

    "load PRIVATE offer, enrich & decay for owner" in {
      forAll(OfferGen, OfferGen) { (offer1, enriched1) =>
        reset(searcherClient, vosClient, enrichManager, decayManager, settingsClient)
        val offer = offer1.updated(_.setSellerType(SellerType.PRIVATE).setUserRef(request.user.ref.toPlain))
        val enriched = enriched1.updated(_.setSellerType(SellerType.PRIVATE).setUserRef(request.user.ref.toPlain))

        val category = CategorySelector.Cars
        val id = offer.id
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(searcherClient.getOffer(?, ?)(?)).thenReturnF(offer)
        when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(enriched)
        when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(enriched)
        when(fakeManager.fake(any[Offer]())(?)).thenReturnF(enriched)

        val result = offerLoader.getOffer(category, id, OfferCardManager.defaultEnrichOptions).await
        result.id shouldBe enriched.id

        verify(vosClient).getOffer(
          category,
          offer.id,
          fromNewDb = true,
          includeRemoved = true,
          forceTeleponyInfo = false
        )(request)
        verify(searcherClient).getOffer(eq(category), eq(id))(eq(trace))
        verify(enrichManager).enrich(offer, EnrichOptions.ForOwnerCard)
        verify(decayManager).decay(enriched, optionsNoTruncate)
      }
    }

    "load offer from master if offer not found in searcher and vos" +
      " and last_offer == current offer id" in {
      forAll(PrivateOfferGen) { offer =>
        reset(searcherClient, vosClient, enrichManager, decayManager, settingsClient)
        implicit val request: RequestImpl = {
          val r = new RequestImpl
          r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionIdGen.next)))
          r.setApplication(Application.iosApp)
          r.setUser(offer.userRef)
          r.setTrace(trace)
          r
        }
        val category = CategorySelector.Cars
        val id = offer.id
        when(searcherClient.getOffer(?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))
        when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map("last_offer" -> id.toPlain))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(enrichManager.enrich(any[Offer](), ?)(?)).thenReturnF(offer)
        when(decayManager.decay(any[Offer](), ?)(?)).thenReturnF(offer)
        when(fakeManager.fake(any[Offer]())(?)).thenReturnF(offer)

        offerLoader.getOffer(category, id, OfferCardManager.defaultEnrichOptions).await shouldBe offer

        verify(searcherClient).getOffer(eq(category), eq(id))(eq(trace))
        verify(vosClient).getOffer(category, id, fromNewDb = true, includeRemoved = true, forceTeleponyInfo = false)(
          request
        )
        verify(settingsClient).getSettings(?, ?)(?)
        verify(vosClient).getUserOffer(
          category,
          offer.userRef.asRegistered,
          id,
          includeRemoved = true,
          executeOnMaster = true,
          forceTeleponyInfo = false
        )(request)
        verify(enrichManager).enrich(offer, EnrichOptions.ForOwnerCard)
        verify(decayManager).decay(offer, optionsNoTruncate)
      }
    }

    "throw OfferNotFound if offer not found in searcher and vos" +
      " and user is anon" in {
      forAll(OfferIDGen) { id =>
        reset(searcherClient, vosClient, enrichManager, settingsClient)
        implicit val request: RequestImpl = {
          val r = new RequestImpl
          r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionIdGen.next)))
          r.setApplication(Application.iosApp)
          r.setUser(AnonymousUserRefGen.next)
          r.setTrace(trace)
          r
        }

        val category = CategorySelector.Cars
        when(searcherClient.getOffer(?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))

        intercept[OfferNotFoundException] {
          offerLoader.getOffer(category, id).await
        }

        verify(searcherClient).getOffer(eq(category), eq(id))(eq(trace))
        verify(vosClient).getOffer(category, id, fromNewDb = true, includeRemoved = true, forceTeleponyInfo = false)(
          request
        )
      }
    }
    "throw OfferNotFound if offer not found in searcher and vos" +
      " and settings.last_offer != current offer id" in {
      forAll(OfferGen) { offer =>
        reset(searcherClient, vosClient, enrichManager, settingsClient)
        implicit val request: RequestImpl = {
          val r = new RequestImpl
          r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionIdGen.next)))
          r.setApplication(Application.iosApp)
          r.setUser(PrivateUserRefGen.next)
          r.setTrace(trace)
          r
        }

        val category = CategorySelector.Cars
        val id = offer.id
        when(searcherClient.getOffer(?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))
        when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))
        when(settingsClient.getSettings(?, ?)(?)).thenReturnF(Map.empty)

        intercept[OfferNotFoundException] {
          offerLoader.getOffer(category, id).await
        }

        verify(searcherClient).getOffer(eq(category), eq(id))(eq(trace))
        verify(vosClient).getOffer(category, id, fromNewDb = true, includeRemoved = true, forceTeleponyInfo = false)(
          request
        )
        verify(settingsClient).getSettings(?, ?)(?)
      }
    }
  }

  "OfferLoader.findRawOffer" when {
    "requesting offers only from vos" should {
      val fromVosOnly = true

      "return tags only from vos" in {
        forAll(StrictCategoryGen, OfferGen, arbitrary[Boolean]) { (category, offer, forceTeleponyInfo) =>
          reset(searcherClient, vosClient, enrichManager, decayManager)

          when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
          when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))

          val result = offerLoader.findRawOffer(category, offer.id, fromVosOnly, forceTeleponyInfo).await

          result.getTagsList.asScala.toVector should contain theSameElementsAs offer.getTagsList.asScala.toVector

          verifyNoMoreInteractions(searcherClient)
          verify(vosClient).getOffer(?, ?, ?, ?, ?)(?)
        }
      }
    }

    "requesting offers not only from vos" should {
      val fromVosOnly = false

      "return tags only from vos" when {
        "searcher request has failed" in {
          forAll(StrictCategoryGen, OfferGen, arbitrary[Boolean]) { (category, offer, forceTeleponyInfo) =>
            reset(searcherClient, vosClient, enrichManager, decayManager)

            when(searcherClient.getOffer(?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))
            when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
            when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))

            val result = offerLoader.findRawOffer(category, offer.id, fromVosOnly, forceTeleponyInfo).await

            result.getTagsList.asScala.toVector should contain theSameElementsAs offer.getTagsList.asScala.toVector

            verify(searcherClient).getOffer(?, ?)(?)
            verify(vosClient).getOffer(?, ?, ?, ?, ?)(?)
          }
        }
      }

      "return tags only from searcher for user".which {
        "is not owner of the offer" in {
          forAll(StrictCategoryGen, OfferGen, OfferTagListGen, arbitrary[Boolean]) {
            (category, offer, vosTagList, forceTeleponyInfo) =>
              reset(searcherClient, vosClient, enrichManager, decayManager)

              val searcherOffer = offer
              val vosOffer = Offer.newBuilder(searcherOffer).clearTags().addAllTags(vosTagList.asJava).build

              when(searcherClient.getOffer(?, ?)(?)).thenReturn(Future.successful(searcherOffer))
              when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(vosOffer))
              when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(vosOffer))

              val requestUserNotOwner = new RequestImpl
              requestUserNotOwner.setRequestParams(
                RequestParams.construct("1.1.1.1", sessionId = Some(SessionIdGen.next))
              )
              requestUserNotOwner.setApplication(Application.iosApp)
              requestUserNotOwner.setUser(PrivateUserRefGen.suchThat(_ != offer.userRef).next)
              requestUserNotOwner.setTrace(trace)

              val result =
                offerLoader.findRawOffer(category, offer.id, fromVosOnly, forceTeleponyInfo)(requestUserNotOwner).await

              result.getTagsList.asScala.toVector should contain theSameElementsAs searcherOffer.getTagsList.asScala.toVector

              verify(searcherClient).getOffer(?, ?)(?)
          }
        }

        "is owner of the offer" when {
          "vos request has failed" in {
            forAll(StrictCategoryGen, OfferGen, arbitrary[Boolean]) { (category, offer, forceTeleponyInfo) =>
              reset(searcherClient, vosClient, enrichManager, decayManager)

              val searcherOffer = offer

              when(searcherClient.getOffer(?, ?)(?)).thenReturn(Future.successful(searcherOffer))
              when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))
              when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))

              val requestUserOwner = new RequestImpl
              requestUserOwner.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionIdGen.next)))
              requestUserOwner.setApplication(Application.iosApp)
              requestUserOwner.setUser(offer.userRef)
              requestUserOwner.setTrace(trace)

              val result =
                offerLoader.findRawOffer(category, offer.id, fromVosOnly, forceTeleponyInfo)(requestUserOwner).await

              result.getTagsList.asScala.toVector should contain theSameElementsAs searcherOffer.getTagsList.asScala.toVector

              verify(searcherClient).getOffer(?, ?)(?)
            }
          }
        }
      }

      "return non-duplicating tags both from vos and searcher for user".which {
        "is owner of the offer" when {
          "there are duplicating tags in responses from vos and searcher" in {
            forAll(StrictCategoryGen, OfferGen, arbitrary[Boolean]) { (category, offer, forceTeleponyInfo) =>
              reset(searcherClient, vosClient, enrichManager, decayManager)

              when(searcherClient.getOffer(?, ?)(?)).thenReturn(Future.successful(offer))
              when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
              when(vosClient.getOffer(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))

              val requestUserOwner = new RequestImpl
              requestUserOwner.setRequestParams(
                RequestParams.construct("1.1.1.1", sessionId = Some(SessionIdGen.next))
              )
              requestUserOwner.setApplication(Application.iosApp)
              requestUserOwner.setUser(offer.userRef)
              requestUserOwner.setTrace(trace)

              val result =
                offerLoader.findRawOffer(category, offer.id, fromVosOnly, forceTeleponyInfo)(requestUserOwner).await

              result.getTagsList.asScala.toVector should contain theSameElementsAs offer.getTagsList.asScala.toVector

              verify(searcherClient).getOffer(?, ?)(?)
            }
          }
        }
      }
    }
  }
}
