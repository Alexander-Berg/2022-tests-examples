package ru.yandex.vertis.general.bonsai.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.bonsai.category_model.Category
import ru.yandex.vertis.general.bonsai.model.BonsaiError
import ru.yandex.vertis.general.bonsai.model.testkit.Generators
import ru.yandex.vertis.general.bonsai.storage.testkit.EntityDaoSpec
import ru.yandex.vertis.general.bonsai.storage.ydb.sign.EntityUpdateChecker
import ru.yandex.vertis.general.bonsai.storage.ydb.{BonsaiTxRunner, YdbEntityDao}
import zio.clock.Clock
import zio.random.Random
import zio.test.TestAspect._
import zio.test._

object YdbCategoryDaoSpec extends DefaultRunnableSpec {
  implicit private val categoryGen: Gen[Random with Sized, Category] = Generators.category()

  override def spec: Spec[_root_.zio.test.environment.TestEnvironment, TestFailure[Any], TestSuccess] =
    (EntityDaoSpec
      .spec[Category]("YdbCategoryDao") @@ nondeterministic @@ sequential)
      .provideCustomLayerShared {
        val ydb = TestYdb.ydb
        val updateChecker = EntityUpdateChecker.live
        val txRunner = (updateChecker ++ (ydb >>> Ydb.txRunner)) >>> BonsaiTxRunner.live
        (ydb ++ updateChecker) >>> (YdbEntityDao.live ++ txRunner ++ Clock.live)
      }
}
