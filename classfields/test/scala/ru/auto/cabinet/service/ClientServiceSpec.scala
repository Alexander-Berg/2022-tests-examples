package ru.auto.cabinet.service

import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

import com.google.protobuf.UInt64Value
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import auto.common.Pagination.{RequestPagination, ResponsePagination}
import ru.auto.amoyak.InternalServiceModel.AmoyakDto
import ru.auto.cabinet.environment._
import ru.auto.api.ResponseModel.DealerOverdraft
import ru.auto.cabinet.ApiModel.{
  AmoCreateClientRequest,
  AmoUpdateClientRequest,
  FindClientsRequest,
  FindClientsResponse
}
import ru.auto.cabinet.dao.entities.{
  BalanceClient,
  ClientOverdraftChecklist,
  ClientOverdraftResolution,
  RegionOutcome
}
import ru.auto.cabinet.dao.jdbc.{
  BalanceDao,
  ClientUserDao,
  ClientsChangedBufferDao,
  CompanyDao,
  CustomerDiscountDao,
  CustomerStockDao,
  JdbcClientDao,
  JdbcPoiDataDao,
  JdbcUpdateClientDao,
  ManagerDao,
  ManagerInternalRecord,
  NotFoundError,
  SubscriptionDao,
  YaBalanceRegistrationBufferDao
}
import ru.auto.cabinet.{environment, ApiModel}
import ru.auto.cabinet.model.{CustomerId => _, _}
import ru.auto.cabinet.multiposting.MultiPostingEventSender
import ru.auto.cabinet.service.ClientService.DefaultOutcomeForOverdraft
import ru.auto.cabinet.service.amoyak.AmoyakDtoEnricher
import ru.auto.cabinet.service.cabinet_php.{
  CabinetPhpClient,
  CreateClientRequest,
  CreateClientResponse
}
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.service.passport.{User => PassportUser, _}
import ru.auto.cabinet.service.telepony.{
  Domains,
  PhoneRedirectInfo,
  PhonesRedirectsService
}
import ru.auto.cabinet.test.TestUtil._
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import org.scalatest.Outcome
import ru.auto.cabinet.ApiModel.FindClientsResponse.ClientDto
import ru.auto.cabinet.trace.Context

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class ClientServiceSpec
    extends FixtureAnyFlatSpec
    with Matchers
    with PropertyChecks
    with ScalaFutures {

  implicit private val rc = Context.unknown
  private def ?[T]: T = any()

  implicit private val instr: Instr = new EmptyInstr("test")

  case class FixtureParam(
      service: ClientService,
      clientDao: JdbcClientDao,
      clientUserDao: ClientUserDao,
      updateClientDao: JdbcUpdateClientDao,
      poiDao: JdbcPoiDataDao,
      managerDao: ManagerDao,
      balanceDao: BalanceDao,
      subscriptionDao: SubscriptionDao,
      customerStockDao: CustomerStockDao,
      customerDiscountDao: CustomerDiscountDao,
      companyDao: CompanyDao,
      phonesRedirectsService: PhonesRedirectsService,
      geoDataSource: GeoDataSource,
      passportClient: PassportClient,
      clientsChangedBufferDao: ClientsChangedBufferDao,
      multiPostingEventSender: MultiPostingEventSender,
      amoyakDtoEnricher: AmoyakDtoEnricher,
      cabinetPhpClient: CabinetPhpClient,
      yaBalanceRegistrationBufferDao: YaBalanceRegistrationBufferDao)

  override protected def withFixture(test: OneArgTest): Outcome = {
    val clientDao = mock[JdbcClientDao]
    val clientUserDao = mock[ClientUserDao]
    val updateClientDao = mock[JdbcUpdateClientDao]
    val poiDao = mock[JdbcPoiDataDao]
    val managerDao = mock[ManagerDao]
    val balanceDao = mock[BalanceDao]
    val subscriptionDao = mock[SubscriptionDao]
    val customerStockDao = mock[CustomerStockDao]
    val customerDiscountDao = mock[CustomerDiscountDao]
    val companyDao = mock[CompanyDao]
    val phonesRedirectsService = mock[PhonesRedirectsService]
    val geoDataSource = mock[GeoDataSource]
    val passportClient = mock[PassportClient]
    val clientsChangedBufferDao = mock[ClientsChangedBufferDao]
    val multiPostingEventSender = mock[MultiPostingEventSender]
    val amoyakDtoEnricher = mock[AmoyakDtoEnricher]
    val cabinetPhpClient = mock[CabinetPhpClient]
    val yaBalanceRegistrationBufferDao = mock[YaBalanceRegistrationBufferDao]

    val service = new ClientService(
      passportClient,
      clientDao,
      poiDao,
      clientUserDao,
      managerDao,
      balanceDao,
      subscriptionDao,
      customerStockDao,
      customerDiscountDao,
      companyDao,
      phonesRedirectsService,
      geoDataSource,
      multiPostingEventSender,
      clientsChangedBufferDao,
      updateClientDao,
      amoyakDtoEnricher,
      cabinetPhpClient,
      yaBalanceRegistrationBufferDao
    )

    val fixture = FixtureParam(
      service,
      clientDao,
      clientUserDao,
      updateClientDao,
      poiDao,
      managerDao,
      balanceDao,
      subscriptionDao,
      customerStockDao,
      customerDiscountDao,
      companyDao,
      phonesRedirectsService,
      geoDataSource,
      passportClient,
      clientsChangedBufferDao,
      multiPostingEventSender,
      amoyakDtoEnricher,
      cabinetPhpClient,
      yaBalanceRegistrationBufferDao
    )

    test(fixture)
  }

  private val testClientId = 222L
  private val testCompanyId = 1L
  private val testBalanceClientId = 4444444L
  private val testParentRegionId = 1L
  private val testCityId = 1L
  private val testUserId = "1"
  private val testWebsite = Some("website.test")

  private val testUser = PassportUser(
    id = testUserId,
    profile = Profile(autoru = AutoruProfile(alias = None)),
    emails = None
  )

  private val testClientProperties = ClientProperties(
    testParentRegionId,
    testCityId,
    "test",
    ClientStatuses.Active,
    environment.now,
    "",
    testWebsite,
    "test@yandex.ru",
    Some("manager@yandex.ru"),
    None,
    None,
    multipostingEnabled = true,
    callsAuctionAvailable = true,
    firstModerated = true,
    isAgent = false
  )

  private val testClient = Client(
    testClientId,
    0L,
    testClientProperties
  )

  private val testCompany =
    Company(testCompanyId, "Test company", OffsetDateTime.now())

  private val testBalanceClient = BalanceClient(
    testBalanceClientId,
    Some(7777777L),
    None,
    Some(1L),
    None,
    None,
    None,
    None,
    None,
    None,
    None)

  private val detailedClient = DetailedClient(
    testClientId,
    Some("Test dealer"),
    isAgent = false,
    Some(1L),
    Some("Test agency"),
    Some(2L),
    Some("Test company"),
    testClientProperties
  )

  val testRegionName = "test region name"
  val testRegionId = 10000
  val testCountryId = 10

  val testGeoRecord = GeoBaseRecord(
    id = testParentRegionId,
    parentId = testParentRegionId,
    regionId = testRegionId,
    name = testRegionName,
    countryId = testCountryId)

  "ClientService.getOverdraft()" should "get overdraft with default region outcome gt" in {
    f =>
      when(f.clientDao.get(testClientId))
        .thenReturn(Future.successful(testClient))
      when(f.clientDao.getClientOverdraftResolution(testClientId))
        .thenReturn(Future.successful(None))
      when(f.clientDao.getRegionAvgOutcome(testClient.properties.regionId))
        .thenReturn(Future.successful(None))

      val clientOutcome = (DefaultOutcomeForOverdraft + 100) / 30
      val response =
        f.service.getClientOverdraft(testClientId, clientOutcome).futureValue

      response shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(true)
        .build()
  }

  "ClientService.getOverdraft()" should "get overdraft with default region outcome lt" in {
    f =>
      when(f.clientDao.get(testClientId))
        .thenReturn(Future.successful(testClient))
      when(f.clientDao.getClientOverdraftResolution(testClientId))
        .thenReturn(Future.successful(None))
      when(f.clientDao.getRegionAvgOutcome(testClient.properties.regionId))
        .thenReturn(Future.successful(None))

      val clientOutcome = (DefaultOutcomeForOverdraft - 100) / 30
      val response =
        f.service.getClientOverdraft(testClientId, clientOutcome).futureValue

      response shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(false)
        .build()
  }

  "ClientService.getManagerInternal()" should "get manager internal" in { f =>
    val manager = ManagerInternalRecord(id = 1, userId = 2)
    when(f.managerDao.getManagerWithoutFio(testClientId))
      .thenReturnF(Some(manager))
    when(f.passportClient.getUserFullName(manager.userId))
      .thenReturnF(Some("Test Testov"))

    f.service.getManagerInternal(testClientId).futureValue shouldBe
      Some(manager.copy(fio = Some("Test Testov")))
  }

  "ClientService.getDetailed()" should "get detailed client" in { f =>
    when(f.clientDao.getDetailed(testClientId))
      .thenReturn(Future.successful(detailedClient))
    when(f.geoDataSource.getRegions(Set(testParentRegionId)))
      .thenReturnF(Map(testParentRegionId -> testGeoRecord))

    f.service.getDetailed(testClientId).futureValue shouldBe detailedClient
      .asProto(Some(testGeoRecord.name))
  }

  "ClientService.getOverdraft()" should "get overdraft with region outcome gt" in {
    f =>
      val regionAvgOutcome = RegionOutcome(1L, 1000.0, 30)

      when(f.clientDao.get(testClientId))
        .thenReturn(Future.successful(testClient))
      when(f.clientDao.getClientOverdraftResolution(testClientId))
        .thenReturn(Future.successful(None))
      when(f.clientDao.getRegionAvgOutcome(testClient.properties.regionId))
        .thenReturn(Future.successful(Some(regionAvgOutcome)))

      val clientOutcome = (regionAvgOutcome.avgOutcome + 100) / 30
      val response =
        f.service.getClientOverdraft(testClientId, clientOutcome).futureValue

      response shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(true)
        .build()
  }

  "ClientService.getOverdraft()" should "get overdraft with region outcome lt" in {
    f =>
      val regionAvgOutcome = RegionOutcome(1L, 1000.0, 30)

      when(f.clientDao.get(testClientId))
        .thenReturn(Future.successful(testClient))
      when(f.clientDao.getClientOverdraftResolution(testClientId))
        .thenReturn(Future.successful(None))
      when(f.clientDao.getRegionAvgOutcome(testClient.properties.regionId))
        .thenReturn(Future.successful(Some(regionAvgOutcome)))

      val clientOutcome = (regionAvgOutcome.avgOutcome - 100) / 30
      val response =
        f.service.getClientOverdraft(testClientId, clientOutcome).futureValue

      response shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(false)
        .build()
  }

  "ClientService.getOverdraft()" should "get overdraft with whitelist resolution" in {
    f =>
      val overdraftResolution = ClientOverdraftResolution(
        testClientId,
        ClientOverdraftChecklist.ChecklistStatuses.Whitelist)

      when(f.clientDao.get(testClientId))
        .thenReturn(Future.successful(testClient))
      when(f.clientDao.getClientOverdraftResolution(testClientId))
        .thenReturn(Future.successful(Some(overdraftResolution)))
      when(f.clientDao.getRegionAvgOutcome(testClient.properties.regionId))
        .thenReturn(Future.successful(None))

      val response = f.service.getClientOverdraft(testClientId, 0.0).futureValue

      response shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(true)
        .build()
  }

  "ClientService.getOverdraft()" should "get overdraft with blacklist resolution" in {
    f =>
      val overdraftResolution = ClientOverdraftResolution(
        testClientId,
        ClientOverdraftChecklist.ChecklistStatuses.Blacklist)

      when(f.clientDao.get(testClientId))
        .thenReturn(Future.successful(testClient))
      when(f.clientDao.getClientOverdraftResolution(testClientId))
        .thenReturn(Future.successful(Some(overdraftResolution)))
      when(f.clientDao.getRegionAvgOutcome(testClient.properties.regionId))
        .thenReturn(Future.successful(None))

      val response =
        f.service.getClientOverdraft(testClientId, 10000000.0).futureValue

      response shouldBe DealerOverdraft
        .newBuilder()
        .setAllowed(false)
        .build()
  }

  "ClientService.getLastOverdraftInvoiceId()" should "return invoice id" in {
    f =>
      val invoiceId = 10000000L

      when(f.balanceDao.getBalanceClient(testClientId)(operateOnMaster = false))
        .thenReturn(Future.successful(testBalanceClient))
      when(f.balanceDao.getLastClientOverdraftInvoiceId(testBalanceClientId))
        .thenReturn(Future.successful(Some(invoiceId)))

      val response =
        f.service
          .getLastClientOverdraftInvoice(testClientId)
          .futureValue

      response shouldBe Some(
        DealerOverdraft
          .newBuilder()
          .setInvoiceId(invoiceId)
          .build())
  }

  "ClientService.getClientCallsInfo()" should "return teleponyInfo" in { f =>
    when(f.phonesRedirectsService.getInfo(?, ?, ?, ?, ?)(?))
      .thenReturn(
        Future.successful(
          Some(
            PhoneRedirectInfo(
              Domains.AutoDealersAuction,
              "123",
              FiniteDuration(1, TimeUnit.SECONDS)))))
    when(f.clientDao.get(?)(?))
      .thenReturn(Future.successful(testClient))
    when(f.balanceDao.getBalanceClient(?)(?)(?))
      .thenReturn(Future.successful(testBalanceClient))
    when(f.clientDao.clientToPoi(?)(?))
      .thenReturn(Future.successful(1L))
    when(f.poiDao.get(1L))
      .thenReturn(Future.successful(
        Some(PoiData(1, Location(1, None), None, properties = None))))

    val response =
      f.service
        .getClientCallsInfo(1, None, None)
        .futureValue

    response.hasTeleponyInfo shouldBe true

  }

  "ClientService.getDetailedBatch" should "return enriched clients" in { f =>
    val notFoundRegionId = -1L
    val invalidRegionProperties =
      detailedClient.clientProperties.copy(regionId = notFoundRegionId)

    val clients = Map(
      1L -> detailedClient.copy(
        id = 1L,
        clientProperties = invalidRegionProperties
      ), //without correct region
      3L -> detailedClient.copy(id = 3L) //correct
    )

    val geos = Map(
      testParentRegionId -> testGeoRecord
    )

    when(f.clientDao.getDetailedBatch(?)(?)).thenReturnF(clients.values.toList)

    when(f.passportClient.getClientUsers(?, ?)(?)).thenReturnF {
      ClientUsersResponse(
        userIds = Some(List("1", "2")),
        users = Some(
          List(
            testUser.copy(
              id = "1",
              emails = Some(List(Email("test1@auto.ru")))
            ),
            testUser.copy(
              id = "2",
              emails = Some(List(Email("test2@auto.ru")))
            )
          ))
      )
    }

    when(f.geoDataSource.getRegions(clients.map { case (_, v) =>
      v.clientProperties.regionId
    }.toSet)).thenReturnF(geos)

    val response = f.service.getDetailedBatch(clients.keySet.toSeq).futureValue

    val responseClients = response.getClientsList.asScala

    responseClients.size shouldBe 2
    responseClients.count(
      _.getProperties.getRegionName == testRegionName) shouldBe 1
    responseClients
      .filter(_.getProperties.getRegionName == testRegionName)
      .map(_.getId)
      .head shouldBe 3L
  }

  "ClientService.amoCreateClient" should "return clientId for new client" in {
    f =>
      val email = Email("123")
      val phone = Phone("321")
      val sessionId = "456"
      val clientId = 789
      val yaCityId = 1L
      val yaRegionId = 2L
      val yaCountryId = 3L
      val responsibleManagerEmail = "resp@mail.mail"

      val amoCreateClientRequest = AmoCreateClientRequest.newBuilder
        .setName("name")
        .setEmail(email.email)
        .setPhone(phone.phone)
        .setContactName("contact name")
        .setYaCityId(yaCityId)
        .setYaRegionId(yaRegionId)
        .setYaCountryId(yaCountryId)
        .setResponsibleManagerEmail(responsibleManagerEmail)
        .build

      val createClientRequest =
        CreateClientRequest(
          "name",
          email.email,
          phone.phone,
          "contact name",
          responsibleManagerEmail,
          yaCityId,
          yaRegionId,
          yaCountryId)

      val responsibleEmailUpdate =
        ClientUpdate(
          clientId = clientId,
          agencyId = None,
          companyId = None,
          responsibleManagerEmail = Some(responsibleManagerEmail),
          comment = None,
          modifiedManagerEmail = None
        )

      when(f.passportClient.getSession(Left(email)))
        .thenReturn(Future.successful(
          SessionResponse(Session(sessionId, "2"), UserEssentials(None))))

      when(f.cabinetPhpClient.create(createClientRequest, sessionId))
        .thenReturn(Future.successful(CreateClientResponse(clientId)))

      when(f.updateClientDao.update(responsibleEmailUpdate))
        .thenReturn(Future.successful(()))

      f.service
        .amoCreateClient(amoCreateClientRequest)
        .futureValue
        .getClientId shouldBe clientId.toString
  }

  "ClientService.amoCreateClient" should "fail for existing client" in { f =>
    val email = Left(Email("123"))

    val amoCreateClientRequest = AmoCreateClientRequest.newBuilder
      .setName("name")
      .setEmail(email.value.email)
      .setContactName("contact name")
      .setYaCityId(1L)
      .build

    when(f.passportClient.getSession(email))
      .thenReturn(Future.successful(
        SessionResponse(Session("1", "2"), UserEssentials(Some("3")))))

    f.service
      .amoCreateClient(amoCreateClientRequest)
      .failed
      .futureValue shouldBe an[AlreadyExistException]
  }

  "ClientService.amoCreateClient" should "fail without cityId" in { f =>
    val email = Left(Email("123"))

    val amoCreateClientRequest = AmoCreateClientRequest.newBuilder
      .setName("name")
      .setEmail(email.value.email)
      .setContactName("contact name")
      .build

    when(f.passportClient.getSession(email))
      .thenReturn(Future.successful(
        SessionResponse(Session("1", "2"), UserEssentials(None))))

    f.service
      .amoCreateClient(amoCreateClientRequest)
      .failed
      .futureValue shouldBe an[IllegalArgumentException]
  }

  "ClientService.amoUpdateClient" should "call clientDao" in { f =>
    val clientUpdate = ClientUpdate(1L, Some(2L), None, None, None, None)

    val amoUpdateClientRequest = AmoUpdateClientRequest.newBuilder
      .setClientId(1L)
      .setAgencyId(UInt64Value.newBuilder.setValue(2L))
      .build

    when(f.updateClientDao.update(clientUpdate))
      .thenReturn(Future.successful(()))

    f.service
      .amoUpdateClient(amoUpdateClientRequest)
      .futureValue shouldBe ((): Unit)
  }

  "ClientService.getAmoClient" should "call enricher" in { f =>
    when(f.clientDao.getDetailed(testClientId))
      .thenReturn(Future.successful(detailedClient))

    when(f.amoyakDtoEnricher.toAmoyakDto(detailedClient))
      .thenReturn(Future.successful(AmoyakDto.getDefaultInstance))

    f.service
      .getAmoClient(testClientId)
      .futureValue shouldBe AmoyakDto.getDefaultInstance
  }

  "ClientService.getAmoCompany" should "return proto company" in { f =>
    when(f.companyDao.findOne(testCompanyId)).thenReturnF(Some(testCompany))
    when(f.clientDao.companyClients(testCompanyId)).thenReturnF(Seq(testClient))

    f.service
      .getAmoCompany(testCompanyId)
      .futureValue shouldBe testCompany.asProto(Seq(testClient))
  }

  "ClientService.getClientsCompany" should "return proto company" in { f =>
    when(f.companyDao.findByClientId(testClientId))
      .thenReturnF(Some(testCompany))
    when(f.clientDao.companyClients(testCompanyId)).thenReturnF(Seq(testClient))

    f.service
      .getClientsCompany(testClientId)
      .futureValue shouldBe testCompany.asProto(Seq(testClient))
  }

  "ClientService.getClientsCompany" should "fail if client or company is not found" in {
    f =>
      when(f.companyDao.findByClientId(testClientId)).thenReturnF(None)

      f.service
        .getClientsCompany(testClientId)
        .failed
        .futureValue shouldBe an[NotFoundError]
  }

  "ClientService.findClients" should "return paginated client list" in { f =>
    val request = FindClientsRequest
      .newBuilder()
      .setFilter(
        FindClientsRequest.Filter
          .newBuilder()
          .setCompanyId(UInt64Value.of(1))
          .build())
      .setPagination(
        RequestPagination
          .newBuilder()
          .setPage(1)
          .setPageSize(1)
          .build())
      .build()

    val phone = PoiPhone(1, "title", 1112223344L, "mask", 1, 10)
    val clientDto = ClientDto
      .newBuilder()

    clientDto
      .setId(testClient.clientId)
      .setOrigin(testClientProperties.originId)
      .setStatus(testClientProperties.status)
      .addUsers(
        ClientDto.User
          .newBuilder()
          .setId(1L)
          .setEmail("test@auto.ru")
          .build())
      .addPhones(
        ClientDto.Phone
          .newBuilder()
          .setCallFrom(phone.callFrom)
          .setCallTill(phone.callTill)
          .setContactName(phone.title)
          .setPhone(phone.phone)
          .setPhoneMask(phone.phoneMask)
          .build())

    testClientProperties.name
      .foreach(clientDto.setName)
    testClientProperties.paidTill.foreach(odt =>
      clientDto.setPaidTill(odt.asProtoTimestamp()))
    testClientProperties.createdDate.foreach(odt =>
      clientDto.setCreateDate(odt.asProtoTimestamp()))

    val expected = FindClientsResponse
      .newBuilder()
      .addClients(clientDto.build())
      .setPagination(
        ResponsePagination
          .newBuilder()
          .setPageNum(1)
          .setPageSize(1)
          .setTotalPageCount(11)
          .setTotalCount(11)
          .build())
      .build()

    when(f.clientDao.clientToPoi(testClient.clientId)).thenReturnF(1L)
    when(f.poiDao.getPhones(1L))
      .thenReturnF(List(phone))
    when(f.clientDao.countClients(request)).thenReturnF(11)
    when(f.clientDao.findClients(request)).thenReturnF(Seq(testClient))

    when(f.companyDao.findByClientId(testClientId)).thenReturnF(None)

    when(f.passportClient.getClientUsers(?, ?)(?)).thenReturnF {
      ClientUsersResponse(
        userIds = Some(List(testUserId)),
        users = Some(
          List(
            testUser.copy(
              id = testUserId,
              emails = Some(List(Email("test@auto.ru")))
            )
          )))
    }

    f.service
      .findClientsPaginated(request)
      .futureValue shouldBe expected
  }

  "ClientService.getCustomerPresets" should "return presets-facets proto" in {
    f =>
      val clientService = f.service
      val customer = ApiModel.Customer
        .newBuilder()
        .setId(888)
        .setCustomerType(ApiModel.CustomerType.COMPANY_GROUP)
        .build()

      val req = FindClientsRequest
        .newBuilder()
        .setFilter(
          FindClientsRequest.Filter
            .newBuilder()
            .setCompanyId(UInt64Value.of(888)))
        .build()

      import scala.jdk.CollectionConverters._
      val expectedPresets = {

        val facets = ApiModel.CustomerPresets.Facets
          .newBuilder()
          .setAll(2)
          .setActive(1)

        val presets = Seq(
          ApiModel.CustomerPresets.Preset
            .newBuilder()
            .setName("Активные")
            .setFacet("active")
            .setCount(1)
            .putAllParams(Map("active" -> int2Integer(1)).asJava),
          ApiModel.CustomerPresets.Preset
            .newBuilder()
            .setName("Замороженные")
            .setFacet("freezed")
            .putAllParams(Map("freezed" -> int2Integer(1)).asJava),
          ApiModel.CustomerPresets.Preset
            .newBuilder()
            .setName("Остановленные")
            .setFacet("stopped")
            .putAllParams(Map("stopped" -> int2Integer(1)).asJava),
          ApiModel.CustomerPresets.Preset
            .newBuilder()
            .setName("Все")
            .setFacet("all")
            .setCount(2)
            .putAllParams(Map("all" -> int2Integer(1)).asJava)
        ).map(_.build())
        ApiModel.CustomerPresets
          .newBuilder()
          .setFacets(facets)
          .addAllPresets(presets.asJava)
          .build
      }

      when(f.clientDao.getCustomerPresets(req)).thenReturnF(
        Seq(
          StatusCounter(ClientStatuses.New, 1),
          StatusCounter(ClientStatuses.Active, 1)))
      clientService
        .getCustomerPresets(customer)
        .futureValue shouldBe expectedPresets
  }
}
