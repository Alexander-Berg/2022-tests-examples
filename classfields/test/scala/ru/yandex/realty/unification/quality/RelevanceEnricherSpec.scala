package ru.yandex.realty.unification.quality

import org.apache.commons.io.IOUtils
import org.joda.time.{Duration, Instant}
import org.junit.runner.RunWith
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.auth.Roles
import ru.yandex.realty.bunker.BunkerResources
import ru.yandex.realty.model.offer.{Offer, OfferType}
import ru.yandex.realty.storage.{PartnerRulesStorage, WeightStorage}
import ru.yandex.realty.telepony.PhoneRelevanceHolder
import ru.yandex.realty.tracing.Traced

/**
  * Specs on HTTP [[RelevanceEnricher]].
  *
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class RelevanceEnricherSpec extends AsyncSpecBase with Matchers with PropertyChecks {

  implicit val trace: Traced = Traced.empty

  private val mockPartnerRulesProvider = Mockito.mock(classOf[Provider[PartnerRulesStorage]])
  private val mockPartnerRulesStorage: PartnerRulesStorage = Mockito.mock(classOf[PartnerRulesStorage])
  Mockito.when(mockPartnerRulesProvider.get()).thenReturn(mockPartnerRulesStorage)
  Mockito.when(mockPartnerRulesStorage.findRule(ArgumentMatchers.any(classOf[Offer]))).thenReturn(null)

  private val mockRelevanceWeightProvider = Mockito.mock(classOf[Provider[WeightStorage]])
  private val relevanceWeights =
    s"""expo_days_1=1
       |expo_bonus_1=70000
       |expo_days_2=2
       |expo_bonus_2=50000
       |expo_days_3=4
       |expo_bonus_3=20000
       |expo_days_4=14
       |expo_bonus_4=5000
       |expo_no_bonus_days=30
       |sell_expo_days_1=1
       |sell_expo_bonus_1=50000
       |sell_expo_days_2=2
       |sell_expo_bonus_2=50000
       |sell_expo_days_3=30
       |sell_expo_bonus_3=30000
       |sell_expo_days_4=60
       |sell_expo_bonus_4=10000
       |sell_expo_no_bonus_days=270
     """.stripMargin
  private val relevanceWeightStorage = new WeightStorage(IOUtils.toInputStream(relevanceWeights, "UTF-8"))
  Mockito.when(mockRelevanceWeightProvider.get()).thenReturn(relevanceWeightStorage)

  private val mockTelephoneRelevanceProvider = Mockito.mock(classOf[Provider[PhoneRelevanceHolder]])
  private val phoneRelevanceHolder = PhoneRelevanceHolder(Map.empty)
  Mockito.when(mockTelephoneRelevanceProvider.get()).thenReturn(phoneRelevanceHolder)

  private val uidWithPenalty = "123"
  private val penaltyValue = 5
  private val bunkerResources = BunkerResources(
    Map.empty[String, Map[String, String]],
    Set.empty[Int],
    Set.empty[String],
    Set.empty[String],
    Set.empty[String],
    Set(uidWithPenalty),
    Map.empty[String, Set[Roles.Value]],
    penaltyValue
  )

  private val mockBunkerResourcesProvider = Mockito.mock(classOf[Provider[BunkerResources]])
  Mockito.when(mockBunkerResourcesProvider.get()).thenReturn(bunkerResources)

  private val relevanceEnricher = new RelevanceEnricher(
    mockPartnerRulesProvider,
    mockRelevanceWeightProvider,
    mockTelephoneRelevanceProvider,
    mockBunkerResourcesProvider
  )

  "RelevanceEnricher" should {
    "add zero bonus for exposition after 270 days" in {
      val offer = new Offer()
      offer.setId(1234567L)
      offer.setOfferType(OfferType.SELL)
      offer.setFullness(0)
      offer.setCreateTime(Instant.now.minus(Duration.standardDays(271)))
      relevanceEnricher.enrich(offer).futureValue
      offer.getRelevance should be(0.0f)
    }

    "add nonzero bonus for exposition after 60 before 270 days" in {
      val offer = new Offer()
      offer.setId(1234567L)
      offer.setOfferType(OfferType.SELL)
      offer.setFullness(0)
      offer.setCreateTime(Instant.now.minus(Duration.standardDays(90)))

      val a = relevanceEnricher.enrich(offer, needLogging = true)

      val decayTime: Float = 270.0f - 60.0f
      val expectedRelevance: Float = 10000.0f * (270.0f - 90.0f) / decayTime
      (offer.getRelevance - expectedRelevance).abs should be < 0.000001f
    }

    "apply penalty to partners from bunker" in {
      val offer = new Offer()
      offer.setId(1234567L)
      offer.setOfferType(OfferType.SELL)
      offer.setFullness(0)
      offer.setCreateTime(Instant.now().minus(Duration.standardDays(90)))

      relevanceEnricher.enrich(offer).futureValue
      val withoutPenalty = offer.getRelevance
      offer.setUid(uidWithPenalty)
      relevanceEnricher.enrich(offer).futureValue

      (offer.getRelevance * penaltyValue - withoutPenalty).abs should be < 0.000001f
    }
  }
}
