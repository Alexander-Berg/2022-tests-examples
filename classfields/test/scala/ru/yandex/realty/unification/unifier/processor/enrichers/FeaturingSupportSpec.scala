package ru.yandex.realty.unification.unifier.processor.enrichers

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.realty.model.message.RealtySchema.RelevanceMessage

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class FeaturingSupportSpec extends AsyncSpecBase with FeaturingSupport with Matchers with OfferModelGenerators {

  "relevanceComparator" should {
    "sort by new relevance if it exists" in {
      val a = createOffer(Some(1.0f), Some(2.0f))
      val b = createOffer(Some(100.0f), Some(1.0f))
      Seq(a, b).sorted(relevanceComparator) should be(Seq(a, b))
      Seq(b, a).sorted(relevanceComparator) should be(Seq(a, b))
    }

    "sort by old relevance if no new relevance in both offers" in {
      val a = createOffer(Some(1.0f), None)
      val b = createOffer(Some(100.0f), None)
      Seq(a, b).sorted(relevanceComparator) should be(Seq(b, a))
      Seq(b, a).sorted(relevanceComparator) should be(Seq(b, a))
    }

    "sort offers with new relevance before offers without new relevance" in {
      val a = createOffer(Some(1.0f), Some(2.0f))
      val b = createOffer(Some(100.0f), None)
      Seq(a, b).sorted(relevanceComparator) should be(Seq(a, b))
      Seq(b, a).sorted(relevanceComparator) should be(Seq(a, b))

    }

    "sort offers with any relevance higher than offers without any relevance" in {
      val a = createOffer(Some(1.0f), None)
      val b = createOffer(None, Some(2.0f))
      val c = createOffer(None, None)
      val orig = Seq(a, b, c)
      val expected = Seq(b, a, c)

      for (actual <- orig.permutations) {
        actual.sorted(relevanceComparator) should be(expected)
      }
    }

    "sort offers correctly" in {
      val a = createOffer(Some(1.0f), Some(2.0f))
      val b = createOffer(Some(100.0f), Some(1.0f))
      val c = createOffer(Some(1.0f), None)
      val d = createOffer(Some(2.0f), None)
      val e = createOffer(None, Some(0.5f))
      val f = createOffer(None, None)

      val orig = Seq(a, b, c, d, e, f)
      val expected = Seq(a, b, e, d, c, f)
      for (actual <- orig.permutations) {
        actual.sorted(relevanceComparator) should be(expected)
      }
    }
  }

  private def createOffer(oldRelevance: Option[Float], newRelevance: Option[Float]): Offer = {
    val offer = offerGen().next
    oldRelevance.map(Float.box).foreach(offer.setRelevance)
    newRelevance.foreach { r =>
      val relevances = RelevanceMessage
        .newBuilder()
        .setSecondary(r)
        .setSecondaryAB(r)
        .setMixed(r)
        .setMixedAB(r)
        .build()
      offer.setRelevances(relevances)
    }
    offer
  }
}
