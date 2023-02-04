package auto.common.clients.passport.test

import com.typesafe.config.ConfigFactory
import auto.common.clients.passport.{Passport, PassportLive}
import common.zio.pureconfig.Pureconfig
import common.zio.sttp.endpoint.Endpoint
import common.zio.config.Configuration
import common.zio.config.Configuration.Configuration
import common.zio.sttp.Sttp

import zio.test.Assertion._
import zio.test._
import zio.{system, Has, ZIO, ZLayer}
import zio.test.{DefaultRunnableSpec, ZSpec}

object PassportIntegrationSpec extends DefaultRunnableSpec {

  private val testLayer = {

    val cfg: ZLayer[Any, Nothing, Configuration] = ZLayer.fromEffect {
      for {
        config <- ZIO.effectTotal {
          ConfigFactory.parseString("""
              |passport = {
              |  schema = "http"
              |  host = "passport-api.vrts-slb.test.vertis.yandex.net"
              |  port = "80"
              |}
              |
              |""".stripMargin)
        }
      } yield new Configuration.Live(config)
    }

    val endpoint = cfg >>> Pureconfig.loadLayer[Endpoint]("passport")

    val sttp = cfg >>> Sttp.live

    (endpoint ++ sttp) >>> PassportLive.live
  }

  val testClient = 16453L
  val testUser = 38742764L
  val testUsers = Seq(63L, 14090654L, 38742764L, 50068410L, 50758814L, 61555044L, 62127784L)

  val getUsersSpec = testM("get users should return proto with list of ids") {
    for {
      result <- Passport.getUsers(testClient)
    } yield {
      assert(result.userIds)(equalTo(testUsers.map(_.toString)))
    }
  }

  val getEssentialsSpec = testM("get essentials should return info with last-seen field") {
    for {
      result <- Passport.getUserEssentials(testUser, true)
    } yield {

      assert(result.lastSeen.nonEmpty)(equalTo(true))
    }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PassportLive")(
//      getUsersSpec, getEssentialsSpec
    ).provideCustomLayer(testLayer.orDie)
}
