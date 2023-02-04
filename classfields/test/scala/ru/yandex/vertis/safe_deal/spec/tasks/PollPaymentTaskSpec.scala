package ru.yandex.vertis.safe_deal.spec.tasks

import cats.syntax.option._
import com.softwaremill.quicklens._
import com.softwaremill.tagging._
import ru.yandex.common.tokenization.NonBlockingTokensFilter
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.safe_deal.client.bank.model._
import ru.yandex.vertis.safe_deal.client.bank.{TinkoffClient, TinkoffClientImpl}
import ru.yandex.vertis.safe_deal.config.TinkoffClientConfig
import ru.yandex.vertis.safe_deal.controller.DealOperationController
import ru.yandex.vertis.safe_deal.dao.OperationDao
import ru.yandex.vertis.safe_deal.model.Arbitraries.{DealArb, OperationArb}
import ru.yandex.vertis.safe_deal.model._
import ru.yandex.vertis.safe_deal.model.ParsedPayment.ParsedOperation
import ru.yandex.vertis.safe_deal.tasks.PaymentStage
import ru.yandex.vertis.safe_deal.tasks.impl.PollPaymentsTaskImpl
import ru.yandex.vertis.zio_baker.util.EmptyString
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig, SslContextConfig}
import ru.yandex.vertis.zio_baker.zio.token_distributor.TokenDistributor
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Task, UIO, ZLayer}

import java.time.LocalDate
import scala.language.reflectiveCalls
import scala.util.matching.Regex

object PollPaymentTaskSpec extends DefaultRunnableSpec {

  private val password: String = ""
  private val token: String = ""

  private val purposeRegex: Regex = """[N№\#]\s*(БС|)\s*([\d]+)""".r

  private lazy val tokenDistributor = new TokenDistributor.Service {
    def tokenCount: Int = 1

    override def getTokens: UIO[TokenDistributor.Tokens] =
      UIO(TokenDistributor.Tokens(1, Set(0)))

    def tokensFilter: UIO[NonBlockingTokensFilter] = ???
  }

  private lazy val op = OperationArb.arbitrary.sample.get

  private val invalidOp =
    OperationArb.arbitrary.sample.get.modify(_.paymentPurpose).setTo(EmptyString.taggedWith[Tag.PaymentPurpose])

  private val parsedOp = ParsedOperation(op, op.dealNumber(purposeRegex), Domain.DOMAIN_AUTO, false, None)

  private val parsedInvalidOp =
    ParsedOperation(invalidOp, invalidOp.dealNumber(purposeRegex), Domain.DOMAIN_AUTO, false, None)

  private val deal = DealArb.arbitrary.sample.get.modify(_.dealNumber).setTo(parsedOp.dealNumber.get)

  private lazy val tinkoffClient = new TinkoffClient.Service {

    def getOperations(from: LocalDate, to: LocalDate): Task[List[Operation]] =
      Task(List(op, invalidOp))

    def runPersonCheck(
        firstName: FirstName,
        middleName: Option[MiddleName],
        lastName: LastName,
        phone: Phone,
        passport: Passport,
        email: Option[Email]): Task[CorrelationId] = ???

    def personCheckStatus(correlationId: CorrelationId): Task[Option[CheckResult]] = ???
  }

  private def dealOperationController(deals: Map[Long, Deal], ops: Map[String, ParsedOperation]) =
    new DealOperationController {
      var _deals = deals
      var _ops = ops

      override def getOperation(operationId: BankOperationId): Task[Option[ParsedOperation]] = ???

      override def listOperations(filter: OperationDao.ListFilter): Task[Seq[ParsedOperation]] = ???

      override def linkOperation(operationId: BankOperationId, dealId: DealId): Task[Option[Deal]] = ???

      override def unlinkOperation(operationId: BankOperationId, dealId: DealId): Task[Option[Deal]] = ???

      override def update(
          dealNumber: Option[DealNumber],
          operationId: BankOperationId
        )(f: (
              Option[Deal],
              Option[ParsedOperation]
          ) => Task[(Option[Deal], Option[ParsedOperation])]): Task[Unit] = {
        val deal = _deals.get(dealNumber.getOrElse(-1L))
        val operation = _ops.get(operationId)
        f(deal, operation).map { case (nextDeal, nextOp) =>
          nextDeal.foreach { d =>
            _deals = _deals + (d.dealNumber -> d)
          }
          nextOp.foreach { op =>
            _ops = _ops + (op.operation.operationId -> op)
          }
        }
      }
    }

  def spec: ZSpec[TestEnvironment, Any] =
    suite("PollPaymentTask")(
      testM("processors when there are no deal and no operation") {
        val expectedOps = Map(
          parsedOp.operation.operationId.toString -> parsedOp,
          parsedInvalidOp.operation.operationId.toString -> parsedInvalidOp
        )
        val dc = dealOperationController(Map.empty, Map.empty)
        val ppt = new PollPaymentsTaskImpl(
          tinkoffClient,
          tokenDistributor,
          dc,
          purposeRegex,
          List(
            new PaymentStage {
              override protected def name: String = "1"

              override def process(
                  payment: ParsedPayment,
                  deal: Option[Deal]
                )(prevOperation: Option[ParsedPayment]): Task[(Option[Deal], Option[ParsedPayment])] =
                Task(deal.map(_.modify(_.totalProvidedRub).using(_ - 1)) -> payment.some)
            }
          )
        )

        ppt.process().as {
          assert(dc._ops)(equalTo(expectedOps))
        }
      }, // @@ TestAspect.ignore,
      testM("processors when there are deal and operation") {
        val initialOps = Map(parsedOp.operation.operationId.toString -> parsedOp)

        val dc = dealOperationController(Map(deal.dealNumber -> deal), initialOps)
        val ppt = new PollPaymentsTaskImpl(
          tinkoffClient,
          tokenDistributor,
          dc,
          purposeRegex,
          List(
            new PaymentStage {
              override protected def name: String = "1"

              override def process(
                  payment: ParsedPayment,
                  deal: Option[Deal]
                )(prevOperation: Option[ParsedPayment]): Task[(Option[Deal], Option[ParsedPayment])] =
                Task(deal.map(_.modify(_.totalProvidedRub).using(_ - 1)) -> None)
            },
            new PaymentStage {
              override protected def name: String = "2"

              override def process(
                  payment: ParsedPayment,
                  deal: Option[Deal]
                )(prevOperation: Option[ParsedPayment]): Task[(Option[Deal], Option[ParsedPayment])] =
                Task(None -> None)
            }
          )
        )

        ppt.process().as {
          assert(dc._deals(deal.dealNumber).totalProvidedRub)(equalTo(deal.totalProvidedRub - 1)) &&
          assert(dc._ops)(equalTo(initialOps))
        }
      }, // @@ TestAspect.ignore,
      testM("check") {
        val httpConf = HttpClientConfig(url = "https://business.tinkoff.ru/openapi")
        val secureHttpConf = HttpClientConfig(
          url = "https://secured-openapi.business.tinkoff.ru",
          sslContextConfig = SslContextConfig(
            EmptyString,
            password
          ).some,
          proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128).some
        )
        val httpClient = ZLayer.requires[Blocking] ++ ZLayer.succeed(secureHttpConf) >>> HttpClient.blockingLayer
        httpClient.build
          .zip(Blocking.live.build)
          .use { case (backend, _) =>
            val config = TinkoffClientConfig(
              httpConf,
              secureHttpConf,
              token,
              "40702810110000850984",
              EmptyString,
              EmptyString,
              EmptyString
            )

            val tc = new TinkoffClientImpl(config)(backend.get)
            // val tsc = new TinkoffSecureClientImpl(config)(backend.get)
            // val ntd = new NotificationTemplateDictionaryMock()

            val ppt =
              new PollPaymentsTaskImpl(
                tc,
                tokenDistributor,
                new DealOperationController {
                  override def getOperation(operationId: BankOperationId): Task[Option[ParsedOperation]] =
                    ???

                  override def listOperations(filter: OperationDao.ListFilter): Task[Seq[ParsedOperation]] = ???

                  override def linkOperation(operationId: BankOperationId, dealId: DealId): Task[Option[Deal]] = ???

                  override def unlinkOperation(operationId: BankOperationId, dealId: DealId): Task[Option[Deal]] = ???

                  def update(
                      dealNumber: Option[DealNumber],
                      operationId: BankOperationId
                    )(f: (
                          Option[Deal],
                          Option[ParsedOperation]
                      ) => Task[(Option[Deal], Option[ParsedOperation])]): Task[Unit] = ???
                },
                purposeRegex,
                List.empty
              )

            ppt.process().as(assert(1)(equalTo(1)))
//              tsc
//                .makePayment(
//                  "c802f88f-a557-4a87-b530-ae6dfdec5727-internal-2",
//                  "40702810610000850756".taggedWith[Tag.AccountNumber],
//                  Receiver(
//                    "ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \"ЯНДЕКС.ВЕРТИКАЛИ\"",
//                    "7704340327".taggedWith[Tag.Inn],
//                    "044525974".taggedWith[Tag.Bic],
//                    "АО \"ТИНЬКОФФ БАНК\"".taggedWith[Tag.BankName],
//                    "30101810145250000974".taggedWith[Tag.CorrAccountNumber],
//                    "40702810110000850984".taggedWith[Tag.AccountNumber]
//                  ),
//                  "Комиссия за услуги ООО \"ЯНДЕКС.ВЕРТИКАЛИ\". НДС не облагается".taggedWith[Tag.PaymentPurpose],
//                  BigDecimal(60).taggedWith[Tag.Amount],
//                  None
//                )
//                .as(assert(1)(equalTo(1)))

//              Каменский Сергей Михайлович
//              ПАО СБЕРБАНК
//                счёт 40817810938050086274
//              кор счёт 30101810400000000225
//              бик 044525225
//              tsc
//                .makePayment(
//                  "50711368827_0-return",
//                  "40702810110000850984".taggedWith[Tag.AccountNumber],
//                  Receiver(
//                    "АХМАДУЛЛИНА НАТАЛЬЯ СЕРГЕЕВНА",
//                    "0".taggedWith[Tag.Inn],
//                    "044525974".taggedWith[Tag.Bic],
//                    "АО «Тинькофф Банк»".taggedWith[Tag.BankName],
//                    "30101810145250000974".taggedWith[Tag.CorrAccountNumber],
//                    "40817810000004382673".taggedWith[Tag.AccountNumber]
//                  ),
//                  "Возврт стоимости автомобиля по договору купли-продажи №БС39970 от 18.11.2021. НДС не облагается"
//                    .taggedWith[Tag.PaymentPurpose],
//                  BigDecimal(0).taggedWith[Tag.Amount],
//                  None
//                )
//                .as(assert(1)(equalTo(1)))
//              tsc
//                .makePaymentToTinkoffCustomer(
//                  "51966995211_0-return",
//                  "40702810110000850984".taggedWith[Tag.AccountNumber],
//                  "0166347294".taggedWith[Tag.TinkoffAgreementNumber],
//                  "Возврат стоимости автомобиля по договору купли-продажи №БС50492 от 19.12.2021. НДС не облагается"
//                    .taggedWith[Tag.PaymentPurpose],
//                  BigDecimal(0).taggedWith[Tag.Amount]
//                )
//                .as(assert(1)(equalTo(1)))
//              tsc
//                .paymentStatus("51966995211_0-return")
//                .map(println)
//                .as(assert(1)(equalTo(1)))
          }
          .provideLayer(Blocking.live ++ Clock.live)
      } @@ TestAspect.ignore
    )
}
