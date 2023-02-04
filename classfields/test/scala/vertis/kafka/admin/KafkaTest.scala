package vertis.kafka.admin

import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import vertis.kafka.admin.KafkaTest.kafkaContainer
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.kafka.admin.conf.KafkaAdminConfig
import vertis.zio.test.ZioSpecBase
import common.zio.logging.Logging
import zio.URManaged
import zio.blocking.Blocking
import zio.duration._

trait KafkaTest extends ZioSpecBase {

  override protected val ioTestTimeout: Duration = 2.minutes

  protected def bootstrapServers: String = kafkaContainer.getBootstrapServers

  protected lazy val kafkaClient: URManaged[Blocking with Logging.Logging, KafkaAdminClient] = {
    val config = KafkaAdminConfig(bootstrapServers = List(bootstrapServers), replicationFactor = 1)
    KafkaAdminClient.make(config)
  }

  protected def kafkaTest(body: KafkaAdminClient => TestBody): Unit =
    ioTest {
      kafkaClient.use(body)
    }
}

object KafkaTest {

  lazy val kafkaContainer: KafkaContainer = {
    val imageName = DockerImageName.parse("confluentinc/cp-kafka").withTag("6.2.1")
    val c = new KafkaContainer(imageName)
    c.start()
    c
  }
}
