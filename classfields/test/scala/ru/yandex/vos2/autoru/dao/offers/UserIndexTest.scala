package ru.yandex.vos2.autoru.dao.offers

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.{AutoruOfferID, TestUtils}
import ru.yandex.vos2.autoru.utils.user.UserIndexUtils
import ru.yandex.vos2.dao.offers.OfferUpdate
import ru.yandex.vos2.dao.users.UserIndexDao
import ru.yandex.vos2.model.UserRef

import scala.jdk.CollectionConverters._

/**
  * Created by sievmi on 26.02.19
  */
class UserIndexTest extends AnyFunSuite with InitTestDbs with BeforeAndAfter with BeforeAndAfterAll {

  initDbs()

  val dao: AutoruOfferDao = components.offerVosDao
  val indexDao: UserIndexDao = components.userIndexDao

  val offerId1 = "123-abc"
  val offerId2 = "456-def"
  val offerId3 = "789-ghi"

  implicit val traced = Traced.empty

  before {
    components.mySql.shards.foreach(shard => {
      shard.master.jdbc.update("DELETE FROM t_users_index")
    })
    components.featureRegistry.updateFeature(components.featuresManager.UpdateUserSearchIndex.name, true)
  }

  after {
    components.featureRegistry.updateFeature(components.featuresManager.UpdateUserSearchIndex.name, false)
  }

  test("User with one offer") {
    pending
    val userRef = UserRef.from("ac_12345")
    val offer = createOffer(offerId1, userRef)

    dao.saveMigrated(Seq(offer), "test")

    val optIndex = indexDao.getSearchUserIndex(userRef)
    assert(optIndex.nonEmpty)
    assert(optIndex.get.getOffersCount == 1)
    assert(
      optIndex.get.getOffersMap.get(offerId1) ==
        UserIndexUtils.createField(offer, components.stringDeduplication)
    )
  }

  test("User with 3 offers") {
    pending
    val userRef = UserRef.from("ac_12345")
    val offer1 = createOffer(offerId1, userRef)
    val offer2 = createOffer(offerId2, userRef)
    val offer3 = createOffer(offerId3, userRef)

    dao.saveMigrated(Seq(offer1, offer2, offer3), "test")

    val optIndex = indexDao.getSearchUserIndex(userRef)
    assert(optIndex.nonEmpty)
    assert(optIndex.get.getOffersCount == 3)
    assert(
      optIndex.get.getOffersMap.get(offerId1) ==
        UserIndexUtils.createField(offer1, components.stringDeduplication)
    )
    assert(
      optIndex.get.getOffersMap.get(offerId2) ==
        UserIndexUtils.createField(offer2, components.stringDeduplication)
    )
    assert(
      optIndex.get.getOffersMap.get(offerId3) ==
        UserIndexUtils.createField(offer3, components.stringDeduplication)
    )
  }

  test("User with 2 active and 1 removed offers") {
    pending
    val userRef = UserRef.from("ac_12345")
    val offer1 = createOffer(offerId1, userRef)
    val offer2 = createOffer(offerId2, userRef)
    val offer3 = createOffer(offerId3, userRef, removed = true)

    dao.saveMigrated(Seq(offer1, offer2, offer3), "test")

    val optIndex = indexDao.getSearchUserIndex(userRef)
    assert(optIndex.nonEmpty)
    assert(optIndex.get.getOffersCount == 2)
    assert(
      optIndex.get.getOffersMap.get(offerId1) ==
        UserIndexUtils.createField(offer1, components.stringDeduplication)
    )
    assert(
      optIndex.get.getOffersMap.get(offerId2) ==
        UserIndexUtils.createField(offer2, components.stringDeduplication)
    )
    assert(optIndex.get.getOffersMap.asScala.get(offerId3).isEmpty)
  }

  test("Remove deleted offer from index") {
    pending
    val userRef = UserRef.from("ac_12345")
    val offer1 = createOffer(offerId1, userRef)
    val offer2 = createOffer(offerId2, userRef)

    dao.saveMigrated(Seq(offer1, offer2), "test")

    val optIndex = indexDao.getSearchUserIndex(userRef)
    assert(optIndex.get.getOffersCount == 2)

    val removeOffer2 = offer2.toBuilder.addFlag(OfferFlag.OF_DELETED).build()
    dao.saveMigrated(Seq(removeOffer2), "test")

    val optIndex2 = indexDao.getSearchUserIndex(userRef)
    assert(optIndex2.get.getOffersCount == 1)
    assert(optIndex2.get.getOffersMap.asScala.get(offerId1).nonEmpty)
    assert(optIndex2.get.getOffersMap.asScala.get(offerId2).isEmpty)
  }

  test("Added new offer to index") {
    pending
    val userRef = UserRef.from("ac_12345")
    val offer1 = createOffer(offerId1, userRef)
    val offer2 = createOffer(offerId2, userRef)

    dao.saveMigrated(Seq(offer1, offer2), "test")

    val optIndex = indexDao.getSearchUserIndex(userRef)
    assert(optIndex.get.getOffersCount == 2)

    val newOffer = createOffer(offerId3, userRef)
    dao.saveMigrated(Seq(newOffer), "test")

    val optIndex2 = indexDao.getSearchUserIndex(userRef)
    assert(optIndex2.get.getOffersCount == 3)
    assert(optIndex2.get.getOffersMap.asScala.get(offerId1).nonEmpty)
    assert(optIndex2.get.getOffersMap.asScala.get(offerId2).nonEmpty)
    assert(optIndex2.get.getOffersMap.asScala.get(offerId3).nonEmpty)
  }

  test("Update if offer changed") {
    pending
    val userRef = UserRef.from("ac_12345")
    val offer = createOffer(offerId1, userRef)

    dao.saveMigrated(Seq(offer), "test")

    val optIndex = indexDao.getSearchUserIndex(userRef)
    assert(optIndex.get.getOffersCount == 1)

    val builder = offer.toBuilder
    builder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("NEWVIN")
    builder.getOfferAutoruBuilder.setUserIndexHash(UserIndexUtils.hash(builder.build()))
    val newOffer = builder.build()
    dao.useOfferID(AutoruOfferID.parse(offerId1), false, "test")(_ => OfferUpdate.visitNow(newOffer))

    val optIndex2 = indexDao.getSearchUserIndex(userRef)
    assert(optIndex2.get.getOffersCount == 1)
    assert(
      optIndex2.get.getOffersMap.get(offerId1).getVin ==
        UserIndexUtils.createField(newOffer, components.stringDeduplication).getVin
    )
  }

  test("check partitions (batch update)") {
    pending
    val userRef = UserRef.from("ac_12345")
    val offer1 = createOffer(offerId1, userRef) // partition = 3
    val offer2 = createOffer(offerId2, userRef) // partition = 2

    dao.saveMigrated(Seq(offer1, offer2), "test")
    val index1 = indexDao.getSearchUserIndex(userRef, partition = Some(UserIndexUtils.partition(offer1)))
    val index2 = indexDao.getSearchUserIndex(userRef, partition = Some(UserIndexUtils.partition(offer2)))

    assert(index1.get.getOffersCount == 1)
    assert(
      index1.get.getOffersMap.get(offerId1) ==
        UserIndexUtils.createField(offer1, components.stringDeduplication)
    )

    assert(index2.get.getOffersCount == 1)
    assert(
      index2.get.getOffersMap.get(offerId2) ==
        UserIndexUtils.createField(offer2, components.stringDeduplication)
    )

    assert(indexDao.getSearchUserIndex(userRef, partition = Some(0)).isEmpty)
    assert(indexDao.getSearchUserIndex(userRef, partition = Some(1)).isEmpty)
  }

  test("check partitions") {
    pending
    val userRef = UserRef.from("ac_12345")
    val offer = createOffer(offerId1, userRef) // partition = 3

    dao.saveMigrated(Seq(offer), "test")

    val optIndex = indexDao.getSearchUserIndex(userRef, partition = Some(UserIndexUtils.partition(offer)))
    assert(optIndex.get.getOffersCount == 1)

    val builder = offer.toBuilder
    builder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("NEWVIN")
    builder.getOfferAutoruBuilder.setUserIndexHash(UserIndexUtils.hash(builder.build()))
    val newOffer = builder.build()
    dao.useOfferID(AutoruOfferID.parse(offerId1), false, "test")(_ => OfferUpdate.visitNow(newOffer))

    val optIndex2 = indexDao.getSearchUserIndex(userRef, partition = Some(UserIndexUtils.partition(offer)))
    assert(optIndex2.get.getOffersCount == 1)
    assert(
      optIndex2.get.getOffersMap.get(offerId1).getVin ==
        UserIndexUtils.createField(newOffer, components.stringDeduplication).getVin
    )
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
}
