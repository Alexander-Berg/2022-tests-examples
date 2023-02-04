package auto.dealers.amoyak.model

import _root_.common.models.finance.Money.Kopecks
import _root_.common.zio.clock.MoscowClock
import auto.dealers.amoyak.model.AmoCrmPatch._
import auto.dealers.amoyak.model.Tariff._
import auto.dealers.amoyak.model.Vas._
import auto.dealers.amoyak.model.blocks._
import auto.dealers.amoyak.model.model.defaultAmoDateTimeFormatter
import cats.syntax.option._
import io.circe._
import io.circe.parser.parse
import zio.test.Assertion._
import zio.test._

import java.time.ZonedDateTime

object ModelJsonRendererSpec extends DefaultRunnableSpec {

  val defaultTimestamp =
    ZonedDateTime
      .of(2020, 1, 1, 0, 0, 0, 0, MoscowClock.timeZone)

  val financePatchFormat = test("check finance patch formatting") {
    val spec = minifyJson(
      """{
         "client_id":"agency-1",
         "finance": {
           "balance": 10000,
           "balance_timestamp": "2020-01-01 00:00:00",
           "average_outcome": 0
         },
        "event": {
          "event_type": "sync",
          "event_name": "client_update"
        },
        "timestamp":"2020-01-01 00:00:00"
       }"""
    )
    val json = FinancePatch(
      clientId = 1L,
      isAgency = true,
      balance = Kopecks(1000000),
      stateEpoch = defaultTimestamp,
      averageOutcome = None,
      timestamp = defaultTimestamp
    ).toJsonString
    assert(json)(equalTo(spec))
  }

  val cabinetClientPatchFormat = test("check cabinet client patch formatting") {
    val spec = minifyJson(
      """{
         "client_id": "client-1",
         "main": {
           "origin": "test origin",
           "name": null,
           "agency": "agency-13",
           "head_company": null,
           "responsible_manager_email": null,
           "status": "new",
           "region": null
         },
         "moderation": {
           "first_moderation": false
         },
         "timestamp": "2020-01-01 00:00:00",
         "event": {
           "event_type": "sync",
           "event_name": "client_update"
         }
      }"""
    )
    val json = CabinetClientPatch(
      clientId = 1L,
      clientType = ClientType.Client,
      origin = "test origin",
      name = None,
      agency = 13L.some,
      headCompany = None,
      responsibleManagerEmail = None,
      status = "new",
      region = None,
      firstModeration = false,
      timestamp = defaultTimestamp
    ).toJsonString
    assert(json)(equalTo(spec))
  }

  val cabinetCompanyPatchFormat = test("check cabinet company patch formatting") {
    val spec = minifyJson(
      """{
         "client_id": "company-1",
         "main": {
           "office7_url": "https://office7.auto.ru/clients/?company_id=1",
           "client_type": "company",
           "name": "company name",
           "created_time": "2020-01-01 00:00:00"
         },
         "timestamp": "2020-01-01 00:00:00",
         "event": {
           "event_type": "sync",
           "event_name": "client_update"
         }
      }"""
    )
    val json = CabinetCompanyPatch(
      clientId = 1L,
      name = Some("company name"),
      createdTime = defaultTimestamp.some,
      timestamp = defaultTimestamp
    ).toJsonString
    assert(json)(equalTo(spec))
  }

  val salesmanPatchFormatWithLoyalty = test("check salesman patch formatting with loyalty data") {
    val spec = minifyJson(
      """{
         "client_id": "client-1",
         "tariffs": {
           "active": [
             "CARS_USED",
             "COMMERCIAL"
           ]
         },
         "timestamp": "2020-01-01 00:00:00",
         "event": {
           "event_type": "sync",
           "event_name": "client_update"
         }
      }"""
    )
    val json = SalesmanPatch(
      clientId = 1L,
      tariffs = Tariffs(active =
        Seq(
          CarsUsed,
          Commercial
        )
      ).some,
      timestamp = defaultTimestamp
    ).toJsonString
    assert(json)(equalTo(spec))
  }

  val salesmanPatchFormatWithoutLoyalty = test("check salesman patch formatting without loyalty data") {
    val spec = minifyJson(
      """{
         "client_id": "client-1",
         "tariffs": {
           "active": [
             "CARS_USED",
             "COMMERCIAL"
           ]
         },
         "timestamp": "2020-01-01 00:00:00",
         "event": {
           "event_type": "sync",
           "event_name": "client_update"
         }
      }"""
    )
    val json = SalesmanPatch(
      clientId = 1L,
      tariffs = Tariffs(active =
        Seq(
          CarsUsed,
          Commercial
        )
      ).some,
      timestamp = defaultTimestamp
    ).toJsonString
    assert(json)(equalTo(spec))
  }

  val salesmanPatchFormatWithoutLoyaltyDoesntDropTariffsNull =
    test("check salesman patch formatting with empty data preserve tariffs field") {
      val spec = minifyJson(
        """{
         "client_id": "client-1",
         "tariffs": null,
         "timestamp": "2020-01-01 00:00:00",
         "event": {
           "event_type": "sync",
           "event_name": "client_update"
         }
      }"""
      )
      val json = SalesmanPatch(
        clientId = 1L,
        tariffs = none,
        timestamp = defaultTimestamp
      ).toJsonString
      assert(json)(equalTo(spec))
    }

  val companiesDataPatchFormat = test("check companies patch formatting") {
    val spec = minifyJson(
      """{
         "client_id": "company-1",
         "main": {
           "name": "test",
           "created_time": "2020-01-01 00:00:00"
         },
         "event": {
           "event_type": "sync",
           "event_name": "client_update"
         },
         "timestamp": "2020-01-01 00:00:00"
      }"""
    )
    val json = CompaniesDataPatch(
      companyId = 1L,
      name = "test",
      createdTime = defaultTimestamp,
      timestamp = defaultTimestamp
    ).toJsonString
    assert(json)(equalTo(spec))
  }

  val expensesPatchFormat = test("check expenses patch formatting") {
    val spec = minifyJson(
      """{
         "client_id": "client-1",
         "event": {
           "event_type": "sync",
           "event_name": "client_update"
         },
         "expenses": {
           "period": {
               "from": "2020-01-01 00:00:00",
               "to": "2020-01-01 00:00:00"
           },
           "products": [
             {
               "product": "placement",
               "amount": 10000,
               "amount_without_discount": 15000,
                 "count": 10
             },
             {
               "product": "boost",
               "amount": 10000,
               "amount_without_discount": 15000,
               "count": 10
             },
             {
               "product": "premium",
               "amount": 10000,
               "amount_without_discount": 15000,
               "count": 10
             },
             {
               "product": "turbo-package",
               "amount": 10000,
               "amount_without_discount": 15000,
               "count": 10
             }
           ]
         },
         "timestamp": "2020-01-01 00:00:00"
      }"""
    )

    val json = ExpensesPatch(
      clientId = 1L,
      isAgency = false,
      from = defaultTimestamp,
      to = defaultTimestamp,
      productExpenses = Seq(
        ProductExpensesSummary("placement", Kopecks(1000000L), Kopecks(1500000L), 10),
        ProductExpensesSummary("boost", Kopecks(1000000L), Kopecks(1500000L), 10),
        ProductExpensesSummary("premium", Kopecks(1000000L), Kopecks(1500000L), 10),
        ProductExpensesSummary("turbo-package", Kopecks(1000000L), Kopecks(1500000L), 10)
      ),
      timestamp = defaultTimestamp
    ).toJsonString
    assert(json)(equalTo(spec))
  }

  val billingIncomingEventFormat = test("billing_up trigger event formatting") {
    val spec = minifyJson("""
        {
        "event": {
          "event_type": "balance",
          "event_name": "balance_up",
          "event_data": {
            "balance": {
              "incoming": 1000
            }
          }
        },

        "client_id": "agency-123",

        "finance": {
          "balance": 10000,
          "balance_timestamp": "2020-05-01 19:18:00"
        },

        "timestamp": "2020-05-01 19:18:00"
       }

      """)

    val json = BalanceIncomingEvent(
      clientId = 123L,
      isAgency = true,
      incoming = Kopecks(100000L),
      balance = Kopecks(1000000L),
      stateEpoch = ZonedDateTime.parse("2020-05-01 19:18:00", defaultAmoDateTimeFormatter),
      timestamp = ZonedDateTime.parse("2020-05-01 19:18:00", defaultAmoDateTimeFormatter)
    ).toJsonString

    assert(json)(equalTo(spec))
  }

  val offersPatchFormat = test("check offers patch formatting") {
    val spec = minifyJson(
      """{
         "client_id":"client-1",
         "offers": {
           "counts_by_tariff": [
              {
                "tariff": "CARS_USED",
                "active": 100,
                "inactive": 30,
                "blocked": 2
              },
              {
                "tariff": "CARS_NEW",
                "active": 0,
                "inactive": 0,
                "blocked": 0
              },
              {
                "tariff": "MOTO",
                "active": 15,
                "inactive": 2,
                "blocked": 0
              },
              {
                "tariff": "COMMERCIAL",
                "active": 20,
                "inactive": 0,
                "blocked": 0
              }
           ]
         },
        "event": {
          "event_type": "sync",
          "event_name": "client_update"
        },
        "timestamp":"2020-01-01 00:00:00"
       }"""
    )
    val json = OffersPatch(
      clientId = 1L,
      offerCounters = Seq(
        OfferCountByTariff(CarsUsed, active = 100, inactive = 30, blocked = 2),
        OfferCountByTariff(CarsNew, active = 0, inactive = 0, blocked = 0),
        OfferCountByTariff(Moto, active = 15, inactive = 2, blocked = 0),
        OfferCountByTariff(Commercial, active = 20, inactive = 0, blocked = 0)
      ),
      timestamp = defaultTimestamp
    ).toJsonString
    assert(json)(equalTo(spec))
  }

  val fullClientFormat = test("full client message formatting") {
    def compose(parts: String*) =
      minifyJson("{" + parts.mkString(",") + ",\"update_timestamp\": \"2020-04-28 09:45:00\"" + "}")

    val mainFieldsSpec = """
         "client_id": "client-123",
         "origin": "msk0001",
         "client_type": "client",
         "name": "Рольф Химки",
         "agency": "agency-123",
         "head_company": "company-45",
         "responsible_manager_email": "rbublik@auto.yandex.ru",
         "status": "new",
         "created_time": "2020-04-28 09:45:00",
         "city": "Химки",
         "region": "Москва и Москвоская область",
         "address": "Ленинградское шоссе 381",
         "phone_number": "+74954562347",
         "email": null,
         "website": "rolf.ru",
         "dealership": ["BMW", "Audi"]
      """
    val main = AmoCrmFullClientMessage(
      clientId = AmoClientId(123L, ClientType.Client),
      origin = "msk0001".some,
      clientType = "client".some,
      name = "Рольф Химки".some,
      agency = AmoClientId(123L, ClientType.Agency).some,
      headCompany = AmoClientId(45, ClientType.Company).some,
      responsibleManagerEmail = "rbublik@auto.yandex.ru".some,
      status = "new".some,
      createdTime = "2020-04-28 09:45:00".some,
      city = "Химки".some,
      region = "Москва и Москвоская область".some,
      address = "Ленинградское шоссе 381".some,
      phoneNumber = "+74954562347".some,
      email = None,
      website = "rolf.ru".some,
      dealership = Seq("BMW", "Audi").some,
      updateTimestamp = parseAmoFormat("2020-04-28 09:45:00").some,
      tariffs = None,
      loyalty = None,
      moderation = None,
      finance = None,
      stock = None,
      lastSession = None
    )

    val tariffsSpec = """
        "tariffs": {
          "placement": {
            "CARS_USED": {
              "name": "Легковые с пробегом",
              "offers_count": 10

            },
            "CARS_NEW" : {
              "name": "Легковые новые",
              "offers_count": 20
            },
            "COMMERCIAL" : {
              "name": "Коммтс",
              "offers_count": 30
            },
            "MOTO" : {
             "name": "Мото",
             "offers_count": 40
           }
         },
         "vas": {
           "boost": {
             "name": "Поднятие в поиске",
             "offers_count": 1
           },
           "premium": {
             "name": "Премиум",
             "offers_count": 2
           },
           "special-offer": {
             "name": "Спецпредложение",
             "offers_count": 4
           },
           "turbo-package": {
             "name": "Турбо-продажа",
             "offers_count": 3
           },
           "all-sale-badge": {
             "name": "Стикеры",
             "offers_count": 5
           }
         }
       }
    """

    val tariffs = TariffsFull(
      placement = Map(
        CarsUsed -> TariffEntity("Легковые с пробегом", 10),
        CarsNew -> TariffEntity("Легковые новые", 20),
        Commercial -> TariffEntity("Коммтс", 30),
        Moto -> TariffEntity("Мото", 40)
      ).some,
      vas = Map(
        Boost -> TariffEntity("Поднятие в поиске", 1),
        Premium -> TariffEntity("Премиум", 2),
        Turbo -> TariffEntity("Турбо-продажа", 3),
        Special -> TariffEntity("Спецпредложение", 4),
        Badge -> TariffEntity("Стикеры", 5)
      ).some
    )

    val loyaltySpec =
      """
      "loyalty": {
        "is_loyal": true,
        "loyalty_level": 1,
        "loyalty_cashback_percent": 10,
        "loyalty_cashback_amount": 10000,
        "comment": "Произвольный текст за что был выдан кэшбэк"
      }
      """
    val loyalty = Loyalty(
      isLoyal = true.some,
      loyaltyLevel = 1.some,
      loyaltyCashbackPercent = 10.some,
      loyaltyCashbackAmount = 10000L.some,
      comment = "Произвольный текст за что был выдан кэшбэк".some
    )

    val moderationSpec =
      """
      "moderation": {
        "first_moderation": true,
        "ban_reasons": "текст",
        "on_moderation": false,
        "sitecheck": null,
        "sitecheck_date": "2020-05-04 00:45:57",
        "moderation_comment": "Жульё! Блокируем навсегда"
      }
      """
    val moderation = ModerationBlock(
      firstModeration = true.some,
      banReasons = "текст".some,
      onModeration = false.some,
      sitecheck = None,
      sitecheckDate = parseAmoFormat("2020-05-04 00:45:57").some,
      moderationComment = "Жульё! Блокируем навсегда".some
    )

    val financeSpec =
      """
      "finance": {
        "balance": 10000,
        "average_outcome": 3450,
        "paid_till": "2020-04-30",
        "rest_days": 12,
        "ya_balance_client_url_internal": "https://admin.balance.yandex-team.ru/client_id=3787474764",
        "ya_balance_client_url_external": "https://admin.balance.yandex.ru/client_id=3787474764",
        "invoice_requests": [
          {
            "date": "2020-05-04 00:45:57",
            "url": "https://admin.balance.yandex.ru/request_id=3787474764"
          },
          {
            "date": "2020-05-04 00:45:57",
            "url": "https://admin.balance.yandex.ru/request_id=3787474764"
          }
        ],
        "expenses": [
          {
            "date": "2020-05-04",
            "amount_rubles": 1000
          },
          {
            "date": "2020-05-04",
            "amount_rubles": 1000
          },
          {
            "date": "2020-05-04",
            "amount_rubles": 1000
          },
          {
            "date": "2020-05-04",
            "amount_rubles": 1000
          }
        ]
      }
      """

    val invoiceRequests = Seq(
      InvoiceRequest(
        date = parseAmoFormat("2020-05-04 00:45:57").some,
        url = "https://admin.balance.yandex.ru/request_id=3787474764".some
      ),
      InvoiceRequest(
        date = parseAmoFormat("2020-05-04 00:45:57").some,
        url = "https://admin.balance.yandex.ru/request_id=3787474764".some
      )
    )
    val expenses = Seq(
      Expense(date = "2020-05-04".some, amountRubles = BigDecimal(1000).some),
      Expense(date = "2020-05-04".some, amountRubles = BigDecimal(1000).some),
      Expense(date = "2020-05-04".some, amountRubles = BigDecimal(1000).some),
      Expense(date = "2020-05-04".some, amountRubles = BigDecimal(1000).some)
    )
    val finance = Finance(
      balance = 10000L,
      averageOutcome = 3450L,
      paidTill = "2020-04-30",
      restDays = 12,
      yaBalanceClientUrlInternal = "https://admin.balance.yandex-team.ru/client_id=3787474764",
      yaBalanceClientUrlExternal = "https://admin.balance.yandex.ru/client_id=3787474764",
      invoiceRequests = invoiceRequests,
      expenses = expenses
    )

    val stockSpec =
      """
      "stock": {
        "CARS_USED": {
          "full_stock": true,
          "amount": 560
        },
        "COMMERCIAL": {
          "full_stock": true,
          "amount": 560
        }
      }
      """
    val stock = Map(
      CarsUsed -> Stock(fullStock = true, amount = 560),
      Commercial -> Stock(fullStock = true, amount = 560)
    )

    val lastSessionSpec =
      """
      "last_session": {
        "client_login": "demo@auto.ru",
        "is_online": true,
        "time": "2020-05-27 14:30:00"
        }
      """
    val lastSession =
      LastSession(
        clientLogin = "demo@auto.ru".some,
        isOnline = true.some,
        time = parseAmoFormat("2020-05-27 14:30:00").some
      )

    val fullAssert = main.copy(
      tariffs = tariffs.some,
      loyalty = loyalty.some,
      moderation = moderation.some,
      finance = finance.some,
      stock = stock.some,
      lastSession = lastSession.some
    )
    assert(fullAssert.toJsonString)(
      equalTo(
        compose(mainFieldsSpec, tariffsSpec, loyaltySpec, moderationSpec, financeSpec, stockSpec, lastSessionSpec)
      )
    )
  }

  private def parseAmoFormat(dt: String): ZonedDateTime = ZonedDateTime.parse(dt, defaultAmoDateTimeFormatter)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Model rendering")(
      financePatchFormat,
      offersPatchFormat,
      cabinetClientPatchFormat,
      cabinetCompanyPatchFormat,
      salesmanPatchFormatWithLoyalty,
      salesmanPatchFormatWithoutLoyalty,
      companiesDataPatchFormat,
      expensesPatchFormat,
      billingIncomingEventFormat,
      fullClientFormat
    )

  private def minifyJson(value: String): String = {
    val either = parse(value)
    if (either.isLeft) { either.left.map(println(_)) }
    either.getOrElse(Json.Null).printWith(Printer.noSpaces)

  }
}
