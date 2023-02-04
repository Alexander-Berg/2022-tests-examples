package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.old_db_migration

import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Inspectors, OptionValues}
import ru.yandex.common.tokenization.TokensDistributor
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.utils.TokenDistributionUtils
import ru.yandex.vos2.model.OfferRef

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class DistributedMigrationTest
  extends AnyFunSuite
  with OptionValues
  with Eventually
  with InitTestDbs
  with Inspectors
  with BeforeAndAfterAll {

  implicit private val t = Traced.empty

  initDbs()

  val (engine1, migrationTokensDistribution1) = createCarsMigrationEngine()
  val (engine2, migrationTokensDistribution2) = createCarsMigrationEngine()

  /**
    * Запускаем как бы два migrationEngine, и проверяем у каждого tokensDistribution -
    * туда должны попасть разные токены - числа от 0 до 7, 4 штуки одному, 4 другому,
    * у каждого токена должен быть владелец - либо первый движок, либо второй
    */
  test("testMigrationDistribution") {
    val tokens1 = migrationTokensDistribution1.tokenDistribution.getTokens
    val tokens2 = migrationTokensDistribution2.tokenDistribution.getTokens

    assert(tokens1.size == 4)
    assert(tokens1.size == tokens2.size)

    eventually(Timeout(2.minutes)) {
      assert(migrationTokensDistribution1.tokenDistribution.getOwnerships.size == 8)
    }
    val ownerships = migrationTokensDistribution1.tokenDistribution.getOwnerships
    assert(ownerships.groupBy(_.owner).size == 2)
    assert(ownerships.groupBy(_.owner).forall(_._2.size == 4))
    assert(ownerships.toList.map(_.token).distinct.length == ownerships.size)
    forEvery(0 until 8) { bucket =>
      assert(ownerships.getOwner(bucket.toString).nonEmpty, s"Bucket $bucket have no owner")
    }
    val allSales = saleIds.map(saleId => getSaleByIdFromDb(saleId))
    val exists = allSales.map(sale => sale.offerRef -> components.offerVosDao.findByRef(sale.offerRef).nonEmpty).toMap

    // engine -> map(bucket -> sale)
    val saleIdsByEngines = allSales
      .map(sale => (sale, (sale.id % 8).toInt))
      .groupBy(x => if (tokens1.contains(x._2.toString)) 1 else 2)
      .map(kv => (kv._1, kv._2.groupBy(_._2).map(xy => (xy._1, xy._2.map(_._1)))))

    // проверяем, что успешно мигрируются объявления из бакетов, которые принадлежат нам
    saleIdsByEngines(1).foreach {
      case (ownBucket, sales) =>
        val offers = sales.map { sale =>
          components.carOfferConverter
            .convertStrict(sale, components.offerVosDao.findByRef(sale.offerRef))
            .converted
            .value
        }
        // объявлений, относящихся к данному бакету, пока еще в базе нет
        assert(offers.forall(offer => components.offerVosDao.findByRef(OfferRef(offer), includeRemoved = true).isEmpty))
        // делаем миграцию данного бакета. Бакет принадлежит нам.
        engine1.process(engine1.createPrometheusMetrics)
        // объявления, относящиеся к данному бакету, появились в базе
        forEvery(offers.map(OfferRef.apply)) { offerRef =>
          val found = components.offerVosDao.findByRef(offerRef, includeRemoved = true)
          assert(found.nonEmpty == exists(offerRef), offerRef)
        }
    }
    // проверяем, что объявления чужих бакетов мы не мигрируем
    saleIdsByEngines(2).foreach {
      case (ownBucket, sales) =>
        val offers = sales.map { sale =>
          components.carOfferConverter
            .convertStrict(sale, components.offerVosDao.findByRef(sale.offerRef))
            .convertedOrFail
        }
        // объявлений, относящихся к данному бакету, пока еще в базе нет
        assert(offers.forall(offer => components.offerVosDao.findByRef(OfferRef(offer), includeRemoved = true).isEmpty))
        // делаем миграцию данного бакета. Бакет нам не принадлежит, так что ничего не должно измениться.
        engine1.process(engine1.createPrometheusMetrics)
        // объявления, относящиеся к данному бакету, в базе не появились
        assert(offers.forall(offer => components.offerVosDao.findByRef(OfferRef(offer), includeRemoved = true).isEmpty))
        // делаем миграцию данного бакета другим движком. Бакет ему принадлежит, объявления должны смигрироваться.
        engine2.process(engine2.createPrometheusMetrics)

        // объявления, относящиеся к данному бакету, в базе появились
        forEvery(offers.map(OfferRef.apply)) { offerRef =>
          val found = components.offerVosDao.findByRef(offerRef, includeRemoved = true)
          assert(found.nonEmpty == exists(offerRef), offerRef)
        }
    }
  }

  private def createCarsMigrationEngine() = {
    val tokensDistributorConfig = TokensDistributor.Config.Default.copy(subscribePeriod = 5.seconds)
    val migrationTokensDistribution =
      TokenDistributionUtils.createTokenDistribution("migration_test", 8, tokensDistributorConfig, components)

    val engine = MigrationEngines
      .create(
        components.env.props.getConfig("migration"),
        components.offerVosDao,
        components.offerYdbDao,
        components.migrationSalesDao,
        components.migrationTruckSalesDao,
        components.migrationMotoSalesDao,
        components.carOfferConverter,
        components.truckOfferConverter,
        components.motoOfferConverter,
        migrationTokensDistribution,
        TestOperationalSupport,
        components.featuresManager
      )
      .cars

    engine -> migrationTokensDistribution
  }
}
