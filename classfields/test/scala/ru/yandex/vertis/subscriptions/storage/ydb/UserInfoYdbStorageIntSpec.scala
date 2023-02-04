package ru.yandex.vertis.subscriptions.storage.ydb

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.subscriptions.SpecBase
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import ru.yandex.vertis.ydb.Ydb
import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import zio.RIO

import scala.concurrent.duration.DurationInt

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class UserInfoYdbStorageIntSpec extends SpecBase with TestYdb with ProducerProvider with BeforeAndAfterAll {

  def ioTest(action: UserInfoYdbStorage => RIO[YEnv, _]): Unit = {
    val ydb = YdbZioWrapper.make(container.tableClient, "/local", 3.seconds)
    val dao = new UserInfoYdbStorage(ydb)
    zioRuntime.unsafeRun(action(dao))
  }

  override def afterStart(): Unit = {
    super.afterStart()
    ioTest { dao =>
      dao.initSchema()
    }
  }

  override def beforeStop(): Unit = {
    container.tableClient.close()
    container.rpc.close()
    super.beforeStop()
  }

  "UserInfoYdbStorage" should {

    "return empty last touch" in ioTest { dao =>
      val user = CoreGenerators.userGen.next
      for {
        v <- Ydb.runTx(dao.getLastTouch(user))
      } yield v shouldBe None
    }

    "set last touch" in ioTest { dao =>
      val user = CoreGenerators.userGen.next
      val v1 = DateTime.now().minusDays(5)
      val v2 = DateTime.now()
      for {
        _ <- Ydb.runTx(dao.setLastTouch(user, v1))
        v1Loaded <- Ydb.runTx(dao.getLastTouch(user))
        _ = v1Loaded shouldBe Some(v1)
        _ <- Ydb.runTx(dao.setLastTouch(user, v2))
        v2Loaded <- Ydb.runTx(dao.getLastTouch(user))
      } yield v2Loaded shouldBe Some(v2)
    }
  }
}
