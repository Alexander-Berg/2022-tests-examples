package vertis.zio.zk

import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.CreateMode
import ru.yandex.vertis.curator.recipes.map.StringValueSerializer
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.zk.ZioZkSubscriberIntSpec.Test
import vertis.zio.zk.listeners.ZkNodeListener
import vertis.zio.zk.test.ZkTest
import zio.UIO

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class ZioZkSubscriberIntSpec extends ZkTest {

  private def myPath(i: Int) = s"/my_path_$i"

  "ZioZkSubscriber" should {
    "publish updates to subscribers" in zkSubscriberTest(myPath(0)) { case Test(curator, subscriber) =>
      for {
        q <- subscriber.subscribe
        _ <- checkM("got initial empty value")(q.take.map(_ shouldBe ""))
        _ <- put(curator, myPath(0), "hello".getBytes)
        _ <- checkM("got update")(q.take.map(_ shouldBe "hello"))
        f <- checkM("got second update")(q.take.map(_ shouldBe "world")).fork
        _ <- put(curator, myPath(0), "world".getBytes)
        _ <- f.await
      } yield ()
    }

    "send last known state as an initial value" in zkSubscriberTest(myPath(1)) { case Test(curator, subscriber) =>
      for {
        q <- subscriber.subscribe.tap(_.take)
        _ <- put(curator, myPath(1), "hello".getBytes)
        _ <- checkM("got update")(q.take.map(_ shouldBe "hello"))
        q2 <- subscriber.subscribe
        _ <- checkM("got initial value")(q2.take.map(_ shouldBe "hello"))
      } yield ()
    }

    Iterator.from(2).take(10).foreach { i =>
      s"not fail on dead subscribers $i" in zkSubscriberTest(myPath(i)) { case Test(curator, subscriber) =>
        for {
          q <- subscriber.subscribe.tap(_.take)
          _ <- subscriber.subscribe.tap(_.shutdown)
          _ <- put(curator, myPath(i), "hello".getBytes)
          _ <- checkM("got update")(q.take.map(_ shouldBe "hello"))
          _ <- put(curator, myPath(i), "world".getBytes)
          _ <- checkM("got update")(q.take.map(_ shouldBe "world"))
        } yield ()
      }
    }
  }

  private def zkSubscriberTest(path: String)(body: Test[String] => TestBody) =
    zkTest { curator =>
      ZioZkSubscriber
        .make(
          new ZkNodeListener[String](curator, path, StringValueSerializer, "", logger),
          "",
          false
        )
        .map(Test(curator, _))
        .use(body)
    }

  private def put(curator: CuratorFramework, path: String, data: Array[Byte]): UIO[Unit] = UIO {
    curator.checkExists.forPath(path) match {
      // scalastyle:off null
      case null =>
        curator
          .create()
          .creatingParentsIfNeeded()
          .withMode(CreateMode.PERSISTENT)
          .forPath(path, data)
      case _ =>
        curator
          .setData()
          .forPath(path, data)
    }
  }.unit
}

object ZioZkSubscriberIntSpec {
  case class Test[V](curator: CuratorFramework, subscriber: ZioZkSubscriber[V])
}
