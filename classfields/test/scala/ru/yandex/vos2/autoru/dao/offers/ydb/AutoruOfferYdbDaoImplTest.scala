package ru.yandex.vos2.autoru.dao.offers.ydb

import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers._
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.scalatest.BetterEitherValues
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils.YdbInterpolator
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferYdbDaoImpl
import ru.yandex.vos2.dao.offers.ng.io.Compressor
import ru.yandex.vos2.model.{OfferRef, UserRef}
import ru.yandex.vos2.util.YdbUtils.YdbShard

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import ru.yandex.vos2.autoru.model.OffersByParamsFilter
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._

/**
  * Created by dmitrySafonov.
  */
//noinspection JavaAccessorEmptyParenCall
// scalastyle:off multiple.string.literals
@RunWith(classOf[JUnitRunner])
class AutoruOfferYdbDaoImplTest
  extends AnyFunSuite
  with OptionValues
  with BetterEitherValues
  with LoneElement
  with InitTestDbs
  with BeforeAndAfterAll {
  implicit private val ec: ExecutionContext = Threads.SameThreadEc
  implicit val trace: Traced = Traced.empty
  private val compressor = Compressor.SnappyCompressor
  private val offers = getOffersFromJson

  override def beforeAll(): Unit = {
    initDbs()
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateSettings.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigratePhones.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateDiscounts.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateVideos.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateIps.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateReasonArchiveUsers.name, true)

    offers.foreach { offer =>
      val offerId = offer.getOfferID
      val shardOfferId = offerId.getYdbShard
      val offerData = compressor.compress(offer.toByteArray)
      val userId = offer.getUserRef
      val shardUserId = userId.getYdbShard
      components.skypper.transaction("insert-initial-data") { executor =>
        executor.updatePrepared("initial-offers")(
          ydb"upsert into offers (shard_id, offer_id,deleted,proto) values ($shardOfferId, $offerId, false, $offerData)"
        )

        executor.updatePrepared("initial-users")(
          ydb"""upsert into users_offers
                   |(shard_id, user_id, offer_id)
                   |values (
                   |$shardUserId,
                   |$userId,
                   |$offerId

                   |)""".stripMargin
        )
      }

    }

    components.offerVosDao.migrateAndSave(getSalesFromDb)((sale, optOffer) => {
      components.carOfferConverter.convertStrict(sale, optOffer).converted
    })(Traced.empty)

    components.offerVosDao.migrateAndSave(getSalesFromDb)((sale, _) => {
      val o = getOfferById(sale.id)
      components.carOfferConverter.convertStrict(sale, Some(o)).converted
    })(Traced.empty)
    // домигрируем комтранс и мото объявления
    val truckOfferWithTags = components.autoruTrucksDao
      .getOffersForMigration(Seq(6541923))
      .map(truck => {
        components.truckOfferConverter
          .convertStrict(truck, None)
          .converted
          .value
          .toBuilder
          .addAllTag(Seq("tag1", "tag2", "tag3").asJava)
          .build()
      })
    val truckOffers = components.autoruTrucksDao
      .getOffersForMigration(Seq(6542117, 6229746))
      .map(truck => {
        components.truckOfferConverter.convertStrict(truck, None).converted.value
      })

    components.offerVosDao.saveMigrated(truckOfferWithTags ++ truckOffers)(Traced.empty)
    val motoOffers = components.autoruMotoDao
      .getOffersForMigration(Seq(2298241))
      .map(moto => {
        components.motoOfferConverter.convertStrict(moto, None).converted.value
      })

    components.offerVosDao.saveMigrated(motoOffers)(Traced.empty)
  }

  override def afterAll(): Unit = {
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateSettings.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigratePhones.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateDiscounts.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateVideos.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateIps.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateReasonArchiveUsers.name, false)
  }

  private def ydbDaoImpl: AutoruOfferYdbDaoImpl = components.offerYdbDao.asInstanceOf[AutoruOfferYdbDaoImpl]

  test("find_by_id_ydb") {
    val testOffer = offers.head
    val offerFromYdb = ydbDaoImpl.findById(testOffer.getOfferID).value
    assert(offerFromYdb == testOffer)
  }

  test("ydb_find_by_ref_ydb") {
    val testOffer = offers.head
    val offerFromYdb = ydbDaoImpl.findByRef(OfferRef(testOffer.getUserRef, testOffer.getOfferID)).value
    assert(offerFromYdb == testOffer)
  }
  test("ydb_find_by_ref_ydb_wrong_user") {
    val testOffer = offers.head
    val offerFromYdb = ydbDaoImpl.findByRef(OfferRef("otherUser", testOffer.getOfferID))
    assert(offerFromYdb.isEmpty)
  }

  test("find_by_refs_ydb") {
    val testOfferRefs = offers.map(testOffer => OfferRef(testOffer.getUserRef, testOffer.getOfferID))
    val offersFromYdb = ydbDaoImpl.findByRefs(testOfferRefs)
    offersFromYdb.values should contain theSameElementsAs offers
  }

  test("findOnShard") {
    val testOffer = offers.head
    val offerFromYdb = ydbDaoImpl.findOnShard(testOffer.getOfferID, null).value
    assert(offerFromYdb == testOffer)
  }
  test("batchFindOnShard") {
    val testOfferRefs = offers.map(_.getOfferID)
    val offersFromYdb = ydbDaoImpl.batchFindOnShard(testOfferRefs, null)
    offersFromYdb.values should contain theSameElementsAs offers
  }

  test("selectWithOffsetOptimization") {
    val testOffer = offers.head
    val offerFromYdb = ydbDaoImpl.selectWithOffsetOptimization(UserRef.from(testOffer.getUserRef)).head
    assert(offerFromYdb == testOffer)
  }

  test("get offers by params (empty filter)") {
    implicit val trace: Traced = Traced.empty
    val offers = ydbDaoImpl.getOffersByParams(
      OffersByParamsFilter(Seq.empty, Seq.empty, Seq.empty, Seq.empty, true),
      Seq.fill(components.mySql.shardCount)(Some((0, Int.MaxValue)))
    )
    assert(offers.isEmpty)
  }

  test("get offers by params (by offer ID)") {
    implicit val trace: Traced = Traced.empty
    val offers = ydbDaoImpl.getOffersByParams(
      OffersByParamsFilter(
        offerIds = Seq("1043045004-977b3"),
        userIds = Seq.empty,
        vins = Seq.empty,
        licensePlates = Seq.empty,
        includeRemoved = true
      ),
      Seq.fill(components.mySql.shardCount)(Some((0, Int.MaxValue)))
    )
    val ids = offers.map(_.getOfferID())
    assert(ids == Seq("1043045004-977b3"))
  }

  test("get offers by params (by user ID)") {
    implicit val trace: Traced = Traced.empty
    val offers = ydbDaoImpl.getOffersByParams(
      OffersByParamsFilter(
        offerIds = Seq.empty,
        userIds = Seq(UserRef.from("a_10591660")),
        vins = Seq.empty,
        licensePlates = Seq.empty,
        includeRemoved = true
      ),
      Seq.fill(components.mySql.shardCount)(Some((0, Int.MaxValue)))
    )
    val ids = offers.map(_.getOfferID())
    assert(ids.contains("1043045004-977b3"))
  }

  test("get offers by params (by VIN)") {
    implicit val trace: Traced = Traced.empty
    val offers = ydbDaoImpl.getOffersByParams(
      OffersByParamsFilter(
        offerIds = Seq.empty,
        userIds = Seq.empty,
        vins = Seq("WDC1648221A770037"),
        licensePlates = Seq.empty,
        includeRemoved = true
      ),
      Seq.fill(components.mySql.shardCount)(Some((0, Int.MaxValue)))
    )
    val ids = offers.map(_.getOfferID())
    assert(ids == Seq("1043045004-977b3"))
  }

  test("get offers by params (by license plate)") {
    implicit val trace: Traced = Traced.empty
    val offers = ydbDaoImpl.getOffersByParams(
      OffersByParamsFilter(
        offerIds = Seq.empty,
        userIds = Seq.empty,
        vins = Seq.empty,
        licensePlates = Seq("Р465ОС18"),
        includeRemoved = true
      ),
      Seq.fill(components.mySql.shardCount)(Some((0, Int.MaxValue)))
    )
    val ids = offers.map(_.getOfferID())
    assert(ids == Seq("1043045004-977b3"))
  }

  test("get offers by params (all parameters)") {
    implicit val trace: Traced = Traced.empty
    val offers = ydbDaoImpl.getOffersByParams(
      OffersByParamsFilter(
        offerIds = Seq("1043045004-977b3"),
        userIds = Seq(UserRef.from("a_10591660")),
        vins = Seq("WDC1648221A770037"),
        licensePlates = Seq("Р465ОС18"),
        includeRemoved = true
      ),
      Seq.fill(components.mySql.shardCount)(Some((0, Int.MaxValue)))
    )
    val ids = offers.map(_.getOfferID())
    assert(ids == Seq("1043045004-977b3"))
  }
}
