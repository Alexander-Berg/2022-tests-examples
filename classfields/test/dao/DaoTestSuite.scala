package auto.dealers.match_maker.logic.dao

import auto.dealers.match_maker.logic.dao.DoobieMatchApplicationDaoSpecOps.getApplicationWithId
import auto.dealers.match_maker.logic.dao.MatchApplicationDao.MatchApplicationDao
import auto.dealers.match_maker.model.MatchApplicationState
import ru.auto.match_maker.model.api.ApiModel.{MatchApplication, UserInfo}
import zio.test.Assertion._
import zio.test._

import scala.concurrent.duration._

object DaoTestSuite {

  def getDaoTestSuite(label: String): Spec[MatchApplicationDao, TestFailure[Throwable], TestSuccess] =
    suite("DoobieMatchApplicationDao")(
      testM("Should create matchApplication in DB") {
        for {
          _ <- MatchApplicationDao.create(
            "create",
            getApplicationWithId("create"),
            MatchApplicationState.NeedsProcessing,
            7.days
          )
          result <- MatchApplicationDao.list(List("create"))
        } yield assert(result.exists(ma => ma.getId == "create"))(isTrue)
      },
      testM("Should return next batch for processing consisting from NeedsProcessing applications") {
        for {
          _ <- MatchApplicationDao.create(
            "nb_first",
            getApplicationWithId("nb_first"),
            MatchApplicationState.NeedsProcessing,
            7.days
          )
          _ <- MatchApplicationDao.create(
            "nb_second",
            getApplicationWithId("nb_second"),
            MatchApplicationState.New,
            7.days
          )
          result <- MatchApplicationDao.nextBatchForProcessing(100)
        } yield assert(result.exists(ma => ma.getId == "nb_first"))(isTrue) &&
          assert(result.exists(ma => ma.getId == "nb_second"))(isFalse)
      },
      testM("Should update match applications") {
        for {
          _ <- MatchApplicationDao.create("upd", getApplicationWithId("upd"), MatchApplicationState.New, 7.days)
          first <- MatchApplicationDao.nextBatchForProcessing(100)
          _ <- MatchApplicationDao.update(
            getApplicationWithId("upd"),
            Some(MatchApplicationState.NeedsProcessing)
          )
          second <- MatchApplicationDao.nextBatchForProcessing(100)
        } yield assert(first.exists(ma => ma.getId == "upd"))(isFalse) &&
          assert(second.exists(ma => ma.getId == "upd"))(isTrue)
      },
      testM("Should not throw exception when trying to create two applications with equal ids") {
        for {
          _ <- MatchApplicationDao.create(
            "eq",
            MatchApplication.getDefaultInstance,
            MatchApplicationState.New,
            7.days
          )
          res <-
            MatchApplicationDao
              .create(
                "eq",
                MatchApplication.getDefaultInstance,
                MatchApplicationState.New,
                7.days
              )
              .either
        } yield assert(res)(isRight(isUnit))
      },
      testM("Should return recent user applications") {
        for {
          _ <- MatchApplicationDao.create(
            "r_first",
            MatchApplication.newBuilder().setUserInfo(UserInfo.newBuilder().setUserId(77)).build(),
            MatchApplicationState.New,
            7.days
          )
          _ <- MatchApplicationDao.create(
            "r_second",
            MatchApplication.newBuilder().setUserInfo(UserInfo.newBuilder().setUserId(66)).build(),
            MatchApplicationState.New,
            7.days
          )
          result <- MatchApplicationDao.getRecentUserApplications(77, 14.days)
        } yield assert(result)(hasSize(equalTo(1))) &&
          assert(result.forall(ma => ma.getUserInfo.getUserId == 77))(isTrue)
      },
      testM("Should lock users which applications are processing at the moment") {
        for {
          res <- MatchApplicationDao.tryAcquireUserLock(113)
        } yield assert(res)(isTrue)
      },
      testM("Should fail lock when somebody tries to lock on one user many times") {
        for {
          _ <- MatchApplicationDao.tryAcquireUserLock(114)
          res <- MatchApplicationDao.tryAcquireUserLock(114)
        } yield assert(res)(isFalse)
      },
      testM("Should release user lock") {
        for {
          _ <- MatchApplicationDao.tryAcquireUserLock(115)
          _ <- MatchApplicationDao.releaseUserLock(115)
          afterRelease <- MatchApplicationDao.tryAcquireUserLock(115)
        } yield assert(afterRelease)(isTrue)
      }
    )

}
