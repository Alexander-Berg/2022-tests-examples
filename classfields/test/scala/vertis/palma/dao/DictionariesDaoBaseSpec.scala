package vertis.palma.dao

import vertis.ydb.YEnv
import vertis.ydb.test.YdbTest
import vertis.ydb.util.TracedYdbZioWrapper
import vertis.zio.test.ZioSpecBase
import zio.RIO

/** @author ruslansd
  */
trait DictionariesDaoBaseSpec extends ZioSpecBase with YdbTest {

  def ioTest(action: DictionariesDaoImpl => RIO[YEnv, _]): Unit = {
    zioRuntime.unsafeRunTask {
      TracedYdbZioWrapper.wrap(ydbWrapper).flatMap { ydb =>
        val dao = new DictionariesDaoImpl(ydb, prometheusRegistry)
        action(dao)
      }
    }
    ()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    ioTest { dao =>
      dao.dictionaries.init() *>
        dao.relations.init() *>
        dao.indexes.init()
    }
  }

}
