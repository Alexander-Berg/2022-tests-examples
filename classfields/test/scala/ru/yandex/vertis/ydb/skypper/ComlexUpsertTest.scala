package ru.yandex.vertis.ydb.skypper

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils.YdbInterpolator

@RunWith(classOf[JUnitRunner])
class ComlexUpsertTest extends AnyFunSuite with Matchers with InitTestYdb {
  implicit private val trace: Traced = Traced.empty

  private val testShardId = 0
  private val testOfferId = "123test"

  private val workersSeq = Seq(
    "worker1",
    "worker2",
    "worker3",
    "worker4",
    "worker5",
    "worker6",
    "worker7",
    "worker8",
    "worker9",
    "worker10",
    "worker11",
    "worker12",
    "worker13",
    "worker14",
    "worker15",
    "worker16",
    "worker17",
    "worker18",
    "worker19",
    "worker20",
    "worker21",
    "worker22",
    "worker23",
    "worker24",
    "worker25",
    "worker26",
    "worker27"
  )
  test("multiply-upsert-test") {
    val values = workersSeq.map(worker => (testShardId, worker, new DateTime(), testOfferId))
    val prepQuery = ydb"upsert into workers_queue (shard_id,worker, next_check, offer_id) values $values"
    ydb.updatePrepared("test-Upsert")(prepQuery)
    val res = ydb
      .queryPrepared("check-count") {
        ydb"select count(*) from workers_queue"
      }(YdbReads(rs => {
        rs.getColumn(0).getUint64
      }), trace)
      .next()
    assert(res == workersSeq.size)
  }

  test("multiply-upsert-test-newlined") {
    val values = workersSeq.map(worker => (testShardId, worker, new DateTime(), testOfferId))
    val prepQuery =
      ydb"""
           | upsert into workers_queue(shard_id,worker, next_check, offer_id)
           | values $values""".stripMargin
    ydb.updatePrepared("test-Upsert")(prepQuery)
    val res = ydb
      .queryPrepared("check-count") {
        ydb"select count(*) from workers_queue"
      }(YdbReads(rs => {
        rs.getColumn(0).getUint64
      }), trace)
      .next()
    assert(res == workersSeq.size)
  }
}
