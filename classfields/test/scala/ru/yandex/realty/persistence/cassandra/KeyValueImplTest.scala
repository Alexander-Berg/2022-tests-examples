package ru.yandex.realty.persistence.cassandra

import com.datastax.driver.core.Session
import org.scalatest._
import ru.yandex.realty.application.IndexerCassandraSession
import ru.yandex.realty.application.ng.cassandra.CassandraSessionConfig
import ru.yandex.realty.persistence.cassandra.update.single.KeyValueImpl
import ru.yandex.realty.persistence._
import ru.yandex.realty.persistence.cassandra.update.partitioned.PartitionedKeyValueImpl

class SingleKeyValueImplTest extends FlatSpec with BaseKeyValueImplTest {
  override def newKeyValue(policy: PersistPolicy): KeyValue = {
    new KeyValueImpl(
      session = session,
      table = "skv1",
      keyColumn = "k",
      valueColumn = "v",
      createIfNotExists = true,
      persistPolicy = policy
    )
  }
}

class PartitionedKeyValueImplTest extends FlatSpec with BaseKeyValueImplTest {
  override def newKeyValue(policy: PersistPolicy): KeyValue = {
    val pkv = PartitionedKeyValueImpl(
      session = session,
      table = "pkv1",
      partitionColumn = "p",
      keyColumn = "k",
      valueColumn = "v",
      createIfNotExists = true,
      persistPolicy = policy
    )

    pkv.get(0).get
  }
}

trait BaseKeyValueImplTest extends FlatSpecLike with Matchers with BeforeAndAfterAll with IndexerCassandraSession {

  import KeyValueImplTest._

  override def indexerCassandraSessionConfig: CassandraSessionConfig = ???

  def session: Session = indexerCassandraSession

  def newKeyValue(policy: PersistPolicy): KeyValue

  // Sleeping to initialize
  Thread.sleep(1000)

  override def afterAll() {
    newKeyValue(PersistPolicy.Immediately).clean()
    // Sleeping to deinitialize
    Thread.sleep(1000)
  }

  "KeyValue" should "be empty on start" in {
    val kv = newKeyValue(PersistPolicy.Immediately)
    kv.keySet should equal(Set.empty)
  }

  it should "add new values" in {
    val kv = newKeyValue(PersistPolicy.Immediately)

    kv.put("1", "123".getBytes).get
    kv.put("2", "890".getBytes).get

    kv.get("1").get.value.asString should equal("123")
    kv.get("2").get.value.asString should equal("890")
  }

  it should "remember saved values" in {
    val kv = newKeyValue(PersistPolicy.Immediately)

    kv.keySet should equal(Set("1", "2"))
    kv.get("1").get.value.asString should equal("123")
    kv.get("2").get.value.asString should equal("890")
  }

  it should "remove values" in {
    val kv = newKeyValue(PersistPolicy.Immediately)
    kv.delete("1").get
    kv.get("1") should equal(None)
  }

  it should "not remember deleted values" in {
    val kv = newKeyValue(PersistPolicy.Immediately)

    kv.keySet should equal(Set("2"))
    kv.get("1") should equal(None)
  }

  it should "rewrite values" in {
    val kv = newKeyValue(PersistPolicy.Immediately)

    kv.put("1", "456".getBytes).get
    kv.get("1").get.value.asString should equal("456")

    val kv2 = newKeyValue(PersistPolicy.Immediately)
    kv2.get("1").get.value.asString should equal("456")
  }

  it should "work properly with by demand persist policy" in {
    val kv = newKeyValue(PersistPolicy.OnDemand)

    kv.put("8", "1".getBytes).get
    kv.put("9", "2".getBytes).get
    kv.get("8").get.value.asString should equal("1")
    kv.get("9").get.value.asString should equal("2")

    val kv2 = newKeyValue(PersistPolicy.OnDemand)
    kv2.get("8") should equal(None)
    kv2.get("9") should equal(None)

    kv.persist().get
    val kv3 = newKeyValue(PersistPolicy.OnDemand)
    kv3.get("8").get.value.asString should equal("1")
    kv3.get("9").get.value.asString should equal("2")
  }

  it should "work correct with force put" in {
    val kv = newKeyValue(PersistPolicy.OnDemand)

    kv.clean().get

    kv.put(Map("1" -> "1".getBytes, "2" -> "2".getBytes)).get
    kv.get("1").get.value.asString should equal("1")
    kv.get("2").get.value.asString should equal("2")

    kv.put(Map("1" -> "100".getBytes, "3" -> "3".getBytes), force = true).get
    kv.get("1").get.value.asString should equal("100")
    kv.get("2").get.value.asString should equal("2")
    kv.get("3").get.value.asString should equal("3")

    val kv2 = newKeyValue(PersistPolicy.OnDemand)
    kv2.get("1").get.value.asString should equal("100")
    kv2.get("2") should equal(None)
    kv2.get("3").get.value.asString should equal("3")

    kv.persist()

    val kv3 = newKeyValue(PersistPolicy.OnDemand)
    kv3.get("1").get.value.asString should equal("100")
    kv3.get("2").get.value.asString should equal("2")
    kv3.get("3").get.value.asString should equal("3")
  }

}

object KeyValueImplTest {
  implicit class ByteArrayWithAsString(val x: Array[Byte]) extends AnyVal {
    def asString: String = new String(x)
  }
}
