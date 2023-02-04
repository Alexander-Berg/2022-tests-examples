package vsmoney.auction_auto_strategy.services.testkit

import vsmoney.auction_auto_strategy.storage.AutoStrategyDAO
import zio.test.mock.mockable

@mockable[AutoStrategyDAO]
object AutoStrategyDAOMock {}
