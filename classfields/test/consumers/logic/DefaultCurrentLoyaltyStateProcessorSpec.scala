package auto.dealers.dealer_pony.scheduler.test.consumers.logic

import common.geobase.model.RegionIds.{MoscowAndMoscowRegion, RegionId, SaintPetersburgAndLeningradOblast, Samara}
import common.scalapb.ScalaProtobuf
import common.zio.clock.MoscowClock
import auto.dealers.dealer_pony.scheduler.consumers.logic.CurrentLoyaltyStateProcessor
import auto.dealers.dealer_pony.scheduler.consumers.logic.DefaultCurrentLoyaltyStateProcessor.BrokenMessage
import auto.dealers.dealer_pony.storage.dao.DealerStatusDao
import auto.dealers.dealer_pony.storage.testkit.DealerStatusDaoMock
import ru.auto.loyalty.loyalty_status_model.CurrentLoyaltyState
import common.zio.logging.Logging
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.ZonedDateTime

object DefaultCurrentLoyaltyStateProcessorSpec extends DefaultRunnableSpec {
  val currentDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, MoscowClock.timeZone).toOffsetDateTime
  val date = ScalaProtobuf.instantToTimestamp(currentDate.toInstant)

  private def getMockData(level: Int, region: RegionId, available: Boolean, stock: Boolean) = {
    val message = CurrentLoyaltyState(
      dealerId = 1L,
      updatedAt = Some(date),
      loyaltyLevel = level,
      regionId = region.id.toInt,
      fullStock = stock
    )
    val expected = DealerStatusDao.Record(
      dealerId = 1L,
      updatedAt = currentDate,
      loyaltyLevel = level,
      regionId = region,
      hasFullStock = stock,
      wlAvailable = available
    )
    (message, expected)
  }

  private val currentLoyaltyStateMoscowRegionLoyaltyConsumerTest =
    testM(
      "White list is available for region Moscow and level 12"
    ) {
      val (message, expected) = getMockData(level = 12, region = MoscowAndMoscowRegion, available = true, stock = true)

      val mocks = DealerStatusDaoMock
        .Upsert(equalTo(expected), unit)
        .toLayer ++ Logging.live >>> CurrentLoyaltyStateProcessor.live
      assertM(CurrentLoyaltyStateProcessor.process(message).provideCustomLayer(mocks))(isUnit)
    }

  private val currentLoyaltyStateSpbRegionLoyaltyConsumerTest =
    testM(
      "White list is available for region Spb and level 12"
    ) {
      val (message, expected) =
        getMockData(level = 12, region = SaintPetersburgAndLeningradOblast, available = true, stock = true)
      val mocks = DealerStatusDaoMock
        .Upsert(equalTo(expected), unit)
        .toLayer ++ Logging.live >>> CurrentLoyaltyStateProcessor.live
      assertM(CurrentLoyaltyStateProcessor.process(message).provideCustomLayer(mocks))(isUnit)
    }

  private val currentLoyaltyStateOtherRegionLoyaltyConsumerTest =
    testM(
      "White list is available for region Other and level 6"
    ) {
      val (message, expected) = getMockData(level = 6, region = Samara, available = true, stock = true)

      val mocks = DealerStatusDaoMock
        .Upsert(equalTo(expected), unit)
        .toLayer ++ Logging.live >>> CurrentLoyaltyStateProcessor.live
      assertM(CurrentLoyaltyStateProcessor.process(message).provideCustomLayer(mocks))(isUnit)
    }

  private val currentLoyaltyStateMoscowRegionNotLoyalConsumerTest =
    testM(
      "White list is not available for region Moscow and level 6"
    ) {
      val (message, expected) = getMockData(level = 6, region = MoscowAndMoscowRegion, available = false, stock = true)

      val mocks = DealerStatusDaoMock
        .Upsert(equalTo(expected), unit)
        .toLayer ++ Logging.live >>> CurrentLoyaltyStateProcessor.live
      assertM(CurrentLoyaltyStateProcessor.process(message).provideCustomLayer(mocks))(isUnit)
    }

  private val currentLoyaltyStateOtherRegionNotLoyalConsumerTest =
    testM(
      "White list is not available for region Other and level 1"
    ) {
      val (message, expected) = getMockData(level = 1, region = Samara, available = false, stock = true)

      val mocks = DealerStatusDaoMock
        .Upsert(equalTo(expected), unit)
        .toLayer ++ Logging.live >>> CurrentLoyaltyStateProcessor.live
      assertM(CurrentLoyaltyStateProcessor.process(message).provideCustomLayer(mocks))(isUnit)
    }

  private val currentLoyaltyStateMoscowRegionNotLoyalNotFullStockConsumerTest =
    testM(
      "White list is not available for region Moscow and level 12 and not full stock"
    ) {
      val (message, expected) =
        getMockData(level = 12, region = MoscowAndMoscowRegion, available = false, stock = false)

      val mocks = DealerStatusDaoMock
        .Upsert(equalTo(expected), unit)
        .toLayer ++ Logging.live >>> CurrentLoyaltyStateProcessor.live
      assertM(CurrentLoyaltyStateProcessor.process(message).provideCustomLayer(mocks))(isUnit)
    }

  private val failsOnEmptyUpdatedAtFieldConsumerTest =
    testM(
      "Failed if message broken and does not contain updated value"
    ) {
      val message = CurrentLoyaltyState(
        dealerId = 1L,
        updatedAt = None,
        loyaltyLevel = 12,
        regionId = 1,
        fullStock = true
      )
      val mocks = DealerStatusDaoMock.empty ++ Logging.live >>> CurrentLoyaltyStateProcessor.live
      assertM(CurrentLoyaltyStateProcessor.process(message).provideCustomLayer(mocks).run)(
        fails(isSubtype[BrokenMessage](anything))
      )
    }

  override val spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("DefaultCurrentLoyaltyStateProcessor")(
      currentLoyaltyStateMoscowRegionLoyaltyConsumerTest,
      currentLoyaltyStateSpbRegionLoyaltyConsumerTest,
      currentLoyaltyStateOtherRegionLoyaltyConsumerTest,
      currentLoyaltyStateMoscowRegionNotLoyalConsumerTest,
      currentLoyaltyStateOtherRegionNotLoyalConsumerTest,
      currentLoyaltyStateMoscowRegionNotLoyalNotFullStockConsumerTest,
      failsOnEmptyUpdatedAtFieldConsumerTest
    )

}
