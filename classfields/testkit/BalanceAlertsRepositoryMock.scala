package auto.dealers.balance_alerts.storage.testkit

import auto.dealers.balance_alerts.storage.BalanceAlertsRepository
import zio.test.mock.mockable

@mockable[BalanceAlertsRepository.Service]
object BalanceAlertsRepositoryMock {}
