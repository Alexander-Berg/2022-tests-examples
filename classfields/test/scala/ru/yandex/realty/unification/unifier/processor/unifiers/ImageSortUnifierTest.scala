package ru.yandex.realty.unification.unifier.processor.unifiers

import com.google.protobuf.FloatValue
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.history.OfferHistory
import ru.yandex.realty.model.offer.{ApartmentInfo, FlatType, Offer}
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.proto.unified.offer.images.ImageSemanticType.IST_PLAN
import ru.yandex.realty.proto.unified.offer.images.{ImageSemanticType, MdsImageId, RealtyPhotoInfo, UnifiedImages}
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.realty.unification.util.RelevanceTestComponents

import scala.collection.JavaConverters._
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.proto.unified.offer.images.MdsImageId.KnownNamespace.REALTY
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class ImageSortUnifierTest extends AsyncSpecBase with Matchers with RelevanceTestComponents {

  implicit val trace: Traced = Traced.empty

  private val imageSortUnifier = new ImageSortUnifier()

  private def mkImage(group: Int, name: String, preview: String, relevance: Float, isPlan: Boolean = false) = {
    val b = RealtyPhotoInfo.newBuilder
    b.setMdsId(MdsImageId.newBuilder.setKnownNamespace(REALTY).setGroup(group).setName(name))
    b.setPreview(preview)
    if (isPlan) b.setSemanticType(ImageSemanticType.IST_PLAN)
    if (relevance != 0) b.setRelevance(FloatValue.of(relevance))
    b
  }

  private def getOfferWrapper(canChangeOrder: Boolean, flatType: Option[FlatType]): OfferWrapper = {
    val offer = new Offer
    offer.setPhotos(
      UnifiedImages.newBuilder
        .addImage(mkImage(1, "a", "a", 0.1f))
        .addImage(mkImage(2, "b", "b", 0.2f, true))
        .build,
      100
    )
    offer.setImageOrderChangeAllowed(canChangeOrder)
    val ai = new ApartmentInfo()
    offer.setApartmentInfo(ai)
    flatType.foreach(ai.setFlatType)
    new OfferWrapper(null, offer, null)
  }

  "ImageSortUnifier" should {
    "sort images for secondary in right order" in {
      var ow = getOfferWrapper(true, None)
      imageSortUnifier.unify(ow).futureValue
      var images = ow.getOffer.getPhotosContainer.getImageList.asScala
      images.size should be(2)
      images.map(_.getMdsId.getName) shouldBe (Seq("b", "a"))

      ow = getOfferWrapper(false, None)
      imageSortUnifier.unify(ow).futureValue
      images = ow.getOffer.getPhotosContainer.getImageList.asScala
      images.size should be(2)
      images.map(_.getMdsId.getName) shouldBe (Seq("a", "b"))
    }

    "sort images for new flat in right order" in {
      var ow = getOfferWrapper(true, Some(FlatType.NEW_FLAT))
      imageSortUnifier.unify(ow).futureValue
      var images = ow.getOffer.getPhotosContainer.getImageList.asScala
      images.size should be(2)
      images.map(_.getMdsId.getName) shouldBe (Seq("b", "a"))

      ow = getOfferWrapper(false, Some(FlatType.NEW_SECONDARY))
      imageSortUnifier.unify(ow).futureValue
      images = ow.getOffer.getPhotosContainer.getImageList.asScala
      images.size should be(2)
      images.map(_.getMdsId.getName) shouldBe (Seq("b", "a"))
    }
  }

}
