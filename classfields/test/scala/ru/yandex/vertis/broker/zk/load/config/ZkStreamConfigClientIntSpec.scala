package ru.yandex.vertis.broker.zk.load.config

import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.CreateMode
import ru.yandex.vertis.broker.zk.load.config.ZkStreamConfigClientIntSpec._
import vertis.core.json.CirceCodecs
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.zk.metrics.NoopZioZkSubscriberMetrics
import vertis.zio.zk.test.ZkTest
import vertis.zio.zk.{CirceZkSerializer, ZioZkSubscriber}
import zio.{Task, UIO}

/** @author kusaeva
  */
class ZkStreamConfigClientIntSpec extends ZkTest {

  /* have to create clients with different names cause of heathchecks registry in CuratorClientFactory */
  "ZkStreamConfigClient" should {
    "sync with zk on start" in configZkTest { curator =>
      clientM(curator).use { client =>
        for {
          curValue <- client.getCurrent
          _ <- check("got initial value")(curValue shouldBe initialValue)
        } yield ()
      }
    }

    "eventually receive update" in configZkTest { curator =>
      clientM(curator).use { client =>
        for {
          sub <- client.subscribe
          // get initial update
          _ <- sub.take
          _ <- Task {
            curator
              .setData()
              .forPath(path, zkSerializer.serialize(updatedValue))
          }
          // await update
          _ <- sub.take
          _ <- logger.info("config updated")
          newRes <- client.getCurrent
          _ <- check("update received:") {
            newRes shouldBe updatedValue
          }
        } yield ()
      }
    }
  }

  private def configZkTest(body: CuratorFramework => TestBody): Unit =
    zkTest { curator =>
      putConfig(curator) *> body(curator)
    }

  private def putConfig(curator: CuratorFramework): UIO[Unit] = UIO {
    val builder = curator.checkExists().forPath(path) match {
      // scalastyle:off null
      case null =>
        curator
          .create()
          .creatingParentsIfNeeded()
          .withMode(CreateMode.PERSISTENT)
      case _ => curator.setData()
    }
    builder.forPath(path, zkSerializer.serialize(initialValue))
  }.unit

  private def clientM(curator: CuratorFramework) =
    ZioZkSubscriber
      .makeByCodec[String](
        curator,
        path,
        "",
        metrics = NoopZioZkSubscriberMetrics
      )(CirceCodecs.stringCodec)
}

object ZkStreamConfigClientIntSpec {

  private val initialValue = "initial"

  private val updatedValue = "updated"

  private val basePath = "/local"
  private val configPath = "config"
  private val path: String = s"$basePath/$configPath"

  private val zkSerializer = CirceZkSerializer.makeSerializer[String](CirceCodecs.stringCodec)
}
