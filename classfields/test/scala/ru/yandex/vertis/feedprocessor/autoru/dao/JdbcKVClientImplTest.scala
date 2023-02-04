package ru.yandex.vertis.feedprocessor.autoru.dao

import org.scalatest.BeforeAndAfter
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.utils.OpsJdbc
import ru.yandex.vertis.feedprocessor.dao.BlockedKVClient
import ru.yandex.vertis.feedprocessor.util.{DatabaseSpec, DummyOpsSupport}

/**
  * @author pnaydenov
  */
class JdbcKVClientImplTest extends WordSpecBase with DatabaseSpec with BeforeAndAfter with DummyOpsSupport {
  implicit val opsJdbcMeters = new OpsJdbc.Meters(operationalSupport.prometheusRegistry)
  val kvClient: BlockedKVClient = new JdbcBlockedKVClientImpl(tasksDb)

  before {
    tasksDb.master.jdbc.update("TRUNCATE key_value")
  }

  "get existing key" in {
    kvClient.set("foo", "bar")
    kvClient.get("foo") should contain("bar")
  }

  "update key" in {
    kvClient.set("foo", "bar")
    kvClient.set("foo", "bar2")
    kvClient.get("foo") should contain("bar2")
  }

  "don't return non existing key" in {
    kvClient.set("foo", "bar")
    kvClient.get("foo2") shouldBe empty
  }

  "remove key" in {
    kvClient.set("foo", "bar")
    kvClient.remove("foo")
    kvClient.get("foo") shouldBe empty
  }
}
