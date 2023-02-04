package vertis.reactor.client

import common.clients.reactor.model._
import ru.yandex.qe.reactor.proto.artifact.YtPathArtifactValueProto
import vertis.reactor.client.config._
import vertis.zio.test.{ZioEventually, ZioSpecBase}
import zio.{UIO, ZIO}

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZonedDateTime}
import scala.util.Random

class RawReactorClientIntSpec extends ZioSpecBase with ZioEventually {

  val ttlDays: Int = 30
  val projectId: Long = 1L

  private val configOpt = sys.env.get("REACTOR_TOKEN").orElse(sys.env.get("REACTOR_TOKEN")).map { token =>
    ReactorConfig(
      url = "https://test.reactor.yandex-team.ru",
      token = token,
      client = ReactorClientConfig(
        reactorBasePath = "/verticals/broker/hahn/int_test"
      )
    )
  }
  private val config = configOpt.getOrElse(sys.error("define REACTOR_TOKEN to run the tests!!!"))

  "RawReactorClient" should {

    val reactorPath = "RawReactorClient/some_artifact"
    val userDate = ZonedDateTime.now().minusDays(Random.nextInt(1000).toLong).withNano(0)
    val ytPath = s"//tmp/${Random.alphanumeric.take(20).mkString}"
    val beginningOfTime = Instant.now().minus(100, ChronoUnit.DAYS)
    val instance = ArtifactInstance(
      userTimestamp = Some(userDate.toInstant),
      metadata = Some(YPathArtifactMeta(ytPath)),
      attributes = Map("i'm" -> "from test")
    )
    var myInstanceId: Long = -1

    "create instance" in ioTest {
      ReactorClientImpl.make(config).use { client =>
        client
          .createInstance(
            reactorPath,
            instance,
            createIfNotExist = false,
            ttlDays = ttlDays,
            projectId = projectId
          )
          .checkResult { instance =>
            myInstanceId = instance.id
            instance.id should be > 0L
          }
      }
    }

    "deprecate instance" in ioTest {
      ReactorClientImpl.make(config).use { client =>
        ZIO.when(myInstanceId > 0) {
          val newInstance = ArtifactInstance(
            userTimestamp = Some(userDate.toInstant),
            metadata = Some(YPathArtifactMeta(ytPath)),
            attributes = Map("i'm" -> "replace")
          )
          client
            .deprecateInstance(
              reactorPath,
              newInstance,
              myInstanceId,
              ttlDays = ttlDays,
              projectId = projectId
            )
            .checkResult { createdInstance =>
              createdInstance.id should be > myInstanceId
            }
        }
      }
    }

    "list instances by user timestamp" in ioTest {
      ReactorClientImpl.make(config).use { client =>
        checkEventually {
          client
            .listInstances[YtPathArtifactValueProto](
              path = reactorPath,
              userTimestampFilter = Some(TimestampFilter.Range.wholeDay(userDate.toLocalDate))
            )
            .checkResult { instances =>
              instances should not be empty
              val myInstances = instances.filter(_.metadata.exists(_.getPath == ytPath))
              myInstances.size shouldBe 1
              val myInstance = myInstances.head
              myInstance.metadata.get.getPath shouldBe instance.metadata.get.path
              myInstance.metadata.get.getCluster shouldBe instance.metadata.get.cluster
              myInstance.userTimestamp shouldBe instance.userTimestamp
            }
        }
      }
    }

    "list instances by creation timestamp" in ioTest {
      ReactorClientImpl.make(config).use { client =>
        client
          .listInstances[YtPathArtifactValueProto](
            path = reactorPath,
            creationTimestampFilter = Some(TimestampFilter.Range(beginningOfTime, Instant.now()))
          )
          .checkResult { instances =>
            instances should not be empty
            val myInstances = instances.filter(_.metadata.exists(_.getPath == ytPath))
            myInstances.size shouldBe 1
            val myInstance = myInstances.head
            myInstance.metadata.get.getPath shouldBe instance.metadata.get.path
            myInstance.metadata.get.getCluster shouldBe instance.metadata.get.cluster
            myInstance.userTimestamp shouldBe instance.userTimestamp
          }
      }
    }

    "get NotFound listing instances for non-existent path" in ioTest {
      ReactorClientImpl.make(config).use { client =>
        client
          .listInstances[YtPathArtifactValueProto](
            path = "exists/not",
            creationTimestampFilter = Some(TimestampFilter.Range(beginningOfTime, Instant.now()))
          )
          .catchSome { case _: ReactorNotFoundError => UIO(Seq.empty) }
          .checkResult { instances =>
            instances shouldBe empty
          }
      }
    }
  }

}
