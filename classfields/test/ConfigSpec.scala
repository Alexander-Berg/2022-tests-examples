package common.zio.tvm.test

import common.zio.tvm.{InvalidTicket, TvmClient, TvmConfig, UserTickets}
import common.zio.config.Configuration
import common.zio.pureconfig.Pureconfig
import ru.yandex.passport.tvmauth.{BlackboxEnv, TicketStatus}
import zio._
import zio.test.Assertion._
import zio.test._

object ConfigSpec extends DefaultRunnableSpec {

  case class TestCase(config: String, expected: TvmConfig)

  val testData = Seq(
    TestCase("", TvmConfig(None, None, None, None, None)),
    TestCase("self-client-id = 22", TvmConfig(Some(22), None, None, None, None)),
    TestCase("environment = ProdYateam", TvmConfig(None, None, None, None, Some(BlackboxEnv.PROD_YATEAM))),
    TestCase("environment = PROD_YATEAM", TvmConfig(None, None, None, None, Some(BlackboxEnv.PROD_YATEAM))),
    TestCase("environment = Test", TvmConfig(None, None, None, None, Some(BlackboxEnv.TEST)))
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("TvmConfig")(
      testM("parse config") {
        checkAllM(Gen.fromIterable(testData)) { testCase =>
          for {
            conf <- Configuration.fromString(testCase.config)
            tvmConf <- Pureconfig.load[TvmConfig].provide(Has(conf))

          } yield assert(tvmConf)(equalTo(testCase.expected))
        }
      }
    )
  }
}
