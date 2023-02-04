package ru.auto.salesman.service.featured

import ru.auto.salesman.Task
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.service.user.UserFeatureService

import ru.auto.salesman.service.UserQuotaModerationService
import ru.yandex.vertis.moderation.proto.Model.Domain.{UsersAutoru => ModerationCategory}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.service.featured.FeaturedUserQuotaModerationServiceSpec._
import zio.ZIO

class FeaturedUserQuotaModerationServiceSpec extends BaseSpec {

  val featureService: UserFeatureService = mock[UserFeatureService]
  val dummyService: DummyService = mock[DummyService]

  val feature = (featureService.userQuotaModerationEventsEnabled _)
    .expects()

  val dummyExecute = (dummyService.execute _)
    .expects()

  val testUser = AutoruUser("user:123")

  val service = new MockedUserQuotaModerationService(dummyService)
    with FeaturedUserQuotaModerationService {
    def featureManager: UserFeatureService = featureService
  }

  "FeaturedUserQuotaModerationService" should {

    "call removeUserQuota on feature = true" in {
      feature.returning(true).anyNumberOfTimes()
      dummyExecute.returningZ(()).once()

      service.removeUserQuota(testUser, Seq(ModerationCategory.CARS)).success
    }

    "dont call removeUserQuota on feature = false" in {
      feature.returning(false).anyNumberOfTimes()
      dummyExecute.returningZ(()).never()

      service.removeUserQuota(testUser, Seq(ModerationCategory.CARS)).success
    }

    "call restoreUserQuota on feature = true" in {
      feature.returning(true).anyNumberOfTimes()
      dummyExecute.returningZ(()).once()

      service.restoreUserQuota(testUser, Seq(ModerationCategory.CARS)).success
    }

    "dont call restoreUserQuota on feature = false" in {
      feature.returning(false).anyNumberOfTimes()
      dummyExecute.returningZ(()).never()

      service.restoreUserQuota(testUser, Seq(ModerationCategory.CARS)).success
    }

  }

}

object FeaturedUserQuotaModerationServiceSpec {

  class DummyService {
    def execute(): Task[Unit] = ZIO.unit
  }

  class MockedUserQuotaModerationService(service: DummyService)
      extends UserQuotaModerationService {

    def removeUserQuota(
        user: AutoruUser,
        categories: Seq[ModerationCategory]
    ): Task[Unit] =
      service.execute()

    def restoreUserQuota(
        user: AutoruUser,
        categories: Seq[ModerationCategory]
    ): Task[Unit] =
      service.execute()
  }

}
