package vertis.yql.container

import pureconfig.ConfigSource
import vertis.yql.container.TestYqlServer.TestResources
import vertis.yt.zio.YtZioTest
import vertis.zio.ServerEnv
import vertis.zio.yql.YqlClient
import vertis.zio.yql.conf.YqlConfig
import vertis.yt.util.config.YtPureReaders._
import pureconfig.generic.auto._
import zio._
import zio.duration._

import java.time.Duration

/**
  */
trait RealYqlTest extends YtZioTest {

  override protected val ioTestTimeout: Duration = 10.minutes

  protected lazy val yqlConfig: YqlConfig = ConfigSource.resources("yql-test.conf").loadOrThrow[YqlConfig]

  protected lazy val makeYqlClient: RManaged[ServerEnv, YqlClient] =
    YqlClient.make(yqlConfig)

  def resources: RManaged[ServerEnv, TestResources] =
    for {
      yt <- ytZio
      yql <- makeYqlClient
    } yield TestResources(yt, yql)
}
