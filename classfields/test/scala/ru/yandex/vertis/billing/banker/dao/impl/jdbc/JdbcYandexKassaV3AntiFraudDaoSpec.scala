package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.{PaymentRequestRecord, StateRecord}
import ru.yandex.vertis.billing.banker.dao.{
  AccountDao,
  PaymentSystemDao,
  YandexKassaV3AntiFraudDao,
  YandexKassaV3PaymentRequestExternalDao
}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{EmptyForm, UrlForm}
import ru.yandex.vertis.billing.banker.model.Raw.RawText
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens.{
  AccountPropertiesGen,
  CentsGen,
  PayloadGen,
  PaymentRequestOptionsGen,
  Producer,
  TimestampGen
}
import ru.yandex.vertis.external.yandexkassa.ApiModel.PaymentSource

/**
 * @author vitya-smirnov
 */
class JdbcYandexKassaV3AntiFraudDaoSpec extends AnyWordSpec with Matchers with AsyncSpecBase with JdbcSpecTemplate {

  private val accountDao: AccountDao =
    new JdbcAccountDao(database)

  private val paymentSystemDao: PaymentSystemDao =
    new JdbcPaymentSystemDao(database, PaymentSystemIds.YandexKassaV3)

  private val externalPaymentRequestDao: YandexKassaV3PaymentRequestExternalDao =
    new JdbcYandexKassaV3PaymentRequestExternalDao(database)

  private val antiFraudDao: YandexKassaV3AntiFraudDao =
    new JdbcYandexKassaV3AntiFraudDao(database)

  override def beforeAll(): Unit = {
    initialAccounts.map(accountDao.upsert).foreach(_.futureValue)
    initialPaymentRequests.map(paymentSystemDao.insertRequest).foreach(_.futureValue)
    initialExternalPayments.map(externalPaymentRequestDao.upsert).foreach(_.futureValue)
    initialExternalPayments.map(p =>
      externalPaymentRequestDao.addPaymentId(
        YandexKassaV3PaymentRequestExternalDao.ForInternalId(p.prId),
        p.pId.getOrElse("awawawaa")
      )
    )
    initialPayments.map(paymentSystemDao.upsert).foreach(_.futureValue)
  }

  private def accounts = List(
    "user:00000000",
    "user:11111111",
    "user:22222222",
    "user:33333333",
    "user:44444444",
    "user:55555555"
  )

  private def paymentRequests = List(
    // n      user-id       payload    bank form
    ("0", "user:44444444", "payload", "sberbank"),
    ("1", "user:22222222", null, "bank-spb"),
    ("2", "user:11111111", "payload", "test-bank"),
    ("3", "user:00000000", null, null),
    ("4", "user:33333333", "payload", null),
    ("5", "user:11111111", "payload", null),
    ("6", "user:11111111", null, null),
    ("7", "user:11111111", "payload", "some-bank"),
    ("8", "user:22222222", null, "bank-spb")
  )

  def provide: AfterWord = afterWord("provide")
  def returns: AfterWord = afterWord("returns")
  def argumentsAre: AfterWord = afterWord("you pass")

  "YandexKassaV3AntiFraudDao" should provide {
    "a getPaymentsInfo(externalPaymentRequestIds: String...) method".which(returns {
      "an empty list" when argumentsAre {
        "no external request ids" in {
          antiFraudDao.getPaymentReports(Set()).futureValue.shouldBe(empty)
        }
        "only non-exist ids" in {
          antiFraudDao.getPaymentReports(Set("noname")).futureValue.shouldBe(empty)
        }
      }
      "a singleton list with found payment information " when argumentsAre {
        "such that there are only one existing external payment request Id" in {
          val payments = antiFraudDao
            .getPaymentReports(
              Set(
                "external-request0-00000000-88",
                "noname",
                "ababa"
              )
            )
            .futureValue
          payments.size.shouldBe(1)
          val its = payments.head
          its.externalPaymentRequestId.shouldBe("external-request0-00000000-88")
          its.paymentRequestId.shouldBe("request0-00000000-88")
          its.account.user.shouldBe("user:44444444")
          its.isTiedCard.shouldBe(false)
          its.payload.shouldBe(a[Payload.Text])
        }
        "only existing external payment request id" in {
          val payments = antiFraudDao.getPaymentReports(Set("external-request3-33333333-88")).futureValue
          payments.size.shouldBe(1)
          val its = payments.head
          its.externalPaymentRequestId.shouldBe("external-request3-33333333-88")
          its.paymentRequestId.shouldBe("request3-33333333-88")
          its.account.user.shouldBe("user:00000000")
          its.isTiedCard.shouldBe(true)
          its.payload.shouldBe(Payload.Empty)
        }
      }
      "payments info list with cardinality equal to arguments size" when argumentsAre {
        "all external payment request ids exist without duplicates" in {
          val givenExternalIds = Set("external-request0-00000000-88", "external-request2-22222222-88")
          val payments = antiFraudDao.getPaymentReports(givenExternalIds).futureValue
          payments.size.shouldBe(2)
          payments.map(_.externalPaymentRequestId) should contain theSameElementsAs givenExternalIds
        }
      }
      "less payment infos than arguments count" when argumentsAre {
        "such that there are at least one non-existing id" in {
          antiFraudDao
            .getPaymentReports(
              Set(
                "external-request0-00000000-88",
                "external-request2-22222222-88",
                "noname",
                "boris",
                "krasava",
                "external-request3-33333333-88"
              )
            )
            .futureValue
            .size
            .shouldBe(3)
        }
      }
    })
  }

  private lazy val initialAccounts: List[Account] =
    accounts.map(id => Account(id, id, properties = AccountPropertiesGen.next))

  private lazy val initialPaymentRequests: List[PaymentRequestRecord] =
    paymentRequests.map { case (number, account, payload, form) =>
      PaymentRequestRecord(
        id = s"request$number-${number * 8}-88",
        method = "sberbank",
        source = PaymentRequest.Source(
          account = account,
          amount = CentsGen.next,
          payload =
            if (payload eq null) Payload.Empty
            else Payload.Text(payload),
          options = PaymentRequestOptionsGen.next,
          receipt = None,
          context = None,
          payGateContext = None
        ),
        form =
          if (form eq null) EmptyForm(s"request$number-${number * 8}-88")
          else UrlForm(s"request$number-${number * 8}-88", s"$form-form.ru", Some(PayloadGen.next.toString))
      )
    }

  private lazy val initialExternalPayments: List[YandexKassaV3PaymentRequestExternalDao.Record] =
    initialPaymentRequests.map(request =>
      new YandexKassaV3PaymentRequestExternalDao.Record(
        prId = request.id,
        pId = Some(s"external-${request.id}"),
        externalId = "external-id",
        source = PaymentSource.getDefaultInstance
      )
    )

  private lazy val initialPayments: List[StateRecord] =
    initialPaymentRequests.map(request =>
      StateRecord(
        State.Incoming(
          id = request.id,
          account = request.source.account,
          amount = request.source.amount,
          timestamp = TimestampGen.next,
          rawData = RawText(request.toString)
        )
      )
    )
}
