package ru.yandex.realty.unification.unifier.processor.unifiers

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.generator.RawOfferGenerator
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.realty.model.offer.{AreaInfo, AreaUnit, CategoryType, IndexingError, OfferType}
import ru.yandex.realty.model.raw.RawAreaOld
import ru.yandex.realty.storage.verba.{TestVerbaDictionaries, VerbaStorage}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.vertis.generators.ProducerProvider._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class AreaUnifierSpec extends AsyncSpecBase with MockFactory with Matchers with OneInstancePerTest {

  private val verbaStorage = new VerbaStorage(Seq(TestVerbaDictionaries.AREA_TYPE).asJava)
  private val verbaProvider = ProviderAdapter.create(verbaStorage)

  private val unifier = new AreaUnifier
  unifier.setVerbaProvider(verbaProvider)

  implicit val trace: Traced = Traced.empty

  "AreaUnifier" should {

    s"ban commercial offers without area" in {
      val rawOffer = RawOfferGenerator.RawOfferGen.next
      rawOffer.setArea(null)
      rawOffer.setLotArea(null)
      val offer = OfferModelGenerators.offerGen().next
      offer.setCategoryType(CategoryType.COMMERCIAL)
      offer.setOfferType(OfferType.SELL)
      unifier.unify(new OfferWrapper(rawOffer, offer, null), trace)

      offer.getOfferState.getErrors.asScala.map(_.getError) shouldEqual (Seq(IndexingError.AREA_NOT_SET))
    }

    s"not ban commercial offers with lot area" in {
      val rawOffer = RawOfferGenerator.RawOfferGen.next
      rawOffer.setArea(null)
      rawOffer.setLotArea(new RawAreaOld(100, "сотка"))
      val offer = OfferModelGenerators.offerGen().next
      offer.setCategoryType(CategoryType.COMMERCIAL)
      offer.setOfferType(OfferType.SELL)
      unifier.unify(new OfferWrapper(rawOffer, offer, null), trace)

      offer.getOfferState.getErrors.asScala.map(_.getError) shouldEqual (Seq.empty)
    }

    s"not ban commercial offers with area" in {
      val rawOffer = RawOfferGenerator.RawOfferGen.next
      rawOffer.setArea(new RawAreaOld(100, "кв. м"))
      rawOffer.setLotArea(null)
      val offer = OfferModelGenerators.offerGen().next
      offer.setCategoryType(CategoryType.COMMERCIAL)
      offer.setOfferType(OfferType.SELL)
      unifier.unify(new OfferWrapper(rawOffer, offer, null), trace)

      offer.getOfferState.getErrors.asScala.map(_.getError) shouldEqual (Seq.empty)
    }

    s"set area for commercial offers with lot area" in {
      val rawOffer = RawOfferGenerator.RawOfferGen.next
      rawOffer.setArea(null)
      rawOffer.setLotArea(new RawAreaOld(100, "сотка"))
      val offer = OfferModelGenerators.offerGen().next
      offer.setCategoryType(CategoryType.COMMERCIAL)
      offer.setOfferType(OfferType.SELL)
      unifier.unify(new OfferWrapper(rawOffer, offer, null), trace)

      val expectedArea = AreaInfo.create(AreaUnit.ARE, 100f)

      offer.getArea shouldEqual (expectedArea)
    }

    s"not ban lot offers with lot area" in {
      val rawOffer = RawOfferGenerator.RawOfferGen.next
      rawOffer.setArea(null)
      rawOffer.setLotArea(new RawAreaOld(100, "сотка"))
      val offer = OfferModelGenerators.offerGen().next
      offer.setCategoryType(CategoryType.LOT)
      offer.setOfferType(OfferType.SELL)
      unifier.unify(new OfferWrapper(rawOffer, offer, null), trace)

      offer.getOfferState.getErrors.asScala.map(_.getError) shouldEqual (Seq.empty)
    }

    s"unify area" in {
      val rawOffer = RawOfferGenerator.RawOfferGen.next
      rawOffer.setArea(new RawAreaOld(100, "кв. м"))
      rawOffer.setLotArea(null)

      val offer = OfferModelGenerators.offerGen().next
      offer.setCategoryType(CategoryType.APARTMENT)
      offer.setOfferType(OfferType.SELL)

      unifier.unify(new OfferWrapper(rawOffer, offer, null), trace)

      offer.getArea.getUnit shouldEqual AreaUnit.SQUARE_METER
      offer.getArea.getValue shouldEqual 100
      offer.getOfferState.getErrors.asScala.map(_.getError) shouldEqual (Seq())
    }
  }
}
