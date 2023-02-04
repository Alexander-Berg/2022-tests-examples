package ru.yandex.vos2.autoru.dao.offers

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{Assertion, BeforeAndAfter, BeforeAndAfterAll}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferID
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.Filters
import ru.yandex.vos2.autoru.dao.proxy.{OffersReader, OffersWriter}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.model.UserRef

import scala.jdk.CollectionConverters._

class RecommendationTagsIndexTest extends AnyFunSuite with InitTestDbs with BeforeAndAfter with BeforeAndAfterAll {

  initDbs()

  val dao: AutoruOfferDao = components.offerVosDao
  val offerWriter: OffersWriter = components.offersWriter
  val offerReader: OffersReader = components.offersReader

  val offerId1 = "123-abc"
  val offerId2 = "456-def"
  val offerId3 = "789-ghi"

  val tagName1 = "test-recommendation-1"
  val tagName2 = "test-recommendation-2"
  val tagName3 = "test-recommendation-3"
  val tagName4 = "test-recommendation-4"

  implicit val traced: Traced = Traced.empty

  before {
    initDbs()
  }

  test("added recommendation tags to index") {
    val userRef = UserRef.from("ac_12345")
    val offer = {
      val b = createOffer(offerId1, userRef).toBuilder
      b.addRecommendationTags("test-recommendation-1")
      b.addRecommendationTags("test-recommendation-2")
      b.addRecommendationTags("test-recommendation-3")
      b.build()
    }
    dao.saveMigrated(Seq(offer), "test-1")
    val readOffer = dao.findById(offerId1, false, true)
    assert(readOffer.nonEmpty)
    val tags = readOffer.get.getRecommendationTagsList.asScala.toList
    assert(tags.size == 3)
  }

  test("filtered by recommendation tags from index") {
    //подготовим записи в бд
    val userRef = UserRef.from("ac_12345")
    val offer1 = {
      val b = createOffer(offerId1, userRef).toBuilder
      b.addRecommendationTags(tagName1)
      b.addRecommendationTags(tagName2)
      b.addRecommendationTags(tagName3)
      b.build()
    }
    val offer2 = {
      val b = createOffer(offerId2, userRef).toBuilder
      b.addRecommendationTags(tagName1)
      b.addRecommendationTags(tagName2)
      b.build()
    }

    val offer3 = {
      val b = createOffer(offerId3, userRef).toBuilder
      b.addRecommendationTags(tagName1)
      b.addRecommendationTags(tagName4)
      b.build()
    }

    dao.saveMigrated(Seq(offer1), "test")
    dao.saveMigrated(Seq(offer2), "test")
    dao.saveMigrated(Seq(offer3), "test")

    val tags1 = Seq(tagName1, tagName2, tagName3)
    val tags2 = Seq(tagName1, tagName2)
    val tags3 = Seq(tagName1)
    val tags4 = Seq(tagName4)
    val tags5 = Seq(tagName3, tagName4)

    val expected1 = Seq(offerId1)
    val expected2 = Seq(offerId1, offerId2)
    val expected3 = Seq(offerId1, offerId2, offerId3)
    val expected4 = Seq(offerId3)
    val expected5 = Seq.empty

    check("получаем offerId1")(userRef, tags1, expected1)
    check("получаем offerId1, offerId2")(userRef, tags2, expected2)
    check("получаем offerId1, offerId2, offerId3")(userRef, tags3, expected3)
    check("получаем offerId3")(userRef, tags4, expected4)
    check("получаем пустой ответ")(userRef, tags5, expected5)
  }

  def check(name: String)(userRef: UserRef, tags: Seq[String], expected: Seq[OfferID]): Assertion = {
    val filters = filterWithRecommendationTags(tags)
    val conditions = filters.listWhere
    val found = dao.select(userRef, conditions)
    val offerIds = found.map(_.getOfferID)
    assert(offerIds == expected)
  }

  private def createOffer(offerId: String, userRef: UserRef, removed: Boolean = false): Offer = {
    val builder = TestUtils.createOffer()
    builder.setOfferID(offerId)
    builder.setUserRef(userRef.toString)

    if (removed) {
      builder.addFlag(OfferFlag.OF_DELETED)
    }
    builder.build()
  }

  private def filterWithRecommendationTags(tags: Seq[String]): Filters = createFilter.copy(recommendationTags = tags)

  private def createFilter: Filters = {
    Filters(
      truckCategory = Seq.empty,
      motoCategory = Seq.empty,
      status = Seq.empty,
      service = Seq.empty,
      multipostingStatus = Seq.empty,
      multipostingService = Seq.empty,
      tag = Seq.empty,
      excludeTag = Seq.empty,
      vin = Seq.empty,
      markModel = Seq.empty,
      priceFrom = None,
      priceTo = None,
      geobaseId = Seq.empty,
      banReasons = Seq.empty,
      section = None,
      createDateFrom = None,
      createDateTo = None,
      noActiveServices = None,
      offerIRef = Seq.empty,
      licensePlate = Seq.empty,
      exteriorPanorama = Seq.empty,
      interiorPanorama = Seq.empty,
      canSendFavoriteMessage = None,
      favoriteMessageWasSent = None,
      yearFrom = None,
      yearTo = None,
      year = Seq.empty,
      availability = Seq.empty,
      hasExteriorPanorama = None,
      hasInteriorPanorama = None,
      hasPhoto = None,
      superGen = Seq.empty,
      canBook = None,
      classifiedStatus = Seq.empty,
      colorHex = Seq.empty,
      callsAuctionBidFrom = None,
      callsAuctionBidTo = None,
      hasCallsAuctionBid = None,
      recommendationTags = Seq.empty
    )
  }
}
