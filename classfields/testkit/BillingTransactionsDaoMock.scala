package auto.dealers.amoyak.storage.testkit

import auto.dealers.amoyak.storage.dao.BillingTransactionsDao
import zio.test.mock.mockable

@mockable[BillingTransactionsDao.Service]
object BillingTransactionsDaoMock {}
