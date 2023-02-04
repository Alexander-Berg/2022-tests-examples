package ru.yandex.vertis.general.bonsai.storage.testkit

import common.zio.ydb.Ydb.HasTxRunner
import izumi.reflect.Tag
import ru.yandex.vertis.general.bonsai.model.BonsaiError
import ru.yandex.vertis.general.bonsai.storage.HierarchyDao
import ru.yandex.vertis.general.bonsai.storage.HierarchyDao.HierarchyDao
import zio.test.Assertion._
import zio.test._

object HierarchyDaoSpec {

  def spec(
      label: String): Spec[zio.ZEnv with HierarchyDao with HasTxRunner, TestFailure[BonsaiError], TestSuccess] = {
    suite(label)(
      testM("add child relationship") {
        for {
          _ <- runTx(HierarchyDao.addChildren("123", "456"))
          children <- runTx(HierarchyDao.getChildren("123"))
        } yield assert(children)(equalTo(List("456")))
      },
      testM("add child relationship  - many") {
        for {
          _ <- runTx(HierarchyDao.addChildren("123", "456"))
          _ <- runTx(HierarchyDao.addChildren("123", "666"))
          children <- runTx(HierarchyDao.getChildren("123"))
        } yield assert(children)(hasSameElements(List("456", "666")))
      },
      testM("add child relationship is idempotent") {
        for {
          _ <- runTx(HierarchyDao.addChildren("123", "456"))
          _ <- runTx(HierarchyDao.addChildren("123", "456"))
          children <- runTx(HierarchyDao.getChildren("123"))
        } yield assert(children)(equalTo(List("456")))
      },
      testM("delete child relationship") {
        for {
          _ <- runTx(HierarchyDao.addChildren("123", "456"))
          children1 <- runTx(HierarchyDao.getChildren("123"))
          _ <- runTx(HierarchyDao.deleteChild("123", "456"))
          children2 <- runTx(HierarchyDao.getChildren("123"))
        } yield assert(children1)(equalTo(List("456"))) && assert(children2)(isEmpty)
      },
      testM("delete child relationship is idempotent") {
        for {
          _ <- runTx(HierarchyDao.deleteChild("123", "456"))
          _ <- runTx(HierarchyDao.deleteChild("123", "456"))
        } yield assertCompletes
      }
    )
  }
}
