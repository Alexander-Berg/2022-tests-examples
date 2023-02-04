package ru.yandex.realty.clusterzier.backend.dao.ydb

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clusterizer.backend.dao.ydb.{YdbOfferClusterKeyActions, YdbOfferClusterKeyTable}
import ru.yandex.realty.clusterzier.backend.dao.TestYdb

@RunWith(classOf[JUnitRunner])
class YbdOfferClusterKeyActionsSpec extends AsyncSpecBase with TestYdb {

  override def beforeAll(): Unit = {
    YdbOfferClusterKeyTable.initTable(ydbWrapper, ydbConfig.tablePrefix)
  }

  private lazy val actions = new YdbOfferClusterKeyActions(ydbConfig.tablePrefix)

  before {
    clean(ydbConfig.tablePrefix, YdbOfferClusterKeyTable.TableName).futureValue
  }

  "YbdOfferClusterKeyActions" should {
    "read None for nonexisting offer" in {
      val getAction = actions.getClusterKey("offer1")
      val res = actionsRunner.run(getAction).futureValue
      res shouldBe None
    }

    "read added cluster key" in {
      val offerId = "offer1"
      val expectedClusterKey = 42
      val actualClusterKeyAction = for {
        _ <- actions.setClusterKey(offerId, expectedClusterKey)
        actualClusterKey <- actions.getClusterKey(offerId)
      } yield actualClusterKey
      val actualClusterKey = actionsRunner.run(actualClusterKeyAction).futureValue
      actualClusterKey shouldBe Some(expectedClusterKey)
    }

    "update existing cluster key" in {
      val offerId = "offer1"
      val previousClusterKey = 41
      val newClusterKey = 42
      val actualClusterKeyAction = for {
        _ <- actions.setClusterKey(offerId, previousClusterKey)
        _ <- actions.setClusterKey(offerId, newClusterKey)
        actualClusterKey <- actions.getClusterKey(offerId)
      } yield actualClusterKey
      val actualClusterKey = actionsRunner.run(actualClusterKeyAction).futureValue
      actualClusterKey shouldBe Some(newClusterKey)
    }

    "set multiple cluster key in transaction" in {
      val offerId1 = "offer1"
      val clusterKey1 = 41
      val offerId2 = "offer2"
      val clusterKey2 = 42
      val insertActions = for {
        _ <- actions.setClusterKey(offerId1, clusterKey1)
        _ <- actions.setClusterKey(offerId2, clusterKey2)
      } yield Unit
      actionsRunner.run(actionsRunner.transactional(insertActions)).futureValue
      val actualClusterKeyAction = for {
        actualClusterKey1 <- actions.getClusterKey(offerId1)
        actualClusterKey2 <- actions.getClusterKey(offerId2)
      } yield (actualClusterKey1, actualClusterKey2)

      val (actualClusterKey1, actualClusterKey2) =
        actionsRunner.run(actionsRunner.transactional(actualClusterKeyAction)).futureValue

      actualClusterKey1 shouldBe Some(clusterKey1)
      actualClusterKey2 shouldBe Some(clusterKey2)
    }

    "delete existing cluster key" in {
      val offerId = "offer1"
      val clusterKey = 42
      val actualClusterKeyAction = for {
        _ <- actions.setClusterKey(offerId, clusterKey)
        _ <- actions.delete(offerId)
        actualClusterKey <- actions.getClusterKey(offerId)
      } yield actualClusterKey
      val actualClusterKey = actionsRunner.run(actualClusterKeyAction).futureValue
      actualClusterKey shouldBe None
    }

  }

}
