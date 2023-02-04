package auto.dealers.amoyak.logic

import amogus.model.AmogusConfig.AmogusServiceConfig
import amogus.model.ValueTypes
import auto.common.clients.cabinet.testkit.CabinetTest
import auto.common.clients.vos.testkit.VosTest
import auto.common.manager.statistics.testkit.StatisticsManagerMock
import auto.dealers.amoyak.logic.AmogusConverter.getCustomFieldValue
import auto.dealers.amoyak.model._
import auto.dealers.amoyak.model.blocks.ProductExpensesSummary
import auto.dealers.amoyak.storage.dao.CompaniesDao.CompanyRecord
import auto.dealers.amoyak.storage.dao.UsersDao
import auto.dealers.amoyak.storage.dao.UsersDao.ResultUserRecord
import auto.dealers.amoyak.storage.testkit.dao.{CompaniesDaoMock, RegionsDaoMock, UsersDaoMock}
import common.clients.geocoder.testkit.GeocoderClientMock
import common.zio.app.Application
import common.zio.sttp.endpoint.Endpoint
import ru.auto.amoyak.common_service_model.CustomerType
import ru.auto.amoyak.internal_service_model.{AmoyakDto, Office7Data}
import ru.auto.cabinet.api_model.ClientIdsResponse.ClientInfo
import ru.yandex.vertis.amogus.model.{Company, CustomField}
import ru.yandex.vertis.amogus.model.CustomField.FieldValue
import ru.yandex.vertis.amogus.company_event.{CompanyAdded, CompanyChanged}
import ru.yandex.vertis.billing.billing_event.{BillingOperation, CommonBillingInfo, TransactionBillingInfo}
import ru.yandex.vertis.billing.model.CustomerId
import yandex.maps.proto.common2.geo_object.geo_object.GeoObject
import yandex.maps.proto.common2.metadata.metadata.Metadata
import yandex.maps.proto.common2.response.response.Response
import yandex.maps.proto.search.geocoder.geocoder.{GeoObjectMetadata, GeocoderProto}
import yandex.maps.proto.search.geocoder_internal.geocoder_internal.{GeocoderInternalProto, ToponymInfo}
import zio.{Has, ZIO, ZLayer}
import zio.clock.Clock
import zio.random.Random
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation
import zio.test.mock.Expectation._

import java.util.UUID

object AmogusConverterLiveSpec extends DefaultRunnableSpec {
  val clientId = 1L
  val agencyId = 2L
  val balanceId = 3L

  val customFieldsConfig: CustomFieldsConfig =
    CustomFieldsConfig(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
      27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54,
      55, 56)

  val robotManagerEmail = "vertis-amocrm-robot@auto.yandex.ru"

  val amogusServiceConfig: AmogusServiceConfig = AmogusServiceConfig(
    serviceId = ValueTypes.ServiceId(UUID.randomUUID),
    serviceName = ValueTypes.ServiceName(""),
    host = Endpoint(host = "autoru.amocrm.ru", port = 443, schema = "https"),
    topic = "",
    webhooks = Set.empty,
    credentials = Seq.empty,
    robotManagerEmail = Some(robotManagerEmail)
  )

  val clientType = "ДЦ"
  val responsibleUserId: Long = 3
  val responsibleUserEmail = "manager@auto.yandex.ru"
  val email = "4"
  val phone = "5"
  val city = 6
  val cityId = 7
  val name = "8"
  val headCompany = 9
  val validManagerEmail1 = "manager-1@auto.yandex.ru"
  val validManagerEmail2 = "manager-2@auto.yandex.ru"
  val modifiedUserId = "11"
  val invalidManagerEmail = "manager@yandex.ru"
  val autoruId: Long = 12
  val amoId: Long = 13

  val positiveIntGen: Gen[Random, Int] = Gen.anyInt.map(_.abs)

  val office7 = Endpoint.testEndpointLayer

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AmogusConverterLive")(
      testM("should convert to CreateClientRequest") {
        val createClientRequest = CreateClientRequest(
          ClientType.Client,
          email,
          phone,
          name,
          cityId,
          validManagerEmail1
        )

        val companyAdded = CompanyAdded.defaultInstance
          .withName(name)
          .withResponsibleUserId(responsibleUserId.toString)
          .withCustomFields(
            Seq(
              CustomField.defaultInstance
                .withCode("EMAIL")
                .withValues(Seq(FieldValue.defaultInstance.withFieldValue(FieldValue.FieldValue.Text(email)))),
              CustomField.defaultInstance
                .withCode("PHONE")
                .withValues(Seq(FieldValue.defaultInstance.withFieldValue(FieldValue.FieldValue.Text(phone)))),
              CustomField.defaultInstance
                .withId(customFieldsConfig.clientType.toString)
                .withValues(Seq(FieldValue.defaultInstance.withFieldValue(FieldValue.FieldValue.Text(clientType)))),
              CustomField.defaultInstance
                .withId(customFieldsConfig.city.toString)
                .withValues(Seq(FieldValue.defaultInstance.withFieldValue(FieldValue.FieldValue.Text(city.toString))))
            )
          )

        val geocoderClient = GeocoderClientMock.Geocode(
          equalTo((city.toString, 1)),
          value(
            Response.defaultInstance
              .withReply {
                GeoObject.defaultInstance.withGeoObject {
                  Seq(
                    GeoObject.defaultInstance.withMetadata {
                      Seq(
                        Metadata()
                          .withExtension(GeocoderProto.gEOOBJECTMETADATA) {
                            Some {
                              GeoObjectMetadata.defaultInstance
                                .withExtension(GeocoderInternalProto.tOPONYMINFO)(
                                  Some(ToponymInfo.defaultInstance.withGeoid(cityId))
                                )
                            }
                          }
                      )
                    }
                  )
                }
              }
          )
        )

        val usersDao = UsersDaoMock.GetByAmoId(
          equalTo(responsibleUserId),
          value(ResultUserRecord(0, responsibleUserId, validManagerEmail1))
        )

        val amogusConverter =
          (Clock.live ++
            CabinetTest.empty ++
            VosTest.empty ++
            geocoderClient.toLayer ++
            StatisticsManagerMock.empty ++
            usersDao ++
            CompaniesDaoMock.empty ++
            RegionsDaoMock.empty ++
            ZLayer.succeed(customFieldsConfig) ++
            ZLayer.succeed(amogusServiceConfig) ++
            office7 ++
            Application.live) >>> AmogusConverterLive.layer

        assertM(AmogusConverter(_.toCreateClientRequest(companyAdded)))(equalTo(createClientRequest))
          .provideCustomLayer(amogusConverter)
      },
      testM("should convert to UpdateClientRequest") {
        testUpdateClient(
          UsersDaoMock.GetByAmoId(
            equalTo(responsibleUserId),
            value(ResultUserRecord(0, responsibleUserId, validManagerEmail1))
          ) ++ UsersDaoMock.GetByAmoId(
            equalTo(modifiedUserId.toLong),
            value(ResultUserRecord(1, modifiedUserId.toLong, validManagerEmail2))
          ),
          validManagerEmail2
        )
      },
      testM("should use robot email when user not found") {
        testUpdateClient(
          UsersDaoMock.GetByAmoId(
            equalTo(responsibleUserId),
            value(ResultUserRecord(0, responsibleUserId, validManagerEmail1))
          ) ++ UsersDaoMock.GetByAmoId(
            equalTo(modifiedUserId.toLong),
            failure(new Exception("(not found in db)"))
          ),
          expectedModifiedManager = robotManagerEmail
        )
      },
      testM("should use robot email when domain is invalid") {
        testUpdateClient(
          UsersDaoMock.GetByAmoId(
            equalTo(responsibleUserId),
            value(ResultUserRecord(0, responsibleUserId, validManagerEmail1))
          ) ++ UsersDaoMock.GetByAmoId(
            equalTo(modifiedUserId.toLong),
            value(ResultUserRecord(1, modifiedUserId.toLong, invalidManagerEmail))
          ),
          expectedModifiedManager = robotManagerEmail
        )
      },
      testM("toCompany should create Client from AmoyakDto Office7Data") {
        val regionClusterId = 1L

        val amoyakDto = AmoyakDto.defaultInstance
          .withClientId(clientId)
          .withCustomerType(CustomerType.CLIENT)
          .withOffice7Data(
            Office7Data.defaultInstance
              .withClients(
                Office7Data.Clients.defaultInstance
                  .withResponsibleManagerEmail(responsibleUserEmail)
                  .withHeadCompany(autoruId)
                  .withCreatedTime("2022-01-02 01:23:45")
              )
          )

        val usersDao = UsersDaoMock.empty
//        val usersDao = UsersDaoMock.GetByEmail(
//          equalTo(responsibleUserEmail),
//          value(ResultUserRecord(0, responsibleUserId, responsibleUserEmail))
//        )

        val companiesDao =
          CompaniesDaoMock.GetByAutoruId(
            equalTo((clientId, ClientType.Client)),
            value(CompanyRecord(clientId, isHeadCompany = false, amoId, Some(responsibleUserId)))
          ) ++ CompaniesDaoMock.GetByAutoruId(
            equalTo((autoruId, ClientType.Company)),
            value(CompanyRecord(clientId, isHeadCompany = true, amoId, Some(responsibleUserId)))
          )

        assertM(AmogusConverter(_.toCompany(amoyakDto, Some(regionClusterId)))) {
//          hasField[Company, Long]("responsibleUserId", _.responsibleUserId, equalTo(responsibleUserId)) &&
          hasField[Company, Option[String]](
            "clientId",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.clientId),
            equalTo(Some(s"client-${clientId.toString}"))
          ) &&
          hasField[Company, Option[String]](
            "clientType",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.clientType),
            equalTo(Some("ДЦ"))
          ) &&
          hasField[Company, Option[String]](
            "headCompanyUrl",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.headCompanyUrl),
            equalTo(Some(amogusServiceConfig.host.toUri.withWholePath(s"companies/detail/$amoId").toString))
          ) &&
          hasField[Company, Option[String]](
            "createdTime",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.createdTime),
            equalTo(Some("2022-01-02T01:23:45Z"))
          ) &&
          hasField[Company, Option[String]](
            "regionClusterUrl",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.regionClusterUrl),
            equalTo(Some(amogusServiceConfig.host.toUri.withWholePath(s"companies/detail/$regionClusterId").toString))
          )
        }.provideCustomLayer(
          (Clock.live ++
            CabinetTest.empty ++
            VosTest.empty ++
            GeocoderClientMock.empty ++
            StatisticsManagerMock.empty ++
            usersDao ++
            companiesDao ++
            RegionsDaoMock.empty ++
            ZLayer.succeed(customFieldsConfig) ++
            ZLayer.succeed(amogusServiceConfig) ++
            office7 ++
            Application.live) >>> AmogusConverterLive.layer
        )
      },
      testM("toCompany should create Company from AmoyakDto Office7Data") {
        val amoyakDto = AmoyakDto.defaultInstance
          .withClientId(clientId)
          .withCustomerType(CustomerType.COMPANY_GROUP)
          .withOffice7Data(
            Office7Data.defaultInstance
              .withClients(
                Office7Data.Clients.defaultInstance
                  .withCreatedTime("")
              )
          )

        val usersDao = UsersDaoMock.empty
        //        val usersDao = UsersDaoMock.GetByEmail(
        //          equalTo(robotManagerEmail),
        //          value(ResultUserRecord(0, responsibleUserId, robotManagerEmail))
        //        )

        val companiesDao = CompaniesDaoMock
          .GetByAutoruId(
            equalTo((clientId, ClientType.Company)),
            value(CompanyRecord(clientId, isHeadCompany = true, amoId, Some(responsibleUserId)))
          )

        assertM(AmogusConverter(_.toCompany(amoyakDto, None))) {
          hasField[Company, Option[String]](
            "clientId",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.clientId),
            equalTo(Some(s"company-${clientId.toString}"))
          ) &&
          hasField[Company, Option[String]](
            "clientType",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.clientType),
            equalTo(Some("ГК"))
          ) &&
          hasField[Company, Option[String]](
            "createdTime",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.createdTime),
            equalTo(None)
          ) &&
          hasField[Company, Option[String]](
            "region",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.region),
            equalTo(None)
          ) &&
          hasField[Company, Option[String]](
            "firstModeration",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.firstModeration),
            equalTo(None)
          )
        }.provideCustomLayer(
          (Clock.live ++
            CabinetTest.empty ++
            VosTest.empty ++
            GeocoderClientMock.empty ++
            StatisticsManagerMock.empty ++
            usersDao ++
            companiesDao ++
            RegionsDaoMock.empty ++
            ZLayer.succeed(customFieldsConfig) ++
            ZLayer.succeed(amogusServiceConfig) ++
            office7 ++
            Application.live) >>> AmogusConverterLive.layer
        )
      },
      testM("toCompany should fill in clientsSearchUrl and regionClustersSearchUrl") {
        val regionClusterId = 100L

        val amoyakDto = AmoyakDto.defaultInstance
          .withClientId(clientId)
          .withCustomerType(CustomerType.COMPANY_GROUP)
          .withOffice7Data(
            Office7Data.defaultInstance
              .withClients(
                Office7Data.Clients.defaultInstance
                  .withResponsibleManagerEmail(responsibleUserEmail)
                  .withCreatedTime("2022-01-02 01:23:45")
              )
          )

        val usersDao = UsersDaoMock.empty
//        val usersDao = UsersDaoMock.GetByEmail(
//          equalTo(responsibleUserEmail),
//          value(ResultUserRecord(0, responsibleUserId, responsibleUserEmail))
//        )

        val companiesDao = CompaniesDaoMock
          .GetByAutoruId(
            equalTo((clientId, ClientType.Company)),
            value(CompanyRecord(clientId, isHeadCompany = true, amoId, Some(responsibleUserId)))
          )

        val clientsSearchUrl = amogusServiceConfig.host.toUri
          .withWholePath("/contacts/list/companies/")
          .addParam(
            s"filter[cf][${customFieldsConfig.clientType}]",
            customFieldsConfig.customerTypeEnumClientValue.toString
          )
          .addParam(s"filter[cf][${customFieldsConfig.headCompanyId}][from]", amoId.toString)
          .addParam(s"filter[cf][${customFieldsConfig.headCompanyId}][to]", amoId.toString)
          .addParam("useFilter", "y")

        val regionClustersSearchUrl = amogusServiceConfig.host.toUri
          .withWholePath("/contacts/list/companies/")
          .addParam(
            s"filter[cf][${customFieldsConfig.clientType}]",
            customFieldsConfig.customerTypeEnumRegionClusterValue.toString
          )
          .addParam(s"filter[cf][${customFieldsConfig.headCompanyId}][from]", amoId.toString)
          .addParam(s"filter[cf][${customFieldsConfig.headCompanyId}][to]", amoId.toString)
          .addParam("useFilter", "y")

        assertM(AmogusConverter(_.toCompany(amoyakDto, Some(regionClusterId)))) {
          // https://a.yandex-team.ru/arc_vcs/classifieds/verticals-backend/auto/dealers/amoyak/logic/src/processors/DefaultAutoRuAmoCrmProcessor.scala?rev=r9253252#L44
          // Оказывается, ответственный раньше не посылался для ГК
          // hasField[Company, Long]("responsibleUserId", _.responsibleUserId, equalTo(responsibleUserId.toLong)) &&

          hasField[Company, Option[String]](
            "createdTime",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.createdTime),
            equalTo(Some("2022-01-02T01:23:45Z"))
          ) &&
          hasField[Company, Option[String]](
            "clientId",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.clientId),
            equalTo(Some(s"company-${clientId.toString}"))
          ) &&
          hasField[Company, Option[String]](
            "regionClusterUrl",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.regionClusterUrl),
            equalTo(Some(amogusServiceConfig.host.toUri.withWholePath(s"companies/detail/$regionClusterId").toString))
          ) &&
          hasField[Company, Option[String]](
            "clientsSearchUrl",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.clientsSearchUrl),
            equalTo(Some(clientsSearchUrl.toString))
          ) &&
          hasField[Company, Option[String]](
            "regionClustersSearchUrl",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.regionClustersSearchUrl),
            equalTo(Some(regionClustersSearchUrl.toString))
          )
        }.provideCustomLayer(
          (Clock.live ++
            CabinetTest.empty ++
            VosTest.empty ++
            GeocoderClientMock.empty ++
            StatisticsManagerMock.empty ++
            usersDao ++
            companiesDao ++
            RegionsDaoMock.empty ++
            ZLayer.succeed(customFieldsConfig) ++
            ZLayer.succeed(amogusServiceConfig) ++
            office7 ++
            Application.live) >>> AmogusConverterLive.layer
        )
      },
      testM("toCompany should create Company from BillingOperation") {
        val balance = 100

        val billingOperation = BillingOperation.defaultInstance
          .withOrderState(TransactionBillingInfo.OrderState.defaultInstance.withBalance(balance))
          .withTransactionInfo(
            CommonBillingInfo.TransactionInfo.defaultInstance.withCustomerId(
              CustomerId.defaultInstance
                .withClientId(balanceId)
            )
          )

        val cabinet = CabinetTest.GetClientByBalanceIds(
          anything,
          value(List(ClientInfo.defaultInstance.withClientId(autoruId).withBalanceId(balanceId)))
        )

        val companiesDao = CompaniesDaoMock.GetByAutoruId(
          equalTo((autoruId, ClientType.Client)),
          value(CompanyRecord(autoruId, isHeadCompany = false, amoId, Some(responsibleUserId)))
        )

        assertM(AmogusConverter(_.toCompany(billingOperation))) {
          hasField[Company, Option[String]](
            "balance",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.balance),
            equalTo(Some(balance.toString))
          )
        }.provideCustomLayer(
          (Clock.live ++
            cabinet ++
            GeocoderClientMock.empty ++
            StatisticsManagerMock.empty ++
            UsersDaoMock.empty ++
            companiesDao ++
            RegionsDaoMock.empty ++
            ZLayer.succeed(customFieldsConfig) ++
            ZLayer.succeed(amogusServiceConfig) ++
            office7 ++
            Application.live) >>> AmogusConverterLive.layer
        )
      },
      testM("toCompany should create Company from clientId and offer counts") {
        val companiesDao = CompaniesDaoMock.GetByAutoruId(
          equalTo((autoruId, ClientType.Client)),
          value(CompanyRecord(autoruId, isHeadCompany = false, amoId, Some(responsibleUserId)))
        )

        assertM(
          AmogusConverter(
            _.toCompany(autoruId, carsUsedCount = 1, carsNewCount = 2, commercialCount = 3, motoCount = 4)
          )
        ) {
          hasField[Company, Option[String]](
            "totalCount",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.totalCount),
            equalTo(Some("10"))
          )
        }.provideCustomLayer(
          (Clock.live ++
            CabinetTest.empty ++
            GeocoderClientMock.empty ++
            StatisticsManagerMock.empty ++
            UsersDaoMock.empty ++
            companiesDao ++
            RegionsDaoMock.empty ++
            ZLayer.succeed(customFieldsConfig) ++
            ZLayer.succeed(amogusServiceConfig) ++
            office7 ++
            Application.live) >>> AmogusConverterLive.layer
        )
      },
      testM("toCompany should create Company from ClientInfo and OrderState") {
        checkM(positiveIntGen, positiveIntGen, positiveIntGen) { case (averageOutcome, totalIncome, totalSpent) =>
          val clientInfo = ClientInfo.defaultInstance.withClientId(clientId)
          val orderState =
            OrderState(clientId, None, totalIncome = totalIncome, totalSpent = totalSpent, lastTouchMicros = 0)

          val statisticsManager = StatisticsManagerMock.GetAverageWeekOutcome(anything, value(averageOutcome))

          val companiesDao = CompaniesDaoMock.GetByAutoruId(
            equalTo((clientId, ClientType.Client)),
            value(CompanyRecord(clientId, isHeadCompany = false, amoId, Some(responsibleUserId)))
          )

          assertM(AmogusConverter(_.toCompany(clientInfo, orderState))) {
            hasField[Company, Option[String]](
              "balance",
              company => getCustomFieldValue(company.customFields, customFieldsConfig.balance),
              equalTo(Some(((totalIncome - totalSpent) / 100).toString))
            ) &&
            hasField[Company, Option[String]](
              "averageOutcome",
              company => getCustomFieldValue(company.customFields, customFieldsConfig.averageOutcome),
              equalTo(Some(averageOutcome.toString))
            )
          }.provideCustomLayer(
            (Clock.live ++
              CabinetTest.empty ++
              GeocoderClientMock.empty ++
              statisticsManager ++
              UsersDaoMock.empty ++
              companiesDao ++
              RegionsDaoMock.empty ++
              ZLayer.succeed(customFieldsConfig) ++
              ZLayer.succeed(amogusServiceConfig) ++
              office7 ++
              Application.live) >>> AmogusConverterLive.layer
          )
        }
      },
      testM("toCompany should create Company from ClientInfo and Seq[ProductExpensesSummary]") {
        val clientInfo = ClientInfo.defaultInstance.withClientId(autoruId).withBalanceId(balanceId)
        val productExpensesSummary = ProductExpensesSummary("badge", 100, 100, 1)

        val companiesDao = CompaniesDaoMock.GetByAutoruId(
          equalTo((autoruId, ClientType.Client)),
          value(CompanyRecord(autoruId, isHeadCompany = false, amoId, Some(responsibleUserId)))
        )

        assertM(AmogusConverter(_.toCompany(clientInfo, List(productExpensesSummary)))) {
          hasField[Company, Option[String]](
            "vasesCount",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.vasesCount),
            equalTo(Some("1"))
          ) &&
          hasField[Company, Option[String]](
            "badgeVasesCount",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.badgeVasesCount),
            equalTo(Some("1"))
          ) &&
          hasField[Company, Option[String]](
            "boostVasesCount",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.boostVasesCount),
            equalTo(Some("0"))
          ) &&
          hasField[Company, Option[String]](
            "tariffs",
            company => getCustomFieldValue(company.customFields, customFieldsConfig.tariffs),
            equalTo(Some("badge"))
          )
        }.provideCustomLayer(
          (Clock.live ++
            CabinetTest.empty ++
            GeocoderClientMock.empty ++
            StatisticsManagerMock.empty ++
            UsersDaoMock.empty ++
            companiesDao ++
            RegionsDaoMock.empty ++
            ZLayer.succeed(customFieldsConfig) ++
            ZLayer.succeed(amogusServiceConfig) ++
            office7 ++
            Application.live) >>> AmogusConverterLive.layer
        )
      }
    )

  private def testUpdateClient(
      usersDaoExpectations: Expectation[Has[UsersDao.Service]],
      expectedModifiedManager: String) = {
    val updateClientRequest = UpdateClientRequest(
      AmoClientId(clientId, isAgency = false),
      agency = None,
      headCompany = Some(AmoClientId(headCompany, isAgency = false)),
      responsibleManagerEmail = Some(validManagerEmail1),
      moderationComment = None,
      modifiedManagerEmail = Some(expectedModifiedManager)
    )

    val companyChanged = CompanyChanged.defaultInstance
      .withName(name)
      .withResponsibleUserId(responsibleUserId.toString)
      .withModifiedUserId(modifiedUserId)
      .withCustomFields(
        Seq(
          CustomField.defaultInstance
            .withId(customFieldsConfig.clientId.toString)
            .withValues(
              Seq(
                FieldValue.defaultInstance
                  .withFieldValue(FieldValue.FieldValue.Text(s"client-${clientId.toString}"))
              )
            ),
          CustomField.defaultInstance
            .withId(customFieldsConfig.headCompanyIdLegacy.toString)
            .withValues(
              Seq(
                FieldValue.defaultInstance
                  .withFieldValue(FieldValue.FieldValue.Text(s"client-${headCompany.toString}"))
              )
            )
        )
      )

    val amogusConverter =
      (Clock.live ++
        CabinetTest.empty ++
        VosTest.empty ++
        GeocoderClientMock.empty ++
        StatisticsManagerMock.empty ++
        usersDaoExpectations ++
        CompaniesDaoMock.empty ++
        RegionsDaoMock.empty ++
        ZLayer.succeed(customFieldsConfig) ++
        ZLayer.succeed(amogusServiceConfig) ++
        office7 ++
        Application.live) >>> AmogusConverterLive.layer

    assertM(AmogusConverter(_.toUpdateClientRequest(companyChanged)))(equalTo(updateClientRequest))
      .provideCustomLayer(amogusConverter)
  }
}
