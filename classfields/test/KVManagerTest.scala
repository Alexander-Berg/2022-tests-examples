package ru.yandex.vertis.general.common.kv_storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.common.kv_storage.{KVManager, YdbKVDao}
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object KVManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite(" KVManagerTest")(
      testM("сохранить, получить, удалить значение из хранилища") {
        val key = "key"
        val value = 10
        for {
          _ <- KVManager.set(key, value)
          saved <- KVManager.get[Int](key)
          _ <- KVManager.delete(key)
          afterDelete <- KVManager.get[Int](key)
        } yield assert(saved)(equalTo(Some(value))) &&
          assert(afterDelete)(equalTo(None))
      }
    )
  }.provideCustomLayerShared {
    val ydb = TestYdb.ydb
    val txRunner = ydb >+> Ydb.txRunner
    val dao = txRunner >+> YdbKVDao.live
    val manger = dao ++ Clock.live >+> KVManager.live
    manger
  }

}
