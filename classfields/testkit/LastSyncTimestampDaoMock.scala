package auto.dealers.amoyak.storage.testkit

import auto.dealers.amoyak.storage.dao.LastSyncTimestampDao
import zio.test.mock.mockable

@mockable[LastSyncTimestampDao.Service]
object LastSyncTimestampDaoMock {}
