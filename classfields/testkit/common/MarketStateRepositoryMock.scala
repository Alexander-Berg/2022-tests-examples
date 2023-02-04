package auto.dealers.dealer_calls_auction.logic.testkit.common

import auto.dealers.dealer_calls_auction.storage.marketstate.MarketStateRepository
import zio.test.mock.mockable

@mockable[MarketStateRepository]
object MarketStateRepositoryMock {}
