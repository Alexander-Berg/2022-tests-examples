package ru.yandex.vertis.clustering.services.impl

import java.io.File
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.config.KafkaTopicConfig
import ru.yandex.vertis.clustering.dao.impl.GraphDaoImpl
import ru.yandex.vertis.clustering.derivatives.DerivativeFactsBuilderImpl
import ru.yandex.vertis.clustering.derivatives.impl.ApiDerivativeFeaturesBuilder
import ru.yandex.vertis.clustering.geobase.impl.EmptyGeoBaseClient
import ru.yandex.vertis.clustering.kafka.EventDeserializers
import ru.yandex.vertis.clustering.kafka.impl.KafkaConsumerBase.KafkaConsumerConfig
import ru.yandex.vertis.clustering.model.GraphGeneration._
import ru.yandex.vertis.clustering.model.{Domains, Neo4jGraphGeneration, PackedGraphGeneration}
import ru.yandex.vertis.clustering.services.NodeDecider.NodeId
import ru.yandex.vertis.clustering.services.impl.SourceFactsFilter._
import ru.yandex.vertis.clustering.utils.DateTimeUtils
import ru.yandex.vertis.events.Event
import ru.yandex.vertis.kafka.util.CommittableConsumerRecord

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Promise}
import scala.util.Try

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class FactsConsumerSpec extends BaseSpec {

  implicit val actorSystem: ActorSystem =
    ActorSystem("karma-graph-test")
  implicit val materializer: ActorMaterializer =
    ActorMaterializer()
  implicit val ec: ExecutionContextExecutor =
    actorSystem.dispatcher

  private val karmaConfig = new KafkaTopicConfig {
    override def connectionString: String =
      "kafka-01-man.test.vertis.yandex.net:9092," +
        "kafka-01-myt.test.vertis.yandex.net:9092," +
        "kafka-01-sas.test.vertis.yandex.net:9092"

    override def topic: String = "vertis-events-topic"
  }

  private val firstGenId: GraphGenerationId = "1"

  private val tarGzFile = new File(ClassLoader.getSystemResource("generation.data").toURI)

  private val workDir = new File(tarGzFile.getParentFile, "karma-records-test")
  workDir.mkdir()

  private val neo4jGeneration = PackedGraphGeneration(firstGenId, tarGzFile)
    .unpack(workDir)(Neo4jGraphGeneration.apply)

  private val graph = new GraphInstanceImpl(neo4jGeneration, Domains.Autoru) with RichGraphInstance {
    override protected def nodeId: NodeId = "alice"
  }

  val graphDao = new GraphDaoImpl(graph, GraphDaoImpl.defaultPolicyForDomain(Domains.Autoru))

  private var lastFactEpoch = DateTimeUtils.now.minusDays(10)

  private val karmaConsumerConfig =
    KafkaConsumerConfig(karmaConfig,
                        s"user-clustering-testing}",
                        new StringDeserializer,
                        EventDeserializers.VertisEventsDeserializer)

  private val derivativeFactsBuilder = new DerivativeFactsBuilderImpl(
    new ApiDerivativeFeaturesBuilder(EmptyGeoBaseClient))
  val vertisEventsFactsParser = new VertisEventsFactsParser(derivativeFactsBuilder)

  private val graphConsumer: FactsConsumer[String, Event] = new FactsConsumer[String, Event](graph,
                                                                                             graphDao,
                                                                                             karmaConsumerConfig,
                                                                                             _ => true,
                                                                                             vertisEventsFactsParser,
                                                                                             emptyFilter) {

    def consume(k: String, v: Event): Try[Unit] = Try {
      vertisEventsFactsParser.parse(v).foreach { fact =>
        if (fact.dateTime.isAfter(lastFactEpoch)) {
          lastFactEpoch = fact.dateTime
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

  "KarmaRecordsGraphInserter" should {
    "read kafka topic" in {
      Try(Await.ready(Promise().future, 6.seconds))
      graphDao.version.get.lastFactEpoch.get shouldBe lastFactEpoch
    }
  }

}
