package auto.dealers.balance_alerts.storage.testkit

import auto.dealers.balance_alerts.storage.BalanceEventsRepository
import zio.test.mock.mockable

@mockable[BalanceEventsRepository.Service]
object BalanceEventsRepositoryMock {}
