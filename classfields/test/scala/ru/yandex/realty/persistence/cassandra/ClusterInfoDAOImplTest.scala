package ru.yandex.realty.persistence.cassandra

import org.joda.time.Instant
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.application.IndexerCassandraSession
import ru.yandex.realty.application.ng.cassandra.CassandraSessionConfig
import ru.yandex.realty.persistence.ClusterInfo

/**
  * Created by abulychev on 18.07.16.
  */
class ClusterInfoDAOImplTest
  extends FlatSpec
  with Matchers
  with ScalaFutures
  with OfferSupport
  with IndexerCassandraSession {

  override def indexerCassandraSessionConfig: CassandraSessionConfig = ???

  val db =
    new CassandraClusterAPI(indexerCassandraSession) with ClusterInfoDAOImpl with ClusterTracerDAOImpl

  "ClusterInfoDAO" should "return empty result for unknown offer" in {
    db.getClusterInfo(1, Seq("1", "2", "3")) should equal(Map.empty)
  }

  it should "update data" in {
    val date1 = Instant.parse("2016-01-01T00:00:00.000Z").toDate
    val date2 = Instant.parse("2015-01-01T00:00:00.000Z").toDate

    val map = Map("1" -> ClusterInfo(date1), "2" -> ClusterInfo(date2))

    db.updateClusterInfo(2, map)
    db.getClusterInfo(2, Seq("1", "2", "3")) should equal(map)
  }
}
