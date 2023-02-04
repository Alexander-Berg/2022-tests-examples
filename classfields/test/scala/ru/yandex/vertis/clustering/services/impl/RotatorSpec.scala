package ru.yandex.vertis.clustering.services.impl

import java.io.File
import akka.actor.ActorSystem
import org.apache.curator.test.TestingServer
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.extdata.core.Data.FileData
import ru.yandex.extdata.core.{Controller, DataType, Instance, InstanceHeader}
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.config.{KafkaTopicConfig, ZookeeperConfig}
import ru.yandex.vertis.clustering.dao.impl.GraphDaoImpl
import ru.yandex.vertis.clustering.derivatives.DerivativeFactsBuilderImpl
import ru.yandex.vertis.clustering.derivatives.impl.BuilderDerivativeFeaturesBuilder
import ru.yandex.vertis.clustering.edl.GraphGenerationFetcher
import ru.yandex.vertis.clustering.kafka.EventDeserializers
import ru.yandex.vertis.clustering.kafka.impl.KafkaConsumerBase.KafkaConsumerConfig
import ru.yandex.vertis.clustering.model.Domains
import ru.yandex.vertis.clustering.model.Domains.Domain
import ru.yandex.vertis.clustering.services.NodeDecider.NodeId
import ru.yandex.vertis.clustering.services.impl.Rotator.{AlwaysAllowKeeper, RotatorKeeper, RotatorKeeperImpl}
import ru.yandex.vertis.clustering.services.impl.SourceFactsFilter._
import ru.yandex.vertis.clustering.services.{NodeDecider, NodeDeciderImpl}
import ru.yandex.vertis.clustering.utils.DateTimeUtils._
import ru.yandex.vertis.clustering.utils.ZookeeperUtils
import ru.yandex.vertis.events.Event

import scala.concurrent.duration._
import scala.util.Success

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class RotatorSpec extends BaseSpec {

  val testingServer = new TestingServer()
  testingServer.start()

  override def afterAll() {
    testingServer.stop()
  }

  private val zookeeperConfig = new ZookeeperConfig {
    override def namespace: String = "user-clustering-testing"
    override def connectString: String = s"localhost:${testingServer.getPort}"
  }

  val zkUtils: ZookeeperUtils =
    new ZookeeperUtils(zookeeperConfig, "clustering")

  val aliceService = new NodesZkClientImpl(zkUtils, Domains.Autoru, acquireTimeout = 5.seconds)

  val bobService = new NodesZkClientImpl(zkUtils, Domains.Autoru, acquireTimeout = 5.seconds)

  private val firstGenProduced = DateTime.now().minusDays(30)

  private val firstGenInstanceId = "123"

  private val tarGzFile = new File(ClassLoader.getSystemResource("generation.data").toURI)

  private val aliceWorkDir = new File(tarGzFile.getParentFile, "aliceworkdir")
  aliceWorkDir.mkdir()

  private val bobWorkDir = new File(tarGzFile.getParentFile, "bobworkdir")
  bobWorkDir.mkdir()

  class TestExtDataGraph(override protected val workDir: File) extends ExtDataGraph {
    override protected def edlClientController: Controller = None.orNull
    def push(instance: Instance): Unit = processExtDataInstance(instance)

    override def domain: Domain = Domains.Autoru

    override protected val extDataType: DataType = GraphGenerationFetcher.AutoruGraphGenerationDataType
  }

  private val alice = new TestExtDataGraph(aliceWorkDir) with RichGraphInstance {
    override protected def nodeId: NodeId = "alice"
  }

  private val bob = new TestExtDataGraph(bobWorkDir) with RichGraphInstance {
    override protected def nodeId: NodeId = "bob"
  }

  val graphExtDataInstance =
    Instance(InstanceHeader(firstGenInstanceId, 1, firstGenProduced), FileData(tarGzFile))

  alice.push(graphExtDataInstance)
  bob.push(graphExtDataInstance)

  implicit val actorSystem: ActorSystem =
    ActorSystem("rotator-test")

  private val karmaConfig = new KafkaTopicConfig {
    override def connectionString: String =
      "kafka-01-man.test.vertis.yandex.net:9092," +
        "kafka-01-myt.test.vertis.yandex.net:9092," +
        "kafka-01-sas.test.vertis.yandex.net:9092"

    override def topic: String = "vertis-events-topic"
  }

  private val kafkaConsumerConfig = KafkaConsumerConfig(karmaConfig,
                                                        "user-clustering-testing",
                                                        new org.apache.kafka.common.serialization.StringDeserializer,
                                                        EventDeserializers.VertisEventsDeserializer)

  private val derivativeFactsBuilder = new DerivativeFactsBuilderImpl(new BuilderDerivativeFeaturesBuilder)
  private val vertisEventsFactsParser = new VertisEventsFactsParser(derivativeFactsBuilder)

  val graphDao = new GraphDaoImpl(alice, GraphDaoImpl.defaultPolicyForDomain(Domains.Autoru))

  private val mockConsumer =
    new FactsConsumer[String, Event](alice,
                                     graphDao,
                                     kafkaConsumerConfig,
                                     _ => true,
                                     vertisEventsFactsParser,
                                     emptyFilter)

  val nodeDecider: NodeDecider = new NodeDeciderImpl

  private val rotatorKeeper = new RotatorKeeperImpl()

  "RotatorKeeperImpl" should {

    "allow rotate first time" in {
      rotatorKeeper.allowRotate(RotatorKeeper.Input(Set("alice")),
                                now.minus(rotatorKeeper.observeTimeout.plusMinutes(1))) shouldBe Success(true)
    }

    "allow rotate when nodes not changed" in {
      rotatorKeeper.allowRotate(RotatorKeeper.Input(Set("alice")),
                                now.minus(rotatorKeeper.observeTimeout.plusMinutes(1))) shouldBe Success(true)
    }

    "forbid rotate when nodes changed before timeout" in {
      rotatorKeeper.allowRotate(RotatorKeeper.Input(Set("alice", "bob")),
                                now.minus(rotatorKeeper.observeTimeout.plusMinutes(1))) shouldBe Success(false)
    }

    "allow rotate when nodes fallback before timeout" in {
      rotatorKeeper.allowRotate(RotatorKeeper.Input(Set("alice")),
                                now.minus(rotatorKeeper.observeTimeout.plusMinutes(1))) shouldBe Success(true)
    }

    "forbid rotate when nodes changed before timeout next time" in {
      rotatorKeeper.allowRotate(RotatorKeeper.Input(Set("alice", "bob")),
                                now.minus(rotatorKeeper.observeTimeout.plusMinutes(1))) shouldBe Success(false)
    }

    "allow rotate when nodes changed after timeout" in {
      rotatorKeeper.allowRotate(RotatorKeeper.Input(Set("alice", "bob")), now) shouldBe Success(true)
    }

    "allow rotate when nodes not changed next time" in {
      rotatorKeeper.allowRotate(RotatorKeeper.Input(Set("bob", "alice")), now) shouldBe Success(true)
    }

    "forbid rotate when nodes changed next time" in {
      rotatorKeeper.allowRotate(RotatorKeeper.Input(Set("bob")), now) shouldBe Success(false)
    }

    "returns Failure when illegal 'at' param" in {
      intercept[IllegalArgumentException] {
        rotatorKeeper.allowRotate(RotatorKeeper.Input(Set("bob")), now.minusHours(1)).get
      }
    }

  }

  private val aliceRotator = new Rotator(alice, Seq(mockConsumer), aliceService, nodeDecider, AlwaysAllowKeeper)
  private val bobRotator = new Rotator(bob, Seq(mockConsumer), bobService, nodeDecider, AlwaysAllowKeeper)

  "ExtDataGraph with Rotator" should {

    "be empty at start" in {
      alice.generation shouldBe None
      bob.generation shouldBe None
    }

    "rotates for ext instance" in {
      val rotates = for {
        r1 <- aliceRotator.rotate()
        r2 <- bobRotator.rotate()
      } yield r1 && r2

      rotates shouldBe Success(true)

      alice.generation.get.datetime shouldBe fromJoda(firstGenProduced)
      bob.generation.get.datetime shouldBe fromJoda(firstGenProduced)

      alice.service shouldBe a[Success[_]]
      bob.service shouldBe a[Success[_]]
    }

    "shutdown" in {
      alice.shutdown
      bob.shutdown
    }
  }
}
