package ru.yandex.realty.searcher.personalization.persistence.cassandra

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.application.ng.cassandra.CassandraSessionConfig
import ru.yandex.realty.application.{IndexerCassandraSession, RealtimeCassandraSession}
import ru.yandex.realty.util.TestPropertiesSetup

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 20.12.16
  */
class DeletedClustersDAOImplTest extends FlatSpec with Matchers with ScalaFutures with RealtimeCassandraSession {
  val MaxDeletedClusters = 10

  override def realtimeCassandraSessionConfig: CassandraSessionConfig = ???

  implicit val config = PatienceConfig(timeout = scaled(Span(1000, Millis)), interval = scaled(Span(300, Millis)))
  private val session = realtimeCassandraSession
  private val api = new CassandraPersonalizationApi(session) {
    override def maxDeletedClusters: Int = MaxDeletedClusters
  }

  "DeletedClustersDAO" should "correct work in base case" in {
    api.deleteCluster("b", 1L).futureValue
    val res1 = api.findDeletedClusters("b").futureValue
    res1.size should be(1)
    res1.head should be(1L)
    api.findDeletedClustersCount("b").futureValue should be(res1.size)
    api.restoreCluster("b", 1L).futureValue
    api.findDeletedClusters("b").futureValue.size should be(0)
    api.findDeletedClustersCount("b").futureValue should be(0)
  }

  it should "correct work with too much deleted offers" in {
    api.asInstanceOf[DeletedClustersDAOImpl].restoreAllClusters("a")
    for (i <- 0.until(MaxDeletedClusters + 2)) {
      api.deleteCluster("a", i).futureValue
    }
    api.findDeletedClusters("a").futureValue.size should be(MaxDeletedClusters)
    api.restoreCluster("a", 5L).futureValue
    api.findDeletedClusters("a").futureValue.size should be(MaxDeletedClusters - 1)
  }

  it should "correct move deleted clusters" in {
    val impl = api.asInstanceOf[DeletedClustersDAOImpl]
    impl.restoreAllClusters("b")
    impl.restoreAllClusters("c")

    api.findDeletedClustersCount("b").futureValue should be(0)
    api.findDeletedClustersCount("c").futureValue should be(0)

    impl.deleteClusters("b", Seq(1L, 2L)).futureValue
    impl.deleteClusters("c", Seq(1L, 3L)).futureValue

    api.findDeletedClustersCount("b").futureValue should be(2)
    api.findDeletedClustersCount("c").futureValue should be(2)

    api.moveDeletedClusters("b", "c").futureValue

    api.findDeletedClustersCount("b").futureValue should be(0)
    api.findDeletedClustersCount("c").futureValue should be(3)
  }
}
