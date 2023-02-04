package ru.yandex.realty.unification.unifier.processor.unifiers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.{FeatureStub, SimpleFeatures}
import ru.yandex.realty.model.history.OfferHistory
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.proto.unified.offer.images.ImageSemanticType.{IST_FLOOR_PLAN, IST_PLAN}
import ru.yandex.realty.proto.unified.offer.images.MdsImageId.KnownNamespace.REALTY
import ru.yandex.realty.proto.unified.offer.images.{RealtyPhotoInfo, RealtyPhotoMeta, UnifiedImages}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.realty.unification.util.RelevanceTestComponents
import ru.yandex.realty.util.Mappings.MapAny

import scala.collection.JavaConverters._

/**
  * User: azakharov
  * Date: 23.01.17
  */
@RunWith(classOf[JUnitRunner])
class RawImageUnifierTest extends AsyncSpecBase with Matchers with RelevanceTestComponents {
  import RawImageUnifierTest._

  implicit val trace: Traced = Traced.empty

  "RawImageUnifier" should {
    "contain extended images and preserve order of images" in {
      val raw = new RawOfferImpl
      val untaggedImage1 = createImage("untagged1")
      val untaggedImage2 = createImage("untagged2")
      val imagePlan = createImage(
        "image-plan",
        tag = Some("plan"),
        setMeta = Some(_.getCbirPredictionsBuilder.getDocsWithPlansBuilder.setScore(0.999))
      )
      val imageFloorPlan = createImage(
        "image-floor-plan",
        tag = Some("floor-plan"),
        setMeta = Some(_.getCbirPredictionsBuilder.getDocsWithPlansBuilder.setScore(0.999))
      )

      raw.setModernImages(
        UnifiedImages
          .newBuilder()
          .addAllImage(
            Seq(
              untaggedImage1,
              imagePlan,
              untaggedImage2,
              imageFloorPlan
            ).asJava
          )
          .build()
      )

      val u = new RawImageUnifier(relevanceModelProvider)
      val offer = new Offer
      u.unify(new OfferWrapper(raw, offer, OfferHistory.justArrived())).futureValue

      offer.getShowableImages.asScala.map(_.getMdsId.getName) shouldBe
        Seq("untagged1", "image-plan", "untagged2", "image-floor-plan")

      offer.getShowableImages.asScala.filter(_.getSemanticType == IST_PLAN).map(_.getMdsId.getName) shouldBe
        Seq("image-plan")

      offer.getShowableImages.asScala.filter(_.getSemanticType == IST_FLOOR_PLAN).map(_.getMdsId.getName) shouldBe
        Seq("image-floor-plan")
    }
  }

}

object RawImageUnifierTest {
  private def createImage(
    name: String,
    tag: Option[String] = None,
    setMeta: Option[RealtyPhotoMeta.Builder => Any] = None
  ): RealtyPhotoInfo = {
    RealtyPhotoInfo
      .newBuilder()
      .applySideEffect(
        _.getMdsIdBuilder
          .setKnownNamespace(REALTY)
          .setGroup(1)
          .setName(name)
      )
      .applySideEffects[String](tag, _ setTag _)
      .applySideEffects[RealtyPhotoMeta.Builder => Any](
        setMeta,
        (b, setter) => setter(b.getMetaBuilder)
      )
      .build()
  }
}
