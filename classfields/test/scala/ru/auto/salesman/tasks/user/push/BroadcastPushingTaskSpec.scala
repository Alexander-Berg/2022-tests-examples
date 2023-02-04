package ru.auto.salesman.tasks.user.push

import ru.auto.salesman.client.pushnoy.PushnoyDelivery.ServicesAndDiscounts
import ru.auto.salesman.client.pushnoy.{PushTemplateV1, PushnoyClient, ToPushDelivery}
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.model.push._
import ru.auto.salesman.service.user.PeriodicalDiscountService
import ru.auto.salesman.tasks.Partition
import ru.auto.salesman.tasks.user.push.BroadcastPushingTask.ProgressTrackingId
import ru.auto.salesman.tasks.user.push.BroadcastPushingTaskSpec._
import ru.auto.salesman.tasks.user.push.model._
import ru.auto.salesman.test.{BaseSpec, TestException}

import scala.language.existentials

class BroadcastPushingTaskSpec extends BaseSpec {

  "BroadcastPushingTask" should {

    "not push anything if there is no active broadcast pushing" in {
      stubGetActiveBroadcastPushingSchedule(None)

      createTask(List(Stream(AutoruUser(1)))).success
      (pushnoyClient.pushToUser _).when(*, *).never()
      (periodicalDiscountService.getExcludedUsersByDiscount _).when(*).never()
    }

    "push to one user if there is an active broadcast and one user is returned by UserSource" in {
      stubGetActiveBroadcastPushingSchedule(Some(pushingSchedule))
      stubGetExcludedUsers(Iterable.empty[AutoruUser])
      (pushnoyClient.pushToUser _).when(*, *).returningZ(1)
      val user = AutoruUser(1)
      createTask(List(Stream(AutoruUser(1)))).success
      verifyTriedToPushOnceTo(user)
    }

    "don't push to user again on the next task execution" in {
      stubGetActiveBroadcastPushingSchedule(Some(pushingSchedule))
      stubGetExcludedUsers(Iterable.empty[AutoruUser])
      (pushnoyClient.pushToUser _).when(*, *).returningZ(1)
      val user = AutoruUser(1)
      val task = createTask(List(Stream(AutoruUser(1))))
      task.success
      task.success
      verifyTriedToPushOnceTo(user)
    }

    "don't push to user again if pushnoy failed once even on the next task execution" in {
      stubGetActiveBroadcastPushingSchedule(Some(pushingSchedule))
      stubGetExcludedUsers(Iterable.empty[AutoruUser])
      val e = new TestException
      (pushnoyClient.pushToUser _).when(*, *).throwingZ(e)
      val user = AutoruUser(1)
      val task = createTask(List(Stream(AutoruUser(1))))
      task.failure.exception shouldBe e
      task.success
      verifyTriedToPushOnceTo(user)
    }

    "push to second user on the next task execution if pushnoy failed for first user" in {
      stubGetActiveBroadcastPushingSchedule(Some(pushingSchedule))
      stubGetExcludedUsers(Iterable.empty[AutoruUser])
      val e = new TestException
      val succeedingUser = AutoruUser(1)
      val failingUser = AutoruUser(2)
      (pushnoyClient.pushToUser _)
        .when(*, pushDelivery(failingUser))
        .throwingZ(e)
      (pushnoyClient.pushToUser _)
        .when(*, pushDelivery(succeedingUser))
        .returningZ(1)
      val task = createTask(List(Stream(succeedingUser, failingUser)))
      task.failure.exception shouldBe e
      task.success
      verifyTriedToPushOnceTo(failingUser)
      verifyTriedToPushOnceTo(succeedingUser)
    }

    "push to users from different partitions" in {
      stubGetActiveBroadcastPushingSchedule(Some(pushingSchedule))
      stubGetExcludedUsers(Iterable.empty[AutoruUser])
      (pushnoyClient.pushToUser _).when(*, *).returningZ(1)
      val partition1User = AutoruUser(2)
      val partition2User = AutoruUser(1)
      val task =
        createTask(List(Stream(partition1User), Stream(partition2User)))
      task.success
      verifyTriedToPushOnceTo(partition1User)
      verifyTriedToPushOnceTo(partition2User)
    }

    "skip excluded users in different partitions" in {
      val partition1User1 = AutoruUser(1)
      val partition1User2 = AutoruUser(2)
      val partition2User1 = AutoruUser(3)
      val partition2User2 = AutoruUser(4)

      stubGetActiveBroadcastPushingSchedule(Some(pushingSchedule))
      stubGetExcludedUsers(Seq(partition1User2, partition2User2))
      (pushnoyClient.pushToUser _).when(*, *).returningZ(1)
      val task =
        createTask(
          List(
            Stream(partition1User1, partition1User2),
            Stream(partition2User1, partition2User2)
          )
        )
      task.success
      verifyTriedToPushOnceTo(partition1User1)
      verifyTriedToPushOnceTo(partition2User1)
      verifyNeverPushTo(partition1User2)
      verifyNeverPushTo(partition2User2)
    }

    "skip excluded users in one partition" in {
      val user = AutoruUser(1)
      val excludedUsers = (2 to 4).map(AutoruUser(_)).toList
      stubGetActiveBroadcastPushingSchedule(Some(pushingSchedule))
      stubGetExcludedUsers(excludedUsers)
      (pushnoyClient.pushToUser _).when(*, *).returningZ(1)
      createTask(
        List(Stream(AutoruUser(1)) #::: excludedUsers.toStream)
      ).success
      verifyTriedToPushOnceTo(user)
      excludedUsers.foreach(verifyNeverPushTo)
    }

    "don't push if all users were excluded" in {
      val users = (1 to 4).map(AutoruUser(_)).toList
      stubGetActiveBroadcastPushingSchedule(Some(pushingSchedule))
      stubGetExcludedUsers(users)
      createTask(List(users.toStream)).success
      (pushnoyClient.pushToUser _).when(*, *).never()
    }
  }

  private val broadcastPushingScheduleService =
    stub[BroadcastPushingScheduleService]

  private val progressService =
    new DummyProgressService[ProgressTrackingId, AutoruUser]

  private val pushnoyClient = stub[PushnoyClient]
  private val periodicalDiscountService = stub[PeriodicalDiscountService]

  private def createTask(users: List[Stream[AutoruUser]]) = {
    val parallelizm = users.size

    new BroadcastPushingTask(
      periodicalDiscountService,
      broadcastPushingScheduleService,
      progressService,
      new DummyPartitionedUserSource(
        Partition.all(parallelizm).zip(users).toMap
      ),
      pushnoyClient
    )(parallelizm).task
  }

  private def stubGetActiveBroadcastPushingSchedule(
      pushingSchedule: Option[BroadcastPushingSchedule]
  ) =
    (broadcastPushingScheduleService.getActive _)
      .when()
      .returningZ(pushingSchedule)

  private def stubGetExcludedUsers(excludedUsers: Iterable[AutoruUser]) =
    (periodicalDiscountService.getExcludedUsersByDiscount _)
      .when(pushingDiscountSourceId.toString)
      .returningZ(excludedUsers)

  private def verifyTriedToPushOnceTo(AutoruUser: AutoruUser) =
    (pushnoyClient.pushToUser _)
      .verify(pushTemplate, pushDelivery(AutoruUser))
      .once()

  private def verifyNeverPushTo(AutoruUser: AutoruUser) =
    (pushnoyClient.pushToUser _)
      .verify(pushTemplate, pushDelivery(AutoruUser))
      .never()
}

object BroadcastPushingTaskSpec {

  private val pushingDiscountSourceId = 100

  private val pushingSchedule =
    BroadcastPushingSchedule(
      BroadcastPushingScheduleId(1),
      PushTitle("Скидка 70%"),
      PushBody("Только сегодня скидка 70% на Турбо-продажу!"),
      PushName("VAS_080919_turbo"),
      start = now(),
      PushSourceType.PeriodicalDiscount,
      PushSourceId(pushingDiscountSourceId)
    )

  private val pushTemplate =
    PushTemplateV1(
      event = "deeplink",
      PushName("VAS_080919_turbo"),
      deepLink = "autoru://app/users.auto.ru/sales",
      PushTitle("Скидка 70%"),
      PushBody("Только сегодня скидка 70% на Турбо-продажу!")
    )

  private def pushDelivery(AutoruUser: AutoruUser) =
    ToPushDelivery(
      s"user:${AutoruUser.id}",
      delivery = Some(ServicesAndDiscounts),
      appVersion = None
    )
}
