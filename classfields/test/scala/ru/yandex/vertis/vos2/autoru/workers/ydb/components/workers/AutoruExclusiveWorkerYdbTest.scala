package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import ru.yandex.vos2.commonfeatures.FeatureRegistryFactory
import ru.yandex.vos2.commonfeatures.FeaturesManager
import org.scalatest.prop.TableDrivenPropertyChecks
import ru.yandex.vos2.OfferModel.Offer
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vos2.model.offer.OfferGenerator
import ru.yandex.vos2.OfferModel.OfferService
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AutoruExclusiveWorkerYdbTest extends AnyFunSuite with TableDrivenPropertyChecks with Matchers {

  private val featureRegistry = FeatureRegistryFactory.inMemory()
  private val featuresManager = new FeaturesManager(featureRegistry)

  case class OfferNeedsTagTestCase(
      accepted: Boolean,
      verified: Boolean,
      fromCallCenter: Boolean,
      manualVerified: Option[Boolean],
      expected: Boolean
  )

  private val offerNeedsTagTestCases = Table(
    "test case",
    OfferNeedsTagTestCase(
      accepted = false,
      verified = false,
      fromCallCenter = false,
      manualVerified = None,
      expected = false
    ),
    OfferNeedsTagTestCase(
      accepted = true,
      verified = false,
      fromCallCenter = false,
      manualVerified = None,
      expected = false
    ),
    OfferNeedsTagTestCase(
      accepted = true,
      verified = true,
      fromCallCenter = false,
      manualVerified = None,
      expected = true
    ),
    OfferNeedsTagTestCase(
      accepted = true,
      verified = true,
      fromCallCenter = true,
      manualVerified = None,
      expected = false
    ),
    // value set by a moderator has priority over everything else
    OfferNeedsTagTestCase(
      accepted = false,
      verified = false,
      fromCallCenter = false,
      manualVerified = Some(true),
      expected = true
    ),
    OfferNeedsTagTestCase(
      accepted = true,
      verified = true,
      fromCallCenter = false,
      manualVerified = Some(false),
      expected = false
    )
  )

  private val sampleOffer = (for {
    offerBase <- OfferGenerator.offerWithRequiredFields(OfferService.OFFER_AUTO)
    autoru <- OfferGenerator.autoruOfferWithRequiredFields()
  } yield offerBase.setOfferAutoru(autoru).build()).sample.get

  test("offerNeedsTag") {
    forAll(offerNeedsTagTestCases) { c =>
      val builder = sampleOffer.toBuilder()
      val autoru = builder
        .getOfferAutoruBuilder()
        .setAcceptedAutoruExclusive(c.accepted)
        .setAutoruExclusiveVerified(c.verified)
      c.manualVerified.foreach(autoru.setAutoruExclusiveManualVerified)
      autoru.getSourceInfoBuilder.setIsCallcenter(c.fromCallCenter)
      val offer = builder.build

      val result = AutoruExclusiveWorkerYdb.offerNeedsTag(featuresManager, offer)
      result shouldBe c.expected
    }
  }
}
