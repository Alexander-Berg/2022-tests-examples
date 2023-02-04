package auto.common.clients.journal

import auto.common.clients.journal.Journal.Journal
import auto.common.model.offers.Paging
import com.typesafe.config.ConfigFactory
import common.zio.config.Configuration
import common.zio.config.Configuration.Configuration
import common.zio.pureconfig.Pureconfig
import common.zio.sttp.Sttp
import common.zio.sttp.endpoint.Endpoint
import common.zio.sttp.model.SttpError
import zio.test._
import zio.{ZIO, ZLayer}

object JournalIntegrationSpec extends DefaultRunnableSpec {

  private val testLayer = {

    val cfg: ZLayer[Any, Nothing, Configuration] = (for {
      config <- ZIO.effectTotal {
        ConfigFactory.parseString("""
            |journal = {
            |  schema = "http"
            |  host = "journal-api-autoru.test.vertis.yandex-team.ru"
            |  port = "80"
            |}
            |
            |""".stripMargin)
      }
    } yield new Configuration.Live(config)).toLayer

    val endpoint = cfg >>> Pureconfig.loadLayer[Endpoint]("journal")
    val sttp = cfg >>> Sttp.live
    (endpoint ++ sttp) >>> JournalLive.live
  }

  val getFeed: ZSpec[Journal, SttpError] = testM("should return proto with list of feed") {
    for {
      result <- Journal.getInfiniteFeed(Paging.Default)
    } yield {
      println("result: " + result)
      assertTrue(result.posts.nonEmpty)
    }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("JournalLive")(
      getFeed
    ).provideCustomLayer(testLayer.orDie)
}
