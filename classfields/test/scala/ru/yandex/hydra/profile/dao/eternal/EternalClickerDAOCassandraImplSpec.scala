package ru.yandex.hydra.profile.dao.eternal

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.hydra.profile.dao.SpecBase
import ru.yandex.hydra.profile.dao.cassandra.TestCassandra

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

/** @author zvez
  */
class EternalClickerDAOCassandraImplSpec
  extends TestKit(ActorSystem("test"))
  with SpecBase
  with ScalaFutures
  with BeforeAndAfterAll
  with TestCassandra {

  val dao = new EternalClickerDAOCassandraImpl(
    session,
    "test_" + Random.nextInt().abs,
    "test",
    createTable = true
  )

  override protected def afterAll(): Unit = {
    session.execute(s"drop table if exists ${dao.tableName}")
    session.getCluster.close()
    super.afterAll()
  }

  "EternalClickerDao" should {
    "return 0 for unknown id" in {
      dao.get("some").futureValue shouldBe 0
    }

    "increment and return new value" in {
      val id = Random.nextLong().toString
      (0 until 10).foreach { i =>
        dao.get(id).futureValue shouldBe i
        dao.increment(id).futureValue
      }
    }
  }
}
