package common.yt.tests.suites

import common.yt.Yt.Yt
import common.yt.tests.Typing.YtBasePath
import zio.Has
import zio.test.ZSpec
import zio.test.environment.TestEnvironment

trait YtSuite {

  def ytSuite: ZSpec[TestEnvironment with Yt with Has[YtBasePath], Any]
}
