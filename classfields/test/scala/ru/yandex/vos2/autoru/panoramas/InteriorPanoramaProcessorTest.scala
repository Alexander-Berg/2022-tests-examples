package ru.yandex.vos2.autoru.panoramas

import org.scalatest.wordspec.AnyWordSpec
import ru.auto.panoramas.PanoramasModel
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.util.InteriorPanoramaUtils._
import ru.yandex.vos2.util.Dates._

import java.time.Instant
import scala.jdk.CollectionConverters._

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class InteriorPanoramaProcessorTest extends AnyWordSpec with MockitoSupport {
  val featurePoiCount: Feature[Boolean] = Feature[Boolean]("notif", _ => true)

  implicit val t: Traced = Traced.empty

  val interiorPanoramasProcessor = new InteriorPanoramasProcessor(featurePoiCount)

  "InteriorPanoramasProcessor" should {
    "update" when {
      "panorama completed" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createInteriorPanorama.setPublished(false)
        panorama.getPanoramaBuilder.setPublished(false)
        panorama.getPanoramaBuilder.setStatus(PanoramasModel.Status.PROCESSING)
        offerBuilder.getOfferAutoruBuilder.clearInteriorPanorama().addInteriorPanorama(panorama)
        val secondPanorama = TestUtils.createInteriorPanorama.build()
        offerBuilder.getOfferAutoruBuilder.addInteriorPanorama(secondPanorama)

        val apiPanorama =
          panorama.build().getPanorama.toBuilder.setPublished(false).setStatus(PanoramasModel.Status.COMPLETED).build()

        val offer: Offer = offerBuilder.build()
        val time = Instant.now().plusSeconds(100)
        val res = interiorPanoramasProcessor.process(offer, apiPanorama, time)

        assert(res.nonEmpty)
        val changedOffer = res.getUpdate.get
        val panoramas = changedOffer.getOfferAutoru.getInteriorPanoramaList.asScala
        assert(changedOffer.getOfferAutoru.getInteriorPanoramaCount == 2)
        val first = panoramas.find(p => p.getPanorama.getId == apiPanorama.getId)
        assert(first.nonEmpty)
        assert(first.get.getPublished)

        val second = panoramas.find(p => p.getPanorama.getId == secondPanorama.getPanorama.getId)
        assert(second.nonEmpty)
        assert(second.get.getPublished)
        assert(changedOffer.getTagList.asScala.contains(InteriorPanoramasTag))
      }

      "panorama failed" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createInteriorPanorama.build()
        offerBuilder.getOfferAutoruBuilder.clearInteriorPanorama().addInteriorPanorama(panorama)
        val apiPanorama = panorama.getPanorama.toBuilder.setStatus(PanoramasModel.Status.FAILED).build()
        val offer: Offer = offerBuilder.build()
        val time = Instant.now().plusSeconds(100)
        offerBuilder.addTag(InteriorPanoramasTag)
        val res = interiorPanoramasProcessor.process(offer, apiPanorama, time)
        assert(res.nonEmpty)
        val changedOffer = res.getUpdate.get
        val panoramas = changedOffer.getOfferAutoru.getInteriorPanoramaList.asScala
        assert(changedOffer.getOfferAutoru.getInteriorPanoramaCount == 1)
        val first = panoramas.find(p => p.getPanorama.getId == apiPanorama.getId)
        assert(first.nonEmpty)
        assert(!first.get.getPublished)
        assert(!changedOffer.getTagList.asScala.contains(InteriorPanoramasTag))
      }

      "poi count increased" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createInteriorPanorama.setUpdateAt(Instant.now()).build()
        offerBuilder.getOfferAutoruBuilder.addInteriorPanorama(panorama)
        offerBuilder.addTag(InteriorPanoramasTag)
        val offer: Offer = offerBuilder.build()
        val time = Instant.now().plusSeconds(100)
        val res = interiorPanoramasProcessor.processPoi(offer, panorama.getPanorama.getId, 2, time)

        assert(res.nonEmpty)
        val changedOffer = res.getUpdate.get
        val panoramas = changedOffer.getOfferAutoru.getInteriorPanoramaList.asScala
        assert(changedOffer.getOfferAutoru.getInteriorPanoramaCount == 1)
        val first = panoramas.find(p => p.getPanorama.getId == panorama.getPanorama.getId)
        assert(first.nonEmpty)
        assert(first.get.getPublished)
        assert(first.get.getPoiCount == 2)
        assert(changedOffer.getTagList.asScala.contains(HasInteriorPoiTag))
      }

      "poi count decreased" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createInteriorPanorama.setUpdateAt(Instant.now()).setPoiCount(2).build()
        offerBuilder.getOfferAutoruBuilder.addInteriorPanorama(panorama)
        offerBuilder.addTag(InteriorPanoramasTag)
        offerBuilder.addTag(HasInteriorPoiTag)
        val offer: Offer = offerBuilder.build()
        val time = Instant.now().plusSeconds(100)
        val res = interiorPanoramasProcessor.processPoi(offer, panorama.getPanorama.getId, 0, time)
        assert(res.nonEmpty)
        val changedOffer = res.getUpdate.get
        val panoramas = changedOffer.getOfferAutoru.getInteriorPanoramaList.asScala
        assert(changedOffer.getOfferAutoru.getInteriorPanoramaCount == 1)
        val first = panoramas.find(p => p.getPanorama.getId == panorama.getPanorama.getId)
        assert(first.nonEmpty)
        assert(first.get.getPublished)
        assert(first.get.getPoiCount == 0)
        assert(!changedOffer.getTagList.asScala.contains(HasInteriorPoiTag))
      }
    }

    "skip" when {
      "panorama outdated" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createInteriorPanorama.setUpdateAt(Instant.now()).build()
        offerBuilder.getOfferAutoruBuilder.addInteriorPanorama(panorama)
        val apiPanorama = panorama.getPanorama //.toBuilder.set
        offerBuilder.addTag(InteriorPanoramasTag)
        val offer: Offer = offerBuilder.build()
        val time = Instant.now().minusSeconds(100)
        val res = interiorPanoramasProcessor.process(offer, apiPanorama, time)
        assert(res.isEmpty)
      }

      "poi event outdated" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createInteriorPanorama.setUpdateAt(Instant.now()).build()
        offerBuilder.getOfferAutoruBuilder.addInteriorPanorama(panorama)
        offerBuilder.addTag(InteriorPanoramasTag)
        val offer: Offer = offerBuilder.build()
        val time = Instant.now().minusSeconds(100)
        val res = interiorPanoramasProcessor.processPoi(offer, panorama.getPanorama.getId, 2, time)
        assert(res.isEmpty)
      }
    }
  }

}
