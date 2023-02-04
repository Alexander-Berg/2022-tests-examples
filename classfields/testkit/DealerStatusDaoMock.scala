package auto.dealers.dealer_pony.storage.testkit

import auto.dealers.dealer_pony.storage.dao.DealerStatusDao
import zio.test.mock.mockable

@mockable[DealerStatusDao.Service]
object DealerStatusDaoMock {}
