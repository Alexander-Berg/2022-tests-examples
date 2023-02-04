package auto.dealers.balance_alerts.logic

import auto.common.clients.billing.model.{AutoruClientId, Requisite, RequisitesResponse}
import auto.common.clients.billing.testkit.BillingClientMock
import auto.common.clients.cabinet.model.{BalanceClient => CabinetBalanceClient, _}
import auto.common.clients.cabinet.testkit.CabinetTest
import common.clients.email.testkit.TestEmailSender
import common.scalapb.ScalaProtobuf.toTimestamp
import common.zio.sttp.model.SttpError.SttpHttpError
import ru.auto.api.response_model.{DealerAccountResponse, DealerOverdraft}
import ru.auto.cabinet.api_model._
import ru.auto.cabinet.api_model.ClientIdsResponse.ClientInfo
import ru.yandex.vertis.billing.billing_event.BillingOperation
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.{BillingDomain, TransactionInfo}
import ru.yandex.vertis.billing.billing_event.TransactionBillingInfo.OrderState
import ru.yandex.vertis.billing.model.CustomerId
import sttp.model.{MediaType, StatusCode}
import zio.{test => _}
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.mock.MockClock

import java.time.OffsetDateTime

object OverdraftProcessorLiveSpec extends DefaultRunnableSpec {
  val clientId: ClientId = 1L
  val balanceClientId: BalanceClientId = 2L
  val balancePersonId: BalancePersonId = 3L
  val balancePersonName: String = "Qwe"
  val balancePersonEmail: String = "qwe@asd.zxc"
  val cabinetBalanceClient: CabinetBalanceClient = CabinetBalanceClient(balancePersonId, None)
  val invoiceId = 123L
  val now: OffsetDateTime = OffsetDateTime.now()

  val env =
    MockClock.CurrentDateTime(value(now)).atMost(3) &&
      MockClock.Sleep(anything).atMost(3)

  val defaultBillingOperation: BillingOperation =
    BillingOperation.defaultInstance
      .withDomain(BillingDomain.AUTORU)
      .withTransactionInfo {
        TransactionInfo.defaultInstance
          .withType(TransactionInfo.TransactionType.WITHDRAW)
          .withCustomerId(CustomerId(0, balancePersonId, None))
      }

  val overdraftEnabledPoiProperties: PoiProperties =
    PoiProperties.defaultInstance
      .withProperties {
        Seq(
          PoiProperty("overdraft_enabled", "1"),
          PoiProperty("overdraft_balance_person_id", balancePersonId.toString)
        )
      }

  def defaultMocks(averageOutcome: Int, limit: Long, spent: Long) =
    CabinetTest.GetClientByBalanceIds(
      equalTo(Set(cabinetBalanceClient)),
      value(Seq(ClientInfo.defaultInstance.withClientId(clientId)))
    ) ++
      CabinetTest.GetClientPoiProperties(
        equalTo(clientId),
        value(overdraftEnabledPoiProperties)
      ) ++
      CabinetTest.GetInvoiceRequests(
        equalTo(clientId),
        value {
          InvoiceRequestsResponse.defaultInstance
            .withInvoiceRequests(
              Seq(
                InvoiceRequestsResponse.BalanceRequest.defaultInstance
                  .withCreateDate(toTimestamp(now.minusHours(25)))
              )
            )
        }
      ) ++
      CabinetTest.GetClientAccount(
        equalTo(clientId),
        value {
          DealerAccountResponse.defaultInstance
            .withAverageOutcome(averageOutcome)
            .withOverdraft {
              DealerOverdraft.defaultInstance
                .withAllowed(true)
                .withLimit(limit)
                .withSpent(100L)
            }
        }
      )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("OverdraftProcessorLive")(
      testM("should successfully create invoice") {
        val billingOperation = defaultBillingOperation
          .withOrderState(OrderState.defaultInstance.withBalance(99L))

        val invoiceId = 123L
        val limit = 1000L
        val spent = 100L

        val subscriptionCategory = "money"
        val email = "email"

        val mocks =
          defaultMocks(averageOutcome = 100, limit, spent) ++
            CabinetTest.CreateInvoice(
              equalTo((clientId, balancePersonId, limit - spent, InvoiceType.Overdraft)),
              value(Invoice(invoiceId, MediaType.ApplicationPdf, "", ""))
            ) ++
            CabinetTest.GetDetailedClient(
              equalTo(clientId),
              value {
                DetailedClient.defaultInstance
                  .withName("")
                  .withProperties {
                    ClientProperties.defaultInstance.withOriginId("")
                  }
              }
            ) ++
            BillingClientMock.GetRequisites(
              equalTo(AutoruClientId(clientId)),
              value {
                RequisitesResponse(
                  List(
                    Requisite(
                      auto.common.clients.billing.model.BalancePersonId(balancePersonId),
                      Requisite.Properties(Requisite.Property.Juridical("Тачки"))
                    )
                  )
                )
              }
            ) ++
            CabinetTest.GetClientSubscriptionsByCategory(
              equalTo((clientId, subscriptionCategory)),
              value(Seq(ClientSubscription(clientId, subscriptionCategory, email)))
            )

        (for {
          _ <- assertM(OverdraftProcessor.process(billingOperation))(isUnit)
          emails <- TestEmailSender.allSent
        } yield assertTrue(emails.headOption.map(_.toEmail).contains(email)))
          .provideCustomLayer(TestEmailSender.layer ++ (env && mocks) >+> OverdraftProcessor.live)
      },
      testM("should skip operation if there's enough balance for single day") {
        val billingOperation = defaultBillingOperation
          .withOrderState(OrderState.defaultInstance.withBalance(10000L))

        val mocks = defaultMocks(averageOutcome = 100, limit = 1000, spent = 0)

        assertM(OverdraftProcessor.process(billingOperation))(isUnit)
          .provideLayer(BillingClientMock.empty ++ TestEmailSender.layer ++ (env && mocks) >>> OverdraftProcessor.live)
      },
      testM("should skip operation if overdraft is not available") {
        val billingOperation = defaultBillingOperation
          .withOrderState(OrderState.defaultInstance.withBalance(100L))

        val mocks = defaultMocks(averageOutcome = 100, limit = 0, spent = 0)

        assertM(OverdraftProcessor.process(billingOperation))(isUnit)
          .provideLayer(BillingClientMock.empty ++ TestEmailSender.layer ++ (env && mocks) >>> OverdraftProcessor.live)
      },
      testM("should skip operation if available overdraft is less than average outcome") {
        val billingOperation = defaultBillingOperation
          .withOrderState(OrderState.defaultInstance.withBalance(100L))

        val mocks = defaultMocks(averageOutcome = 100, limit = 99, spent = 0)

        assertM(OverdraftProcessor.process(billingOperation))(isUnit)
          .provideLayer(BillingClientMock.empty ++ TestEmailSender.layer ++ (env && mocks) >>> OverdraftProcessor.live)
      },
      testM("should skip operation if client doesn't have overdraft enabled") {
        val billingOperation = defaultBillingOperation
          .withOrderState(OrderState.defaultInstance.withBalance(100L))

        val mocks = {
          CabinetTest.GetClientByBalanceIds(
            equalTo(Set(cabinetBalanceClient)),
            value(Seq(ClientInfo.defaultInstance.withClientId(clientId)))
          ) ++
            CabinetTest.GetClientPoiProperties(
              equalTo(clientId),
              value(PoiProperties.defaultInstance)
            )
        }

        assertM(OverdraftProcessor.process(billingOperation))(isUnit)
          .provideLayer(
            MockClock.empty ++
              BillingClientMock.empty ++
              TestEmailSender.layer ++
              mocks >>>
              OverdraftProcessor.live
          )
      },
      testM("should skip operation if client has an invoice request within a day") {
        val billingOperation = defaultBillingOperation
          .withOrderState(OrderState.defaultInstance.withBalance(100L))

        val mocks =
          CabinetTest.GetClientByBalanceIds(
            equalTo(Set(cabinetBalanceClient)),
            value(Seq(ClientInfo.defaultInstance.withClientId(clientId)))
          ) ++
            CabinetTest.GetClientPoiProperties(
              equalTo(clientId),
              value(overdraftEnabledPoiProperties)
            ) ++
            MockClock.CurrentDateTime(value(now)) ++
            CabinetTest.GetInvoiceRequests(
              equalTo(clientId),
              value {
                InvoiceRequestsResponse.defaultInstance
                  .withInvoiceRequests(
                    Seq(
                      InvoiceRequestsResponse.BalanceRequest.defaultInstance
                        .withCreateDate(toTimestamp(now))
                    )
                  )
              }
            )

        assertM(OverdraftProcessor.process(billingOperation))(isUnit)
          .provideLayer(BillingClientMock.empty ++ TestEmailSender.layer ++ mocks >>> OverdraftProcessor.live)
      },
      suite("on createInvoice errors")(
        testM("should retry general errors") {
          val billingOperation = defaultBillingOperation
            .withOrderState(OrderState.defaultInstance.withBalance(99L))

          val invoiceId = 123L
          val limit = 1000L
          val spent = 100L

          val subscriptionCategory = "money"
          val email = "email"

          val mocks =
            defaultMocks(averageOutcome = 100, limit, spent) ++
              CabinetTest
                .CreateInvoice(
                  equalTo((clientId, balancePersonId, limit - spent, InvoiceType.Overdraft)),
                  failure(CabinetException("test CabinetException", new Exception("any kind of internal exception")))
                )
                .twice
                .andThen(
                  CabinetTest.CreateInvoice(
                    equalTo((clientId, balancePersonId, limit - spent, InvoiceType.Overdraft)),
                    value(Invoice(invoiceId, MediaType.ApplicationPdf, "", ""))
                  )
                ) ++
              CabinetTest.GetDetailedClient(
                equalTo(clientId),
                value {
                  DetailedClient.defaultInstance
                    .withName("")
                    .withProperties {
                      ClientProperties.defaultInstance.withOriginId("")
                    }
                }
              ) ++
              BillingClientMock.GetRequisites(
                equalTo(AutoruClientId(clientId)),
                value {
                  RequisitesResponse(
                    List(
                      Requisite(
                        auto.common.clients.billing.model.BalancePersonId(balancePersonId),
                        Requisite.Properties(Requisite.Property.Juridical("Тачки"))
                      )
                    )
                  )
                }
              ) ++
              CabinetTest.GetClientSubscriptionsByCategory(
                equalTo((clientId, subscriptionCategory)),
                value(Seq(ClientSubscription(clientId, subscriptionCategory, email)))
              )

          (for {
            _ <- assertM(OverdraftProcessor.process(billingOperation))(isUnit)
            emails <- TestEmailSender.allSent
          } yield assertTrue(emails.headOption.map(_.toEmail).contains(email)))
            .provideCustomLayer(
              TestEmailSender.layer ++
                (env && mocks) >+>
                OverdraftProcessor.live
            )
        },
        testM("should not retry on NOT_ENOUGH_OVERDRAFT_LIMIT error") {
          val billingOperation = defaultBillingOperation
            .withOrderState(OrderState.defaultInstance.withBalance(99L))

          val limit = 1000L
          val spent = 100L

          val mocks =
            defaultMocks(averageOutcome = 100, limit, spent) ++
              CabinetTest
                .CreateInvoice(
                  equalTo((clientId, balancePersonId, limit - spent, InvoiceType.Overdraft)),
                  failure(
                    CabinetException(
                      "test overdraft CabinetException",
                      SttpHttpError(
                        "test_service",
                        "test_name",
                        "test_uri",
                        StatusCode.InternalServerError,
                        """{
                        |"errorCode":"ru.auto.cabinet.remote.http.RpcFault",
                        |"message":"-1 NOT_ENOUGH_OVERDRAFT_LIMIT Client has no enough overdraft limit
                        | 38500.00RUB to proceed with this invoice sum 38600.000000RUB
                        |Client has no enough overdraft limit 38500.00RUB to proceed with this invoice sum 38600.000000RUB"
                        |}""".stripMargin
                      )
                    )
                  )
                )

          assertM(OverdraftProcessor.process(billingOperation))(isUnit)
            .provideLayer(
              BillingClientMock.empty ++
                TestEmailSender.layer ++
                (env && mocks) >>>
                OverdraftProcessor.live
            )
        }
      )
    )
}
