package auto.dealers.multiposting.storage.test.zookeeper

import auto.dealers.multiposting.storage.zookeeper.ZkLastProcessedTimestampDao
import common.zio.clients.kv.KvClient
import common.zio.clients.kv.KvClient.NoKeyError
import common.zio.clients.kv.zookeeper.ZkKvClient
import common.zookeeper.testkit.TestZookeeper
import zio.ZIO
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.Assertion._

import java.time.Instant

object ZkEodProcessingDateDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("ZkEodProcessingDateDao")(
      testM("get and set last processed timestamp") {
        val timestamp = Instant.ofEpochSecond(123)

        for {
          kvClient <- ZIO.service[KvClient.Service]
          dao = new ZkLastProcessedTimestampDao(kvClient)
          _ <- dao.get.catchSome { case _: NoKeyError => ZIO.unit }
          _ <- dao.set(timestamp)
          result <- dao.get
        } yield {
          assert(result)(equalTo(timestamp))
        }
      }
    ).provideCustomLayerShared {
      (TestZookeeper.testCluster >>> TestZookeeper.curatorFramework) >>> ZkKvClient.live
    }
  }
}
