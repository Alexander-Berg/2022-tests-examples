package ru.yandex.vertis.parsing.realty.scheduler.workers

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.components.time.DefaultTimeService
import ru.yandex.vertis.parsing.features.SimpleFeatures
import ru.yandex.vertis.parsing.realty.ParsingRealtyModel.{OfferCategory, OfferType}
import ru.yandex.vertis.parsing.realty.bunkerconfig.{BunkerConfig, CallCenterConfig, Percents}
import ru.yandex.vertis.parsing.realty.dao.offers.{ParsedRealtyOffersDao, ParsedRealtyRow, SiteCallCenter}
import ru.yandex.vertis.parsing.realty.features.ParsingRealtyFeatures
import ru.yandex.vertis.parsing.realty.parsers.CommonRealtyParser
import ru.yandex.vertis.parsing.realty.parsers.smartagent.avito.SmartAgentAvitoRealtyParser
import ru.yandex.vertis.parsing.realty.workers.importers.RealtyCategory
import ru.yandex.vertis.parsing.util.RandomUtil._
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.util.workmoments.WorkMoments
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.parsing.realty.util.TestDataUtils._

import scala.concurrent.duration._
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class DistributorTest extends FunSuite with MockitoSupport {
  distributorTest =>
  implicit private val trace: Traced = TracedUtils.empty

  private val dao = mock[ParsedRealtyOffersDao]

  private val rows = mutable.HashMap[String, ParsedRealtyRow]()

  stub(dao.getReadyWithoutCallCenter(_: Seq[RealtyCategory], _: Int, _: Seq[String], _: Boolean)(_: Traced)) {
    case (categories, batch, knownCallcenters, _, _) =>
      val phones = rows.values
        .filter(r =>
          categories.contains(r.category) &&
            (r.callCenter.isEmpty || !knownCallcenters.contains(r.callCenter.get))
        )
        .map(_.phone)
        .toSeq
        .distinct
        .take(batch)
        .toSet
      val a = rows.values
        .filter(r =>
          categories.contains(r.category) &&
            (r.callCenter.isEmpty || !knownCallcenters.contains(r.callCenter.get)) &&
            phones.contains(r.phone)
        )
        .toArray
      shuffle(a)
      a.map(_.forDistribution).toList
  }

  stub(dao.getCurrentDaySentOrReadyWithCallCenters(_: Seq[RealtyCategory], _: Seq[String])(_: Traced)) {
    case (categories, knownCallcenters, _) =>
      rows.values
        .filter(r =>
          categories.contains(r.category) &&
            r.callCenter.nonEmpty && knownCallcenters.contains(r.callCenter.get)
        )
        .groupBy(row => {
          SiteCallCenter(row.site, row.callCenter.get)
        })
        .mapValues(_.map(_.phone).toSeq.distinct.size)
  }

  stub(dao.setCallCenter(_: Seq[String], _: String, _: Boolean)(_: Traced)) {
    case (hashes, callCenter, _, _) =>
      hashes
        .map(hash => {
          val shouldUpdate = rows.contains(hash)
          if (shouldUpdate) {
            rows(hash) = rows(hash).copy(callCenter = Some(callCenter))
          }
          (hash, shouldUpdate)
        })
        .toMap
  }

  implicit private val timeService: DefaultTimeService = new DefaultTimeService

  private val workMoments: WorkMoments = WorkMoments.every(2.minutes)

  private val batchSize = 10

  private val callcenter1 = "callcenter1"
  private val callcenter2 = "callcenter2"

  private val realtyConfig = Seq(
    CallCenterConfig(
      name = callcenter1,
      emails = Seq.empty,
      percents = Percents(
        avito = 60,
        others = 0
      ),
      init_size = 0,
      init_hour = 0,
      init_minute = 0,
      start_hour = 0,
      start_minute = 0,
      end_hour = 0,
      end_minute = 0,
      size_gt = 0,
      size_lt = 0,
      period = 0,
      published_emails = Seq.empty[String]
    ),
    CallCenterConfig(
      name = callcenter2,
      emails = Seq.empty,
      percents = Percents(
        avito = 40,
        others = 0
      ),
      init_size = 0,
      init_hour = 0,
      init_minute = 0,
      start_hour = 0,
      start_minute = 0,
      end_hour = 0,
      end_minute = 0,
      size_gt = 0,
      size_lt = 0,
      period = 0,
      published_emails = Seq.empty[String]
    )
  )

  private val bunkerConfig = BunkerConfig(
    Seq(),
    Seq(),
    Set(),
    realtyConfig,
    Seq()
  )

  private val features: ParsingRealtyFeatures = new SimpleFeatures with ParsingRealtyFeatures {
    override def smartAgentParsers: List[CommonRealtyParser] = List(SmartAgentAvitoRealtyParser)

    override def bunkerConfig: BunkerConfig = distributorTest.bunkerConfig
  }

  test("call centers distribution: feature is disabled") {
    rows.clear()
    val offerType = OfferType.SELL
    val offerCategory = OfferCategory.APARTMENT
    val category = (offerType, offerCategory)
    features.DistributeCategory(category).setEnabled(false)
    val distributor = newDistributor
    (1 to 100).foreach(_ => {
      val url = testAvitoApartmentUrl
      val row = testRow(url, offerType = offerType, offerCategory = offerCategory, phone = Some(getRandomPhone))
      rows += row.hash -> row
    })
    val worked = distributor.distribute()
    assert(!worked)
    assert(rows.values.count(_.callCenter.nonEmpty) == 0)
  }

  test("call centers distribution: feature is enabled") {
    rows.clear()
    val offerType = OfferType.SELL
    val offerCategory = OfferCategory.APARTMENT
    val category = (offerType, offerCategory)
    features.DistributeCategory(category).setEnabled(true)
    val distributor = newDistributor
    (1 to 100).foreach(_ => {
      val url = testAvitoApartmentUrl
      val row = testRow(url, offerType = offerType, offerCategory = offerCategory, phone = Some(getRandomPhone))
      rows += row.hash -> row
    })
    val worked = distributor.distribute()
    assert(worked)
    assert(rows.values.count(_.callCenter.nonEmpty) == 100)
    assert(rows.values.count(_.callCenter.get == callcenter1) == 60)
    assert(rows.values.count(_.callCenter.get == callcenter2) == 40)
    features.DistributeCategory(category).setEnabled(false)
  }

  test("call centers distribution: unknown call centers") {
    rows.clear()
    val offerType = OfferType.SELL
    val offerCategory = OfferCategory.APARTMENT
    val category = (offerType, offerCategory)
    features.DistributeCategory(category).setEnabled(true)
    val distributor = newDistributor
    (1 to 100).foreach(_ => {
      val url = testAvitoApartmentUrl
      val row = testRow(url, offerType = offerType, offerCategory = offerCategory, phone = Some(getRandomPhone))
      rows += row.hash -> row
    })
    (1 to 100).foreach(_ => {
      val url = testAvitoApartmentUrl
      val row = testRow(
        url,
        offerType = offerType,
        offerCategory = offerCategory,
        callCenter = Some("unknown"),
        phone = Some(getRandomPhone)
      )
      rows += row.hash -> row
    })
    val worked = distributor.distribute()
    assert(worked)
    assert(rows.values.count(_.callCenter.nonEmpty) == 200)
    assert(rows.values.count(_.callCenter.get == callcenter1) == 120)
    assert(rows.values.count(_.callCenter.get == callcenter2) == 80)
    features.DistributeCategory(category).setEnabled(false)
  }

  test("call centers distribution: different sites") {
    rows.clear()
    val offerType = OfferType.SELL
    val offerCategory = OfferCategory.APARTMENT
    val category = (offerType, offerCategory)
    features.DistributeCategory(category).setEnabled(true)
    val distributor = newDistributor
    val phone = getRandomPhone
    // у телефна 11 объявлений на Авито и 10 на Циане - распределяем по настройкам Авито, т.к. там больше
    (0 until 11).foreach(_ => {
      val url = testAvitoApartmentUrl
      val row = testRow(url, offerType = offerType, offerCategory = offerCategory, phone = Some(phone))
      rows += row.hash -> row
    })
    (0 until 10).foreach(_ => {
      val url = testCianApartmentUrl
      val row = testRow(url, offerType = offerType, offerCategory = offerCategory, phone = Some(phone))
      rows += row.hash -> row
    })
    val worked = distributor.distribute()
    assert(worked)
    assert(rows.values.count(_.callCenter.nonEmpty) == 21)
    // распределяем один телефон - все идет в последний ненулевой КЦ
    assert(rows.values.count(_.callCenter.get == callcenter1) == 0)
    assert(rows.values.count(_.callCenter.get == callcenter2) == 21)
    features.DistributeCategory(category).setEnabled(false)
  }

  test("call centers distribution: by phones") {
    rows.clear()
    val offerType = OfferType.SELL
    val offerCategory = OfferCategory.APARTMENT
    val category = (offerType, offerCategory)
    features.DistributeCategory(category).setEnabled(true)
    val distributor = newDistributor
    val phones = (1 to 10).map(_ => getRandomPhone).toArray
    (0 until 100).foreach(i => {
      val url = testAvitoApartmentUrl
      val phone =
        if (i < 40) phones(0)
        else if (i < 60) phones(1)
        else if (i < 70) phones(2)
        else choose(phones.drop(3))
      val row = testRow(url, offerType = offerType, offerCategory = offerCategory, phone = Some(phone))
      rows += row.hash -> row
    })
    val worked = distributor.distribute()
    assert(worked)
    assert(rows.values.count(_.callCenter.nonEmpty) == 100)
    assert(rows.values.filter(_.callCenter.get == callcenter1).map(_.phone).toSeq.distinct.length == 6)
    assert(rows.values.filter(_.callCenter.get == callcenter2).map(_.phone).toSeq.distinct.length == 4)
    features.DistributeCategory(category).setEnabled(false)
  }

  test("call centers distribution: one category enabled, one disabled") {
    rows.clear()
    val offerType = OfferType.SELL
    val offerCategory = OfferCategory.APARTMENT
    val category = (offerType, offerCategory)
    val disabledOfferType = OfferType.SELL
    val disabledOfferCategory = OfferCategory.HOUSE
    val disabledCategory = (disabledOfferType, disabledOfferCategory)
    features.DistributeCategory(category).setEnabled(true)
    features.DistributeCategory(disabledCategory).setEnabled(false)
    val distributor = newDistributor
    (1 to 100).foreach(_ => {
      val url = testAvitoApartmentUrl
      val row = testRow(url, offerType = offerType, offerCategory = offerCategory, phone = Some(getRandomPhone))
      rows += row.hash -> row
    })
    (1 to 100).foreach(_ => {
      val url = testAvitoApartmentUrl
      val row =
        testRow(url, offerType = disabledOfferType, offerCategory = disabledOfferCategory, phone = Some(getRandomPhone))
      rows += row.hash -> row
    })
    val worked = distributor.distribute()
    assert(worked)
    assert(rows.values.count(_.callCenter.nonEmpty) == 100)
    assert(rows.values.count(_.callCenter.isEmpty) == 100)
    assert(rows.values.filter(_.callCenter.nonEmpty).count(_.category == category) == 100)
    assert(rows.values.filter(_.callCenter.nonEmpty).count(_.category == disabledCategory) == 0)
    assert(rows.values.filter(_.callCenter.nonEmpty).count(_.callCenter.get == callcenter1) == 60)
    assert(rows.values.filter(_.callCenter.nonEmpty).count(_.callCenter.get == callcenter2) == 40)
    features.DistributeCategory(category).setEnabled(false)
  }

  private def newDistributor: Distributor = {
    new Distributor(batchSize, workMoments, dao, features) {
      override def start(): Unit = ???

      override def shouldWork: Boolean = true

      override def stop(): Unit = ???

      override def bunkerConfig: BunkerConfig = distributorTest.bunkerConfig
    }
  }

  private def shuffle[A](arr: Array[A]) {
    def swap(i: Int, j: Int) {
      val tmp = arr(i)
      arr(i) = arr(j)
      arr(j) = tmp
    }

    var i = 0
    while (i < arr.length) {
      val r = (math.random * (i + 1)).toInt
      swap(i, r)
      i += 1
    }
  }
}
