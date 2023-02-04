package ru.yandex.vertis.general.woody.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.woody.model.Queries.toStorageQueries
import ru.yandex.vertis.general.woody.model.{
  PgCheckedCluster,
  PreValidateQueries,
  Queries,
  RegionCount,
  Statistics,
  StatisticsCluster,
  StoragePreValidateCluster
}
import ru.yandex.vertis.general.woody.storage.postgresql.PgPreValidateQueriesDao
import ru.yandex.vertis.general.woody.storage.PreValidateQueriesDao
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object PreValidateQueriesSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PreValidateQueriesDao")(
      testM("crud prevalid queries") {
        val validQueries =
          Seq(
            PreValidateQueries(
              "cluster",
              Queries(Seq("queries")),
              Some(true),
              Some(Statistics(Seq(RegionCount(1, 5))))
            ),
            PreValidateQueries(
              "cluster1",
              Queries(Seq("queries1", "queries2")),
              Some(true),
              Some(Statistics(Seq(RegionCount(2, 7), RegionCount(15, 10))))
            )
          )

        val updatedValidQueries = Seq(
          PreValidateQueries(
            "cluster",
            Queries(Seq("updatedQueries")),
            Some(true),
            Some(Statistics(Seq(RegionCount(5, 2))))
          ),
          PreValidateQueries(
            "cluster1",
            Queries(Seq("updatedQueries1", "updatedQueries2")),
            Some(true),
            Some(Statistics(Seq(RegionCount(25, 15), RegionCount(1, 1))))
          )
        )

        def preValidateQueriesToPreValidateCluster(queries: Seq[PreValidateQueries]) =
          queries.map(q => StoragePreValidateCluster(q.cluster, toStorageQueries(q.queries)))

        def preValidateQueriesToPgCheckedCluster(queries: Seq[PreValidateQueries]) =
          queries.map(q => PgCheckedCluster(q.cluster, q.isValid.getOrElse(false)))

        def preValidateQueriesToStatisticsCluster(queries: Seq[PreValidateQueries]) =
          queries.map(q => StatisticsCluster(q.cluster, q.statistics.getOrElse(Statistics(Seq.empty))))

        for {
          dao <- ZIO.service[PreValidateQueriesDao.Service]
          _ <- dao.createOrUpdate(preValidateQueriesToPreValidateCluster(validQueries)).transactIO
          created1 <- dao.getByCluster(validQueries.head.cluster).transactIO
          created2 <- dao.getByCluster(validQueries.last.cluster).transactIO

          uncheckedQueries <- dao.getUnchecked.transactIO

          _ <- dao.updateValidation(preValidateQueriesToPgCheckedCluster(validQueries)).transactIO

          checkedValidQueriesResult <- dao.getByValid(true).transactIO

          _ <- dao.createOrUpdate(preValidateQueriesToPreValidateCluster(updatedValidQueries)).transactIO
          updated1 <- dao.getByCluster(validQueries.head.cluster).transactIO
          updated2 <- dao.getByCluster(validQueries.last.cluster).transactIO

          _ <- dao.updateStatistics(preValidateQueriesToStatisticsCluster(updatedValidQueries)).transactIO
          withStatistics1 <- dao.getByCluster(validQueries.head.cluster).transactIO
          withStatistics2 <- dao.getByCluster(validQueries.last.cluster).transactIO

          allQueries <- dao.getAll.transactIO

          _ <- dao.delete(validQueries.head.cluster).transactIO
          deleted <- dao.getByCluster(validQueries.head.cluster).transactIO

        } yield {
          val createdQueries = validQueries.map(_.copy(statistics = None, isValid = None))
          val updatedQueries = updatedValidQueries.map(_.copy(statistics = None))
          val checkedValidQueries = validQueries.map(_.copy(statistics = None))

          val createdQueriesResult = Seq(created1, created2).collect { case Some(v) => v }
          val updatedQueriesResult = Seq(updated1, updated2).collect { case Some(v) => v }
          val withStatisticsQueriesResult = Seq(withStatistics1, withStatistics2).collect { case Some(v) => v }

          assert(createdQueriesResult)(equalTo(createdQueries)) &&
          assert(updatedQueriesResult)(equalTo(updatedQueries)) &&
          assert(uncheckedQueries.size)(equalTo(2)) &&
          assert(checkedValidQueriesResult)(equalTo(checkedValidQueries)) &&
          assert(withStatisticsQueriesResult)(equalTo(updatedValidQueries)) &&
          assert(allQueries.size)(equalTo(2)) &&
          assert(deleted)(isNone)
        }
      }
    ) @@ sequential)
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgPreValidateQueriesDao.live
      }
  }
}
