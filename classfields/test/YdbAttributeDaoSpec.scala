package ru.yandex.vertis.general.bonsai.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.bonsai.attribute_model.AttributeDefinition
import ru.yandex.vertis.general.bonsai.model.testkit.Generators
import ru.yandex.vertis.general.bonsai.storage.testkit.EntityDaoSpec
import ru.yandex.vertis.general.bonsai.storage.ydb.sign.EntityUpdateChecker
import ru.yandex.vertis.general.bonsai.storage.ydb.{BonsaiTxRunner, YdbEntityDao}
import zio.clock.Clock
import zio.random.Random
import zio.test.TestAspect._
import zio.test._

object YdbAttributeDaoSpec extends DefaultRunnableSpec {
  implicit private val categoryGen: Gen[Random with Sized, AttributeDefinition] = Generators.attribute()

  override def spec: Spec[_root_.zio.test.environment.TestEnvironment, TestFailure[Any], TestSuccess] =
    (EntityDaoSpec
      .spec[AttributeDefinition]("YdbAttributeDao") @@ nondeterministic @@ sequential)
      .provideCustomLayerShared {
        val ydb = TestYdb.ydb
        val updateChecker = EntityUpdateChecker.live
        val txRunner = (updateChecker ++ (ydb >>> Ydb.txRunner)) >>> BonsaiTxRunner.live
        (ydb ++ updateChecker) >>> (YdbEntityDao.live ++ txRunner ++ Clock.live)
      }
}
