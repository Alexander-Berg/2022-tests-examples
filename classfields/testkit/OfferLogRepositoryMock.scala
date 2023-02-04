package auto.dealers.trade_in_notifier.storage.testkit

import auto.dealers.trade_in_notifier.storage.OfferLogRepository
import zio.test.mock._

@mockable[OfferLogRepository.Service]
object OfferLogRepositoryMock
