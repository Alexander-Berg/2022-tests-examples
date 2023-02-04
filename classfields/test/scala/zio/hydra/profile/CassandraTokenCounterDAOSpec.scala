package vertis.hydra.profile

import com.datastax.driver.core.Session
import common.zio.logging.Logging
import ru.yandex.hydra.profile.cassandra.testkit.TestCassandra
import ru.yandex.hydra.profile.dao.counter.TokenCounterDAO
import ru.yandex.hydra.profile.dao.counter.impl.CassandraTokenCounterDAO
import zio._
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._

import java.util.Random

object CassandraTokenCounterTestDaoSpec extends DefaultRunnableSpec {
  val PROJECT = "p" + math.abs(new Random().nextInt())
  val LOCALE = "l" + math.abs(new Random().nextInt())
  val COMPONENT = "c" + math.abs(new Random().nextInt())

  val dao: URLayer[Has[Session], Has[TokenCounterDAO]] =
    ZIO
      .service[Session]
      .map(
        new CassandraTokenCounterDAO(
          _,
          PROJECT,
          LOCALE,
          COMPONENT,
          createTable = true
        )
      )
      .toLayer

  val ID = "id" + math.abs(new Random().nextInt())
  val MEGA_ID = "megaid" + math.abs(new Random().nextInt())
  val count = 250

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    (suite("CassandraTokenCounterTestDao")(
      testM("register initial hit") {
        assertM(TestDao.get(ID))(equalTo(0)) *>
          TestDao.add(ID, "1") *>
          assertM(TestDao.get(ID))(equalTo(1))
      },
      testM("skip the same token") {
        TestDao.add(ID, "1") *>
          assertM(TestDao.get(ID))(equalTo(1))
      },
      testM("add another token") {
        assertM(TestDao.get(ID))(equalTo(1)) *>
          TestDao.add(ID, "2") *>
          assertM(TestDao.get(ID))(equalTo(2))
      },
      testM("skip another token") {
        TestDao.add(ID, "2") *>
          assertM(TestDao.get(ID))(equalTo(2))
      },
      testM(s"insert and count $count tokens") {
        for {
          _ <- ZIO.foreach_(0 until count)(i => TestDao.add(MEGA_ID, i.toString))
          (elapsed, result) <- TestDao.get(MEGA_ID).timed
          _ <- Logging.info(s"${elapsed.toMillis}ms to count $count tokens")

        } yield assert(result)(equalTo(count))
      },
      testM("fail empty multi ID") {
        assertM(TestDao.multiGet(Set.empty[String]).run)(fails(isSubtype[IllegalArgumentException](anything)))
      },
      testM("get nonexistent multi ID") {
        val ID = "id" + math.abs(new Random().nextInt())
        assertM(TestDao.multiGet(Set(ID)))(equalTo(Map(ID -> 0)))
      },
      testM("get one multi ID") {
        val ID = "id" + math.abs(new Random().nextInt())
        TestDao.add(ID, "1") *>
          assertM(TestDao.multiGet(Set(ID)))(equalTo(Map(ID -> 1)))
      },
      testM("get two multi IDs") {
        val ID1 = "id" + math.abs(new Random().nextInt())
        val ID2 = "id" + math.abs(new Random().nextInt())

        TestDao.add(ID1, "11") *>
          TestDao.add(ID2, "21") *>
          TestDao.add(ID2, "22") *>
          assertM(TestDao.multiGet(Set(ID1, ID2)))(equalTo(Map(ID1 -> 1, ID2 -> 2)))
      },
      testM("get two multi IDs and nonexistent ID") {
        val ID1 = "id" + math.abs(new Random().nextInt())
        val ID2 = "id" + math.abs(new Random().nextInt())
        val ID3 = "id" + math.abs(new Random().nextInt())

        TestDao.add(ID1, "11") *>
          TestDao.add(ID2, "21") *>
          TestDao.add(ID2, "22") *>
          assertM(TestDao.multiGet(Set(ID1, ID2, ID3)))(equalTo(Map(ID1 -> 1, ID2 -> 2, ID3 -> 0)))
      }
    ) @@ sequential).provideCustomLayerShared((TestCassandra.live >>> dao) ++ Logging.live)
}
