package ru.yandex.realty.unification.unifier.processor.unifiers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.model.history.{OfferHistory, PriceHistory, RgidHistory}
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.model.raw.{RawOffer, RawOfferImpl}
import ru.yandex.realty.proto.unified.offer.state.IndexationWarning
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.realty.proto.unified.offer.images.{RealtyPhotoInfo, UnifiedImages}
import org.joda.time.Instant
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class WarnByBadImagePortUnifierSpec extends AsyncSpecBase {
  private val warnByBadImagePortUnifier = new WarnByBadImagePortUnifier()

  implicit val trace: Traced = Traced.empty

  "WarnByBadImagePortUnifier in unify" should {
    val history = buildOfferHistory()

    "add BAD_IMAGE_PORT warning if offer is fromFeed and offer has images with bad port" in {
      val rawOffer = buildRawOffer("http://fakeurl:500/photo.jpg")
      val feedOffer = buildOffer(false)
      val ow = new OfferWrapper(rawOffer, feedOffer, history)
      warnByBadImagePortUnifier.unify(ow).futureValue
      feedOffer.getOfferState.getWarnings.asScala
        .exists(_.getWarning == IndexationWarning.Code.BAD_IMAGE_PORT) shouldBe true
    }

    "add BAD_IMAGE_PORT warning if offer is fromFeed and offer has images with invalid url" in {
      val rawOffer = buildRawOffer("invalid-url")
      val feedOffer = buildOffer(false)
      val ow = new OfferWrapper(rawOffer, feedOffer, history)
      warnByBadImagePortUnifier.unify(ow).futureValue
      feedOffer.getOfferState.getWarnings.asScala
        .exists(_.getWarning == IndexationWarning.Code.BAD_IMAGE_PORT) shouldBe true
    }

    "do not add BAD_IMAGE_PORT warning if offer is from feed and offer has not images" in {
      val rawOffer = buildRawOffer("")
      val feedOffer = buildOffer(false)
      val ow = new OfferWrapper(rawOffer, feedOffer, history)
      warnByBadImagePortUnifier.unify(ow).futureValue
      feedOffer.getOfferState.getWarnings.asScala
        .exists(_.getWarning == IndexationWarning.Code.BAD_IMAGE_PORT) shouldBe false
    }

    "do not add BAD_IMAGE_PORT warning if offer is from feed and offer has images with valid http port" in {
      val rawOffer = buildRawOffer("http://localhost")
      val feedOffer = buildOffer(false)
      val ow = new OfferWrapper(rawOffer, feedOffer, history)
      warnByBadImagePortUnifier.unify(ow).futureValue
      feedOffer.getOfferState.getWarnings.asScala
        .exists(_.getWarning == IndexationWarning.Code.BAD_IMAGE_PORT) shouldBe false
    }

    "do not add BAD_IMAGE_PORT warning if offer is from feed and offer has images with valid https port" in {
      val rawOffer = buildRawOffer("https://localhost")
      val feedOffer = buildOffer(false)
      val ow = new OfferWrapper(rawOffer, feedOffer, history)
      warnByBadImagePortUnifier.unify(ow).futureValue
      feedOffer.getOfferState.getWarnings.asScala
        .exists(_.getWarning == IndexationWarning.Code.BAD_IMAGE_PORT) shouldBe false
    }

    "do not add BAD_IMAGE_PORT warning if offer is from vos" in {
      val rawOffer = buildRawOffer("")
      val vosOffer = buildOffer(true)
      val ow = new OfferWrapper(rawOffer, vosOffer, history)
      warnByBadImagePortUnifier.unify(ow).futureValue
      vosOffer.getOfferState.getWarnings.asScala
        .exists(_.getWarning == IndexationWarning.Code.BAD_IMAGE_PORT) shouldBe false
    }
  }

  private def buildOffer(isFromVos: Boolean): Offer = {
    val offer = new Offer()
    offer.setFromVos(isFromVos)
    offer
  }

  private def buildOfferHistory(): OfferHistory = {
    OfferHistory.build(
      true,
      Instant.now(),
      new PriceHistory(List.empty.asJava),
      new RgidHistory(List.empty.asJava)
    )
  }

  private def buildRawOffer(externalUrl: String): RawOffer = {
    val rawOffer = new RawOfferImpl()
    rawOffer
      .setModernImages(
        UnifiedImages
          .newBuilder()
          .addImage(RealtyPhotoInfo.newBuilder().setExternalUrl(externalUrl))
          .build()
      )
    rawOffer
  }
}
