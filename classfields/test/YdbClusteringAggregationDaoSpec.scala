package ru.yandex.vertis.general.aglomerat.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.aglomerat.storage.model.Address
import ru.yandex.vertis.general.aglomerat.model._
import ru.yandex.vertis.general.aglomerat.storage.ClusteringAggregationDao
import ru.yandex.vertis.general.aglomerat.storage.ClusteringAggregationDao._
import ru.yandex.vertis.general.aglomerat.storage.ydb.YdbClusteringAggregationDao
import zio.ZIO
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion.{isNone, isSome}
import zio.test._
import zio.test.magnolia.DeriveGen

object YdbClusteringAggregationDaoSpec extends DefaultRunnableSpec {
  private val clusterTypeGen = DeriveGen.gen[ClusterType].derive
  private val addressesGen = Gen.const(Seq.empty[Address])

  private val clusteringAggregationGen = (Gen.anyInstant <&> addressesGen).map { case (now, addresses) =>
    FullDuplicatesAggregation(now, addresses)
  }
  private val clusterIdGen = DeriveGen.instance(Gen.anyUUID.map(uuid => ClusterId(uuid.toString)).noShrink).derive

  private val clusteringAggregationWithIdGen =
    DeriveGen
      .instance((clusterTypeGen <&> clusterIdGen <&> Gen.anyInt <&> clusteringAggregationGen).map {
        case (((clusterType, clusterId), version), aggregation) =>
          ClusteringAggregationWithId(clusterType, clusterId, version, aggregation)
      })
      .derive

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbClusteringAggregationDao")(
      testM("save aggregation") {
        for {
          values <- clusteringAggregationWithIdGen.runCollectN(10)
          _ <- runTx(ClusteringAggregationDao.saveClusteringAggregation(values))
        } yield assertCompletes
      },
      testM("get aggregation") {
        for {
          values <- clusteringAggregationWithIdGen.runCollectN(10)
          _ <- runTx(ClusteringAggregationDao.saveClusteringAggregation(values))
          results <- ZIO.foreach(values) { aggregation =>
            runTx(
              ClusteringAggregationDao.getClusteringAggregation(
                (aggregation.clusterType, aggregation.clusterId, aggregation.version)
              )
            )
          }
        } yield {
          val excepted = values.map(withId =>
            withId.aggregation match {
              case FullDuplicatesAggregation(indexedAt, addresses) => FullDuplicatesAggregation(indexedAt, addresses)
            }
          )
          assert(excepted)(Assertion.hasSameElements(results.flatMap(_.values.headOption)))
        }
      },
      testM("delete aggregation") {
        for {
          value <- clusteringAggregationWithIdGen.runHead.map(_.get)
          _ <- runTx(saveClusteringAggregation(value :: Nil))
          found <- runTx(
            getClusteringAggregation((value.clusterType, value.clusterId, value.version)).map(_.values.headOption)
          )
          toDelete = (value.clusterType, value.clusterId, value.version)
          _ <- runTx(deleteClusteringAggregation(toDelete))
          notFound <- runTx(
            getClusteringAggregation((value.clusterType, value.clusterId, value.version)).map(_.values.headOption)
          )
        } yield assert(found)(isSome) && assert(notFound)(isNone)
      }
    )
  }.provideCustomLayerShared {
    TestYdb.ydb >>> (YdbClusteringAggregationDao.live ++ Ydb.txRunner ++ Random.live ++ Clock.live ++ Sized.live(10))
  }
}
