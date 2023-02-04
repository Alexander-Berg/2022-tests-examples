package auto.dealers.dealer_calls_auction.logic.testkit.common

import auto.common.model.ClientId
import auto.dealers.dealer_calls_auction.logic.timezone.DealerZoneOffsetManager
import zio.{Has, ULayer}
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.mock.Expectation.value
import zio.test.mock.MockClock

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object ClockDependentMocks {

  private val mskZoneOffset = ZoneOffset.ofHours(3)

  val currentDateTime: Instant = OffsetDateTime
    .of(2022, 5, 20, 12, 0, 0, 0, mskZoneOffset)
    .toInstant

  val clockMock: ULayer[Clock] = MockClock
    .CurrentTime(
      equalTo(TimeUnit.MILLISECONDS),
      value(currentDateTime.toEpochMilli)
    )
    .toLayer

  val dealerZoneOffsetManagerMock: ClientId => ULayer[Has[DealerZoneOffsetManager]] = (dealer: ClientId) =>
    DealerZoneOffsetManagerMock
      .GetDealerStartOfDay(
        equalTo((dealer, currentDateTime)),
        value(currentDateTime.minus(12, ChronoUnit.HOURS))
      )
      .toLayer
}
