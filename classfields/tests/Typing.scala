package common.yt.tests

import common.tagged.tag
import common.tagged.tag.@@
import common.yt.YPathConfigReader
import pureconfig.ConfigReader
import ru.yandex.inside.yt.kosher.cypress.YPath

object Typing {
  trait YtBasePathTag
  type YtBasePath = YPath @@ YtBasePathTag

  def wrapBasePath(p: YPath): YtBasePath =
    tag[YtBasePathTag][YPath](p)

  implicit val reader: ConfigReader[YtBasePath] =
    YPathConfigReader.YPathReader.map(wrapBasePath)
}
