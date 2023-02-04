package baker.shark.core.dao.testkit

import ru.yandex.vertis.shark.dao.{
  CreditApplicationBankClaimDao,
  CreditApplicationClaimDao,
  CreditApplicationDao,
  CreditApplicationSchedulerQueueDao
}
import zio.test.mock.mockable

object Mock {

  @mockable[CreditApplicationDao.Service]
  object CreditApplicationDaoMock

  @mockable[CreditApplicationSchedulerQueueDao.Service]
  object CreditApplicationSchedulerQueueDaoMock

  @mockable[CreditApplicationClaimDao.Service]
  object CreditApplicationClaimDaoMock

  @mockable[CreditApplicationBankClaimDao.Service]
  object CreditApplicationBankClaimDaoMock
}
