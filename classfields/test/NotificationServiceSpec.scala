package auto.dealers.balance_alerts.logic

import auto.common.clients.cabinet.Cabinet.Cabinet
import auto.common.clients.cabinet.model.ClientSubscription
import auto.common.clients.cabinet.testkit.CabinetTest
import auto.dealers.balance_alerts.logic.NotificationService.{NoSubscriptions, TemplateServiceError}
import auto.dealers.balance_alerts.logic.email.SenderTemplateService
import auto.dealers.balance_alerts.model.{BalanceAlert, BalanceEventType, DealerId}
import common.clients.email.EmailSender.EmailSender
import common.clients.email.model.EmailSenderTemplate
import common.clients.email.testkit.EmailSenderTest
import common.zio.logging.Logging
import common.zio.sttp.model.SttpError
import common.zio.testkit.failsWith
import io.circe.{Json, JsonObject}
import ru.auto.api.response_model.DealerAccountResponse
import ru.auto.cabinet.api_model.DetailedClient
import zio.clock.Clock
import zio.{Has, ULayer, ZIO, ZLayer}
import zio.test.Assertion.{anything, equalTo, isUnit}
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.OffsetDateTime
import java.util.UUID

object NotificationServiceSpec extends DefaultRunnableSpec {

  private val config = NotificationService.Config(maxNotifications = 5)

  private val testDealerId = 14L
  private val dealerWithoutNotifications = 56L
  private val dealerWithoutSubscriptions = 100L
  private val dealerWithCabinetError = 200L

  private val emailToSendNotification = "some@test.email"
  private val testDealerName = "test dealer"

  private def createTestEnv(
      cabinetEnv: ULayer[Cabinet] = CabinetTest.empty,
      emailSenderEnv: ULayer[EmailSender] = EmailSenderTest.empty): ZLayer[Any, Nothing, Has[NotificationService]] =
    (Logging.live ++
      Clock.live ++
      ZLayer.succeed(config) ++
      cabinetEnv ++
      emailSenderEnv) >+> SenderTemplateService.layer >>> NotificationService.layer

  private def createAlertForNotification(dealerId: DealerId): BalanceAlert =
    BalanceAlert(
      id = UUID.randomUUID(),
      dealerId = dealerId,
      balanceEventType = BalanceEventType.OneDayLeft,
      timestamp = OffsetDateTime.now(),
      lastNotified = None,
      notificationsCount = 0
    )

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("NotificationService")(
      testM("create notification when should") {
        val alertForNotification = createAlertForNotification(testDealerId)

        val cabinetMock =
          CabinetTest
            .GetClientSubscriptionsByCategory(
              equalTo((testDealerId, "money")),
              value(Seq(ClientSubscription(testDealerId, "money", emailToSendNotification)))
            ) ++
            CabinetTest
              .GetDetailedClient(
                equalTo(testDealerId),
                value(DetailedClient(id = testDealerId, name = testDealerName))
              ) ++
            CabinetTest
              .GetClientAccount(
                equalTo(testDealerId),
                value(
                  DealerAccountResponse(
                    accountId = testDealerId,
                    balance = 5678L,
                    balanceClientId = 100L,
                    averageOutcome = 0,
                    restDays = 1
                  )
                )
              )

        val emailSenderMock =
          EmailSenderTest
            .SendEmail(
              equalTo(
                (
                  EmailSenderTemplate(
                    "cabinet.money_runout",
                    JsonObject.fromMap(
                      Map(
                        "dealer_name" -> testDealerName,
                        "origin" -> "",
                        "money" -> "5678",
                        "days" -> "1 день"
                      ).map { case (key, strValue) => key -> Json.fromString(strValue) }
                    )
                  ),
                  emailToSendNotification
                )
              ),
              value(())
            )

        val testEnv = createTestEnv(cabinetMock, emailSenderMock)
        assertM(NotificationService(_.notify(alertForNotification)))(isUnit).provideLayer(testEnv)
      },
      testM("doesn't create notification when shouldn't") {
        val alertNotForNotification = BalanceAlert(
          id = UUID.randomUUID(),
          dealerId = dealerWithoutNotifications,
          balanceEventType = BalanceEventType.Ok,
          timestamp = OffsetDateTime.now(),
          lastNotified = None,
          notificationsCount = 10
        )

        assertM(NotificationService(_.notify(alertNotForNotification)))(isUnit).provideLayer(createTestEnv())
      },
      testM("fails when there are no subscriptions") {
        val alertForNotification = createAlertForNotification(dealerWithoutSubscriptions)

        val cabinetMock =
          CabinetTest
            .GetClientSubscriptionsByCategory(
              equalTo((dealerWithoutSubscriptions, "money")),
              value(Nil)
            )

        val testEnv = createTestEnv(cabinetEnv = cabinetMock)
        assertM(NotificationService(_.notify(alertForNotification)).run)(failsWith[NoSubscriptions])
          .provideLayer(testEnv)
      },
      testM("fails if there are errors in template creation") {
        val alertForNotification = createAlertForNotification(dealerWithCabinetError)

        val cabinetMock =
          CabinetTest
            .GetClientSubscriptionsByCategory(
              equalTo((dealerWithCabinetError, "money")),
              value(Seq(ClientSubscription(dealerWithCabinetError, "money", emailToSendNotification)))
            ) ++
            CabinetTest
              .GetDetailedClient(
                equalTo(dealerWithCabinetError),
                failure(SttpError.SttpUnknownError("test", "dummy", "http://localhost", new Exception("some msg")))
              )

        val testEnv = createTestEnv(cabinetEnv = cabinetMock)
        assertM(NotificationService(_.notify(alertForNotification)).run)(failsWith[TemplateServiceError])
          .provideLayer(testEnv)
      }
    )
  }
}
