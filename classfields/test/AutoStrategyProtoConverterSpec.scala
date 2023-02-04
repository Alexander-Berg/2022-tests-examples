package vsmoney.auction_auto_strategy.converters.test

import common.models.finance.Money.Kopecks
import vsmoney.auction.common_model.{Money => MoneyProto}
import vsmoney.auction_auto_strategy.converters.{AutoStrategyProtoConverter, ConverterError}
import vsmoney.auction_auto_strategy.model.AutoStrategySettings
import vsmoney.auction_auto_strategy.model.auction.Bid
import vsmoney.auction_auto_strategy.settings.{
  AutoStrategy => AutoStrategyProto,
  MaxPositionForPrice => MaxPositionForPriceProto
}
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object AutoStrategyProtoConverterSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AutoStrategyProtoConverter")(
      suite("settingsFromProto")(
        testM("should convert Option[AutoStrategyProto] to AutoStrategySettings") {
          assertM(AutoStrategyProtoConverter.settingsFromProto.convert(Some(testRequest)).run)(
            succeeds(equalTo(testAutoStrategySettings))
          )
        },
        testM("should throw exception if auto strategy is Empty") {
          val request = testRequest.copy(algorithm = AutoStrategyProto.Algorithm.Empty)
          assertM(AutoStrategyProtoConverter.settingsFromProto.convert(Some(request)).run)(
            fails(isSubtype[ConverterError.IllegalFieldValueError](anything))
          )
        },
        testM("should throw exception if auto strategy is None") {
          assertM(AutoStrategyProtoConverter.settingsFromProto.convert(None).run)(
            fails(isSubtype[ConverterError.IllegalFieldValueError](anything))
          )
        }
      ),
      suite("toProto") {
        testM("should convert AutoStrategySettings to AutoStrategyProto") {
          assertM(AutoStrategyProtoConverter.toProto.convert(testAutoStrategySettings).run)(
            succeeds(equalTo(testRequest))
          )
        }
      }
    )
  }

  private val moneyRequest = MoneyProto(222)

  private val testRequest = AutoStrategyProto(
    maxBid = Some(moneyRequest),
    algorithm = AutoStrategyProto.Algorithm.MaxPositionForPrice(MaxPositionForPriceProto())
  )

  private val testAutoStrategySettings = AutoStrategySettings.MaximumPositionForBid(Bid(Kopecks(222)))

}
