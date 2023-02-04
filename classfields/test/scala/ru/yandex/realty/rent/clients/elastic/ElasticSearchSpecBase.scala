package ru.yandex.realty.rent.clients.elastic

import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import com.sksamuel.elastic4s.http.{JavaClient, NoOpHttpClientConfigCallback, NoOpRequestConfigCallback}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.time.{Millis, Minutes, Span}
import org.testcontainers.containers.BindMode
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.pos.TestOperationalComponents
import ru.yandex.realty.rent.application.ElasticSearchClientSupplier

import java.util.Collections.singletonMap
import scala.util.control.NonFatal

/**
  * @author azakharov
  *
  * https://www.testcontainers.org/modules/elasticsearch/
  */
trait ElasticSearchSpecBase
  extends ElasticSearchClientSupplier
  with TestOperationalComponents
  with AsyncSpecBase
  with BeforeAndAfterAll {

  lazy val elasticContainer: ElasticsearchContainer = {
    val c = new ElasticsearchContainer(
      DockerImageName
        .parse("registry.yandex.net/vertis/elasticsearch/elasticsearch:7.14.0")
        .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch:7.14.0")
    )
    c.withTmpFs(singletonMap("/tmpfs", "rw"))
    c.withClasspathResourceMapping(
      "elasticsearch.yml",
      "/usr/share/elasticsearch/config/elasticsearch.yml",
      BindMode.READ_ONLY
    )
    c.withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m")
    sys.addShutdownHook {
      try {
        c.close()
      } catch {
        case NonFatal(e) =>
          e.printStackTrace()
      }
    }
    c.start()
    c
  }

  override lazy val elasticSearchClient: ElasticSearchClient = {
    val conf = ElasticClientConfig("http://" + elasticContainer.getHttpHostAddress, "")
    val properties = ElasticProperties(conf.nodes)
    val elasticClient = ElasticClient(JavaClient(properties, NoOpRequestConfigCallback, NoOpHttpClientConfigCallback))
    new DefaultElasticSearchClient(elasticClient, this)
  }

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  override protected def afterAll(): Unit = {
    elasticContainer.stop()
  }
}
