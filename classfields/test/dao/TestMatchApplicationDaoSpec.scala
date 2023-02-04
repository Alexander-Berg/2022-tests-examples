package auto.dealers.match_maker.logic.dao

import auto.dealers.match_maker.logic.TestMatchApplicationDao
import zio.test.DefaultRunnableSpec

object TestMatchApplicationDaoSpec extends DefaultRunnableSpec {

  def spec =
    DaoTestSuite
      .getDaoTestSuite("TestMatchApplicationDaoSpec")
      .provideCustomLayer(TestMatchApplicationDao.make.toLayer)
}
