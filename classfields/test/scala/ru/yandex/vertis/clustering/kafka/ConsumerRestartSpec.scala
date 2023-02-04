package ru.yandex.vertis.clustering.kafka

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.curator.test.TestingServer
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.config.{KafkaTopicConfig, ZookeeperConfig}
import ru.yandex.vertis.clustering.dao.impl.GraphDaoImpl
import ru.yandex.vertis.clustering.derivatives.DerivativeFactsBuilderImpl
import ru.yandex.vertis.clustering.derivatives.impl.ApiDerivativeFeaturesBuilder
import ru.yandex.vertis.clustering.geobase.impl.EmptyGeoBaseClient
import ru.yandex.vertis.clustering.kafka.Consumer.Restarted
import ru.yandex.vertis.clustering.kafka.impl.KafkaConsumerBase.KafkaConsumerConfig
import ru.yandex.vertis.clustering.model.GraphGeneration._
import ru.yandex.vertis.clustering.model.{Domains, Neo4jGraphGeneration, PackedGraphGeneration}
import ru.yandex.vertis.clustering.services.NodeDecider.{Node, NodeId}
import ru.yandex.vertis.clustering.services.impl.SourceFactsFilter._
import ru.yandex.vertis.clustering.services.impl._
import ru.yandex.vertis.clustering.utils.concurrent._
import ru.yandex.vertis.clustering.utils.{DateTimeUtils, ZookeeperUtils}
import ru.yandex.vertis.events.Event
import ru.yandex.vertis.kafka.util.CommittableConsumerRecord

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.Try

/**
  * @author devreggs
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class ConsumerRestartSpec extends BaseSpec {

  implicit val actorSystem: ActorSystem =
    ActorSystem("karma-graph-test")
  implicit val materializer: ActorMaterializer =
    ActorMaterializer()
  implicit val ec: ExecutionContextExecutor =
    actorSystem.dispatcher

  private val vertisEventstTopic = "vertis-events-topic"

  private val karmaConfig = new KafkaTopicConfig {
    override def connectionString: String =
      "kafka-01-man.test.vertis.yandex.net:9092," +
        "kafka-01-myt.test.vertis.yandex.net:9092," +
        "kafka-01-sas.test.vertis.yandex.net:9092"

    override def topic: String = vertisEventstTopic
  }

  private val kafkaConsumerConfig = KafkaConsumerConfig(karmaConfig,
                                                        "user-clustering-dev-spec-1",
                                                        new org.apache.kafka.common.serialization.StringDeserializer,
                                                        EventDeserializers.VertisEventsDeserializer)

  private val gen: GraphGenerationId = "1"
  private val tarGzFile = new File(ClassLoader.getSystemResource("generation.data").toURI)
  private val workDir = new File(tarGzFile.getParentFile, "streamRotator-spec")
  workDir.mkdir()

  private val neo4jGeneration = PackedGraphGeneration(gen, tarGzFile)
    .unpack(workDir)(Neo4jGraphGeneration.apply)

  val alice = "alice"

  private val graph = new GraphInstanceImpl(neo4jGeneration, Domains.Autoru) with RichGraphInstance {
    override protected def nodeId: NodeId = alice
  }

  val graphDao = new GraphDaoImpl(graph, GraphDaoImpl.defaultPolicyForDomain(Domains.Autoru))

  val testingServer = new TestingServer()
  testingServer.start()

  override def afterAll() {
    testingServer.stop()
    setNodeStateExecutor.shutdown()
  }

  private val zookeeperConfig = new ZookeeperConfig {
    override def namespace: String = "user-clustering-dev-spec"
    override def connectString: String = s"localhost:${testingServer.getPort}"
  }

  val zkUtils: ZookeeperUtils =
    new ZookeeperUtils(zookeeperConfig, "clustering")

  val nodeZkClient = new NodesZkClientImpl(zkUtils, Domains.Autoru, acquireTimeout = 5.seconds)

  private val starts = DateTimeUtils.now.minusDays(3)
  private var lastFactEpoch = DateTimeUtils.now.minusDays(10)

  var enough = 50

  private val derivativeFactsBuilder = new DerivativeFactsBuilderImpl(
    new ApiDerivativeFeaturesBuilder(EmptyGeoBaseClient))
  private val vertisEventsFactsParser = new VertisEventsFactsParser(derivativeFactsBuilder)

  private val graphConsumer =
    new FactsConsumer[String, Event](graph,
                                     graphDao,
                                     kafkaConsumerConfig,
                                     _ => true,
                                     vertisEventsFactsParser,
                                     emptyFilter) {

      override def offsetsBeforeStarts: Option[Consumer.Offsets] = {
        Some(Consumer.OffsetsByTimestamp(starts.toInstant))
      }

      protected def consume(k: String, v: Event): Try[Unit] = Try {
        if (enough > 0) {

          vertisEventsFactsParser.parse(v).foreach { fact =>
            if (fact.dateTime.isAfter(lastFactEpoch)) {
              lastFactEpoch = fact.dateTime
            }
            enough = enough - 1
          }
        }
      }

      override def consume(data: Iterable[CommittableConsumerRecord[String, Event]]): Try[Unit] = Try {
        super.consume(data).get
        data.foreach { rec =>
          consume(rec.key, rec.value).get
        }
      }
    }

  graphConsumer.run

  val setNodeStateDelay: FiniteDuration = 1.seconds

  private val setNodeStateExecutor = newScheduledThreadPool("consumer-%d", 1)

  private def getNode: Node = {
    val lastFactEpoch =
      new GraphDaoImpl(graph, GraphDaoImpl.defaultPolicyForDomain(Domains.Autoru)).version.toOption
        .flatMap(_.lastFactEpoch)
    val lastRotated = graph.generation.map(_.datetime)
    Node(alice, lastFactEpoch, lastRotated)
  }

  setNodeStateExecutor.scheduleWithFixedDelay(setNodeStateDelay) {
    nodeZkClient.set(getNode)
  }

  while (enough > 0) Thread.sleep(1000)

  "Consumer" should {

    "actualize graph" in {
      graphDao.version.get.lastFactEpoch.get shouldBe lastFactEpoch
    }

    "restarts consuming" in {
      graphConsumer.setState(Restarted)
      enough = 50
      while (enough > 0) Thread.sleep(1000)
      graphDao.version.get.lastFactEpoch.get shouldBe lastFactEpoch
    }

    "send state to zkClient" in {
      val node = nodeZkClient.nodes().get.head
      node.lastFactEpoch shouldBe Some(lastFactEpoch)
      node.nodeId shouldBe alice
    }
  }

}
