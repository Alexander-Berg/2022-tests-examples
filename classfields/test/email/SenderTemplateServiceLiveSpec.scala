package auto.dealers.balance_alerts.logic.email

import auto.common.clients.cabinet.testkit.CabinetTest
import auto.dealers.balance_alerts.model.{BalanceAlert, BalanceEventType}
import common.clients.email.model.EmailSenderTemplate
import io.circe.JsonObject
import io.circe.syntax.EncoderOps
import ru.auto.api.response_model.DealerAccountResponse
import ru.auto.cabinet.api_model.DetailedClient
import zio.{test => _}
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation._

import java.time.OffsetDateTime

object SenderTemplateServiceLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("SenderTemplateServiceLive")(
      testM("should create correct email template") {
        val clientId = 1L
        val dealerName = "Dealer"
        val balance = 100

        val env = {
          CabinetTest.GetDetailedClient(
            equalTo(clientId),
            value(DetailedClient.defaultInstance.withName(dealerName))
          ) ++
            CabinetTest.GetClientAccount(
              equalTo(clientId),
              value(DealerAccountResponse.defaultInstance.withBalance(balance))
            )
        }

        val balanceAlert = BalanceAlert(
          java.util.UUID.randomUUID(),
          clientId,
          BalanceEventType.OneDayLeft,
          timestamp = OffsetDateTime.now,
          lastNotified = None,
          notificationsCount = 0
        )

        val expectedVariables = Map(
          "dealer_name" -> dealerName.asJson,
          "origin" -> "".asJson,
          "money" -> balance.toString.asJson,
          "days" -> "1 день".asJson
        )

        assertM(SenderTemplateService(_.getBalanceAlertTemplate(balanceAlert)))(
          equalTo(EmailSenderTemplate("cabinet.money_runout", JsonObject.fromMap(expectedVariables)))
        )
          .provideLayer(env >>> SenderTemplateService.layer)
      }
    )
}
