package ru.auto.api.managers.enrich.enrichers

import org.mockito.Mockito.verify
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.model.ModelUtils._
import ru.auto.api.BaseSpec
import ru.auto.api.CounterModel.AggregatedCounter
import ru.auto.api.auth.Application
import ru.auto.api.managers.counters.CountersManager
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.model.ModelGenerators.{DealerOfferGen, PrivateOfferGen}
import ru.auto.api.model.{RequestParams, UserRef}
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

class CountersEnricherSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  private val countersManager = mock[CountersManager]
  private val enricher = new CountersEnricher(countersManager)

  implicit val trace: Traced = Traced.empty

  val userRef: UserRef = UserRef.parse("user:123")
  val dealerRef: UserRef = UserRef.parse("dealer:123")

  val userRequest: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(trace)
    req.setRequestParams(RequestParams.empty)
    req.setUser(userRef)
    req
  }

  val dealerRequest: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(trace)
    req.setRequestParams(RequestParams.empty)
    req.setDealer(dealerRef.asDealer)
    req
  }

  "CountersEnricher.useFreshDateForOffer" should {
    val enrichOptions = EnrichOptions(counters = true, countersUseFreshDate = true)
    val counterResponse = AggregatedCounter.getDefaultInstance

    "return `true` for dealer" in {
      forAll(DealerOfferGen) { (offer) =>
        when(countersManager.getCounters(?, ?, ?, ?)(?)).thenReturnF(Map(offer.id -> counterResponse))
        enricher.getFunction(Seq(offer), enrichOptions)(dealerRequest)
        verify(countersManager).getCounters(
          Seq(offer),
          includePhoneShows = false,
          useFreshDate = true,
          forceUseRealCreationDate = false
        )(dealerRequest)
      }
    }

    "return `false` for private user" in {
      forAll(PrivateOfferGen) { (offer) =>
        when(countersManager.getCounters(?, ?, ?, ?)(?)).thenReturnF(Map(offer.id -> counterResponse))
        enricher.getFunction(Seq(offer), enrichOptions)(userRequest)
        verify(countersManager).getCounters(
          Seq(offer),
          includePhoneShows = false,
          useFreshDate = false,
          forceUseRealCreationDate = false
        )(userRequest)
      }
    }
  }
}
