package ru.vertistraf.traffic_feed_tests.common.model.yt

import common.tagged.tag.@@
import ru.yandex.inside.yt.kosher.cypress.YPath

object YtTempDir {
  trait YtTempDirT
  type YtTempDir = YPath @@ YtTempDirT
}
