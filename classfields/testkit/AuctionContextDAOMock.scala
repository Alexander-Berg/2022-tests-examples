package vsmoney.auction_auto_strategy.services.testkit

import vsmoney.auction_auto_strategy.storage.AuctionContextDAO
import zio.test.mock.mockable

@mockable[AuctionContextDAO]
object AuctionContextDAOMock {}
