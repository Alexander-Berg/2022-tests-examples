package auto.dealers.calltracking.storage.test

import java.time.{Instant, LocalDate, ZoneOffset}
import java.time.temporal.ChronoUnit
import common.zio.doobie.ConnManager
import auto.dealers.calltracking.model.{ClientId, RedirectPhone}
import auto.dealers.calltracking.storage.RedirectConfirmationsDao
import auto.dealers.calltracking.storage.postgresql.PgRedirectConfirmationsDao
import auto.dealers.calltracking.storage.testkit.TestPostgresql
import zio.test.Assertion._
import zio.test.DefaultRunnableSpec
import zio.test.TestAspect._
import zio.test._

object PgRedirectConfirmationsSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("PgRedirectConfirmationDao")(
      testM("select redirects by confirmation") {
        val falseConfirmed = RedirectPhone(20101L, "+71112223344", "autoru", Instant.EPOCH, confirmed = false)
        for {
          _ <- RedirectConfirmationsDao.insert(falseConfirmed)
          _ <- RedirectConfirmationsDao.insert(
            RedirectPhone(20101L, "+71112223311", "autoru", Instant.ofEpochSecond(-10), confirmed = false)
          )
          _ <- RedirectConfirmationsDao.insert(
            RedirectPhone(20101L, "+71112223355", "autoru", Instant.EPOCH, confirmed = true)
          )
          _ <- RedirectConfirmationsDao.insert(
            RedirectPhone(68L, "+71112223344", "autoru", Instant.EPOCH, confirmed = false)
          )
          selected <- RedirectConfirmationsDao.fetchRedirects(
            ClientId(20101L),
            confirmed = Some(false),
            Instant.ofEpochSecond(-1)
          )
        } yield assert(selected)(hasSameElements(List(falseConfirmed)))
      },
      testM("set confirmed to false and update deadline if redirect is expired") {
        val deadline = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
        val expiredDeadline =
          RedirectPhone(20101L, "+70000000001", "autoru", deadline.minus(1, ChronoUnit.DAYS), confirmed = true)

        for {
          _ <- RedirectConfirmationsDao.insert(expiredDeadline)
          expired <- RedirectConfirmationsDao.getOrCreateConfirmation(
            ClientId(20101L),
            "autoru",
            "+70000000001",
            deadline.plus(1, ChronoUnit.DAYS)
          )
          expiredUpdated <- RedirectConfirmationsDao.fetchRedirect(ClientId(20101L), "autoru", "+70000000001")
        } yield {
          assert(expired)(isFalse) &&
          assert(expiredUpdated)(
            isSome(equalTo(expiredDeadline.copy(confirmed = false, deadline = deadline.plus(1, ChronoUnit.DAYS))))
          )
        }
      },
      testM("get actual confirmation status and update deadline if redirect is not expired") {
        val deadline = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
        val notExpiredDeadline =
          RedirectPhone(20101L, "+70000000002", "autoru", deadline.plus(1, ChronoUnit.DAYS), confirmed = true)

        for {
          _ <- RedirectConfirmationsDao.insert(notExpiredDeadline)
          notExpired <- RedirectConfirmationsDao.getOrCreateConfirmation(
            ClientId(20101L),
            "autoru",
            "+70000000002",
            deadline.plus(2, ChronoUnit.DAYS)
          )
          notExpiredUpdated <- RedirectConfirmationsDao.fetchRedirect(ClientId(20101L), "autoru", "+70000000002")
        } yield {
          assert(notExpired)(isTrue) &&
          assert(notExpiredUpdated)(
            isSome(equalTo(notExpiredDeadline.copy(deadline = deadline.plus(2, ChronoUnit.DAYS))))
          )
        }
      },
      testM("create new redirect confirmation if redirect does not exist") {
        val deadline = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)

        for {
          created <- RedirectConfirmationsDao.getOrCreateConfirmation(
            ClientId(20101L),
            "autoru",
            "+70000000003",
            deadline
          )
          createdConfirmation <- RedirectConfirmationsDao.fetchRedirect(ClientId(20101L), "autoru", "+70000000003")
        } yield {
          assert(created)(isFalse) &&
          assert(createdConfirmation)(
            isSome(
              equalTo(
                RedirectPhone(20101L, "+70000000003", "autoru", deadline, confirmed = false)
              )
            )
          )
        }
      },
      testM("confirm existent redirect") {
        val redirect =
          RedirectPhone(20101L, "+70000000004", "autoru", Instant.ofEpochSecond(100), confirmed = false)

        for {
          _ <- RedirectConfirmationsDao.insert(redirect)
          result <- RedirectConfirmationsDao.confirmRedirect(ClientId(20101L), "autoru", "+70000000004")
          confirmedRedirect <- RedirectConfirmationsDao.fetchRedirect(ClientId(20101L), "autoru", "+70000000004")
        } yield assert(result)(isTrue) &&
          assert(confirmedRedirect)(isSome(equalTo(redirect.copy(confirmed = true))))
      },
      testM("create new redirect confirmation if redirect does not exist with confirmed = true if platform = avito") {
        val deadline = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)

        for {
          created <- RedirectConfirmationsDao.getOrCreateConfirmation(
            ClientId(20101L),
            "avito",
            "+70000000003",
            deadline
          )
          createdConfirmation <- RedirectConfirmationsDao.fetchRedirect(ClientId(20101L), "avito", "+70000000003")
        } yield {
          assert(created)(isTrue) &&
          assert(createdConfirmation)(
            isSome(
              equalTo(
                RedirectPhone(20101L, "+70000000003", "avito", deadline, confirmed = true)
              )
            )
          )
        }
      },
      testM("set confirmed = true and update deadline if redirect is expired for platform = avito") {
        val deadline = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
        val expiredDeadline =
          RedirectPhone(20101L, "+70000000001", "avito", deadline.minus(1, ChronoUnit.DAYS), confirmed = true)

        for {
          _ <- RedirectConfirmationsDao.insert(expiredDeadline)
          expired <- RedirectConfirmationsDao.getOrCreateConfirmation(
            ClientId(20101L),
            "avito",
            "+70000000001",
            deadline.plus(1, ChronoUnit.DAYS)
          )
          expiredUpdated <- RedirectConfirmationsDao.fetchRedirect(ClientId(20101L), "avito", "+70000000001")
        } yield {
          assert(expired)(isTrue) &&
          assert(expiredUpdated)(
            isSome(equalTo(expiredDeadline.copy(confirmed = true, deadline = deadline.plus(1, ChronoUnit.DAYS))))
          )
        }
      },
      testM("get actual confirmation status and update deadline if redirect is not expired for platform = avito") {
        val deadline = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
        val notExpiredDeadline =
          RedirectPhone(20101L, "+70000000002", "avito", deadline.plus(1, ChronoUnit.DAYS), confirmed = true)

        for {
          _ <- RedirectConfirmationsDao.insert(notExpiredDeadline)
          notExpired <- RedirectConfirmationsDao.getOrCreateConfirmation(
            ClientId(20101L),
            "avito",
            "+70000000002",
            deadline.plus(2, ChronoUnit.DAYS)
          )
          notExpiredUpdated <- RedirectConfirmationsDao.fetchRedirect(ClientId(20101L), "avito", "+70000000002")
        } yield {
          assert(notExpired)(isTrue) &&
          assert(notExpiredUpdated)(
            isSome(equalTo(notExpiredDeadline.copy(deadline = deadline.plus(2, ChronoUnit.DAYS))))
          )
        }
      },
      testM("return false when was made an attempt to confirm non existent redirect") {
        for {
          result <- RedirectConfirmationsDao.confirmRedirect(ClientId(12345L), "non-existent", "number")
        } yield assert(result)(isFalse)
      }
    ) @@ after(PgRedirectConfirmationsDao.clean) @@ beforeAll(PgRedirectConfirmationsDao.initSchema.orDie) @@ sequential
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor >+> ConnManager.fromTransactor >>> PgRedirectConfirmationsDao.live
  )
}
