package common.zio.kafka.test

import common.zio.kafka.testkit.TestKafka
import zio.test.Assertion._
import zio.test._

object TestKafkaSpec extends DefaultRunnableSpec {

  def spec =
    suite("TestKafka")(
      testM("starts") {
        assertM(TestKafka.bootstrapServers)(isNonEmpty)
      },
      testM("create topic") {
        for {
          _ <- TestKafka.createTopic("test-topic")
        } yield assertCompletes
      }
    ).provideCustomLayer(TestKafka.live)
}
