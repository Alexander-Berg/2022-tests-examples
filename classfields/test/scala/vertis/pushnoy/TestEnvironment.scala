package vertis.pushnoy

import pureconfig.ConfigSource
import pureconfig.generic.auto._
import com.typesafe.config.ConfigFactory
import vertis.pushnoy.conf.XivaConfig

/** Created by Karpenko Maksim (knkmx@yandex-team.ru) on 11/07/2017.
  */
trait TestEnvironment {

  lazy val xivaConfig = {
    val raw = ConfigFactory.load("dao.development.conf")

    ConfigSource.fromConfig(raw.getConfig("xiva")).loadOrThrow[XivaConfig]
  }
}
