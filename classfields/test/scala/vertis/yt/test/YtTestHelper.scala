package vertis.yt.test

import common.yt.YtError
import common.zio.schedule.Schedules
import common.zio.schedule.{RetryConf, Schedules}
import ru.yandex.inside.yt.kosher.cypress.YPath
import zio.{Schedule, ZEnv}

/** @author kusaeva
  */
object YtTestHelper {
  val ytBasePath: YPath = YPath.simple("//home/verticals/broker/dev/warehouse")

  val createTempTableSchedule: Schedule[ZEnv, YtError, Any] =
    Schedule.recurWhile[YtError](_.isRetryable) &&
      Schedules.exponentialRetry(RetryConf(5))
}
