package ru.yandex.vertis.parsing.scheduler.workers

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.components.TestCatalogsAndFeaturesComponents
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.auto.components.bunkerconfig.{BunkerConfig, CallCenterConfig, Percents}
import ru.yandex.vertis.parsing.components.time.DefaultTimeService
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRowUtils._
import ru.yandex.vertis.parsing.auto.dao.parsedoffers.ParsedOffersDao
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.util.workmoments.WorkMoments
import ru.yandex.vertis.tracing.Traced

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DistributorTest extends FunSuite with MockitoSupport {
  distributorTest =>
  implicit private val trace: Traced = TracedUtils.empty

  private val components = TestCatalogsAndFeaturesComponents

  private val parsedOffersDao = mock[ParsedOffersDao]

  private val rows = mutable.HashMap[String, ParsedRow]()

  private val regionTree = components.regionTree

  stub(parsedOffersDao.getReadyWithoutCallCenter(_: Category, _: Int, _: Seq[String])(_: Traced)) {
    case (category, batch, knownCallcenters, _) =>
      val a = rows.values
        .filter(r =>
          r.category == category &&
            (r.callCenter.isEmpty || !knownCallcenters.contains(r.callCenter.get))
        )
        .take(batch)
        .toArray
      shuffle(a)
      a.map(_.forDistribution(regionTree)).toList
  }

  stub(parsedOffersDao.getCurrentDaySentOrReadyWithCallCenters(_: Category)(_: Traced)) {
    case (category, _) =>
      rows.values
        .filter(r => r.category == category && r.callCenter.nonEmpty)
        .groupBy(row => {
          (row.site, row.callCenter.get, row.optFederalSubject(regionTree).map(_.id).get)
        })
        .mapValues(_.size)
  }

  stub(parsedOffersDao.setCallCenter(_: Seq[String], _: String)(_: Traced)) {
    case (hashes, callCenter, _) =>
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

  private val carsConfig = Seq(
    CallCenterConfig(
      name = "cc1",
      emails = Seq.empty,
      percents = Seq(
        Percents(
          region = "Рязанская область",
          avito = 100,
          drom = 100,
          others = 100
        ),
        Percents(
          region = "",
          avito = 35,
          drom = 0,
          others = 35
        )
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
      published_emails = Seq.empty[String],
      category = Category.CARS
    ),
    CallCenterConfig(
      name = "cc2",
      emails = Seq.empty,
      percents = Seq(
        Percents(
          region = "Рязанская область",
          avito = 0,
          drom = 0,
          others = 0
        ),
        Percents(
          region = "",
          avito = 65,
          drom = 100,
          others = 65
        )
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
      published_emails = Seq.empty[String],
      category = Category.CARS
    ),
    CallCenterConfig(
      name = "cc3",
      emails = Seq.empty,
      percents = Seq(
        Percents(
          region = "Рязанская область",
          avito = 0,
          drom = 0,
          others = 0
        ),
        Percents(
          region = "",
          avito = 0,
          drom = 0,
          others = 0
        )
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
      published_emails = Seq.empty[String],
      category = Category.CARS
    )
  )

  private val trucksConfig = Seq(
    CallCenterConfig(
      name = "cc1",
      emails = Seq.empty,
      percents = Seq(
        Percents(
          region = "",
          avito = 100,
          drom = 100,
          others = 100
        )
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
      published_emails = Seq.empty[String],
      category = Category.TRUCKS
    )
  )

  private val bunkerConfig = BunkerConfig(
    Set(),
    Set(),
    Set(),
    Set(),
    Set(),
    Set(),
    carsConfig,
    trucksConfig
  )

  private val RyazanOblast = 10776

  private val Moscow = 213

  private val Protvino = 20576

  private val Ryazan = 11

  private val Tula = 15

  test("call centers distribution for cars") {
    rows.clear()
    val category = Category.CARS
    components.features.DistributeCars.setEnabled(true)

    val distributor =
      new Distributor(batchSize, category, workMoments, parsedOffersDao, components.features, components.regionTree) {
        override def start(): Unit = ???

        override def shouldWork: Boolean = true

        override def stop(): Unit = ???

        override def bunkerConfig: BunkerConfig = distributorTest.bunkerConfig
      }
    (1 to 100).foreach(_ => {
      val url = testAvitoCarsUrl
      val row = testRow(url, category = category, geobaseId = Protvino)
      rows += row.hash -> row
    })

    (1 to 100).foreach(_ => {
      val url = testAvitoCarsUrl
      val row = testRow(url, category = category, geobaseId = RyazanOblast)
      rows += row.hash -> row
    })
    (1 to 100).foreach(_ => {
      val url = testAvitoCarsUrl
      val row = testRow(url, category = category, geobaseId = Moscow).copy(callCenter = Some("unknown_cc"))
      rows += row.hash -> row
    })
    val url = testAvitoCarsUrl
    val row = testRow(url, category = category, geobaseId = Tula)
    rows += row.hash -> row
    (1 to 100).foreach(_ => {
      val url = testDromCarsUrl
      val row = testRow(url, category = category, geobaseId = Moscow)
      rows += row.hash -> row
    })
    (1 to 100).foreach(_ => {
      val url = testDromCarsUrl
      val row = testRow(url, category = category, geobaseId = Ryazan)
      rows += row.hash -> row
    })
    (1 to 100).foreach(_ => {
      val url = testAmruCarsUrl
      val row = testRow(url, category = category, geobaseId = Moscow)
      rows += row.hash -> row
    })
    (1 to 100).foreach(_ => {
      val url = testAmruCarsUrl
      val row = testRow(url, category = category, geobaseId = Ryazan)
      rows += row.hash -> row
    })
    distributor.distribute(parsedOffersDao.getReadyWithoutCallCenter(category, batchSize, Seq("cc1", "cc2")))
    assert(
      rows.values.count(r =>
        r.site == Site.Drom && !regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc1")
      ) == 0
    )
    assert(
      rows.values.count(r =>
        r.site == Site.Drom && regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc1")
      ) == 100
    )
    assert(
      rows.values.count(r =>
        r.site == Site.Drom && !regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc2")
      ) == 100
    )
    assert(
      rows.values.count(r =>
        r.site == Site.Drom && regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc2")
      ) == 0
    )

    assert(
      rows.values.count(r =>
        r.site == Site.Avito && !regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc1")
      ) == 70
    )
    assert(
      rows.values.count(r =>
        r.site == Site.Avito && regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc1")
      ) == 100
    )
    assert(
      rows.values.count(r =>
        r.site == Site.Avito && !regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc2")
      ) == 131
    )
    assert(
      rows.values.count(r =>
        r.site == Site.Avito && regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc2")
      ) == 0
    )

    assert(
      rows.values.count(r =>
        r.site == Site.Amru && !regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc1")
      ) == 35
    )
    assert(
      rows.values.count(r =>
        r.site == Site.Amru && regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc1")
      ) == 100
    )
    assert(
      rows.values.count(r =>
        r.site == Site.Amru && !regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc2")
      ) == 65
    )
    assert(
      rows.values.count(r =>
        r.site == Site.Amru && regionTree.isInside(r.geobaseId, Set(RyazanOblast)) && r.callCenter.contains("cc2")
      ) == 0
    )

    components.features.DistributeCars.setEnabled(false)
  }

  test("call centers distribution for trucks") {
    components.features.DistributeTrucks.setEnabled(true)
    rows.clear()
    val category = Category.TRUCKS

    val distributor =
      new Distributor(batchSize, category, workMoments, parsedOffersDao, components.features, components.regionTree) {
        override def start(): Unit = ???

        override def shouldWork: Boolean = true

        override def stop(): Unit = ???

        override def bunkerConfig: BunkerConfig = distributorTest.bunkerConfig
      }
    (1 to 100).foreach(_ => {
      val url = testAvitoTrucksUrl
      val row = testRow(url, category = category, geobaseId = Moscow)
      rows += row.hash -> row
    })
    (1 to 100).foreach(_ => {
      val url = testDromTrucksUrl
      val row = testRow(url, category = category, geobaseId = Moscow)
      rows += row.hash -> row
    })
    distributor.distribute(parsedOffersDao.getReadyWithoutCallCenter(category, batchSize, Seq("cc1")))
    assert(rows.values.count(r => r.site == Site.Drom && r.callCenter.contains("cc1")) == 100)
    assert(rows.values.count(r => r.site == Site.Avito && r.callCenter.contains("cc1")) == 100)
    components.features.DistributeTrucks.setEnabled(false)
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
