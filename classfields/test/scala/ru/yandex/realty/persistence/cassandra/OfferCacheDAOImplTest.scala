package ru.yandex.realty.persistence.cassandra

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.application.IndexerCassandraSession
import ru.yandex.realty.application.ng.cassandra.CassandraSessionConfig
import ru.yandex.realty.application.ng.{DefaultMdsUrlBuilderSupplier, MdsUrlBuilderConfig}
import ru.yandex.realty.persistance.OfferCacheType._
import ru.yandex.realty.persistence.cassandra.KeyValueImplTest._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 15.09.16
  */
class OfferCacheDAOImplTest
  extends FlatSpec
  with Matchers
  with ScalaFutures
  with DefaultMdsUrlBuilderSupplier
  with OfferSupport
  with IndexerCassandraSession {

  override def mdsUrlBuilderConfig: MdsUrlBuilderConfig = ???

  override def indexerCassandraSessionConfig: CassandraSessionConfig = ???

  val db = new CassandraAPI(indexerCassandraSession, mdsUrlBuilder)

  val groupId = 2
  val offerId1 = "2"
  val offerId2 = "3"

  implicit val config = PatienceConfig(timeout = scaled(Span(1000, Millis)), interval = scaled(Span(300, Millis)))

  "OfferCacheDAO" should "return None on empty offer" in {
    db.getCache(1, "1").futureValue should be(None)
  }

  it should "return prior added values" in {
    db.putCache(groupId, offerId1, REALTIME_CASSANDRA_HASH, "123".getBytes).futureValue
    db.getCache(groupId, offerId1).futureValue.get.get(REALTIME_CASSANDRA_HASH).get.asString should be("123")
  }

  it should "update values" in {

    db.putCache(groupId, offerId1, REALTIME_CASSANDRA_HASH, "456".getBytes).futureValue
    db.putCache(groupId, offerId1, VOS_OFFER_STATE_HASH, "678".getBytes).futureValue

    val cache = db.getCache(groupId, offerId1).futureValue.get
    cache.get(REALTIME_CASSANDRA_HASH).get.asString should be("456")
    cache.get(VOS_OFFER_STATE_HASH).get.asString should be("678")
  }

  it should "update values batched" in {
    val values = Map(
      offerId1 -> "aaa".getBytes,
      offerId2 -> "bbb".getBytes
    )

    db.putCache(groupId, VOS_OFFER_STATE_HASH, values).futureValue

    val cache1 = db.getCache(groupId, offerId1).futureValue.get
    cache1.get(REALTIME_CASSANDRA_HASH).get.asString should be("456")
    cache1.get(VOS_OFFER_STATE_HASH).get.asString should be("aaa")

    val cache2 = db.getCache(groupId, offerId2).futureValue.get
    cache2.get(VOS_OFFER_STATE_HASH).get.asString should be("bbb")
  }
}
