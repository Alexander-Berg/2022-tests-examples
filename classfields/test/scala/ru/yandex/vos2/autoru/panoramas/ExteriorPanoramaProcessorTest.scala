package ru.yandex.vos2.autoru.panoramas

import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.auto.panoramas.PanoramasModel
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.util.ExternalPanoramaUtils._
import ru.yandex.vos2.commonfeatures.VosFeatureTypes.{VosFeature, WithGeneration}
import ru.yandex.vos2.util.Dates._
import ru.yandex.vos2.util.ExternalPanoramaUtils.RichVos._

import java.time.Instant
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class ExteriorPanoramaProcessorTest extends AnyWordSpec with MockitoSupport {

  val featureNotification: Feature[Boolean] = Feature[Boolean]("notif", _ => true)
  val featurePoiCount: VosFeature = mock[VosFeature]
  when(featurePoiCount.value).thenReturn(WithGeneration(value = true))
  implicit val t: Traced = Traced.empty

  val exteriorPanoramasProcessor = new ExteriorPanoramasProcessor(featureNotification, featurePoiCount)

  "ExteriorPanoramasProcessor" should {
    "update" when {
      "panorama completed" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createExternalPanorama.setPublished(false).setIsNext(true).build()
        offerBuilder.getOfferAutoruBuilder.clearExternalPanorama().addExternalPanorama(panorama)
        val secondPanorama = TestUtils.createExternalPanorama.build()
        offerBuilder.getOfferAutoruBuilder.addExternalPanorama(secondPanorama)
        val apiPanorama = panorama.asApi

        val offer: Offer = offerBuilder.build()
        val time = Instant.now().plusSeconds(100)
        val res = exteriorPanoramasProcessor.process(offer, apiPanorama, time)

        assert(res.nonEmpty)
        val changedOffer = res.getUpdate.get
        val panoramas = changedOffer.getOfferAutoru.getExternalPanoramaList.asScala
        assert(changedOffer.getOfferAutoru.getExternalPanoramaCount == 2)
        val first = panoramas.find(p => p.getId == apiPanorama.getId)
        assert(first.nonEmpty)
        assert(first.get.getPublished)
        val second = panoramas.find(p => p.getId == secondPanorama.getId)
        assert(second.nonEmpty)
        assert(!second.get.getPublished)
        assert(changedOffer.getTagList.asScala.contains(ExternalPanoramasTag))
      }

      "panorama failed" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createExternalPanorama.setPublished(false).setIsNext(true).build()
        offerBuilder.getOfferAutoruBuilder.clearExternalPanorama().addExternalPanorama(panorama)
        val apiPanorama = panorama.asApi.toBuilder.setStatus(PanoramasModel.Status.FAILED).build()
        val offer: Offer = offerBuilder.build()
        val time = Instant.now().plusSeconds(100)
        val res = exteriorPanoramasProcessor.process(offer, apiPanorama, time)

        assert(res.nonEmpty)
        val changedOffer = res.getUpdate.get
        val panoramas = changedOffer.getOfferAutoru.getExternalPanoramaList.asScala
        assert(changedOffer.getOfferAutoru.getExternalPanoramaCount == 1)
        val first = panoramas.find(p => p.getId == apiPanorama.getId)
        assert(first.nonEmpty)
        assert(!first.get.getPublished)
        assert(!changedOffer.getTagList.asScala.contains(ExternalPanoramasTag))
      }

      "poi count increased" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createExternalPanorama.setUpdateAt(Instant.now()).build()
        offerBuilder.getOfferAutoruBuilder.addExternalPanorama(panorama)
        offerBuilder.addTag(ExternalPanoramasTag)
        val offer: Offer = offerBuilder.build()
        val time = Instant.now().plusSeconds(100)
        val res = exteriorPanoramasProcessor.processPoi(offer, panorama.getId, 2, time)

        assert(res.nonEmpty)
        val changedOffer = res.getUpdate.get
        val panoramas = changedOffer.getOfferAutoru.getExternalPanoramaList.asScala
        assert(changedOffer.getOfferAutoru.getExternalPanoramaCount == 1)
        val first = panoramas.find(p => p.getId == panorama.getId)
        assert(first.nonEmpty)
        assert(first.get.getPublished)
        assert(first.get.getPoiCount == 2)
        assert(changedOffer.getTagList.asScala.contains(HasExteriorPoiTag))
      }

      "poi count decreased" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createExternalPanorama.setUpdateAt(Instant.now()).setPoiCount(2).build()
        offerBuilder.getOfferAutoruBuilder.addExternalPanorama(panorama)
        offerBuilder.addTag(ExternalPanoramasTag)
        val offer: Offer = offerBuilder.build()
        val time = Instant.now().plusSeconds(100)
        val res = exteriorPanoramasProcessor.processPoi(offer, panorama.getId, 0, time)
        assert(res.nonEmpty)
        val changedOffer = res.getUpdate.get
        val panoramas = changedOffer.getOfferAutoru.getExternalPanoramaList.asScala
        assert(changedOffer.getOfferAutoru.getExternalPanoramaCount == 1)
        val first = panoramas.find(p => p.getId == panorama.getId)
        assert(first.nonEmpty)
        assert(first.get.getPublished)
        assert(first.get.getPoiCount == 0)
        assert(!changedOffer.getTagList.asScala.contains(HasExteriorPoiTag))
      }
    }

    "skip" when {
      "panorama outdated" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createExternalPanorama.setUpdateAt(Instant.now()).build()
        offerBuilder.getOfferAutoruBuilder.addExternalPanorama(panorama)
        val apiPanorama = panorama.asApi //.toBuilder.set
        offerBuilder.addTag(ExternalPanoramasTag)
        val offer: Offer = offerBuilder.build()
        val time = Instant.now().minusSeconds(100)
        val res = exteriorPanoramasProcessor.process(offer, apiPanorama, time)
        assert(res.isEmpty)
      }

      "poi event outdated" in {
        val offerBuilder = TestUtils.createOffer(dealer = true)
        val panorama = TestUtils.createExternalPanorama.setUpdateAt(Instant.now()).build()
        offerBuilder.getOfferAutoruBuilder.addExternalPanorama(panorama)
        offerBuilder.addTag(ExternalPanoramasTag)
        val offer: Offer = offerBuilder.build()
        val time = Instant.now().minusSeconds(100)
        val res = exteriorPanoramasProcessor.processPoi(offer, panorama.getId, 2, time)
        assert(res.isEmpty)
      }
    }
  }
}
