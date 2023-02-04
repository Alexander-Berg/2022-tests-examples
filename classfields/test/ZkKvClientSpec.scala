package common.zio.clients.kv.zookeeper

import common.zio.clients.kv.KvClient
import common.zio.clients.kv.KvClient.{Decoder, Encoder, NoKeyError}
import common.zookeeper.testkit.TestZookeeper
import zio.ZIO
import zio.test._
import zio.test.Assertion._

import java.nio.charset.StandardCharsets

object ZkKvClientSpec extends DefaultRunnableSpec {
  implicit val stringEncoder: Encoder[String] = (value: String) => value.getBytes(StandardCharsets.UTF_8)

  implicit val stringDecoder: Decoder[String] = (value: Array[Byte]) => new String(value, StandardCharsets.UTF_8)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("ZkKvClient")(
      testM("get and set value by key") {

        for {
          kvClient <- ZIO.service[KvClient.Service]
          emptyGet <- kvClient.get[String]("/key").catchSome { case _: NoKeyError => ZIO.succeed("empty") }
          _ <- kvClient.set("/key", "value")
          result <- kvClient.get[String]("/key")
        } yield {
          assert(emptyGet)(equalTo("empty")) &&
          assert(result)(equalTo("value"))
        }
      }
    ).provideCustomLayerShared {
      (TestZookeeper.testCluster >>> TestZookeeper.curatorFramework) >>> ZkKvClient.live
    }
  }
}
