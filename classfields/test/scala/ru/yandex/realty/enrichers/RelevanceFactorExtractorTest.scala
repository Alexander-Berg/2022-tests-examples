package ru.yandex.realty.enrichers

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest._
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.ranking.RankingCatFactor._
import ru.yandex.realty.model.offer.ranking.RankingNumFactor._
import ru.yandex.realty.model.offer.{ApartmentInfo, BuildingInfo, FlatType, Offer, PriceInfo, Transaction}
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.relevance.RelevanceFactorExtractor._

import collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RelevanceFactorExtractorTest
  extends FlatSpec
  with Matchers
  with PropertyChecks
  with BeforeAndAfterAll
  with OfferModelGenerators {

  "RelevanceFeatureExtractor" should "extract features from offer without errors" in {
    val generator = OfferModelGenerators.offerGen()

    val numOffers = 10

    forAll(Gen.listOfN(numOffers, generator)) { offers =>
      offers.foreach(extractAllNumericalFactors)
      offers.foreach(extractAllCategoricalFactors)
    }
  }

  "numericalExtractor" should "extract features correctly" in {

    val featureNames =
      Seq(
        YEAR,
        ROOMS_TOTAL,
        FLOOR,
        FLOOR_RATIO,
        TOTAL_IMAGES,
        COMISSION_PERCENTAGE,
        PREPAYMENT_PERCENTAGE,
        DESCRIPTION_LENGTH,
        DESCRIPTION_POSSIBLE_LENGTH_RATIO,
        DESCRIPTION_UNIQUE_CHARS,
        DESCRIPTION_UNIQUE_TOKENS,
        DESCRIPTION_CAPS_WORDS_RATIO,
        IMAGE_META_SIMILAR_EURO_MAIN
      )

    val offer = new Offer()

    val a = new ApartmentInfo()
    a.setRooms(2)
    a.setFloors(List(1.asInstanceOf[Integer]).asJava)
    offer.setApartmentInfo(a)

    val b = new BuildingInfo()
    b.setBuildYear(1984)
    b.setFloorsTotal(10)
    offer.setBuildingInfo(b)

    val t = new Transaction()
    t.setCommission(50f)
    t.setPrepayment(50)
    offer.setTransaction(t)

    offer.setDescription("Предлагается новая, ОТЛИЧНАЯ квартира!!!")

    val features = extractAllNumericalFactors(offer)

    featureNames.collect(features) shouldEqual Seq(1984, 2, 1, 0.1, 0, 50, 50, 40, 0.15936255, 19, 4, 0.25,
      0).map(_.toFloat)
  }

  "categoricalExtractor" should "extract features correctly" in {

    val featureNames = Seq(FLAT_TYPE, SUBJECT_FEDERATION_ID)

    val offer = new Offer()

    val a = new ApartmentInfo()
    a.setFlatType(FlatType.SECONDARY)
    offer.setApartmentInfo(a)

    val l = new Location()
    l.setSubjectFederation(Regions.MOSCOW, NodeRgid.MOSCOW)
    offer.setLocation(l)

    val features = extractAllCategoricalFactors(offer)

    featureNames.collect(features) shouldEqual Seq(FlatType.SECONDARY.value, Regions.MOSCOW).map(_.toLong)
  }
}
