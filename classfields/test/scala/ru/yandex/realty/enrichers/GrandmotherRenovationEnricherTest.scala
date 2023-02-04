package ru.yandex.realty.enrichers

import java.util.concurrent.atomic.AtomicInteger
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.history.OfferHistory
import ru.yandex.realty.model.offer.{ApartmentInfo, Offer, Renovation}
import ru.yandex.realty.model.raw.RawOffer
import ru.yandex.realty.proto.unified.offer.images.MdsImageId.KnownNamespace.REALTY
import ru.yandex.realty.proto.unified.offer.images.{MdsImageId, RealtyPhotoMeta, UnifiedImages}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.realty.util.Mappings.MapAny

@RunWith(classOf[JUnitRunner])
class GrandmotherRenovationEnricherTest extends AsyncSpecBase with MockFactory with Matchers with OneInstancePerTest {

  import GrandmotherRenovationEnricherTest._

  implicit val trace: Traced = Traced.empty

  def offerWrapper(metas: Meta*): OfferWrapper = {
    val offer = new Offer()
    offer.setApartmentInfo(new ApartmentInfo)
    offer.setPhotos(
      UnifiedImages
        .newBuilder()
        .applySideEffects[Meta](
          metas,
          (b, m) =>
            b.addImageBuilder()
              .setMdsId(
                MdsImageId.newBuilder().setKnownNamespace(REALTY).setGroup(IdSource.incrementAndGet()).setName("a")
              )
              .setMeta(m.toMeta)
        )
        .build(),
      100
    )
    new OfferWrapper(mock[RawOffer], offer, OfferHistory.justArrived())
  }

  s"An offer in".which {
    "all images are interior and average score > -0.14" should {
      "be non 'grandmother'" in {
        val o = offerWrapper(
          Meta(repairQualityScore = -0.10f),
          Meta(repairQualityScore = -0.14f)
        )
        new GrandmotherRenovationEnricher().process(o).futureValue
        o.getOffer.getApartmentInfo.getEnrichedRenovation should contain theSameElementsAs Seq(
          Renovation.NON_GRANDMOTHER
        )
      }
    }
    "all images have average score < -0.14 and 'grandmother' images ratio > 0.5" should {
      "be 'grandmother'" in {
        val o = offerWrapper(
          Meta(repairQualityScore = -0.25f),
          Meta(repairQualityScore = -0.21f),
          Meta(repairQualityScore = -0.08f)
        )
        new GrandmotherRenovationEnricher().process(o).futureValue
        o.getOffer.getApartmentInfo.getEnrichedRenovation should contain theSameElementsAs Seq(Renovation.GRANDMOTHER)
      }
    }
    "all images have average score < -0.14 and 'grandmother' images ratio < 0.5" should {
      "be non 'grandmother'" in {
        val o = offerWrapper(
          Meta(repairQualityScore = -0.04f),
          Meta(repairQualityScore = -0.01f),
          Meta(repairQualityScore = -0.80f) // for avg < 0.14
        )
        new GrandmotherRenovationEnricher().process(o).futureValue
        o.getOffer.getApartmentInfo.getEnrichedRenovation should contain theSameElementsAs Seq(
          Renovation.NON_GRANDMOTHER
        )
      }
    }
    "images meta not have required field" should {
      "not have 'grandmother' renovation feature" in {
        val o = offerWrapper()
        o.getOffer.setPhotos(
          UnifiedImages
            .newBuilder()
            .applySideEffect(
              _.addImageBuilder()
                .setMdsId(
                  MdsImageId.newBuilder().setKnownNamespace(REALTY).setGroup(IdSource.incrementAndGet()).setName("a")
                )
            )
            .build(),
          100
        )
        new GrandmotherRenovationEnricher().process(o).futureValue
        o.getOffer.getApartmentInfo.getEnrichedRenovation shouldBe empty
      }
    }
    "all images are not interior" should {
      "be non 'grandmother'" in {
        val o = offerWrapper(
          // Test will be passed only if all followed below meta will be ignored
          Meta(repairQualityScore = -5f, outside = 201), // check interior >= outside
          Meta(repairQualityScore = -5f, interior = 99), // check interior > 100
          Meta(repairQualityScore = -5f, entranceStairs = 101), // check entranceStairs <= 100
          Meta(repairQualityScore = -5f, docsWithPlans = 200), // check docsWithPlans < 200
          Meta(repairQualityScore = -5f, maps = 200), // check maps < 200
          Meta(repairQualityScore = -5f, docsWoPlans = 1) // check docsWoPlans < 1
        )
        new GrandmotherRenovationEnricher().process(o).futureValue
        o.getOffer.getApartmentInfo.getEnrichedRenovation should contain theSameElementsAs Seq(
          Renovation.NON_GRANDMOTHER
        )
      }
    }
  }

  private case class Meta(
    interior: Int = 200,
    outside: Int = 0,
    entranceStairs: Int = 0,
    docsWithPlans: Int = 0,
    maps: Int = 0,
    docsWoPlans: Int = 0,
    repairQualityScore: Float = 0
  ) {

    def toMeta: RealtyPhotoMeta = {
      val b = RealtyPhotoMeta.newBuilder()
      b.getNnPredictionsBuilder
        .setRealtyInterior(interior)
        .setRealtyOutside(outside)
        .setRealtyEntranceStairs(entranceStairs)
        .setRealtyDocsWithPlans(docsWithPlans)
        .setRealtyMaps(maps)
        .setRealtyDocsWoPlans(docsWoPlans)
      b.getThinRepairQualityV1Builder.setScore(repairQualityScore)
      b.build()
    }
  }

}

object GrandmotherRenovationEnricherTest {
  private val IdSource = new AtomicInteger()
}
