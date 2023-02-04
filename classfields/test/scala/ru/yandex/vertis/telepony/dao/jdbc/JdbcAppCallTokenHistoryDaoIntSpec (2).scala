package ru.yandex.vertis.telepony.dao.jdbc

import org.joda.time.DateTime
import ru.yandex.vertis.telepony.dao.AppCallTokenHistoryDao
import ru.yandex.vertis.telepony.model.TypedDomains.autoru_def
import ru.yandex.vertis.telepony.model.{AppCallToken, AppCallTokenInfo, RedirectId}
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate
import ru.yandex.vertis.telepony.util.db.SlickDb.{HasPrivilege, Master}
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}
import slick.dbio.{DBIOAction, NoStream}
import slick.dbio.Effect.{Read, Write}

import scala.concurrent.Future

class JdbcAppCallTokenHistoryDaoIntSpec extends SpecBase with JdbcSpecTemplate with DatabaseSpec {

  private lazy val appCallTokenHistoryDao: AppCallTokenHistoryDao = new JdbcAppCallTokenHistoryDao()

  private def writeTx[T, E <: Write](action: DBIOAction[T, NoStream, E])(implicit p: Master HasPrivilege E): Future[T] =
    dualDb.master.run("master", action)
  private def readTx[T](action: DBIOAction[T, NoStream, Read]): Future[T] = dualDb.slave.run("slave", action)

  override def beforeEach(): Unit = {
    super.beforeEach()
    writeTx(appCallTokenHistoryDao.clear()).futureValue
  }

  "AppCallTokenHistoryDao" should {
    "create token and get it" in {
      val tokenInfo = AppCallTokenInfo(
        AppCallToken("token"),
        "sourceUsername",
        Some("targetUsername"),
        autoru_def,
        RedirectId("redirect-id"),
        DateTime.now()
      )
      writeTx(appCallTokenHistoryDao.createOrUpdate(tokenInfo)).futureValue

      val actualTokenInfo = writeTx(appCallTokenHistoryDao.get(tokenInfo.token, tokenInfo.redirectId)).futureValue
      actualTokenInfo should not be empty
      actualTokenInfo.get shouldBe tokenInfo
    }

    "update token" in {
      val tokenInfo = AppCallTokenInfo(
        AppCallToken("token"),
        "sourceUsername",
        Some("targetUsername"),
        autoru_def,
        RedirectId("redirect-id"),
        DateTime.now()
      )
      writeTx(appCallTokenHistoryDao.createOrUpdate(tokenInfo)).futureValue
      val tokenInfo2 = tokenInfo.copy(
        sourceUsername = "anotherUsername",
        targetUsername = Some("anotherUsername"),
        createTime = DateTime.now().plusDays(1)
      )
      writeTx(appCallTokenHistoryDao.createOrUpdate(tokenInfo2)).futureValue

      val result = writeTx(appCallTokenHistoryDao.get(tokenInfo.token, tokenInfo.redirectId)).futureValue
      result should not be empty
      result.get shouldBe tokenInfo2
    }

    "create token with empty target username" in {
      val tokenInfo =
        AppCallTokenInfo(
          AppCallToken("token"),
          "sourceUsername",
          None,
          autoru_def,
          RedirectId("redirect-id"),
          DateTime.now()
        )
      writeTx(appCallTokenHistoryDao.createOrUpdate(tokenInfo)).futureValue

      val actualTokenInfo = writeTx(appCallTokenHistoryDao.get(tokenInfo.token, tokenInfo.redirectId)).futureValue
      actualTokenInfo should not be empty
      actualTokenInfo.get shouldBe tokenInfo
    }

    "get empty for not existing token" in {
      val actualTokenInfo = readTx(appCallTokenHistoryDao.list(AppCallToken("some_token"))).futureValue
      actualTokenInfo shouldBe empty
    }

    "list token info with desc createTime ordering" in {
      val now = DateTime.now()
      val tokenInfo1 = AppCallTokenInfo(
        AppCallToken("token"),
        "sourceUsername",
        Some("targetUsername"),
        autoru_def,
        RedirectId("redirect-id1"),
        now
      )
      val tokenInfo2 = tokenInfo1.copy(redirectId = RedirectId("redirect-id2"), createTime = now.plusDays(1))
      writeTx(appCallTokenHistoryDao.createOrUpdate(tokenInfo1)).futureValue
      writeTx(appCallTokenHistoryDao.createOrUpdate(tokenInfo2)).futureValue

      val actualTokenInfoRes = readTx(appCallTokenHistoryDao.list(tokenInfo1.token)).futureValue
      actualTokenInfoRes shouldBe Seq(tokenInfo2, tokenInfo1)
    }

    "list tokens by token filter" in {
      val tokenInfo = AppCallTokenInfo(
        AppCallToken("token"),
        "sourceUsername",
        Some("targetUsername"),
        autoru_def,
        RedirectId("redirect-id1"),
        DateTime.now()
      )
      val anotherTokenInfo = tokenInfo.copy(token = AppCallToken("token2"), redirectId = RedirectId("redirect-id2"))
      writeTx(appCallTokenHistoryDao.createOrUpdate(tokenInfo)).futureValue
      writeTx(appCallTokenHistoryDao.createOrUpdate(anotherTokenInfo)).futureValue

      val actualTokenInfoRes = readTx(appCallTokenHistoryDao.list(tokenInfo.token)).futureValue
      actualTokenInfoRes shouldBe Seq(tokenInfo)
    }
  }

}
