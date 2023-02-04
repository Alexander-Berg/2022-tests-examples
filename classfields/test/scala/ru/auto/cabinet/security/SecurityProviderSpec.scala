package ru.auto.cabinet.security

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import ru.auto.cabinet.AclResponse._
import ru.auto.cabinet.dao.entities.acl.AclResource.InvalidAclResourceException
import ru.auto.cabinet.dao.entities.acl.{
  AclGroup,
  AclGroupResource,
  AclResource
}
import ru.auto.cabinet.dao.jdbc.{
  AccessDao,
  AclDao,
  ClientUserDao,
  CompanyUserDao,
  JdbcClientDao,
  UpdateResult
}
import ru.auto.cabinet.model._
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.service.{AccessDeniedException, NotFoundException}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.when
import ru.auto.cabinet.trace.Context

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SecurityProviderSpec
    extends FlatSpec
    with Matchers
    with PropertyChecks
    with ScalaFutures {

  private val aclDao = mock[AclDao]
  private val accessDao = mock[AccessDao]
  private val clientDao = mock[JdbcClientDao]
  private val clientUserDao = mock[ClientUserDao]
  private val companyUserDao = mock[CompanyUserDao]

  implicit private val instr: Instr = new EmptyInstr("test")
  implicit private val rc = Context.unknown

  private val service =
    new SecurityProvider(
      aclDao,
      accessDao,
      clientDao,
      clientUserDao,
      companyUserDao)

  private val testUserId = 111L
  private val testClientId = 222L
  private val testAgencyId = 333L
  private val testCompanyId = 444L

  it should "authorize client" in {
    when(aclDao.userRoles(testUserId))
      .thenReturn(Future.successful(User(testUserId, Seq())))
    when(clientDao.checkIsAgent(testClientId))
      .thenReturn(Future.successful(false))
    when(clientDao.validateAgency(testClientId, testAgencyId))
      .thenReturn(Future.successful(false))
    when(clientDao.validateCompany(testClientId, testCompanyId))
      .thenReturn(Future.successful(false))
    when(clientUserDao.validate(testClientId, testUserId))
      .thenReturn(Future.successful(true))
    when(clientUserDao.clientIdByUser(testUserId)).thenReturn(
      Future.successful(Some(ClientUser(1L, testClientId, testUserId))))
    when(companyUserDao.companyIdByUser(testUserId))
      .thenReturn(Future.successful(None))

    val response =
      service.authorizeWithRole(testClientId, testUserId).futureValue

    response.get.roles.head shouldBe Role.Client
  }

  it should "authorize manager" in {
    when(aclDao.userRoles(testUserId))
      .thenReturn(Future.successful(User(testUserId, Seq(Role.Manager))))
    when(clientDao.checkIsAgent(testClientId))
      .thenReturn(Future.successful(false))
    when(clientDao.validateAgency(testClientId, testAgencyId))
      .thenReturn(Future.successful(false))
    when(clientDao.validateCompany(testClientId, testCompanyId))
      .thenReturn(Future.successful(false))
    when(clientUserDao.validate(testClientId, testUserId))
      .thenReturn(Future.successful(true))
    when(clientUserDao.clientIdByUser(testUserId)).thenReturn(
      Future.successful(Some(ClientUser(1L, testClientId, testUserId))))
    when(companyUserDao.companyIdByUser(testUserId))
      .thenReturn(Future.successful(None))

    val response =
      service.authorizeWithRole(testClientId, testUserId).futureValue

    response.get.roles.head shouldBe Role.Manager
  }

  it should "authorize agency" in {
    when(aclDao.userRoles(testUserId))
      .thenReturn(Future.successful(User(testUserId, Seq())))
    when(clientUserDao.clientIdByUser(testUserId)).thenReturn(
      Future.successful(Some(ClientUser(1L, testAgencyId, testUserId))))
    when(clientDao.checkIsAgent(testAgencyId))
      .thenReturn(Future.successful(true))
    when(clientDao.validateAgency(testClientId, testAgencyId))
      .thenReturn(Future.successful(true))
    when(clientDao.validateCompany(testClientId, testCompanyId))
      .thenReturn(Future.successful(false))
    when(clientUserDao.validate(testAgencyId, testUserId))
      .thenReturn(Future.successful(true))
    when(companyUserDao.companyIdByUser(testUserId))
      .thenReturn(Future.successful(None))

    val response =
      service.authorizeWithRole(testClientId, testUserId).futureValue

    response.get.roles.head shouldBe Role.Agency
  }

  it should "authorize company" in {
    when(aclDao.userRoles(testUserId))
      .thenReturn(Future.successful(User(testUserId, Seq())))
    when(clientUserDao.clientIdByUser(testUserId))
      .thenReturn(Future.successful(None))
    when(clientDao.validateCompany(testClientId, testCompanyId))
      .thenReturn(Future.successful(true))
    when(clientUserDao.validate(testCompanyId, testUserId))
      .thenReturn(Future.successful(true))
    when(companyUserDao.companyIdByUser(testUserId)).thenReturn(
      Future.successful(Some(CompanyUser(testCompanyId, testUserId))))
    when(companyUserDao.validate(testCompanyId, testUserId))
      .thenReturn(Future.successful(true))

    val response =
      service.authorizeWithRole(testClientId, testUserId).futureValue

    response.get.roles.head shouldBe Role.Company
  }

  it should "get access groups for customer type" in {
    val customerId = 20101L
    val customerType = CustomerTypes.Client

    val groupsGrants = Map(
      AclGroup(
        id = 1L,
        "Admins",
        CustomerTypes.Client,
        customerId = None,
        isEditable = false) ->
        List(
          Right(
            AclGroupResource(
              groupId = 1L,
              ResourceAlias.DASHBOARD,
              AccessLevel.READ_WRITE)),
          Right(
            AclGroupResource(
              groupId = 1L,
              ResourceAlias.WALLET,
              AccessLevel.READ_WRITE))
        ),
      AclGroup(
        id = 2L,
        "Custom",
        CustomerTypes.Client,
        Some(20101),
        isEditable = true) ->
        List(
          Right(
            AclGroupResource(
              groupId = 2L,
              ResourceAlias.DASHBOARD,
              AccessLevel.READ_ONLY)),
          Right(
            AclGroupResource(
              groupId = 2L,
              ResourceAlias.OFFERS,
              AccessLevel.READ_ONLY)),
          Left(InvalidAclResourceException("INVALID_RESOURCE"))
        )
    )

    when(accessDao.getCustomerGroupsGrants(20101L, CustomerTypes.Client))
      .thenReturn(Future.successful(groupsGrants))

    val resources = List(
      Right(
        AclResource(
          ResourceAlias.DASHBOARD,
          "Dashboard",
          "Dashboard resource")),
      Right(AclResource(ResourceAlias.OFFERS, "Offers", "Offers resource")),
      Right(AclResource(ResourceAlias.WALLET, "Wallet", "Wallet resource")),
      Left(InvalidAclResourceException("INVALID_ACL"))
    )

    when(accessDao.getAllResources())
      .thenReturn(Future.successful(resources))

    val expected =
      GroupsList
        .newBuilder()
        .addGroups {
          Group
            .newBuilder()
            .setId(1L)
            .setName("Admins")
            .setEditable(false)
            .addGrants {
              Resource
                .newBuilder()
                .setAlias(ResourceAlias.DASHBOARD)
                .setName("Dashboard")
                .setAccess(AccessLevel.READ_WRITE)
            }
            .addGrants {
              Resource
                .newBuilder()
                .setAlias(ResourceAlias.WALLET)
                .setName("Wallet")
                .setAccess(AccessLevel.READ_WRITE)
            }
        }
        .addGroups {
          Group
            .newBuilder()
            .setId(2L)
            .setName("Custom")
            .setEditable(true)
            .addGrants {
              Resource
                .newBuilder()
                .setAlias(ResourceAlias.DASHBOARD)
                .setName("Dashboard")
                .setAccess(AccessLevel.READ_ONLY)
            }
            .addGrants {
              Resource
                .newBuilder()
                .setAlias(ResourceAlias.OFFERS)
                .setName("Offers")
                .setAccess(AccessLevel.READ_ONLY)
            }
        }
        .build()

    service
      .getCustomerAccessGroups(customerId, customerType)
      .futureValue(timeout(Span(1, Seconds))) shouldBe expected
  }

  it should "get access group" in {
    val groupId = 2L

    val groupGrants = Map(
      AclGroup(
        groupId,
        "Custom",
        CustomerTypes.Client,
        Some(20101),
        isEditable = true) ->
        List(
          Right(
            AclGroupResource(
              groupId,
              ResourceAlias.SALON,
              AccessLevel.READ_ONLY)),
          Right(
            AclGroupResource(
              groupId,
              ResourceAlias.SALON_REQUISITES,
              AccessLevel.READ_ONLY))
        )
    )

    when(accessDao.getGroupGrants(groupId))
      .thenReturn(Future.successful(groupGrants))

    val resources = List(
      Right(AclResource(ResourceAlias.SALON, "Salon", "Salon resource")),
      Right(
        AclResource(
          ResourceAlias.SALON_REQUISITES,
          "Salon requisites",
          "Salon requisites resource"))
    )

    when(accessDao.getAllResources())
      .thenReturn(Future.successful(resources))

    val expected =
      Group
        .newBuilder()
        .setId(groupId)
        .setName("Custom")
        .setEditable(true)
        .addGrants {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.SALON)
            .setName("Salon")
            .setAccess(AccessLevel.READ_ONLY)
        }
        .addGrants {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.SALON_REQUISITES)
            .setName("Salon requisites")
            .setAccess(AccessLevel.READ_ONLY)
        }
        .build()

    service.getAccessGroup(groupId).futureValue shouldBe expected
  }

  it should "fail on group not found" in {
    when(accessDao.getGroupGrants(100L))
      .thenReturn(
        Future.successful(
          Map.empty[
            AclGroup,
            List[Either[InvalidAclResourceException, AclGroupResource]]]))

    service
      .getAccessGroup(100L)
      .failed
      .futureValue shouldBe a[NotFoundException]
  }

  it should "get access resources for customer type" in {
    val customerId = 20101L
    val customerType = CustomerTypes.Client

    val resources = List(Right(
      AclResource(ResourceAlias.DASHBOARD, "Dashboard", "Dashboard resource")))

    when(accessDao.getCustomerTypeResources(CustomerTypes.Client))
      .thenReturn(Future.successful(resources))

    val expected =
      ResourcesList
        .newBuilder()
        .addResources {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.DASHBOARD)
            .setName("Dashboard")
            .setDescription("Dashboard resource")
        }
        .build()

    service
      .getAccessResources(customerId, customerType)
      .futureValue shouldBe expected
  }

  it should "put customer access group" in {
    val customerId = 20101L
    val customerType = CustomerTypes.Client

    val group =
      Group
        .newBuilder()
        .setName("Customer group")
        .build()

    val updateResult = UpdateResult(id = 1L, rowCount = 1)

    when(accessDao.validateGroup(group))
      .thenReturn(Future.successful(()))

    when(accessDao.putCustomerGroup(20101L, CustomerTypes.Client, group))
      .thenReturn(Future.successful(updateResult))

    service
      .putCustomerAccessGroup(customerId, customerType, group)
      .futureValue shouldBe updateResult
  }

  it should "fail put customer access group on validation" in {
    val customerId = 20101L
    val customerType = CustomerTypes.Client

    val group =
      Group
        .newBuilder()
        .setName("Customer group")
        .build()

    when(accessDao.validateGroup(group))
      .thenReturn {
        Future.failed(new IllegalArgumentException("Validation failed"))
      }

    service
      .putCustomerAccessGroup(customerId, customerType, group)
      .failed
      .futureValue shouldBe a[IllegalArgumentException]
  }

  it should "delete customer access group" in {
    val customerId = 20101L
    val customerType = CustomerTypes.Client

    val groupId = 1L

    val group = AclGroup(
      groupId,
      "Custom group",
      CustomerTypes.Client,
      Some(customerId),
      isEditable = true)

    when(accessDao.getGroup(groupId))
      .thenReturn(Future.successful(Some(group)))

    val deleteResult = UpdateResult(groupId, rowCount = 1)

    when(accessDao.deleteGroup(groupId))
      .thenReturn(Future.successful(deleteResult))

    service
      .deleteCustomerAccessGroup(customerId, customerType, groupId)
      .futureValue shouldBe deleteResult
  }

  it should "delete system access group if editable" in {
    val customerId = 20101L
    val customerType = CustomerTypes.Client

    val groupId = 1L

    val group = AclGroup(
      groupId,
      "System group",
      CustomerTypes.Client,
      customerId = None,
      isEditable = true)

    when(accessDao.getGroup(groupId))
      .thenReturn(Future.successful(Some(group)))

    val deleteResult = UpdateResult(groupId, rowCount = 1)

    when(accessDao.deleteGroup(groupId))
      .thenReturn(Future.successful(deleteResult))

    service
      .deleteCustomerAccessGroup(customerId, customerType, groupId)
      .futureValue shouldBe deleteResult
  }

  it should "fail on delete unknown group" in {
    val customerId = 20101L
    val customerType = CustomerTypes.Client

    val groupId = 1L

    when(accessDao.getGroup(groupId))
      .thenReturn(Future.successful(None))

    service
      .deleteCustomerAccessGroup(customerId, customerType, groupId)
      .failed
      .futureValue shouldBe a[NotFoundException]
  }

  it should "fail on delete non editable group" in {
    val customerId = 20101L
    val customerType = CustomerTypes.Client

    val groupId = 1L

    val group = AclGroup(
      groupId,
      "Custom group",
      CustomerTypes.Client,
      Some(customerId),
      isEditable = false)

    when(accessDao.getGroup(groupId))
      .thenReturn(Future.successful(Some(group)))

    service
      .deleteCustomerAccessGroup(customerId, customerType, groupId)
      .failed
      .futureValue shouldBe an[AccessDeniedException]
  }

  it should "fail on delete group with customer type mismatch" in {
    val customerId = 20101L
    val customerType = CustomerTypes.Client

    val groupId = 1L

    val group = AclGroup(
      groupId,
      "Custom group",
      CustomerTypes.Agency,
      Some(customerId),
      isEditable = true)

    when(accessDao.getGroup(groupId))
      .thenReturn(Future.successful(Some(group)))

    service
      .deleteCustomerAccessGroup(customerId, customerType, groupId)
      .failed
      .futureValue shouldBe an[AccessDeniedException]
  }

  it should "fail on delete another customer's group" in {
    val customerId = 20101L
    val customerType = CustomerTypes.Client

    val groupId = 1L

    val group = AclGroup(
      groupId,
      "Custom group",
      CustomerTypes.Client,
      Some(1111L),
      isEditable = true)

    when(accessDao.getGroup(groupId))
      .thenReturn(Future.successful(Some(group)))

    service
      .deleteCustomerAccessGroup(customerId, customerType, groupId)
      .failed
      .futureValue shouldBe an[AccessDeniedException]
  }

}
