package auto.dealers.dealer_pony.storage.testkit

import auto.dealers.dealer_pony.storage.dao.DealerPhonesDao
import zio.test.mock.mockable

@mockable[DealerPhonesDao.Service]
object DealerPhonesDaoMock {}
