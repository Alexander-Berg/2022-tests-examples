package ru.auto.salesman.tasks.kafka.processors

import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.service.UserQuotaModerationService
import ru.auto.salesman.tasks.kafka.processors.exceptions.InvalidCategoryException
import ru.auto.salesman.tasks.kafka.processors.impl.UserQuotaChangedProcessorImpl
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.passport.model.proto.{
  EmailSent,
  Event,
  EventPayload,
  UserQuotaRemoved,
  UserQuotaRestored
}
import ru.yandex.vertis.moderation.proto.Model.Domain.{UsersAutoru => ModerationCategory}

import scala.collection.JavaConverters._

class UserQuotaChangedProcessorSpec extends BaseSpec {
  private val service = mock[UserQuotaModerationService]
  private val processor = new UserQuotaChangedProcessorImpl(service)

  "UserQuotaChangedProcessorSpec" should {
    "process remove quota event" in {
      val event = Event
        .newBuilder()
        .setPayload(
          EventPayload
            .newBuilder()
            .setUserId("123")
            .setUserQuotaRemoved(
              UserQuotaRemoved
                .newBuilder()
                .addAllCategories(List("CARS", "BUS").asJava)
            )
        )
        .build()

      (service.removeUserQuota _)
        .expects(
          AutoruUser("user:123"),
          List(ModerationCategory.CARS, ModerationCategory.BUS)
        )
        .returningZ(unit)

      processor.process(event).success.value shouldBe unit
    }

    "process restore quota event" in {
      val event = Event
        .newBuilder()
        .setPayload(
          EventPayload
            .newBuilder()
            .setUserId("234")
            .setUserQuotaRestored(
              UserQuotaRestored
                .newBuilder()
                .addAllCategories(List("CARS", "BUS").asJava)
            )
        )
        .build()

      (service.restoreUserQuota _)
        .expects(
          AutoruUser("user:234"),
          List(ModerationCategory.CARS, ModerationCategory.BUS)
        )
        .returningZ(unit)

      processor.process(event).success.value shouldBe unit
    }

    "fail on quota event with invalid category" in {
      val event = Event
        .newBuilder()
        .setPayload(
          EventPayload
            .newBuilder()
            .setUserId("123")
            .setUserQuotaRestored(
              UserQuotaRestored
                .newBuilder()
                .addAllCategories(List("CARS1").asJava)
            )
        )
        .build()

      processor
        .process(event)
        .failure
        .exception shouldBe an[InvalidCategoryException]
    }

    "ignore another user events" in {
      val event = Event
        .newBuilder()
        .setPayload(
          EventPayload
            .newBuilder()
            .setUserId("123")
            .setEmailSent(EmailSent.newBuilder())
        )
        .build()

      processor.process(event).success.value shouldBe unit
    }

    "ignore another events without user" in {
      val event = Event
        .newBuilder()
        .setPayload(
          EventPayload
            .newBuilder()
            .setEmailSent(EmailSent.newBuilder())
        )
        .build()

      processor.process(event).success.value shouldBe unit
    }
  }
}
