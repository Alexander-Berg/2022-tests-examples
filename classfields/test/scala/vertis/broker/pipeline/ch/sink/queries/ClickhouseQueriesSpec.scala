package vertis.broker.pipeline.ch.sink.queries

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import vertis.clickhouse.model.ChTableId

class ClickhouseQueriesSpec extends AnyWordSpec with Matchers {

  "parseShowTable" should {
    "parse some table with ttl and settings" in {
      val show =
        """
          |CREATE TABLE stats.event
          |(
          |    `timestamp` DateTime64(9, 'Europe/Moscow'),
          |    `event_type` String,
          |    `offer_id` String,
          |    `_id` String,
          |    `_partition` String,
          |    `_offset` UInt64,
          |    `is_owner` UInt8
          |)
          |ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/stats.event', '{replica}')
          |PARTITION BY toYYYYMM(timestamp)
          |ORDER BY _id
          |TTL toDateTime(timestamp + toIntervalDay(180))
          |SETTINGS index_granularity = 8192, merge_with_ttl_timeout = 86400, ttl_only_drop_parts = 1
          |""".stripMargin.linesIterator.toSeq

      val res = ClickhouseQueries.parseShowTable(show)
      res.id shouldBe ChTableId("stats", "event")
      res.columns.length shouldBe 7
      res.expireInDays shouldBe Some(180)
      res.settings shouldBe Map(
        "index_granularity" -> 8192,
        "merge_with_ttl_timeout" -> 86400,
        "ttl_only_drop_parts" -> 1
      )
    }
  }

}
