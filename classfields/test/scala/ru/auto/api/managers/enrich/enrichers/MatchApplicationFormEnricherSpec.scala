package ru.auto.api.managers.enrich.enrichers

import org.mockito.Mockito.{never, reset, verify}
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Location
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.managers.matchapplications.MatchApplicationsManager
import ru.auto.api.managers.searcher.SearchRequestContext
import ru.auto.api.model.ModelGenerators.DealerCarsUsedOfferGen
import ru.auto.api.model.ModelUtils.RichOfferOrBuilder
import ru.auto.api.model.{RequestParams, SessionID, UserRef}
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class MatchApplicationFormEnricherSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with OptionValues {

  private val matchApplicationsManager = mock[MatchApplicationsManager]
  private val enricher = new MatchApplicationFormEnricher(matchApplicationsManager)

  implicit private val request: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionID("test_session"))))
    r.setUser(UserRef.anon("42"))
    r.setApplication(Application.desktop)
    r
  }

  "MatchApplicationForm enricher" should {

    "not replace" in {
      val usedOffer = DealerCarsUsedOfferGen.next
      val offer2 = {
        val b = usedOffer.toBuilder
        b.setDescription("text")
        b.build()
      }

      reset(matchApplicationsManager)
      val searchContexts = List(SearchRequestContext.Type.Listing, SearchRequestContext.Type.GroupCard)
      when(matchApplicationsManager.offerSearchContexts(?)(?)).thenReturn(Some(searchContexts))
      val forEnrich = usedOffer.updated { offer =>
        val locationBuilder = Location.newBuilder().mergeFrom(offer.getSeller.getLocation)
        offer.getSellerBuilder.setLocation(locationBuilder.setGeobaseId(1L))
      }

      val enrich = enricher.getFunction(Seq(forEnrich), EnrichOptions(matchApplicationForm = true)).futureValue
      val enriched = enrich(offer2)
      enriched.getDescription shouldBe "text"
    }

    "enrich offers with search context for used offer" in {
      val usedOffer = DealerCarsUsedOfferGen.next
      reset(matchApplicationsManager)
      val searchContexts = List(SearchRequestContext.Type.Listing, SearchRequestContext.Type.GroupCard)
      when(matchApplicationsManager.offerSearchContexts(?)(?)).thenReturn(Some(searchContexts))
      val forEnrich = usedOffer.updated { offer =>
        val locationBuilder = Location.newBuilder().mergeFrom(offer.getSeller.getLocation)
        offer.getSellerBuilder.setLocation(locationBuilder.setGeobaseId(1L))
      }

      val enrich = enricher.getFunction(Seq(forEnrich), EnrichOptions(matchApplicationForm = true)).futureValue
      val enriched = enrich(forEnrich)
      val contexts = enriched.getAdditionalInfo.getMatchApplicationForm.getContextsList
      contexts.asScala should contain theSameElementsAs searchContexts.map(_.toString)
      verify(matchApplicationsManager).offerSearchContexts(eq(forEnrich))(eq(request))
    }

    "don't enrich offers with wrong enrich options" in {
      val usedOffer = DealerCarsUsedOfferGen.next
      reset(matchApplicationsManager)
      val searchContexts = List(SearchRequestContext.Type.Listing, SearchRequestContext.Type.GroupCard)
      when(matchApplicationsManager.offerSearchContexts(?)(?)).thenReturn(Some(searchContexts))
      val enrich = enricher.getFunction(Seq(usedOffer), EnrichOptions()).futureValue
      val enriched = enrich(usedOffer)
      val contexts = enriched.getAdditionalInfo.getMatchApplicationForm.getContextsList
      contexts.asScala shouldBe Nil
      verify(matchApplicationsManager, never()).offerSearchContexts(?)(?)
    }

    "don't enrich offers without contexts" in {
      val usedOffer = DealerCarsUsedOfferGen.next
      reset(matchApplicationsManager)
      when(matchApplicationsManager.offerSearchContexts(?)(?)).thenReturn(None)
      val forEnrich = usedOffer.updated { offer =>
        val locationBuilder = Location.newBuilder().mergeFrom(offer.getSeller.getLocation)
        offer.getSellerBuilder.setLocation(locationBuilder.setGeobaseId(1L))
      }
      val enrich = enricher.getFunction(Seq(forEnrich), EnrichOptions(matchApplicationForm = true)).futureValue
      val enriched = enrich(forEnrich)
      val contexts = enriched.getAdditionalInfo.getMatchApplicationForm.getContextsList
      contexts.asScala shouldBe Nil
      verify(matchApplicationsManager).offerSearchContexts(eq(forEnrich))(eq(request))
    }
  }
}
