package ru.yandex.vos2.autoru.dao.offers.mysql

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.CommonModel.PaidService
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.scalatest.BetterEitherValues
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService.ServiceType
import ru.yandex.vos2.AutoruModel.AutoruOffer.TruckInfo.TruckCategory
import ru.yandex.vos2.AutoruModel.AutoruOffer.UserChangeAction
import ru.yandex.vos2.AutoruModel.AutoruOffer.UserChangeAction.UserChangeActionType
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.BasicsModel.CompositeStatus.CS_ACTIVE
import ru.yandex.vos2.OfferModel.Multiposting.Classified.ClassifiedName
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.ActivationSuccess
import ru.yandex.vos2.autoru.dao.offers.{AutoruOfferDao, AutoruOfferDaoImpl}
import ru.yandex.vos2.autoru.dao.proxy.FormWriteParams
import ru.yandex.vos2.autoru.model.AutoruModelUtils._
import ru.yandex.vos2.autoru.model.{AutoruOfferID, OffersByParamsFilter, TestUtils}
import ru.yandex.vos2.autoru.utils.FeedprocessorHashUtils
import ru.yandex.vos2.autoru.utils.testforms.{CarFormInfo, FormInfo, TestFormParams, TestFormsGenerator}
import ru.yandex.vos2.dao.offers.OfferUpdate
import ru.yandex.vos2.dao.utils.SimpleRowMapper
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.model.{OfferRef, UserRef}
import ru.yandex.vos2.{getNow, OfferID, OfferModel}

import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by andrey on 8/29/16.
  */
//noinspection JavaAccessorEmptyParenCall
// scalastyle:off multiple.string.literals
@RunWith(classOf[JUnitRunner])
class AutoruOfferDaoImplTest
  extends AnyFunSuite
  with OptionValues
  with BetterEitherValues
  with LoneElement
  with InitTestDbs
  with BeforeAndAfterAll {

  implicit private val ec: ExecutionContext = Threads.SameThreadEc

  override def beforeAll(): Unit = {
    initDbs()
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateSettings.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigratePhones.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateDiscounts.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateVideos.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateIps.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateReasonArchiveUsers.name, true)

  }

  override def afterAll(): Unit = {
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateSettings.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigratePhones.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateDiscounts.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateVideos.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateIps.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateReasonArchiveUsers.name, false)
  }

  private val offers = getOffersFromJson

  private val testFormGenerator = new TestFormsGenerator(components)

  private def daoImpl: AutoruOfferDaoImpl = components.getOfferDao().asInstanceOf[AutoruOfferDaoImpl]

  /**
    * в тесте проверяем метод insertUserMapping, который для offerId объявления вставляет в таблицу t_offers_users
    * userId, чтобы в дальнейшем можно было вычислить шард, на котором лежит offer. Таблица t_offers_users шардирована
    * по offerId.
    */
  test("insertUserMapping") {
    components.featureRegistry.updateFeature(components.featuresManager.WriteToYdb.name, false)

    assert(daoImpl.findUserRefByOfferId("1043270830-6b56a").isEmpty)
    daoImpl.insertUserMappingForOffers(getOffersFromJson)
    val userRef: UserRef =
      daoImpl.findUserRefByOfferId("1043270830-6b56a").value
    assert(userRef == UserRef.refAid(18318774))
    // проверим, что не кинет исключение, если передать пустой список
    daoImpl.insertUserMappingForOffers(Seq.empty)
    components.featureRegistry.updateFeature(components.featuresManager.WriteToYdb.name, true)

  }

  test("findUserRefByOfferIds") {
    components.featureRegistry.updateFeature(components.featuresManager.WriteToYdb.name, false)

    daoImpl.insertUserMappingForOffers(getOffersFromJson)
    val offerIds = getOffersFromJson.map(_.getOfferID)
    val mapping = daoImpl.findUserRefByOfferIds(offerIds)
    assert(mapping.size == offerIds.length)
    getOffersFromJson.foreach(offer => {
      assert(mapping(offer.getOfferID) == UserRef.from(offer.getUserRef))
    })
    components.featureRegistry.updateFeature(components.featuresManager.WriteToYdb.name, true)

  }

  /**
    * Проверим что migrateAndSave умеет конвертировать и сохранять объявления
    */
  test("migrateAndSave") {
    components.offerVosDao.migrateAndSave(getSalesFromDb)((sale, optOffer) => {
      components.carOfferConverter.convertStrict(sale, optOffer).converted
    })(Traced.empty)

    val migrated =
      components.mySql.shards.map(shard => components.offerVosDao.getWatchListSize(shard)(Traced.empty)).sum
    assert(migrated == saleIds.size)
  }

  /**
    * проверим корректность смигрированного объявления
    */
  test("migrated") {
    val expectedId = offers.head.getOfferID
    val expectedUserRef = offers.head.getUserRef
    val offer = components.offerVosDao
      .findByRef(
        OfferRef(offers.find(_.getOfferID == expectedId).value),
        includeRemoved = true
      )(Traced.empty)
      .value
    assert(offer.getOfferID == expectedId)
    assert(offer.getUserRef == expectedUserRef)
  }

  /**
    * Проверим метод actualize - после его вызова метод getActualizationDate должен вернуть дату, близкую к текущей
    */
  test("actualize") {
    // частное бесплатное активное объявление
    val now = new DateTime()
    val offer =
      offers
        .find(_.getOfferID == "1044216699-0f1a0")
        .value
        .toBuilder
        .setTimestampWillExpire(now.plusDays(3).getMillis)
        .build()
    val ref = OfferRef(offer)
    // offer должен быть не expired, сделаем ему expire date через три дня
    components.offerVosDao.useRef(ref)(_ => OfferUpdate.visitNow(offer))(Traced.empty)
    // пробуем актуализировать
    components.offerVosDao.useRef(ref)(offer => {
      OfferUpdate.visitNow(offer.toBuilder.actualize().build)
    })(Traced.empty)
    val updatedOffer = components.offerVosDao.findByRef(ref, includeRemoved = true)(Traced.empty).value
    assert(System.currentTimeMillis() - updatedOffer.getTimestampTtlStart < 1000)
  }

  /**
    * проверим метод findByRefs - который возвращает по списку офферреф список офферов
    */
  test("findByRefs") {
    val result = components.offerVosDao.findByRefs(offers.map(OfferRef(_)), includeRemoved = true)(Traced.empty, ec)
    assert(result.size == offers.size)
    assert(result.keySet == offers.map(OfferRef(_)).toSet)
  }

  test("getShardByOfferId") {
    components.featureRegistry.updateFeature(components.featuresManager.WriteToYdb.name, false)

    assert(
      daoImpl.getShardForUserMapping("1043270830-6b56a") ==
        components.mySql.shards.head
    )
    assert(
      daoImpl.getShardForUserMapping("1043140898-81de") ==
        components.mySql.shards(1)
    )
    components.featureRegistry.updateFeature(components.featuresManager.WriteToYdb.name, true)

  }

  test("searchOfferOnAllShards") {
    components.featureRegistry.updateFeature(components.featuresManager.WriteToYdb.name, false)

    // объявление на шарде 0
    assert(daoImpl.searchOfferOnAllShards("1043270830-6b56a").nonEmpty)
    // объявление на шарде 1
    assert(daoImpl.searchOfferOnAllShards("1043026846-83484c").nonEmpty)
    // такого объвления нет
    assert(daoImpl.searchOfferOnAllShards("1043026846-83484cc").isEmpty)
    components.featureRegistry.updateFeature(components.featuresManager.WriteToYdb.name, true)

  }

  // тут будем проверять заполнение индексных таблиц
  test("indexTables") {
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

    val vins = loadIndexData("t_offers_vin", v_value => v_value.toString)
    val licensePlates = loadIndexData("t_offers_license_plate", v_value => v_value.toString)
    val categories = loadIndexData("t_offers_category", v_value => v_value.toString.toInt)
    val geobaseIds = loadIndexData("t_offers_geobase_id", v_value => v_value.toString.toLong)
    val markModels = loadIndexData("t_offers_mark_model", v_value => v_value)
    val regionIds = loadIndexData("t_offers_region_id", v_value => v_value.toString.toLong)
    val truckCategories = loadIndexData("t_offers_truck_category", v_value => v_value.toString.toInt)
    val motoCategories = loadIndexData("t_offers_moto_category", v_value => v_value.toString.toInt)
    val extStatuses = loadIndexData("t_offers_ext_status", v_value => v_value.toString.toInt)
    val tags = loadIndexData("t_offers_tag", v_value => v_value.toString)
    val names = loadIndexData("t_offers_name", v_value => v_value.toString)
    val prices = loadIndexData("t_offers_price", v_value => v_value.toString.toDouble)
    val years = loadIndexData("t_offers_year", v_value => v_value.toString.toInt)
    val offerHash = loadIndexData("t_offers_hash3", v_value => v_value.toString)
    val offerSection = loadIndexData("t_offers_section", v_value => v_value.toString)
    val offerCreateDate = loadIndexData("t_offers_timestamp_create", v_value => v_value.toString)
    val callsAuctionBid = loadIndexData("t_offers_calls_auction_bid", v_value => v_value.toString.toLong)
    val vinSuggest = loadVinSuggestIndexData()

    assert(vins.size == 6)
    checkIndexData(
      vins,
      Seq(
        ("1042409964-038a", "WA1LFBFP1BA109828"),
        ("1043211458-fbbd39", "SALLSAA547A984722"),
        ("1044159039-33be8", "VF37L9HECCJ650918"),
        ("1043026846-83484c", "MC000000000000496"),
        ("6542117-19888", "XTT390995C0470897"),
        ("1043045004-977b3", "WDC1648221A770037")
      )
    )

    assert(licensePlates.size == 1)
    checkIndexData(
      licensePlates,
      Seq(
        ("1043045004-977b3", "Р465ОС18")
      )
    )

    assert(categories.size == 14)
    checkIndexData(
      categories,
      Seq(
        ("1037186558-e06d8", 1),
        ("1042409964-038a", 1),
        ("1043026846-83484c", 1),
        ("1043045004-977b3", 1),
        ("1043140898-81de", 1),
        ("1043211458-fbbd39", 1),
        ("1043270830-6b56a", 1),
        ("1044159039-33be8", 1),
        ("1044214673-960f", 1),
        ("1044216699-0f1a0", 1),
        ("2298241-e7e0", 2),
        ("6229746-6d65", 3),
        ("6541923-08a9", 3),
        ("6542117-19888", 3)
      )
    )

    assert(geobaseIds.size == 11)
    checkIndexData(
      geobaseIds,
      Seq(
        ("1037186558-e06d8", 213),
        ("1042409964-038a", 213),
        ("1043026846-83484c", 213),
        ("1043045004-977b3", 213),
        ("1043140898-81de", 967),
        ("1043211458-fbbd39", 213),
        ("1043270830-6b56a", 213),
        ("1044159039-33be8", 213),
        ("1044214673-960f", 10738),
        ("1044216699-0f1a0", 21642),
        ("6542117-19888", 2)
      )
    )

    assert(markModels.size == 10) // будут отсутсвовать все что прилетело из мото/комтранс
    checkIndexData(
      markModels,
      Seq(
        ("1037186558-e06d8", "VOLKSWAGEN#MULTIVAN"),
        ("1042409964-038a", "AUDI#Q5"),
        ("1043026846-83484c", "TOYOTA#ALPHARD"),
        ("1043045004-977b3", "MERCEDES#GL_KLASSE"),
        ("1043140898-81de", "BMW#3ER"),
        ("1043211458-fbbd39", "LAND_ROVER#RANGE_ROVER_SPORT"),
        ("1043270830-6b56a", "HYUNDAI#IX35"),
        ("1044159039-33be8", "PEUGEOT#PARTNER"),
        ("1044214673-960f", "AUDI#A6"),
        ("1044216699-0f1a0", "MAZDA#DEMIO")
//      ("2298241-e7e0", "HONDA#TRX_500"),
//      ("6229746-6d65", "PEUGEOT#BOXER"),
//      ("6541923-08a9", "GAZ#GAZEL_3302"),
//      ("6542117-19888", "UAZ#3909")
      )
    )

    assert(regionIds.size == 11)
    checkIndexData(
      regionIds,
      Seq(
        ("1037186558-e06d8", 1),
        ("1042409964-038a", 1),
        ("1043026846-83484c", 1),
        ("1043045004-977b3", 1),
        ("1043140898-81de", 10693),
        ("1043211458-fbbd39", 1),
        ("1043270830-6b56a", 1),
        ("1044159039-33be8", 1),
        ("1044214673-960f", 1),
        ("1044216699-0f1a0", 1),
        ("6542117-19888", 10174)
      )
    )

    assert(truckCategories.size == 3)
    checkIndexData(
      truckCategories,
      Seq(
        ("6229746-6d65", 31),
        ("6541923-08a9", 31),
        ("6542117-19888", 31)
      )
    )

    assert(motoCategories.size == 1)
    checkIndexData(
      motoCategories,
      Seq(
        ("2298241-e7e0", 3)
      )
    )

    assert(extStatuses.size == 14)
    checkIndexData(
      extStatuses,
      Seq(
        ("1037186558-e06d8", 40),
        ("1042409964-038a", 0),
        ("1043026846-83484c", 0),
        ("1043045004-977b3", 10),
        ("1043140898-81de", 40),
        ("1043211458-fbbd39", 0),
        ("1043270830-6b56a", 10),
        ("1044159039-33be8", 40),
        ("1044214673-960f", 10),
        ("1044216699-0f1a0", 0),
        ("2298241-e7e0", 110),
        ("6229746-6d65", 0),
        ("6541923-08a9", 0),
        ("6542117-19888", 0)
      )
    )

    // tags
    assert(tags.size == 1)
    checkIndexData(
      tags,
      Seq(
        ("6541923-08a9", "tag1"),
        ("6541923-08a9", "tag2"),
        ("6541923-08a9", "tag3")
      )
    )

    assert(names.size == 9)
    checkIndexData(
      names,
      Seq(
        ("1037186558-e06d8", "Volkswagen Multivan"),
        ("1042409964-038a", "Audi Q5"),
        ("1043026846-83484c", "Toyota Alphard"),
        ("1043045004-977b3", "Mercedes-Benz GL-klasse"),
        ("1043140898-81de", "BMW 3 серия"),
        ("1043211458-fbbd39", "Land Rover Range Rover Sport"),
        ("1043270830-6b56a", "Hyundai ix35"),
        ("1044159039-33be8", "Peugeot Partner"),
        ("1044216699-0f1a0", "Mazda Demio")
//      ("2298241-e7e0", "Honda TRX 500 (Foreman)"),
//      ("6229746-6d65", "Peugeot Boxer"),
//      ("6541923-08a9", "ГАЗ ГАЗель (3302)"),
//      ("6542117-19888", "УАЗ 3909")
      )
    )

    assert(prices.size == 14)
    checkIndexData(
      prices,
      Seq(
        ("1037186558-e06d8", 1650000.0),
        ("1042409964-038a", 1015000.0),
        ("1043026846-83484c", 5000000.0),
        ("1043045004-977b3", 1850000.0),
        ("1043140898-81de", 680000.0),
        ("1043211458-fbbd39", 633333.0),
        ("1043270830-6b56a", 875000.0),
        ("1044159039-33be8", 1000000000.0), // миллиард успешно индексируется
        ("1044214673-960f", 200000.0),
        ("1044216699-0f1a0", 89000.0),
        ("2298241-e7e0", 500000.0),
        ("6229746-6d65", 350000.0),
        ("6541923-08a9", 270000.0),
        ("6542117-19888", 320000.0)
      )
    )

    assert(years.size == 14)
    checkIndexData(
      years,
      Seq(
        ("1037186558-e06d8", 2011),
        ("1042409964-038a", 2011),
        ("1043026846-83484c", 2016),
        ("1043045004-977b3", 2011),
        ("1043140898-81de", 2007),
        ("1043211458-fbbd39", 2007),
        ("1043270830-6b56a", 2011),
        ("1044159039-33be8", 2012),
        ("1044214673-960f", 1998),
        ("1044216699-0f1a0", 1999),
        ("2298241-e7e0", 2009),
        ("6229746-6d65", 2010),
        ("6541923-08a9", 2010),
        ("6542117-19888", 2012)
      )
    )

    assert(vinSuggest.size == 5)
    assert(vinSuggest("1042409964-038a")._1 == "ac_10086")
    assert(vinSuggest("1042409964-038a")._2 == "WA1LFBFP1BA109828")
    assert(vinSuggest("1042409964-038a")._3 == 1)
    assert(vinSuggest("1042409964-038a")._4 == 1)

    assert(offerHash.size == 14)
    offerHash.foreach(h => {
      assert(h._2.length == 1)
      assert(h._2.head.nonEmpty)
    })

    assert(offerSection.size == 14)
    assert(offerSection("1044159039-33be8").head == "true")
    assert(offerSection("1043026846-83484c").head == "false")

    assert(offerCreateDate.size == 14)
    assert(offerCreateDate("1042409964-038a").head == "1470016087000")

    assert(callsAuctionBid.size == 3)
    checkIndexData(
      callsAuctionBid,
      Seq(
        "1044216699-0f1a0" -> 20000L,
        "1043045004-977b3" -> 30000L,
        "1043211458-fbbd39" -> 25000L
      )
    )
  }

  test("findByRef") {
    // проверим findByRef, что проверяется непосредственное равенство владельца объявления указанному в OfferRef,
    // а не просто вычисляется шард
    val offer1 = components.offerVosDao.findByRef(OfferRef("ac_10086", "1042409964-038a"))(Traced.empty)
    assert(offer1.nonEmpty)
    assert(components.mySql.getShardIdByUserId("ac_10086") == components.mySql.getShardIdByUserId("ac_10087"))
    val offer2 = components.offerVosDao.findByRef(OfferRef("ac_10087", "1042409964-038a"))(Traced.empty)
    assert(offer2.isEmpty)
  }

  test("userLock") {
    val userRef = UserRef.refAutoruClient(10087)
    val userId: String = userRef.toPlain
    val shard = components.mySql.getShardByUserId(userId)
    val optUserId1: Option[String] = shard.master.jdbc
      .query("select k_ref from t_users where k_ref = ?", SimpleRowMapper(rs => rs.getString(1)), userId)
      .asScala
      .headOption
    assert(optUserId1.isEmpty)
    var i = 0
    val t1HaveLock = new AtomicBoolean()

    def longOperationWhichStartedEarlier: Thread = {
      val thread: Thread = new Thread(() => {
        components.offerVosDao.userLock(userRef) {
          t1HaveLock.set(true)
          Thread.sleep(1000)
          i += 10
        }
      })
      thread.start()
      thread
    }

    def fastOperationWhichStartedLater: Thread = {
      val thread: Thread = new Thread(() => {
        components.offerVosDao.userLock(userRef) {
          i *= 2
        }
      })
      thread.start()
      thread
    }

    val t1 = longOperationWhichStartedEarlier
    while (!t1HaveLock.get()) {
      Thread.sleep(10)
    }
    val t2 = fastOperationWhichStartedLater
    t1.join()
    t2.join()
    assert(i == 20)
    // проверим, что в t_users существует запись для пользователя ac_10087
    val optUserId2: Option[String] = shard.master.jdbc
      .query("select k_ref from t_users where k_ref = ?", SimpleRowMapper(rs => rs.getString(1)), userId)
      .asScala
      .headOption
    assert(optUserId2.nonEmpty)
  }

  private def loadIndexData[T](tableName: String, extract: AnyRef => T): Map[String, Seq[T]] = {
    components.mySql.shards
      .flatMap(shard => {
        shard.master.jdbc
          .query(s"select k_id, v_value from $tableName", new SimpleRowMapper[(String, T)](rs => {
            (rs.getString("k_id"), extract(rs.getObject("v_value")))
          }))
          .asScala
          .toSeq
      })
      .groupBy(_._1)
      .view
      .mapValues(v => v.map(_._2))
      .toMap
  }

  private def loadVinSuggestIndexData(): Map[String, (String, String, Int, Int)] = {
    val rm = SimpleRowMapper(rs => {
      rs.getString("k_id") -> (rs.getString("user_ref"), rs.getString("vin"), rs.getInt("section"),
      rs.getInt("category"))
    })

    components.mySql.shards
      .flatMap(shard =>
        {
          shard.master.jdbc.query(
            s"select k_id, user_ref, vin, section, category from " +
              s"${AutoruOfferDaoImpl.UserVinsTable}",
            rm
          )
        }.asScala
      )
      .toMap
  }

  private def checkIndexData[T](data: Map[String, Seq[T]], mustBe: Seq[(String, T)]): Unit = {
    mustBe.foreach {
      case (kId, vValue) => assert(data.getOrElse(kId, Seq.empty).contains(vValue), s"For offer $kId")
    }
  }

  test("checkMigratedOfferNotCorrupted") {
    // проверим, что в ходе миграции проверяется корректность оффера
    val sale = getSaleByIdFromDb(1043270830L)
    components.offerVosDao.migrateAndSave(Seq(sale))((sale, optOffer) => {
      components.carOfferConverter.convertStrict(sale, optOffer).converted
    })(Traced.empty)

    // внезапно меняем пользователя

    {
      val numErrors = components.offerVosDao.migrateAndSave(Seq(sale.copy(userId = 111)))((sale, optOffer) => {
        assert(optOffer.nonEmpty)
        components.carOfferConverter.convertStrict(sale, optOffer).converted
      })(Traced.empty)
      assert(numErrors == 1)
    }
  }

  test("update active services") {
    // создаем объявление с активным сервисом
    val now = DateTime.now()
    val category = "cars"
    val categoryEnum = testFormGenerator.categoryByString(category)
    val formInfo: FormInfo = testFormGenerator.createForm(category, TestFormParams[CarFormInfo]())
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, categoryEnum, formInfo.form, now.getMillis, ad, None, FormWriteParams(), "test", None)(
        Traced.empty
      )
      .right
      .value
      .offer
    val offerId: OfferID = offer.getOfferID
    val autoruOfferId = AutoruOfferID.parse(offerId)
    components.offersWriter.addServices(
      offerId,
      Some(userRef),
      Some(categoryEnum),
      None,
      Seq(PaidService.newBuilder().setIsActive(true).setService("all_sale_fresh").build()),
      allowDealers = true
    )(Traced.empty)

    def checkService(contains: Boolean): Unit = {
      val offerFromDb = components.getOfferDao().findById(offerId, includeRemoved = true)(Traced.empty).value
      components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)
      val services: Map[String, Seq[String]] = loadIndexData("t_offers_active_service", v_value => v_value.toString)
      val offerServices = services.getOrElse(offerId, Seq.empty)
      if (contains) assert(offerServices.contains("FRESH"))
      else assert(offerServices.isEmpty)
    }

    // убеждаемся, что в индексной таблице оказался активный сервис
    checkService(contains = true)

    // удаляем услуги
    components.getOfferDao().updateFunc(autoruOfferId)(Traced.empty) { offer =>
      OfferUpdate.visitSoon(offer.updated(_.getOfferAutoruBuilder.clearServices))
    }

    // проверяем, что статус сервиса сменился в индексной таблице (точнее, пропал из нее)
    checkService(contains = false)

    // добавляем сервис
    components.offersWriter.addServices(
      offerId,
      Some(userRef),
      Some(categoryEnum),
      None,
      Seq(PaidService.newBuilder().setIsActive(true).setService("all_sale_fresh").build()),
      allowDealers = true
    )(Traced.empty)
    checkService(contains = true)
  }

  test("push badges") {
    val now = DateTime.now()
    val category = "cars"
    val categoryEnum = testFormGenerator.categoryByString(category)
    val formInfo: FormInfo = testFormGenerator.createForm(category, TestFormParams[CarFormInfo](generateBadges = false))
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val offer = components.formWriter
      .createOffer(userRef, categoryEnum, formInfo.form, now.getMillis, ad, None, FormWriteParams(), "test", None)(
        Traced.empty
      )
      .right
      .value
      .offer
    val offerId = offer.getOfferID

    def badgeService(isActive: Boolean, createDate: Long = System.currentTimeMillis()): PaidService = {
      val b = PaidService
        .newBuilder()
        .setService("all_sale_badge")
        .setIsActive(isActive)
        .setCreateDate(createDate)
        .setExpireDate(createDate + 7.days.toMillis)
      b.build()
    }

    def addService(service: PaidService): Unit = addServices(service)

    def addServices(services: PaidService*): Unit = {
      components.offersWriter.addServices(
        offerId,
        Some(userRef),
        Some(categoryEnum),
        None,
        services,
        allowDealers = true
      )(Traced.empty)
    }

    def fetchBadgesService(): AutoruOffer.PaidService = {
      components.offersReader
        .findOffer(None, offerId, None, includeRemoved = true, operateOnMaster = true)(Traced.empty)
        .map(
          _.getOfferAutoru.getServicesList.asScala
            .filter(_.getServiceType == ServiceType.BADGE)
            .loneElement
        )
        .value
    }
    addService(badgeService(isActive = true, createDate = 123L))

    val badge1 = fetchBadgesService()
    assert(badge1.getIsActive)
    assert(badge1.getCreated == 123L)
    assert(badge1.getServiceType == ServiceType.BADGE)

    addService(badgeService(isActive = false, createDate = 123L))
    val badge2 = fetchBadgesService()
    assert(!badge2.getIsActive)
    assert(badge2.getCreated == 123L)
    assert(badge2.getServiceType == ServiceType.BADGE)

    intercept[IllegalArgumentException] {
      addServices(
        badgeService(isActive = true, createDate = 123L),
        badgeService(isActive = true, createDate = 124L)
      )
    }
  }

  test("find created by feedprocessor offers") {
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createDealer().setUserRef("ac_1").build()
    val offerBuilder = TestUtils.createOffer(dealer = true)
    val offer = dao.create(user, offerBuilder.build())(Traced.empty)
    val feedprocessorId = FeedprocessorHashUtils.getFeedprocessorHash(offer)
    val userRef = UserRef.from(offer.getUserRef)

    val found =
      dao.findAllByFeedprocessorId(userRef, Seq(feedprocessorId), Seq(CompositeStatus.CS_BANNED))(Traced.empty)
    found.toList match {
      case (`feedprocessorId`, Seq(offerResult)) :: Nil =>
        assert(offerResult.getOfferID == offer.getOfferID)
    }
  }

  test("find created by feedprocessor and recently archived offers") {
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createDealer().setUserRef("ac_2").build()
    val offerBuilder = TestUtils.createOffer(dealer = true)
    val offer = dao.create(user, offerBuilder.build())(Traced.empty)
    val feedprocessorId = FeedprocessorHashUtils.getFeedprocessorHash(offer)
    val userRef = UserRef.from(offer.getUserRef)

    val found0 =
      dao.findAllByFeedprocessorId(userRef, Seq(feedprocessorId), Seq(CompositeStatus.CS_BANNED))(Traced.empty)
    found0.toList match {
      case (`feedprocessorId`, Seq(offerResult)) :: Nil =>
        assert(offerResult.getOfferID == offer.getOfferID)
    }

    dao.setArchiveBatch(
      userRef,
      Seq(AutoruOfferID.parse(offer.getOfferID)),
      archive = true,
      fromFeedprocessor = true,
      comment = "Test"
    )(Traced.empty)

    val found =
      dao.findAllByFeedprocessorId(userRef, Seq(feedprocessorId), Seq(CompositeStatus.CS_BANNED))(Traced.empty)
    found.toList match {
      case (`feedprocessorId`, Seq(offerResult)) :: Nil =>
        assert(offerResult.getOfferID == offer.getOfferID)
    }
  }

  test("don't find too old archived feedprocessor's offers") {
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createDealer().setUserRef("ac_3").build()
    val offerBuilder = TestUtils.createOffer(dealer = true)
    val offer = dao.create(user, offerBuilder.build())(Traced.empty)
    val feedprocessorId = FeedprocessorHashUtils.getFeedprocessorHash(offer)
    val userRef = UserRef.from(offer.getUserRef)

    dao.setArchive(
      AutoruOfferID.parse(offer.getOfferID),
      Some(userRef),
      Some(offer.getOfferAutoru.getCategory),
      archive = true,
      comment = "Test",
      None
    )(Traced.empty)

    val found =
      dao.findAllByFeedprocessorId(userRef, Seq(feedprocessorId), Seq(CompositeStatus.CS_BANNED))(Traced.empty)
    assert(found.isEmpty)
  }

  test("don't find removed offers") {
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createDealer().setUserRef("ac_4").build()
    val offerBuilder = TestUtils.createOffer(dealer = true)
    val offer = dao.create(user, offerBuilder.build())(Traced.empty)
    val feedprocessorId = FeedprocessorHashUtils.getFeedprocessorHash(offer)
    val userRef = UserRef.from(offer.getUserRef)

    dao.setArchive(
      AutoruOfferID.parse(offer.getOfferID),
      Some(userRef),
      Some(offer.getOfferAutoru.getCategory),
      archive = true,
      comment = "Test",
      None
    )(Traced.empty)

    val found =
      dao.findAllByFeedprocessorId(userRef, Seq(feedprocessorId), Seq(CompositeStatus.CS_BANNED))(Traced.empty)
    assert(found.isEmpty)
  }

  test("can find banned offers") {
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createDealer().setUserRef("ac_4").build()
    val offerBuilder = TestUtils.createOffer(dealer = true)
    val offer = dao.create(user, offerBuilder.build())(Traced.empty)
    val feedprocessorId = FeedprocessorHashUtils.getFeedprocessorHash(offer)
    val userRef = UserRef.from(offer.getUserRef)

    banOffer(offer.getOfferID)

    val found = dao.findAllByFeedprocessorId(userRef, Seq(feedprocessorId), Seq())(Traced.empty)
    found.toList match {
      case (`feedprocessorId`, Seq(offerResult)) :: Nil =>
        assert(offerResult.getOfferID == offer.getOfferID)
    }
  }

  test("can find offer by changed but equal VIN") {
    val vin1 = "Xw0ZzZ4L1EG001019"
    val vin2 = "xW0zZz4l1eg001019"

    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createDealer().setUserRef("ac_4").build()
    val offerBuilder1 = TestUtils.createOffer(dealer = true)
    offerBuilder1.getOfferAutoruBuilder.getDocumentsBuilder.setVin(vin1)
    val offer1 = dao.create(user, offerBuilder1.build())(Traced.empty)
    val offerBuilder2 = offer1.toBuilder
    offerBuilder2.getOfferAutoruBuilder.getDocumentsBuilder.setVin(vin2)
    val offer2 = offerBuilder2.build()
    val feedprocessorId = FeedprocessorHashUtils.getFeedprocessorHash(offer2)
    assert(FeedprocessorHashUtils.getFeedprocessorHash(offer2) == feedprocessorId)
    val userRef = UserRef.from(offer1.getUserRef)

    val found = dao.findAllByFeedprocessorId(userRef, Seq(feedprocessorId), Seq())(Traced.empty)
    found.toList match {
      case (`feedprocessorId`, Seq(offerResult)) :: Nil =>
        assert(offerResult.getOfferID == offer1.getOfferID)
    }
  }

  test("find offer after color change") {
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createDealer().setUserRef("ac_4").build()
    val offerBuilder1 = TestUtils.createOffer(dealer = true)
    offerBuilder1.getOfferAutoruBuilder.setColorHex("C49648")
    val offer1 = dao.create(user, offerBuilder1.build())(Traced.empty)
    val offerBuilder2 = offer1.toBuilder
    offerBuilder2.getOfferAutoruBuilder.setColorHex("FAFBFB")
    val offer2 = offerBuilder2.build()
    val feedprocessorId = FeedprocessorHashUtils.getFeedprocessorHash(offer2)
    assert(FeedprocessorHashUtils.getFeedprocessorHash(offer2) == feedprocessorId)
    val userRef = UserRef.from(offer1.getUserRef)

    val found = dao.findAllByFeedprocessorId(userRef, Seq(feedprocessorId), Seq())(Traced.empty)
    found.toList match {
      case (`feedprocessorId`, offerResults) :: Nil =>
        assert(offerResults.map(_.getOfferID).contains(offer1.getOfferID))
    }
  }

  test("find offer after tech_param_id change") {
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createDealer().setUserRef("ac_4").build()
    val offerBuilder1 = TestUtils.createOffer(dealer = true)
    offerBuilder1.getOfferAutoruBuilder.getCarInfoBuilder.setTechParamId(123)
    val offer1 = dao.create(user, offerBuilder1.build())(Traced.empty)
    val offerBuilder2 = offer1.toBuilder
    offerBuilder2.getOfferAutoruBuilder.getCarInfoBuilder.setTechParamId(12345)
    val offer2 = offerBuilder2.build()
    val feedprocessorId = FeedprocessorHashUtils.getFeedprocessorHash(offer2)
    assert(FeedprocessorHashUtils.getFeedprocessorHash(offer2) == feedprocessorId)
    val userRef = UserRef.from(offer1.getUserRef)

    val found = dao.findAllByFeedprocessorId(userRef, Seq(feedprocessorId), Seq())(Traced.empty)
    found.toList match {
      case (`feedprocessorId`, offerResults) :: Nil =>
        assert(offerResults.map(_.getOfferID).contains(offer1.getOfferID))
    }
  }

  test("set archive") {
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createUser().build()
    val offer = TestUtils.createOffer().clearFlag().build()

    val createdOffer = dao.create(user, offer, "")(Traced.empty)

    val findAlreadyCreated =
      dao.findById(createdOffer.getOfferID)(Traced.empty)
    val id = AutoruOfferID.parse(createdOffer.getOfferID)
    val userRef = Some(UserRef.from(createdOffer.getUserRef))

    val resultArchiveTrue =
      dao.setArchive(id, userRef, Some(offer.getOfferAutoru.getCategory), archive = true, "", None)(Traced.empty)
    val findAfterArchiveTrue =
      dao.findById(createdOffer.getOfferID, includeRemoved = true)(Traced.empty)
    val resultArchiveFalse =
      dao.setArchive(id, userRef, Some(offer.getOfferAutoru.getCategory), archive = false, "", None)(Traced.empty)
    val findAfterArchiveFalse =
      dao.findById(createdOffer.getOfferID, includeRemoved = true)(Traced.empty)

    assert(createdOffer.getFlagCount == 0)
    assert(findAlreadyCreated.get.getFlagCount == 0)
    assert(resultArchiveTrue)
    assert(findAfterArchiveTrue.get.getFlagCount == 1)
    assert(findAfterArchiveTrue.get.getFlagList.get(0) == OfferFlag.OF_DELETED)
    assert(resultArchiveFalse)
    assert(findAfterArchiveFalse.get.getFlagCount == 1)
    assert(findAfterArchiveFalse.get.getFlagList.get(0) == OfferFlag.OF_NEED_ACTIVATION)
  }

  test("set archive multiposting") {
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createDealer().build()
    val multipostingOffer = TestUtils.createOffer(dealer = true, withMultiposting = true).clearFlag().build()
    val nonMultipostingOffer = TestUtils.createOffer(dealer = true, withMultiposting = false).clearFlag().build()

    val createdOfferMultiposting = dao.create(user, multipostingOffer, "")(Traced.empty)
    val createdOfferNonMultiposting = dao.create(user, nonMultipostingOffer, "")(Traced.empty)

    val multipostingId = AutoruOfferID.parse(createdOfferMultiposting.getOfferID)
    val nonMultipostingId = AutoruOfferID.parse(createdOfferNonMultiposting.getOfferID)
    val userRef = Some(UserRef.from(createdOfferMultiposting.getUserRef))

    dao.setArchive(
      multipostingId,
      userRef,
      Some(multipostingOffer.getOfferAutoru.getCategory),
      archive = true,
      "",
      None
    )(Traced.empty)
    val multipostingFindAfterArchiveTrue =
      dao.findById(createdOfferMultiposting.getOfferID, includeRemoved = true)(Traced.empty)
    dao.setArchive(
      multipostingId,
      userRef,
      Some(multipostingOffer.getOfferAutoru.getCategory),
      archive = false,
      "",
      None
    )(Traced.empty)
    val multipostingFindAfterArchiveFalse =
      dao.findById(createdOfferMultiposting.getOfferID, includeRemoved = true)(Traced.empty)

    dao.setArchive(
      nonMultipostingId,
      userRef,
      Some(nonMultipostingOffer.getOfferAutoru.getCategory),
      archive = true,
      "",
      None
    )(Traced.empty)
    val nonMultipostingFindAfterArchiveTrue =
      dao.findById(createdOfferNonMultiposting.getOfferID, includeRemoved = true)(Traced.empty)
    dao.setArchive(
      nonMultipostingId,
      userRef,
      Some(nonMultipostingOffer.getOfferAutoru.getCategory),
      archive = false,
      "",
      None
    )(Traced.empty)
    val nonMultipostingFindAfterArchiveFalse =
      dao.findById(createdOfferNonMultiposting.getOfferID, includeRemoved = true)(Traced.empty)

    assert(multipostingFindAfterArchiveTrue.get.getMultiposting.getStatus == CompositeStatus.CS_REMOVED)
    assert(multipostingFindAfterArchiveFalse.get.getMultiposting.getStatus == CompositeStatus.CS_ACTIVE)
    assert(nonMultipostingFindAfterArchiveTrue.get.getMultiposting.getStatus == CompositeStatus.CS_UNKNOWN)
    assert(nonMultipostingFindAfterArchiveFalse.get.getMultiposting.getStatus == CompositeStatus.CS_UNKNOWN)
  }

  test("set archive batch") {
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createUser().build()
    val offers = (1 to 2).map(_ => TestUtils.createOffer(withMultiposting = true, dealer = true).clearFlag().build())
    val createdOffers = offers.map(offer => dao.create(user, offer, "")(Traced.empty))

    val findAlreadyCreated = createdOffers.map(offer => dao.findById(offer.getOfferID)(Traced.empty))

    val ids = createdOffers.map(offer => AutoruOfferID.parse(offer.getOfferID))
    val userRef = UserRef.from(createdOffers.head.getUserRef)

    dao.setArchiveBatch(userRef, ids, archive = true, fromFeedprocessor = true, comment = "")(Traced.empty)
    val findAfterArchiveTrue =
      createdOffers.map(offer => dao.findById(offer.getOfferID, includeRemoved = true)(Traced.empty))

    dao.setArchiveBatch(userRef, ids, archive = false, fromFeedprocessor = true, comment = "")(Traced.empty)
    val findAfterArchiveFalse =
      createdOffers.map(offer => dao.findById(offer.getOfferID, includeRemoved = true)(Traced.empty))

    assert(createdOffers.forall(_.getFlagCount == 0))
    assert(findAlreadyCreated.forall(_.get.getFlagCount == 0))
    assert(findAfterArchiveTrue.forall(_.get.getFlagCount == 1))
    assert(findAfterArchiveTrue.forall(_.get.getOfferAutoru.getFeedprocessorRecoverable))
    assert(findAfterArchiveTrue.forall(_.get.getFlagList.get(0) == OfferFlag.OF_DELETED))
    assert(findAfterArchiveTrue.forall(_.get.getMultiposting.getStatus == CompositeStatus.CS_REMOVED))
    assert(findAfterArchiveFalse.forall(_.get.getFlagCount == 0))
    assert(findAfterArchiveFalse.forall(!_.get.getOfferAutoru.getFeedprocessorRecoverable))
  }

  test("set archive multiposting batch") {

    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createDealer().build()
    val multipostingOffer = TestUtils.createOffer(dealer = true, withMultiposting = true).clearFlag().build()
    val nonMultipostingOffer = TestUtils.createOffer(dealer = true, withMultiposting = false).clearFlag().build()

    val createdOfferMultiposting = dao.create(user, multipostingOffer, "")(Traced.empty)
    val createdOfferNonMultiposting = dao.create(user, nonMultipostingOffer, "")(Traced.empty)

    val multipostingId = AutoruOfferID.parse(createdOfferMultiposting.getOfferID)
    val nonMultipostingId = AutoruOfferID.parse(createdOfferNonMultiposting.getOfferID)
    val userRef = UserRef.from(createdOfferMultiposting.getUserRef)

    dao.setArchiveBatch(
      userRef,
      Seq(multipostingId),
      archive = true,
      fromFeedprocessor = true,
      comment = ""
    )(Traced.empty)
    val multipostingFindAfterArchiveTrue =
      dao.findById(createdOfferMultiposting.getOfferID, includeRemoved = true)(Traced.empty)
    dao.setArchiveBatch(
      userRef,
      Seq(multipostingId),
      archive = false,
      fromFeedprocessor = true,
      ""
    )(Traced.empty)
    val multipostingFindAfterArchiveFalse =
      dao.findById(createdOfferMultiposting.getOfferID, includeRemoved = true)(Traced.empty)

    dao.setArchiveBatch(
      userRef,
      Seq(nonMultipostingId),
      archive = true,
      fromFeedprocessor = true,
      ""
    )(Traced.empty)
    val nonMultipostingFindAfterArchiveTrue =
      dao.findById(createdOfferNonMultiposting.getOfferID, includeRemoved = true)(Traced.empty)
    dao.setArchiveBatch(
      userRef,
      Seq(nonMultipostingId),
      archive = false,
      fromFeedprocessor = true,
      ""
    )(Traced.empty)
    val nonMultipostingFindAfterArchiveFalse =
      dao.findById(createdOfferNonMultiposting.getOfferID, includeRemoved = true)(Traced.empty)

    assert(multipostingFindAfterArchiveTrue.get.getMultiposting.getStatus == CompositeStatus.CS_REMOVED)
    assert(multipostingFindAfterArchiveFalse.get.getMultiposting.getStatus == CompositeStatus.CS_ACTIVE)
    assert(nonMultipostingFindAfterArchiveTrue.get.getMultiposting.getStatus == CompositeStatus.CS_UNKNOWN)
    assert(nonMultipostingFindAfterArchiveFalse.get.getMultiposting.getStatus == CompositeStatus.CS_UNKNOWN)
  }

  test("return 0 on empty offer ids list in countUserOfferIds") {
    assert(components.offerVosDao.countUserOfferIds(UserRef(1), Nil)(Traced.empty) == 0)
  }

  test("findNonModified") {
    val now = Instant.now()
    val user = TestUtils.createDealer().setUserRef("ac_7").build()
    val userRef = UserRef.refAutoruClient(7)
    val offerTemplate = TestUtils.createOffer(dealer = true)
    offerTemplate.clearFlag().getOfferAutoruBuilder.setSection(Section.USED)
    // create outside of feed
    components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)
    offerTemplate.getOfferAutoruBuilder.getSourceInfoBuilder
      .setFeedprocessorFeedId("doesn't matter")
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(now.minusSeconds(10).toEpochMilli)
    val nonModified = components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)
    // create removed
    offerTemplate.clearFlag().addFlag(OfferFlag.OF_DELETED)
    components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)
    // create with wrong status
    offerTemplate.clearFlag().addFlag(OfferFlag.OF_BANNED)
    components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)
    offerTemplate.clearFlag()
    // create from another feed
    val offerSectionNew = offerTemplate.build().toBuilder
    offerSectionNew
      .clearFlag()
      .getOfferAutoruBuilder
      .setSection(Section.NEW)
      .getSourceInfoBuilder
      .setFeedprocessorFeedId("doesn't matter 2")
    components.offerVosDao.create(user, offerSectionNew.build())(Traced.empty)
    // create modified
    offerTemplate.getOfferAutoruBuilder.getSourceInfoBuilder.setFeedprocessorTaskId(81)
    components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)
    offerTemplate.getOfferAutoruBuilder.getSourceInfoBuilder
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(now.toEpochMilli)
    val tooFresh = components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)

    val nonModified1 = components.offerVosDao.findNonModifiedByFeedprocessor(
      userRef,
      "CARS/USED/7",
      81,
      None,
      Seq(CompositeStatus.CS_BANNED)
    )(Traced.empty)
    val nonModified2 = components.offerVosDao.findNonModifiedByFeedprocessor(
      userRef,
      "CARS/USED/7",
      81,
      Some(now),
      Seq(CompositeStatus.CS_BANNED)
    )(Traced.empty)

    assert(nonModified1.toSet == Set(nonModified.getOfferID, tooFresh.getOfferID))
    assert(nonModified2.toSet == Set(nonModified.getOfferID))
  }

  test("activate old dealer's offer") {
    val user = TestUtils.createDealer().setUserRef("ac_7").build()
    val userRef = UserRef.refAutoruClient(7)
    val offerTemplate = TestUtils.createOffer(dealer = true)
    val autoruBuilder = offerTemplate.getOfferAutoruBuilder
    autoruBuilder.getRecallInfoBuilder.setRecallTimestamp(getNow - 200.days.toMillis)
    autoruBuilder.getSellerBuilder.addPhoneBuilder().setNumber("1234")
    val initial = components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)
    val activationStatus = components.offerVosDao.activate(
      AutoruOfferID.parse(initial.getOfferID),
      Some(userRef),
      None,
      needActivation = false,
      "test comment"
    )(Traced.empty)
    assert(activationStatus == ActivationSuccess(CS_ACTIVE))
  }

  test("findAllNonModified for cars") {
    val now = Instant.now()
    val user = TestUtils.createDealer().setUserRef("ac_8").build()
    val user2 = TestUtils.createDealer().setUserRef("ac_7").build()
    val userRef = UserRef.refAutoruClient(8)
    val offerTemplate = TestUtils.createOffer(dealer = true).clearFlag().setUser(user).setUserRef(user.getUserRef)
    val offerTemplateTruck = TestUtils
      .createOffer(dealer = true, category = Category.TRUCKS)
      .clearFlag()
      .setUser(user)
      .setUserRef(user.getUserRef)
    offerTemplate.getOfferAutoruBuilder.setSection(Section.USED)
    offerTemplateTruck.getOfferAutoruBuilder.setSection(Section.USED)
    val feedOfferTemplate = offerTemplate.build().toBuilder()
    feedOfferTemplate
      .clearFlag()
      .getOfferAutoruBuilder
      .getSourceInfoBuilder
      .setFeedprocessorFeedId("doesn't matter")
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(now.minusSeconds(10).toEpochMilli)

    val outsideFeed = components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)

    // create outside of feed with another category
    components.offerVosDao.create(user, offerTemplateTruck.build())(Traced.empty)

    val outsideFeedAnotherSectionOffer = offerTemplate.build().toBuilder
    outsideFeedAnotherSectionOffer.getOfferAutoruBuilder.setSection(Section.NEW)
    components.offerVosDao.create(user, outsideFeedAnotherSectionOffer.build())(Traced.empty)

    val offerUser2 = offerTemplate.build().toBuilder
    offerUser2.setUser(user2).setUserRef(user2.getUserRef)
    components.offerVosDao.create(user2, offerUser2.build())(Traced.empty)

    val nonModified = components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    // create removed
    feedOfferTemplate.clearFlag().addFlag(OfferFlag.OF_DELETED)
    components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)
    feedOfferTemplate.clearFlag()

    // create with wrong status
    feedOfferTemplate.addFlag(OfferFlag.OF_BANNED)
    components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)
    feedOfferTemplate.clearFlag()

    // create draft
    offerTemplate.addFlag(OfferFlag.OF_DRAFT)
    components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)
    offerTemplate.clearFlag()

    val offerAnotherFeed = feedOfferTemplate.build().toBuilder
    offerAnotherFeed.getOfferAutoruBuilder
      .setSection(Section.NEW)
      .getSourceInfoBuilder
      .setFeedprocessorFeedId("doesn't matter 2")
    components.offerVosDao.create(user, offerAnotherFeed.build())(Traced.empty)

    // create modified
    feedOfferTemplate.getOfferAutoruBuilder.getSourceInfoBuilder.setFeedprocessorTaskId(81)
    components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    feedOfferTemplate.getOfferAutoruBuilder.getSourceInfoBuilder
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(now.toEpochMilli)
    val nonModifiedButFresh = components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    val nonModified1 = components.offerVosDao.findAllNonModified(
      userRef,
      "CARS/USED/8",
      81,
      updatedAfterTimestamp = None,
      notAllowedStatuses = Seq(CompositeStatus.CS_BANNED)
    )(Traced.empty)

    val nonModified2 =
      components.offerVosDao.findAllNonModified(
        userRef,
        "CARS/USED/8",
        81,
        updatedAfterTimestamp = Some(now),
        notAllowedStatuses = Seq(CompositeStatus.CS_BANNED)
      )(Traced.empty)

    assert(nonModified1.toSet == Set(nonModified.getOfferID, nonModifiedButFresh.getOfferID, outsideFeed.getOfferID))
    assert(nonModified2.toSet == Set(nonModified.getOfferID, outsideFeed.getOfferID))
  }

  test("findAllNonModified for trucks") {
    val now = Instant.now()
    val user = TestUtils.createDealer().setUserRef("ac_8").build()
    val user2 = TestUtils.createDealer().setUserRef("ac_7").build()
    val userRef = UserRef.refAutoruClient(8)
    val offerTemplate = TestUtils
      .createOffer(dealer = true, category = Category.TRUCKS)
      .clearFlag()
      .setUser(user)
      .setUserRef(user.getUserRef)
    offerTemplate.getOfferAutoruBuilder
      .setSection(Section.USED)
      .getTruckInfoBuilder
      .setAutoCategory(TruckCategory.TRUCK_CAT_BUS)
    val offerTemplateCar = TestUtils
      .createOffer(dealer = true, category = Category.CARS)
      .clearFlag()
      .setUser(user)
      .setUserRef(user.getUserRef)
    offerTemplateCar.getOfferAutoruBuilder.setSection(Section.USED)
    val feedOfferTemplate = offerTemplate.build().toBuilder()
    feedOfferTemplate
      .clearFlag()
      .getOfferAutoruBuilder
      .getSourceInfoBuilder
      .setFeedprocessorFeedId("doesn't matter")
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(now.minusSeconds(10).toEpochMilli)

    val outsideFeed = components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)

    // create outside of feed from another category
    components.offerVosDao.create(user, offerTemplateCar.build())(Traced.empty)

    // create outside of feed from another truck category
    val lcvOffer = offerTemplate.build().toBuilder
    lcvOffer.getOfferAutoruBuilder.getTruckInfoBuilder.setAutoCategory(TruckCategory.TRUCK_CAT_LCV)
    components.offerVosDao.create(user, lcvOffer.build())(Traced.empty)

    // create outside of feed with another section
    val outsideFeedAnotherSectionOffer = offerTemplate.build().toBuilder
    outsideFeedAnotherSectionOffer.getOfferAutoruBuilder.setSection(Section.NEW)
    components.offerVosDao.create(user, outsideFeedAnotherSectionOffer.build())(Traced.empty)

    val offerUser2 = offerTemplate.build().toBuilder
    offerUser2.setUser(user2).setUserRef(user2.getUserRef)
    components.offerVosDao.create(user2, offerUser2.build())(Traced.empty)

    val nonModified = components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    // create removed
    feedOfferTemplate.clearFlag().addFlag(OfferFlag.OF_DELETED)
    components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)
    feedOfferTemplate.clearFlag()

    // create with wrong status
    feedOfferTemplate.addFlag(OfferFlag.OF_BANNED)
    components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)
    feedOfferTemplate.clearFlag()

    // create draft
    offerTemplate.addFlag(OfferFlag.OF_DRAFT)
    components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)
    offerTemplate.clearFlag()

    val offerAnotherFeed = feedOfferTemplate.build().toBuilder
    offerAnotherFeed.getOfferAutoruBuilder
      .setSection(Section.NEW)
      .getSourceInfoBuilder
      .setFeedprocessorFeedId("doesn't matter 2")
    components.offerVosDao.create(user, offerAnotherFeed.build())(Traced.empty)

    // create modified
    feedOfferTemplate.getOfferAutoruBuilder.getSourceInfoBuilder.setFeedprocessorTaskId(81)
    components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    feedOfferTemplate.getOfferAutoruBuilder.getSourceInfoBuilder
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(now.toEpochMilli)
    val nonModifiedButFresh = components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    val nonModified1 = components.offerVosDao.findAllNonModified(
      userRef,
      "BUS/USED/8",
      81,
      updatedAfterTimestamp = None,
      notAllowedStatuses = Seq(CompositeStatus.CS_BANNED)
    )(Traced.empty)

    val nonModified2 = components.offerVosDao.findAllNonModified(
      userRef,
      "BUS/USED/8",
      81,
      updatedAfterTimestamp = Some(now),
      notAllowedStatuses = Seq(CompositeStatus.CS_BANNED)
    )(Traced.empty)

    assert(nonModified1.toSet == Set(nonModified.getOfferID, nonModifiedButFresh.getOfferID, outsideFeed.getOfferID))
    assert(nonModified2.toSet == Set(nonModified.getOfferID, outsideFeed.getOfferID))
  }

  test("findAllNonModified can find NEW section") {
    val now = Instant.now()
    val user = TestUtils.createDealer().setUserRef("ac_9").build()
    val userRef = UserRef.refAutoruClient(9)
    val offerTemplate = TestUtils.createOffer(dealer = true).clearFlag().setUser(user).setUserRef(user.getUserRef)
    offerTemplate.getOfferAutoruBuilder.setSection(Section.NEW)
    val feedOfferTemplate = offerTemplate.build().toBuilder()
    feedOfferTemplate
      .clearFlag()
      .getOfferAutoruBuilder
      .getSourceInfoBuilder
      .setFeedprocessorFeedId("doesn't matter")
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(now.minusSeconds(10).toEpochMilli)

    val outsideFeed = components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)

    val nonModified = components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    val anotherSectionOffer = offerTemplate.build().toBuilder
    anotherSectionOffer.getOfferAutoruBuilder.setSection(Section.USED)
    components.offerVosDao.create(user, anotherSectionOffer.build())(Traced.empty)

    feedOfferTemplate.getOfferAutoruBuilder.getSourceInfoBuilder
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(now.toEpochMilli)
    val nonModifiedButFresh = components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    val nonModified1 = components.offerVosDao.findAllNonModified(
      userRef,
      "CARS/NEW/9",
      81,
      updatedAfterTimestamp = None,
      notAllowedStatuses = Seq(CompositeStatus.CS_BANNED)
    )(Traced.empty)

    val nonModified2 = components.offerVosDao.findAllNonModified(
      userRef,
      "CARS/NEW/9",
      81,
      updatedAfterTimestamp = Some(now),
      notAllowedStatuses = Seq(CompositeStatus.CS_BANNED)
    )(Traced.empty)

    assert(nonModified1.toSet == Set(nonModified.getOfferID, nonModifiedButFresh.getOfferID, outsideFeed.getOfferID))
    assert(nonModified2.toSet == Set(nonModified.getOfferID, outsideFeed.getOfferID))
  }

  test("findByFeedAndVin should find appropriate offer") {
    val user = TestUtils.createDealer().setUserRef("ac_10").build()
    val user2 = TestUtils.createDealer().setUserRef("ac_11").build()
    val userRef = UserRef.refAutoruClient(10)
    val offerTemplate = TestUtils.createOffer(dealer = true).clearFlag().setUser(user).setUserRef(user.getUserRef)
    offerTemplate.getOfferAutoruBuilder.setSection(Section.NEW).getDocumentsBuilder.setVin("SEARCH-VIN")

    val feedOfferTemplate = offerTemplate.build().toBuilder()
    feedOfferTemplate
      .clearFlag()
      .getOfferAutoruBuilder
      .getSourceInfoBuilder
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(1)

    val duplicateOffer = feedOfferTemplate.build().toBuilder.addFlag(OfferFlag.OF_NEED_ACTIVATION)
    components.offerVosDao.create(user, duplicateOffer.build())(Traced.empty)

    val outsideFeedOffer = offerTemplate.build().toBuilder
    components.offerVosDao.create(user, outsideFeedOffer.build())(Traced.empty)

    val anotherSectionOffer = offerTemplate.build().toBuilder
    anotherSectionOffer.getOfferAutoruBuilder.setSection(Section.USED)
    components.offerVosDao.create(user, anotherSectionOffer.build())(Traced.empty)

    val offerUser2 = offerTemplate.build().toBuilder
    offerUser2.setUser(user2).setUserRef(user2.getUserRef)
    components.offerVosDao.create(user2, offerUser2.build())(Traced.empty)

    val targetOffer = components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    feedOfferTemplate.getOfferAutoruBuilder.getDocumentsBuilder.setVin("ANOTHER-VIN")
    components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    feedOfferTemplate.getOfferAutoruBuilder.getDocumentsBuilder.clearVin()
    components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    val offerIds = components.offerVosDao.findByFeedAndVin(
      userRef,
      "CARS/NEW/10",
      Seq("SEARCH-VIN"),
      notAllowedStatuses = Seq(CompositeStatus.CS_BANNED)
    )(Traced.empty)
    assert(offerIds.values.map(_.offerRef.offerId).toSet == Set(targetOffer.getOfferID))
  }

  test("findByFeedAndVin should map results to initial VIN-s") {
    val user = TestUtils.createDealer().setUserRef("ac_10").build()
    val userRef = UserRef.refAutoruClient(10)
    val offerTemplate = TestUtils.createOffer(dealer = true).clearFlag().setUser(user).setUserRef(user.getUserRef)
    offerTemplate.getOfferAutoruBuilder
      .setSection(Section.NEW)
      .getSourceInfoBuilder
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(1)
    offerTemplate.getOfferAutoruBuilder.getDocumentsBuilder.setVin("vN001")
    val offer1 = components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)
    offerTemplate.getOfferAutoruBuilder.getDocumentsBuilder.setVin("vN002")
    val offer2 = components.offerVosDao.create(user, offerTemplate.build())(Traced.empty)

    assert(
      components.offerVosDao
        .findByFeedAndVin(
          userRef,
          "CARS/NEW/10",
          Seq("vN001", "vN002"),
          notAllowedStatuses = Seq(CompositeStatus.CS_BANNED)
        )(Traced.empty)
        .keySet == Set("vN001", "vN002")
    )

    val offerIds = components.offerVosDao.findByFeedAndVin(
      userRef,
      "CARS/NEW/10",
      Seq("Vn001", "Vn002"),
      notAllowedStatuses = Seq(CompositeStatus.CS_BANNED)
    )(Traced.empty)
    assert(offerIds.keySet == Set("Vn001", "Vn002"))
    assert(offerIds("Vn001").offerRef.offerId == offer1.getOfferID)
    assert(offerIds("Vn002").offerRef.offerId == offer2.getOfferID)
  }

  test("findByFeedAndUniqueId should find appropriate offer") {
    val user = TestUtils.createDealer().setUserRef("ac_10").build()
    val user2 = TestUtils.createDealer().setUserRef("ac_11").build()
    val userRef = UserRef.refAutoruClient(10)
    val offerTemplate = TestUtils.createOffer(dealer = true).clearFlag().setUser(user).setUserRef(user.getUserRef)
    offerTemplate.getOfferAutoruBuilder.setSection(Section.NEW)

    val feedOfferTemplate = offerTemplate.build().toBuilder()
    feedOfferTemplate
      .clearFlag()
      .getOfferAutoruBuilder
      .setFeedprocessorUniqueId("SEARCH-UID")
      .getSourceInfoBuilder
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(1)

    val duplicateOffer = feedOfferTemplate.build().toBuilder.addFlag(OfferFlag.OF_NEED_ACTIVATION)
    components.offerVosDao.create(user, duplicateOffer.build())(Traced.empty)

    val outsideFeedOffer = offerTemplate.build().toBuilder
    components.offerVosDao.create(user, outsideFeedOffer.build())(Traced.empty)

    val anotherSectionOffer = offerTemplate.build().toBuilder
    anotherSectionOffer.getOfferAutoruBuilder.setSection(Section.USED)
    components.offerVosDao.create(user, anotherSectionOffer.build())(Traced.empty)

    val offerUser2 = offerTemplate.build().toBuilder
    offerUser2.setUser(user2).setUserRef(user2.getUserRef)
    components.offerVosDao.create(user2, offerUser2.build())(Traced.empty)

    val targetOffer = components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    feedOfferTemplate.getOfferAutoruBuilder.setFeedprocessorUniqueId("ANOTHER-UID")
    components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    feedOfferTemplate.getOfferAutoruBuilder.clearFeedprocessorUniqueId()
    components.offerVosDao.create(user, feedOfferTemplate.build())(Traced.empty)

    val offerIds = components.offerVosDao.findByFeedAndUniqueId(
      userRef,
      "CARS/NEW/10",
      Seq("SEARCH-UID"),
      notAllowedStatuses = Seq(CompositeStatus.CS_BANNED)
    )(Traced.empty)
    assert(offerIds.values.map(_.offerRef.offerId).toSet == Set(targetOffer.getOfferID))
  }

  test("updateFeedprocessorTask should update appropriate offer") {
    val user = TestUtils.createDealer().setUserRef("ac_10").build()
    val userRef = UserRef.refAutoruClient(10)
    val offerTemplate = TestUtils.createOffer(dealer = true).clearFlag().setUser(user).setUserRef(user.getUserRef)
    offerTemplate.getOfferAutoruBuilder.setSection(Section.NEW)

    val feedOfferTemplate = offerTemplate.build().toBuilder()

    feedOfferTemplate
      .clearFlag()
      .getOfferAutoruBuilder
      .getSourceInfoBuilder
      .setFeedprocessorTaskId(80)
      .setFeedprocessorTimestampUpdate(1)

    val outsideFeedOffer = components
      .getOfferDao()
      .create(
        user,
        offerTemplate
          .addFlag(OfferFlag.OF_NEED_ACTIVATION)
          .build()
      )(Traced.empty)

    val feedOffer = components.getOfferDao().create(user, feedOfferTemplate.build())(Traced.empty)

    feedOfferTemplate.getOfferAutoruBuilder.getDocumentsBuilder.setVin("NON-EMPTY-VIN")
    val anotherOffer = components.getOfferDao().create(user, feedOfferTemplate.build())(Traced.empty)

    components
      .getOfferDao()
      .updateFeedprocessorTask(
        userRef,
        "CARS/NEW/10",
        81,
        2,
        Seq(outsideFeedOffer.getOfferID, feedOffer.getOfferID)
      )(Traced.empty)

    val offerFromDb1 =
      components.getOfferDao().findById(outsideFeedOffer.getOfferID, includeRemoved = true)(Traced.empty).value
    val offerFromDb2 =
      components.getOfferDao().findById(feedOffer.getOfferID, includeRemoved = true)(Traced.empty).value
    val offerFromDb3 =
      components.getOfferDao().findById(anotherOffer.getOfferID, includeRemoved = true)(Traced.empty).value
    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb1, offerFromDb2, offerFromDb3))(Traced.empty)

    val outsideFeedOffer2 = components.getOfferDao().findById(outsideFeedOffer.getOfferID)(Traced.empty)
    val feedOffer2 = components.getOfferDao().findById(feedOffer.getOfferID)(Traced.empty)
    val anotherOffer2 = components.getOfferDao().findById(anotherOffer.getOfferID)(Traced.empty)

    assert(outsideFeedOffer2.get.getOfferAutoru.getSourceInfo.getFeedprocessorTaskId == 81)
    assert(outsideFeedOffer2.get.getOfferAutoru.getSourceInfo.getFeedprocessorTimestampUpdate == 2)

    assert(feedOffer2.get.getOfferAutoru.getSourceInfo.getFeedprocessorTaskId == 81)
    assert(feedOffer2.get.getOfferAutoru.getSourceInfo.getFeedprocessorTimestampUpdate == 2)

    assert(anotherOffer2.get.getOfferAutoru.getSourceInfo.getFeedprocessorTaskId == 80)
    assert(anotherOffer2.get.getOfferAutoru.getSourceInfo.getFeedprocessorTimestampUpdate == 1)
  }

  test("save user actions") {
    val now = DateTime.now()
    val category = "cars"
    val categoryEnum = testFormGenerator.categoryByString(category)
    val formInfo: FormInfo = testFormGenerator.createForm(category, TestFormParams[CarFormInfo](generateBadges = false))
    val userRef = formInfo.userRef
    val ad = components.offersReader.loadAdditionalData(userRef, formInfo.form)(Traced.empty)
    val action = UserChangeAction
      .newBuilder()
      .setIp("127.0.0.1")
      .setUserId("test-user")
      .setActionType(UserChangeActionType.CREATE)
      .build()
    val offer = components.formWriter
      .createOffer(
        userRef,
        categoryEnum,
        formInfo.form,
        now.getMillis,
        ad,
        None,
        FormWriteParams(),
        "test",
        Some(action)
      )(Traced.empty)
      .right
      .value
      .offer
    val offerId = offer.getOfferID
    val created = components.getOfferDao().findById(offerId)(Traced.empty).get
    val a = created.getOfferAutoru.getUserChangeActionHistoryList.get(0)
    assert(a.getIp == "127.0.0.1")
  }

  private def compareWithoutMillis(dateTime: DateTime, expected: Long): Boolean = {
    compareWithoutMillis(dateTime.getMillis, expected)
  }

  private def compareWithoutMillis(timestamp: Long, expected: Long): Boolean = {
    math.abs(timestamp - expected) <= 1000
  }

  test("updateService with active placement") {
    val now = DateTime.now()
    val cars = Category.CARS

    val formInfo: FormInfo = testFormGenerator.createForm("cars", TestFormParams[CarFormInfo]())
    val userRef = formInfo.userRef

    val offer = TestUtils
      .createOffer(category = cars)
      .setUserRef(userRef.toPlain)
      .setTimestampWillExpire(getNow - 5.days.toMillis)
      .build()

    val ad = components.offersReader.loadAdditionalData(offer)(Traced.empty)

    val createOfferResult = components.formWriter.createOffer(
      userRef,
      cars,
      formInfo.form,
      now.getMillis,
      ad,
      Some(offer),
      FormWriteParams(),
      "test",
      None
    )(Traced.empty)

    val oldOffer = createOfferResult.right.value.offer

    val offerId: OfferID = oldOffer.getOfferID
    val autoruOfferId = AutoruOfferID.parse(offerId)

    val serviceExpireDate = getNow + 7.days.toMillis

    components
      .getOfferDao()
      .migrateAndSave(components.autoruSalesDao.getOffers(Seq(autoruOfferId.id)), "test") {
        case (sale, optOffer) => components.carOfferConverter.convertStrict(sale, optOffer).converted
      }(Traced.empty)

    components.offersWriter.addServices(
      offerId,
      Some(userRef),
      Some(cars),
      None,
      Seq(
        PaidService
          .newBuilder()
          .setService("all_sale_activate")
          .setIsActive(true)
          .setExpireDate(serviceExpireDate)
          .build()
      )
    )(Traced.empty)

    val actual = components.getOfferDao().findById(offerId)(Traced.empty)
    assert(compareWithoutMillis(actual.get.getTimestampWillExpire, serviceExpireDate))
  }

  test("updateService with inactive placement") {
    val now = DateTime.now()
    val cars = Category.CARS

    val formInfo: FormInfo = testFormGenerator.createForm("cars", TestFormParams[CarFormInfo]())
    val userRef = formInfo.userRef

    val offer = TestUtils
      .createOffer(category = cars)
      .setUserRef(userRef.toPlain)
      .build()

    val ad = components.offersReader.loadAdditionalData(offer)(Traced.empty)

    val createOfferResult = components.formWriter.createOffer(
      userRef,
      cars,
      formInfo.form,
      now.getMillis,
      ad,
      Some(offer),
      FormWriteParams(),
      "test",
      None
    )(Traced.empty)

    val oldOffer: OfferModel.Offer = createOfferResult match {
      case Left(a) => fail(s"FValidation errors: $a")
      case Right(b) => b.offer
    }

    val offerId: OfferID = oldOffer.getOfferID
    val autoruOfferId = AutoruOfferID.parse(offerId)

    components.offerVosDao.migrateAndSave(components.autoruSalesDao.getOffers(Seq(autoruOfferId.id)), "test") {
      case (sale, optOffer) => components.carOfferConverter.convertStrict(sale, optOffer).converted
    }(Traced.empty)

    val serviceExpireDate = getNow + 7.days.toMillis

    components.offersWriter.addServices(
      offerId,
      Some(userRef),
      Some(cars),
      None,
      Seq(
        PaidService
          .newBuilder()
          .setService("all_sale_activate")
          .setIsActive(false)
          .setExpireDate(serviceExpireDate)
          .build()
      )
    )(Traced.empty)

    val expectedExpireDate = oldOffer.getTimestampWillExpire

    val actual = components.offerVosDao.findById(offerId)(Traced.empty)
    assert(compareWithoutMillis(actual.get.getTimestampWillExpire, expectedExpireDate))

    val actualOld = components.autoruSalesDao.getOffer(autoruOfferId.id)(Traced.empty)
    assert(compareWithoutMillis(actualOld.get.expireDate, expectedExpireDate))
  }

  test("updateService with new fresh") {

    val formInfo: FormInfo = testFormGenerator.createForm("cars", TestFormParams[CarFormInfo]())
    val now = DateTime.now()

    val cars = Category.CARS
    val userRef = formInfo.userRef
    val offerTemplate = TestUtils.createOffer(category = cars)
    offerTemplate.setUserRef(userRef.toPlain)

    offerTemplate.getOfferAutoruBuilder.setFreshDate(getNow - 5.days.toMillis)

    val expectedFreshDate = getNow

    val ad = components.offersReader.loadAdditionalData(offerTemplate.build())(Traced.empty)

    val createOfferResult = components.formWriter.createOffer(
      userRef,
      cars,
      formInfo.form,
      now.getMillis,
      ad,
      Some(offerTemplate.build()),
      FormWriteParams(),
      "test",
      None
    )(Traced.empty)

    val offer: OfferModel.Offer = createOfferResult match {
      case Left(a) => fail(s"FValidation errors: $a")
      case Right(b) => b.offer
    }
    val offerId = offer.getOfferID

    components.offersWriter.addServices(
      offerId,
      Some(userRef),
      Some(cars),
      None,
      Seq(
        PaidService
          .newBuilder()
          .setService("all_sale_fresh")
          .setIsActive(true)
          .setCreateDate(expectedFreshDate)
          .build()
      )
    )(Traced.empty)
    val offerFromDb = components.getOfferDao().findById(offerId, includeRemoved = true)(Traced.empty).value
    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)
    val actual = components.getOfferDao().findById(offerId)(Traced.empty)
    assert(actual.get.getOfferAutoru.getFreshDate == expectedFreshDate)

  }

  test("updateService with old fresh") {

    val formInfo: FormInfo = testFormGenerator.createForm("cars", TestFormParams[CarFormInfo]())
    val now = DateTime.now()

    val cars = Category.CARS
    val userRef = formInfo.userRef
    val offerTemplate = TestUtils.createOffer(category = cars)
    offerTemplate.setUserRef(userRef.toPlain)

    val expectedFreshDate = getNow + 5.days.toMillis
    offerTemplate.getOfferAutoruBuilder.setFreshDate(expectedFreshDate)

    val ad = components.offersReader.loadAdditionalData(offerTemplate.build())(Traced.empty)

    val createOfferResult = components.formWriter.createOffer(
      userRef,
      cars,
      formInfo.form,
      now.getMillis,
      ad,
      Some(offerTemplate.build()),
      FormWriteParams(),
      "test",
      None
    )(Traced.empty)

    val offer: OfferModel.Offer = createOfferResult match {
      case Left(a) => fail(s"FValidation errors: $a")
      case Right(b) => b.offer
    }
    val offerId = offer.getOfferID

    components.offersWriter.addServices(
      offerId,
      Some(userRef),
      Some(cars),
      None,
      Seq(
        PaidService
          .newBuilder()
          .setService("all_sale_fresh")
          .setIsActive(true)
          .setCreateDate(getNow)
          .build()
      )
    )(Traced.empty)
    val offerFromDb = components.getOfferDao().findById(offerId, includeRemoved = true)(Traced.empty).value
    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)
    val actual = components.getOfferDao().findById(offerId)(Traced.empty)
    assert(actual.get.getOfferAutoru.getFreshDate == expectedFreshDate)
  }

  test("find batch on shard") {
    implicit val trace: Traced = Traced.empty
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createUser().build()
    val createdOffer1 = dao.create(user, TestUtils.createOffer().build(), "")
    val createdOffer2 = dao.create(user, TestUtils.createOffer().build(), "")

    val found = dao.batchFindOnShard(
      Seq(createdOffer1.getOfferID, createdOffer2.getOfferID),
      components.mySql.getShardByUserRef(UserRef.from(user.getUserRef))
    )

    assert(found.size === 2)
    assert(found.exists(_._1.offerId === createdOffer1.getOfferID))
    assert(found.exists(_._1.offerId === createdOffer2.getOfferID))
  }

  test("enable multiposting for user") {
    implicit val trace: Traced = Traced.empty
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createUser().build()

    val activeOffer = dao.create(
      user,
      TestUtils
        .createOffer()
        .setMultiposting(
          TestUtils
            .createMultiposting(CompositeStatus.CS_INACTIVE)
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AUTORU, enabled = false))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AVITO, enabled = false))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.DROM, enabled = false))
        )
        .build(),
      ""
    )
    val activeOffer2 = dao.create(
      user,
      TestUtils
        .createOffer()
        .addFlag(OfferFlag.OF_MIGRATED)
        .build(),
      ""
    )

    val activeOffer3 = dao.create(
      user,
      TestUtils
        .createOffer()
        .setMultiposting(
          TestUtils
            .createMultiposting(CompositeStatus.CS_INACTIVE)
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AUTORU, enabled = true))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AVITO, enabled = true))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.DROM, enabled = true))
        )
        .build(),
      ""
    )

    val inactiveOffer = dao.create(
      user,
      TestUtils
        .createOffer()
        .addFlag(OfferFlag.OF_INACTIVE)
        .setMultiposting(
          TestUtils
            .createMultiposting(CompositeStatus.CS_INACTIVE)
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AUTORU, enabled = false))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AVITO, enabled = false))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.DROM, enabled = false))
        )
        .build(),
      ""
    )
    val inactiveOffer2 = dao.create(
      user,
      TestUtils
        .createOffer()
        .addFlag(OfferFlag.OF_INACTIVE)
        .build(),
      ""
    )
    val expiredOffer = dao.create(
      user,
      TestUtils
        .createOffer()
        .addFlag(OfferFlag.OF_EXPIRED)
        .build(),
      ""
    )
    val draftOffer = dao.create(
      user,
      TestUtils
        .createOffer()
        .addFlag(OfferFlag.OF_DRAFT)
        .setMultiposting(
          TestUtils
            .createMultiposting(CompositeStatus.CS_INACTIVE)
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AUTORU, enabled = false))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AVITO, enabled = false))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.DROM, enabled = false))
        )
        .build(),
      ""
    )

    dao.enableMultiposting(UserRef.from(user.getUserRef))

    val found = dao.batchFindOnShard(
      Seq(
        activeOffer.getOfferID,
        activeOffer2.getOfferID,
        activeOffer3.getOfferID,
        inactiveOffer.getOfferID,
        inactiveOffer2.getOfferID,
        expiredOffer.getOfferID,
        draftOffer.getOfferID
      ),
      components.mySql.getShardByUserRef(UserRef.from(user.getUserRef))
    )

    val activeOfferUpdated = found.find(_._1.offerId == activeOffer.getOfferID).map(_._2)
    val activeOffer2Updated = found.find(_._1.offerId == activeOffer2.getOfferID).map(_._2)
    val activeOffer3Updated = found.find(_._1.offerId == activeOffer3.getOfferID).map(_._2)
    val inactiveOfferUpdated = found.find(_._1.offerId == inactiveOffer.getOfferID).map(_._2)
    val inactiveOffer2Updated = found.find(_._1.offerId == inactiveOffer2.getOfferID).map(_._2)
    val expiredOfferUpdated = found.find(_._1.offerId == expiredOffer.getOfferID).map(_._2)
    val draftOfferUpdated = found.find(_._1.offerId == draftOffer.getOfferID).map(_._2)

    assert(activeOfferUpdated.value.getMultiposting.getStatus == CompositeStatus.CS_ACTIVE)
    assert(activeOfferUpdated.value.isClassifiedEnabled(ClassifiedName.AUTORU))
    assert(!activeOfferUpdated.value.isClassifiedEnabled(ClassifiedName.AVITO))
    assert(!activeOfferUpdated.value.isClassifiedNeedActivation(ClassifiedName.AVITO))
    assert(!activeOfferUpdated.value.isClassifiedEnabled(ClassifiedName.DROM))
    assert(!activeOfferUpdated.value.isClassifiedNeedActivation(ClassifiedName.DROM))

    assert(activeOffer2Updated.value.getMultiposting.getStatus == CompositeStatus.CS_ACTIVE)
    assert(activeOffer2Updated.value.isClassifiedEnabled(ClassifiedName.AUTORU))
    assert(!activeOffer2Updated.value.isClassifiedEnabled(ClassifiedName.AVITO))
    assert(!activeOffer2Updated.value.isClassifiedNeedActivation(ClassifiedName.AVITO))
    assert(!activeOffer2Updated.value.isClassifiedEnabled(ClassifiedName.DROM))
    assert(!activeOffer2Updated.value.isClassifiedNeedActivation(ClassifiedName.DROM))

    assert(activeOffer3Updated.value.getMultiposting.getStatus == CompositeStatus.CS_ACTIVE)
    assert(activeOffer3Updated.value.isClassifiedEnabled(ClassifiedName.AUTORU))
    assert(activeOffer3Updated.value.isClassifiedEnabled(ClassifiedName.AVITO))
    assert(activeOffer3Updated.value.isClassifiedNeedActivation(ClassifiedName.AVITO))
    assert(activeOffer3Updated.value.isClassifiedEnabled(ClassifiedName.DROM))
    assert(activeOffer3Updated.value.isClassifiedActive(ClassifiedName.DROM))

    assert(inactiveOfferUpdated.value.getMultiposting.getStatus == CompositeStatus.CS_INACTIVE)
    assert(inactiveOfferUpdated.value.isClassifiedEnabled(ClassifiedName.AUTORU))
    assert(!inactiveOfferUpdated.value.isClassifiedEnabled(ClassifiedName.AVITO))
    assert(!inactiveOfferUpdated.value.isClassifiedNeedActivation(ClassifiedName.AVITO))
    assert(!inactiveOfferUpdated.value.isClassifiedEnabled(ClassifiedName.DROM))
    assert(!inactiveOfferUpdated.value.isClassifiedNeedActivation(ClassifiedName.DROM))

    assert(inactiveOffer2Updated.value.getMultiposting.getStatus == CompositeStatus.CS_INACTIVE)
    assert(inactiveOffer2Updated.value.isClassifiedEnabled(ClassifiedName.AUTORU))
    assert(!inactiveOffer2Updated.value.isClassifiedEnabled(ClassifiedName.AVITO))
    assert(!inactiveOffer2Updated.value.isClassifiedNeedActivation(ClassifiedName.AVITO))
    assert(!inactiveOffer2Updated.value.isClassifiedEnabled(ClassifiedName.DROM))
    assert(!inactiveOffer2Updated.value.isClassifiedNeedActivation(ClassifiedName.DROM))

    assert(expiredOfferUpdated.value.getMultiposting.getStatus == CompositeStatus.CS_ACTIVE)
    assert(expiredOfferUpdated.value.isClassifiedEnabled(ClassifiedName.AUTORU))
    assert(!expiredOfferUpdated.value.isClassifiedEnabled(ClassifiedName.AVITO))
    assert(!expiredOfferUpdated.value.isClassifiedNeedActivation(ClassifiedName.AVITO))
    assert(!expiredOfferUpdated.value.isClassifiedEnabled(ClassifiedName.DROM))
    assert(!expiredOfferUpdated.value.isClassifiedNeedActivation(ClassifiedName.DROM))

    assert(draftOfferUpdated.value.getMultiposting.getStatus == CompositeStatus.CS_INACTIVE)
    assert(!draftOfferUpdated.value.isClassifiedEnabled(ClassifiedName.AUTORU))
    assert(!draftOfferUpdated.value.isClassifiedEnabled(ClassifiedName.AVITO))
    assert(!draftOfferUpdated.value.isClassifiedNeedActivation(ClassifiedName.AVITO))
    assert(!draftOfferUpdated.value.isClassifiedEnabled(ClassifiedName.DROM))
    assert(!draftOfferUpdated.value.isClassifiedNeedActivation(ClassifiedName.DROM))
  }

  test("disable multiposting for user") {
    implicit val trace: Traced = Traced.empty
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createUser().build()

    val activeMultipostingOffer = dao.create(
      user,
      TestUtils
        .createOffer()
        .setMultiposting(
          TestUtils
            .createMultiposting(CompositeStatus.CS_ACTIVE)
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AUTORU))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AVITO))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.DROM))
        )
        .build(),
      ""
    )
    val inactiveMultipostingOffer = dao.create(
      user,
      TestUtils
        .createOffer()
        .setMultiposting(
          TestUtils
            .createMultiposting(CompositeStatus.CS_INACTIVE)
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AUTORU))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AVITO))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.DROM))
        )
        .build(),
      ""
    )

    dao.disableMultiposting(UserRef.from(user.getUserRef))

    val found = dao.batchFindOnShard(
      Seq(activeMultipostingOffer.getOfferID, inactiveMultipostingOffer.getOfferID),
      components.mySql.getShardByUserRef(UserRef.from(user.getUserRef))
    )

    val activeUpdated = found.find(_._1.offerId == activeMultipostingOffer.getOfferID).map(_._2)
    val inactiveUpdated = found.find(_._1.offerId == inactiveMultipostingOffer.getOfferID).map(_._2)

    assert(activeUpdated.value.getMultiposting.getStatus == CompositeStatus.CS_INACTIVE)
    assert(activeUpdated.value.isClassifiedEnabled(ClassifiedName.AUTORU))
    assert(activeUpdated.value.isClassifiedActive(ClassifiedName.AUTORU))
    assert(activeUpdated.value.isClassifiedEnabled(ClassifiedName.AVITO))
    assert(activeUpdated.value.isClassifiedInactive(ClassifiedName.AVITO))
    assert(activeUpdated.value.isClassifiedEnabled(ClassifiedName.DROM))
    assert(activeUpdated.value.isClassifiedInactive(ClassifiedName.DROM))

    // do not change inactive multiposting offer at all
    assert(inactiveUpdated.value.getMultiposting.getStatus == CompositeStatus.CS_INACTIVE)
    assert(inactiveUpdated.value.isClassifiedEnabled(ClassifiedName.AUTORU))
    assert(inactiveUpdated.value.isClassifiedActive(ClassifiedName.AUTORU))
    assert(inactiveUpdated.value.isClassifiedEnabled(ClassifiedName.AVITO))
    assert(inactiveUpdated.value.isClassifiedActive(ClassifiedName.AVITO))
    assert(inactiveUpdated.value.isClassifiedEnabled(ClassifiedName.DROM))
    assert(inactiveUpdated.value.isClassifiedActive(ClassifiedName.DROM))
  }

  test("archive offers for user") {
    implicit val trace: Traced = Traced.empty
    val dao: AutoruOfferDao = components.offerVosDao
    val user = TestUtils.createUser().build()

    val activeMultipostingOffer = dao.create(
      user,
      TestUtils
        .createOffer()
        .setMultiposting(
          TestUtils
            .createMultiposting(CompositeStatus.CS_ACTIVE)
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AUTORU))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AVITO))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.DROM))
        )
        .build(),
      ""
    )
    val inactiveMultipostingOffer = dao.create(
      user,
      TestUtils
        .createOffer()
        .setMultiposting(
          TestUtils
            .createMultiposting(CompositeStatus.CS_INACTIVE)
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AUTORU))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.AVITO))
            .addClassifieds(TestUtils.createClassified(ClassifiedName.DROM))
        )
        .build(),
      ""
    )

    val userRef = UserRef.from(user.getUserRef)
    val offers = Seq(activeMultipostingOffer.getOfferID, inactiveMultipostingOffer.getOfferID)
    dao.setArchiveBatch(userRef, offers.map(AutoruOfferID.parse), true, true, "test")

    val found = dao.batchFindOnShard(
      Seq(activeMultipostingOffer.getOfferID, inactiveMultipostingOffer.getOfferID),
      components.mySql.getShardByUserRef(UserRef.from(user.getUserRef)),
      true
    )

    val activeUpdated = found.find(_._1.offerId == activeMultipostingOffer.getOfferID).map(_._2)
    val inactiveUpdated = found.find(_._1.offerId == inactiveMultipostingOffer.getOfferID).map(_._2)

    assert(activeUpdated.value.getMultiposting.getStatus == CompositeStatus.CS_REMOVED)
    assert(!activeUpdated.value.isActive)

    // do not change inactive multiposting offer at all
    assert(inactiveUpdated.value.getMultiposting.getStatus == CompositeStatus.CS_REMOVED)
    assert(!inactiveUpdated.value.isActive)

  }

  test("get offer ids by VINs") {
    implicit val trace: Traced = Traced.empty
    val dao: AutoruOfferDao = components.offerVosDao
    val offereIds = dao.getOfferIdsByVins(Seq("WA1LFBFP1BA109828"), includeRemoved = true)
    assert(offereIds.contains("1042409964-038a"))
  }

  test("get offers by params (empty filter)") {
    implicit val trace: Traced = Traced.empty
    val dao: AutoruOfferDao = components.offerVosDao
    val offers = dao.getOffersByParams(
      OffersByParamsFilter(Seq.empty, Seq.empty, Seq.empty, Seq.empty, true),
      Seq.fill(components.mySql.shardCount)(Some((0, Int.MaxValue)))
    )
    assert(offers.isEmpty)
  }

  test("get offers by params (by offer ID)") {
    implicit val trace: Traced = Traced.empty
    val dao: AutoruOfferDao = components.offerVosDao
    val offers = dao.getOffersByParams(
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
    val dao: AutoruOfferDao = components.offerVosDao
    val offers = dao.getOffersByParams(
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
    val dao: AutoruOfferDao = components.offerVosDao
    val offers = dao.getOffersByParams(
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
    val dao: AutoruOfferDao = components.offerVosDao
    val offers = dao.getOffersByParams(
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
    val dao: AutoruOfferDao = components.offerVosDao
    val offers = dao.getOffersByParams(
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
