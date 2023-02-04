package ru.yandex.auto.vin.decoder.scheduler.local.utils

import auto.carfax.common.utils.app.TestJaegerTracingSupport
import auto.carfax.common.utils.config.Environment
import ru.yandex.auto.vin.decoder.components.DefaultCoreComponents
import ru.yandex.auto.vin.decoder.ydb.YdbConfig
import ru.yandex.auto.vin.decoder.yql.{YQLJdbc, YqlConfig}
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.time.LocalDateTime

object PushData extends App with TestJaegerTracingSupport {

  implicit val ops = TestOperationalSupport
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val coreComponents = new DefaultCoreComponents()

  val yqlConfig: YqlConfig = YqlConfig.fromConfig(Environment.config.getConfig("auto-vin-decoder.yql.config"))
  val yqlJdbc: YQLJdbc = new YQLJdbc(yqlConfig)
  val ydbConfig: YdbConfig = YdbConfig.fromConfig(Environment.config.getConfig("auto-vin-decoder.ydb"))

  val sourceTable = "//home/verticals/carfax/raw_storage/vin_raw_storage"
  val tableName = "/carfax/vin_raw_storage"

  val query = buildQuery(
    sourceTable,
    ydbConfig.root,
    ydbConfig.endpoint,
    tableName,
    ydbConfig.token,
    parallelJobs = 20
  )

  val result = {
    println(s"Started at ${LocalDateTime.now()}")
    yqlJdbc.execute(
      query,
      rs => {
        val bytes = rs.getLong("TotalBytes")
        val rows = rs.getLong("TotalRows")
        println(s"Total bytes $bytes")
        println(s"Total rows $rows")
        println(s"Finished at ${LocalDateTime.now()}")
      }
    )
  }

  val y = 2

  def buildQuery(
      sourceYTTable: String,
      db: String,
      endpoint: String,
      name: String,
      token: String,
      parallelJobs: Int = 7,
      maxJobFails: Int = 50): String = {
    val tableName = db + name
    val copyToYbd =
      s"""
         |USE `$db`;
         |
         |$$ydb_endpoint = "$endpoint";
         |$$ydb_database = "$db";
         |$$ydb_table = "$tableName";
         |
         |PRAGMA yt.InferSchema;
         |-- Please note that YQL schema not specified, so we're telling YQL to infer it from data.
         |PRAGMA yt.InferSchema = '1';
         |
         |PRAGMA yt.QueryCacheMode = "disable";
         |PRAGMA yt.DataSizePerJob = "67108864";
         |PRAGMA yt.MaxJobCount = "1000";
         |PRAGMA yt.UserSlots = "$parallelJobs";
         |PRAGMA yt.DefaultMaxJobFails = "$maxJobFails";
         |
         |
         |
         |
         |SELECT
         |    SUM(Bytes) AS TotalBytes,
         |    SUM(Rows) AS TotalRows,
         |    SUM(Batches) AS TotalBatches,
         |    SUM(Retries) AS TotalRetries
         |FROM (
         |    PROCESS (
         |    select
         |        cast(vin as utf8) as vin,
         |        cast(source_id as utf8) as source_id,
         |        cast(timestamp_create as uint64) as timestamp_create,
         |        cast(raw_data_hash as utf8) as raw_data_hash,
         |        cast(timestamp_update as uint64) as timestamp_update,
         |        cast(raw_data as utf8) as raw_data,
         |        cast(response_status as utf8) as response_status,
         |        cast(prepared_data as Json) as prepared_data,
         |        cast(timestamp_added as uint64) as timestamp_added,
         |        cast(group_id as utf8) as group_id
         |        FROM hahn.`$sourceYTTable`
         |        where vin = 'XW8AD6NE4GH007727'
         |    )
         |    USING YDB::PushData(
         |        TableRows(),
         |        $$ydb_endpoint,
         |        $$ydb_database,
         |        $$ydb_table,
         |        AsTuple("unsecure", "$token")
         |    )
         |);
    """.stripMargin
    copyToYbd
  }

}
