package ru.yandex.vertis.shark.controller.impl

import cats.syntax.option._
import com.softwaremill.tagging.Tagger
import org.scalatest.PrivateMethodTester._
import ru.yandex.vertis.shark.controller.CreditApplicationController._
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.CreditApplication
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.event.CreditApplicationProcessingEvent
import ru.yandex.vertis.shark.proto.model.CreditApplicationProcessingEvent.EventType
import ru.yandex.vertis.shark.proto.model.CreditApplication.Claim.ClaimState
import zio.test.Assertion._
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import zio.test.environment.TestEnvironment

import java.time.Instant

object ProcessingEventCreditApplicationControllerSpec extends DefaultRunnableSpec {

  import org.scalacheck.magnolia._

  private val now = Instant.now()
  private val application = generate[AutoruCreditApplication].sample.get

  private val claim = generate[CreditApplication.AutoruClaim].sample.get.copy(
    processAfter = None
  )
  private val notification = generate[PushNotification].sample.get
  private val personProfile = generate[PersonProfileImpl].sample.get
  private val phoneEntity = generate[Entity.PhoneEntity].sample.get
  private val emailEntity = generate[Entity.EmailEntity].sample.get

  private val eventsForUpsertMethod =
    PrivateMethod[Seq[CreditApplicationProcessingEvent]](Symbol("eventsForUpsert"))

  private def eventsForUpsert(timestamp: Instant, data: UpsertData) =
    ProcessingEventCreditApplicationController.invokePrivate(eventsForUpsertMethod(timestamp, data))

  private def checkEventsForUpsert(
      description: String,
      data: UpsertData,
      expected: Set[CreditApplicationProcessingEvent]) = test(description)(
    assert(eventsForUpsert(now, data).toSet)(equalTo(expected))
  )

  private def eventsForUpsertTests =
    Seq(
      checkEventsForUpsert(
        "add phone",
        SimpleUpsertData(
          actual = application.copy(borrowerPersonProfile =
            personProfile
              .copy(
                phones = Block.PhonesBlock(Seq(phoneEntity)).some
              )
              .some
          ),
          previous = application
            .copy(borrowerPersonProfile =
              personProfile
                .copy(phones = Block.PhonesBlock(Seq.empty).some)
                .some
            )
            .some
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.UPDATE
          )
        )
      ),
      checkEventsForUpsert(
        "remove phone",
        SimpleUpsertData(
          actual = application.copy(borrowerPersonProfile =
            personProfile
              .copy(phones = Block.PhonesBlock(Seq.empty).some)
              .some
          ),
          previous = application
            .copy(borrowerPersonProfile =
              personProfile
                .copy(
                  phones = Block.PhonesBlock(Seq(phoneEntity)).some
                )
                .some
            )
            .some
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.UPDATE
          )
        )
      ),
      checkEventsForUpsert(
        "add email",
        SimpleUpsertData(
          actual = application.copy(borrowerPersonProfile =
            personProfile
              .copy(
                emails = Block.EmailsBlock(Seq(emailEntity)).some
              )
              .some
          ),
          previous = application
            .copy(borrowerPersonProfile =
              personProfile
                .copy(emails = Block.EmailsBlock(Seq.empty).some)
                .some
            )
            .some
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.UPDATE
          )
        )
      ),
      checkEventsForUpsert(
        "remove email",
        SimpleUpsertData(
          actual = application.copy(borrowerPersonProfile =
            personProfile
              .copy(emails = Block.EmailsBlock(Seq.empty).some)
              .some
          ),
          previous = application
            .copy(borrowerPersonProfile =
              personProfile
                .copy(
                  emails = Block.EmailsBlock(Seq(emailEntity)).some
                )
                .some
            )
            .some
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.UPDATE
          )
        )
      ),
      checkEventsForUpsert(
        "add phone&email on create (without previous state)",
        SimpleUpsertData(
          actual = application.copy(borrowerPersonProfile =
            personProfile
              .copy(
                phones = Block.PhonesBlock(Seq(phoneEntity)).some,
                emails = Block.EmailsBlock(Seq(emailEntity)).some
              )
              .some
          ),
          previous = none
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.UPDATE
          )
        )
      ),
      checkEventsForUpsert(
        "add notification",
        SimpleUpsertData(
          actual = application.copy(notifications = Seq(notification)),
          previous = application.copy(notifications = Seq.empty).some
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.ADD_NOTIFICATION
          )
        )
      ),
      checkEventsForUpsert(
        "remove notification",
        SimpleUpsertData(
          actual = application.copy(notifications = Seq.empty),
          previous = application.copy(notifications = Seq(notification)).some
        ),
        expected = Set.empty
      ),
      checkEventsForUpsert(
        "replace notification",
        SimpleUpsertData(
          actual = application
            .copy(notifications = Seq(notification.copy(id = "foo".taggedWith[Tag.NotificationId]))),
          previous = application
            .copy(notifications = Seq(notification.copy(id = "bar".taggedWith[Tag.NotificationId])))
            .some
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.ADD_NOTIFICATION
          )
        )
      ),
      checkEventsForUpsert(
        "add product",
        AddProductsUpsertData(
          actual = application.copy(claims = Seq(claim.copy(state = ClaimState.DRAFT))),
          previous = application.copy(claims = Seq.empty).some,
          suitables = Seq.empty
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.ADD_CREDIT_PRODUCT
          )
        )
      ),
      checkEventsForUpsert(
        "add product unexpected unexpected state",
        AddProductsUpsertData(
          actual = application.copy(claims = Seq(claim.copy(state = ClaimState.APPROVED))),
          previous = application.copy(claims = Seq.empty).some,
          suitables = Seq.empty
        ),
        expected = Set.empty
      ),
      checkEventsForUpsert(
        "add product with delayed processing",
        AddProductsUpsertData(
          actual = application
            .copy(claims = Seq(claim.copy(state = ClaimState.DRAFT, processAfter = now.plusSeconds(10).some))),
          previous = application.copy(claims = Seq.empty).some,
          suitables = Seq.empty
        ),
        expected = Set.empty
      ),
      checkEventsForUpsert(
        "add product with expired delay",
        AddProductsUpsertData(
          actual = application
            .copy(claims = Seq(claim.copy(state = ClaimState.DRAFT, processAfter = now.minusSeconds(10).some))),
          previous = application.copy(claims = Seq.empty).some,
          suitables = Seq.empty
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.ADD_CREDIT_PRODUCT
          )
        )
      ),
      checkEventsForUpsert(
        "remove product",
        AddProductsUpsertData(
          actual = application.copy(claims = Seq.empty),
          previous = application.copy(claims = Seq(claim)).some,
          suitables = Seq.empty
        ),
        expected = Set.empty
      ),
      checkEventsForUpsert(
        "replace product",
        AddProductsUpsertData(
          actual = application.copy(claims =
            Seq(claim.copy(creditProductId = "foo".taggedWith[Tag.CreditProductId], state = ClaimState.DRAFT))
          ),
          previous =
            application.copy(claims = Seq(claim.copy(creditProductId = "bar".taggedWith[Tag.CreditProductId]))).some,
          suitables = Seq.empty
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.ADD_CREDIT_PRODUCT
          )
        )
      ),
      checkEventsForUpsert(
        "add multiple fields",
        SimpleUpsertData(
          actual = application.copy(
            borrowerPersonProfile = personProfile
              .copy(
                phones = Block.PhonesBlock(Seq(phoneEntity)).some,
                emails = Block.EmailsBlock(Seq(emailEntity)).some
              )
              .some,
            notifications = Seq(notification),
            claims = Seq(claim.copy(state = ClaimState.DRAFT))
          ),
          previous = application
            .copy(
              borrowerPersonProfile = personProfile
                .copy(
                  phones = none,
                  emails = none
                )
                .some,
              notifications = Seq.empty,
              claims = Seq.empty
            )
            .some
        ),
        expected = Set(
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.UPDATE
          ),
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.ADD_CREDIT_PRODUCT
          ),
          CreditApplicationProcessingEvent(
            application.id,
            now,
            EventType.ADD_NOTIFICATION
          )
        )
      )
    )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("eventsForUpsert")(
      eventsForUpsertTests: _*
    )
}
