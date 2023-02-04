package vertis.spamalot.mocks

import vertis.spamalot.services.SendingTimeService
import zio.Task

import java.time.{Instant, ZoneId}

object TestSendingTimeService extends SendingTimeService.Service {
  override def isAllowedNow(now: Instant, timezone: ZoneId): Task[Boolean] = Task.succeed(true)

  override def nextAllowedTime(now: Instant, timezone: ZoneId): Task[Instant] = Task.succeed(now)
}
