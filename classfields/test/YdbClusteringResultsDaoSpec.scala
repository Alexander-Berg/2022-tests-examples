package ru.yandex.vertis.general.aglomerat.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.aglomerat.storage.model.OfferFields
import ru.yandex.vertis.general.aglomerat.model.{ClusterId, ClusterType, ClusteringResult, OfferId}
import ru.yandex.vertis.general.aglomerat.storage.ClusteringResultsDao
import ru.yandex.vertis.general.aglomerat.storage.ydb.YdbClusteringResultsDao
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import zio.test._

object YdbClusteringResultsDaoSpec extends DefaultRunnableSpec {

  private def clusteringResult(offerId: OfferId, clusterId: ClusterId, version: Int) = {
    ClusteringResult(
      offerId = offerId,
      clusterId = clusterId,
      version = version,
      clusterType = ClusterType.FullDuplicates,
      offerFields = OfferFields(Seq.empty)
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbClusteringResultDao")(
      testM("save result") {
        val value = clusteringResult(OfferId("someofferid321"), ClusterId("somecluster654"), 4)
        for {
          _ <- runTx(ClusteringResultsDao.saveResults(value :: Nil))
        } yield assertCompletes
      },
      testM("get clusterId") {
        val clusterId = ClusterId("another_cluster654")
        val offerId = OfferId("another_offerid321")
        val version = 3
        val value = clusteringResult(offerId, clusterId, version)
        for {
          _ <- runTx(ClusteringResultsDao.saveResults(value :: Nil))
          res <- runTx(ClusteringResultsDao.getClusterIds(offerId :: Nil, ClusterType.FullDuplicates, version))
        } yield assertTrue {
          res.get(offerId).contains(clusterId)
        }
      },
      testM("get offerIds") {
        val clusterId = ClusterId("one_more_cluster_id234")
        val offerId = OfferId("one_more_offer_id_342234")
        val version = 5
        val value = clusteringResult(offerId, clusterId, version)
        for {
          _ <- runTx(ClusteringResultsDao.saveResults(value :: Nil))
          res <- runTx(ClusteringResultsDao.getOfferIds(ClusterType.FullDuplicates, clusterId, 5, LimitOffset(1, 0)))
        } yield assertTrue {
          res.contains(offerId)
        }
      },
      testM("get addresses") {
        val clusterId = ClusterId("one_more_cluster_id_86566523")
        val offerId = OfferId("one_more_offer_id_23423423")
        val version = 11
        val value = clusteringResult(offerId, clusterId, version)
        for {
          _ <- runTx(ClusteringResultsDao.saveResults(value :: Nil))
          result <- runTx(ClusteringResultsDao.getAddresses((ClusterType.FullDuplicates, offerId, version)))
        } yield assertTrue {
          result
            .get((ClusterType.FullDuplicates, offerId, version))
            .contains(value.offerFields.addresses)
        }
      },
      testM("delete results") {
        val offerId = OfferId("offer_to_delete")
        val clusterId = ClusterId("cluster_to_delete")
        val version = 77
        val value = clusteringResult(offerId, clusterId, version)
        for {
          _ <- runTx(ClusteringResultsDao.saveResults(value :: Nil))
          found <- runTx(ClusteringResultsDao.getClusterIds(offerId :: Nil, ClusterType.FullDuplicates, version))
          _ <- runTx(ClusteringResultsDao.deleteResults((value.clusterType, value.offerId, value.version)))
          notFound <- runTx(ClusteringResultsDao.getClusterIds(offerId :: Nil, ClusterType.FullDuplicates, version))
        } yield assertTrue {
          found.get(offerId).contains(clusterId) &&
          notFound.isEmpty
        }
      }
    )
  }.provideCustomLayerShared {
    TestYdb.ydb >>> (YdbClusteringResultsDao.live ++ Ydb.txRunner)
  }
}
