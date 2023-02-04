package ru.auto.salesman.service.impl.user.notify.impl

import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.dao.notification.ProlongationFailedNotificationRateLimiterDao
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.model.notification.LastProlongationFailedNotificationSentAt
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}

class RateLimiterImplSpec extends BaseSpec with IntegrationPropertyCheckConfig {

  val prolongationFailedNotificationRateLimiterDao =
    mock[ProlongationFailedNotificationRateLimiterDao]

  val rateLimiter = new UserMessageRateLimiterServiceImpl(
    prolongationFailedNotificationRateLimiterDao
  )

  "UserMessageRateLimiterServiceImpl" should {
    "return true if dao return None and service need call save in dao" in {
      val userId = AutoruUser(134)
      mockGetLastSaveTime(userId, None)

      (prolongationFailedNotificationRateLimiterDao.save _)
        .expects(userId)
        .returningZ(1)

      checkRateLimiter(userId, Task.succeed(()))
    }

    "return false if dao return rate limit with last save time less 3 hours" in {
      val userId = AutoruUser(140)
      mockGetLastSaveTime(
        userId,
        Some(
          LastProlongationFailedNotificationSentAt(
            userId = userId,
            lastSentAt = DateTime.now().minusHours(2)
          )
        )
      )

      val exceptions = rateLimiter
        .limit(userId)(Task.succeed(()))
        .failure
        .cause
        .failures
      exceptions.size shouldBe 1
      exceptions.head.getMessage shouldBe "prolongation failed notification recently departed"

    }

    "return true if dao return rate limit with last save time more 3 hours" in {
      val userId = AutoruUser(44)
      mockGetLastSaveTime(
        userId,
        Some(
          LastProlongationFailedNotificationSentAt(
            userId = userId,
            lastSentAt = DateTime.now().minusHours(4)
          )
        )
      )
      mockSaveLastUpdateTime(userId)

      checkRateLimiter(userId, Task.succeed(()))
    }
  }

  private def mockGetLastSaveTime(
      userId: AutoruUser,
      result: Option[LastProlongationFailedNotificationSentAt]
  ): Unit =
    (prolongationFailedNotificationRateLimiterDao.get _)
      .expects(userId)
      .returningZ(result)

  private def mockSaveLastUpdateTime(userId: AutoruUser): Unit =
    (prolongationFailedNotificationRateLimiterDao.save _)
      .expects(userId)
      .returningZ(1)

  private def checkRateLimiter(userId: AutoruUser, result: Task[Unit]): Unit =
    rateLimiter
      .limit(userId)(result)
      .success

}
