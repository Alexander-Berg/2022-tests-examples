package ru.yandex.realty.persistence.cassandra

import org.scalatest._
import ru.yandex.realty.application.IndexerCassandraSession
import ru.yandex.realty.application.ng.cassandra.CassandraSessionConfig
import ru.yandex.realty.persistence.cassandra.update.single.KeyValueImpl
import ru.yandex.realty.persistence._

class UpdateControllerImplSpec extends FlatSpec with Matchers with BeforeAndAfterAll with IndexerCassandraSession {

  override def indexerCassandraSessionConfig: CassandraSessionConfig = ???

  val session = indexerCassandraSession

  lazy val kv = new KeyValueImpl(session, "test_hashes2", persistPolicy = PersistPolicy.OnDemand)

  def newController(): UpdateController = {
    kv.reload()
    new UpdateController(kv, None)
  }

  // Sleeping to initialize
  Thread.sleep(1000)

  override def afterAll() {
    kv.clean()
    // Sleeping to deinitialize
    Thread.sleep(1000)
  }

  "UpdateController" should "add new entities" in {
    val h = newController()
    h.consume("1", "123".getBytes).get should equal(NewEntity)
    h.consume("2", "890".getBytes).get should equal(NewEntity)
    h.stale should equal(Set.empty)
    h.updated(Seq("1", "2")).get
    h.persist().get
  }

  it should "recognize same entities" in {
    val h = newController()
    h.consume("1", "123".getBytes).get should equal(HashNotUpdated)
    h.consume("2", "890".getBytes).get should equal(HashNotUpdated)
    h.stale should equal(Set.empty)
    h.persist().get
  }

  it should "recognize one same entity and one updated entity" in {
    val h = newController()
    h.consume("1", "123".getBytes).get should equal(HashNotUpdated)
    new String(h.consume("2", "234".getBytes).get.asInstanceOf[HashUpdated].old) should equal("890")
    h.stale should equal(Set.empty)
    h.updated("2").get
    h.persist().get
  }

  it should "recognize one same entity and one removed entity" in {
    val h = newController()
    h.consume("2", "234".getBytes).get should equal(HashNotUpdated)
    h.stale should equal(Set("1"))
    h.deleteStale().get
    h.persist().get

    val h2 = newController()
    h2.consume("2", "234".getBytes).get should equal(HashNotUpdated)
    h2.stale should equal(Set.empty)
    h2.persist().get
  }

  it should "work well with unfinished data" in {
    val h = newController()
    h.consume("1", "123".getBytes).get should equal(NewEntity)
    h.consume("2", "234".getBytes).get should equal(HashNotUpdated)
    h.stale should equal(Set.empty)
    // unfinished

    val h2 = newController()
    h2.consume("1", "123".getBytes).get should equal(Unfinished)
    h2.consume("2", "234".getBytes).get should equal(HashNotUpdated)
    h2.stale should equal(Set.empty)
    h2.updated("1").get
    h2.updated("2").get
    h2.persist().get

    val h3 = newController()
    h3.consume("1", "123".getBytes).get should equal(HashNotUpdated)
    h3.stale should equal(Set("2"))
    h3.deleted("2")
    h3.persist().get

    val h4 = newController()
    h4.consume("1", "123".getBytes).get should equal(HashNotUpdated)
    h4.consume("2", "234".getBytes).get should equal(NewEntity)
    h4.stale should equal(Set.empty)
    // unfinished

    val h5 = newController()
    h5.consume("1", "123".getBytes).get should equal(HashNotUpdated)
    h5.stale should equal(Set("2"))
    h5.deleteStale()
    h5.persist().get

    val h6 = newController()
    h6.oldEntities should equal(Set("1"))
  }
}
