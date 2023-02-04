package vertis.yt

import pureconfig.ConfigSource
import ru.yandex.inside.yt.kosher.impl.YtConfiguration
import vertis.yt.config.YtConfig
import vertis.yt.util.config.YtPureReaders._
import pureconfig.generic.auto._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait RealYtTest extends YtTest {

  override protected lazy val ytConfig: YtConfig = {
    val conf = ConfigSource
      .resources("yt-test.conf")
      .loadOrThrow[YtConfig]

    conf.copy(basePath = conf.basePath.child(TestYtFolder.folder))
  }

  override protected lazy val ytConf: YtConfiguration = ytConfig.clientConf

  override def initContainer(): Unit = ()
}
