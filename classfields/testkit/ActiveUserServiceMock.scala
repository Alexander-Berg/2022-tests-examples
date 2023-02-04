package vsmoney.auction_auto_strategy.scheduler.testkit

import vsmoney.auction_auto_strategy.scheduler.service.ActiveUserService
import zio.test.mock.mockable

@mockable[ActiveUserService]
object ActiveUserServiceMock {}
