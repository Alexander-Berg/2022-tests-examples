package vsmoney.auction_auto_strategy.services.testkit

import vsmoney.auction_auto_strategy.clients.AuctionServiceClient
import zio.test.mock.mockable

@mockable[AuctionServiceClient]
object AuctionServiceMock {}
