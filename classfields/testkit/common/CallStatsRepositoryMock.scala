package auto.dealers.dealer_calls_auction.logic.testkit.common

import auto.dealers.dealer_calls_auction.storage.calls.CallStatsRepository
import zio.test.mock.mockable

@mockable[CallStatsRepository]
object CallStatsRepositoryMock {}
