package ru.yandex.vertis.billing

import org.scalatest.Assertions
import ru.yandex.vertis.billing.balance.model.{Balance, ClientUser, OperatorId}
import ru.yandex.vertis.billing.dao.CustomerDao.HeaderFilter
import ru.yandex.vertis.billing.dao.CustomerDao.HeaderFilter.ForCustomerAndResourceRef
import ru.yandex.vertis.billing.model_core.gens.{AgencyGen, ClientGen, Producer}
import ru.yandex.vertis.billing.model_core.{AutoRuUid, Customer, CustomerHeader, Uid, User, UserResourceRef}
import ru.yandex.vertis.billing.service.RoleService.UserRole
import ru.yandex.vertis.billing.service.{AccessDenyException, CustomerService, RoleService}
import ru.yandex.vertis.billing.util.{OperatorContext, RequestContext}

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * Stuff for test security issues
  *
  * @author dimas
  */
package object security {

  import ru.yandex.vertis.mockito.MockitoSupport.{mock, stub}

  val SuperUserUid = Uid(1)
  val SuperUserAutoRuUid = AutoRuUid("123")

  val ClientUid = Uid(2)
  val AgencyUid = Uid(3)
  val OtherUid = Uid(4)
  val OtherClientUid = Uid(5)

  val AutoRuUser = AutoRuUid("456")

  val client = ClientGen.next

  val clientUser = ClientUser(
    ClientUid.id.toString,
    "alice@yandex.ru",
    Some("alice"),
    Some(client.id),
    isMain = true
  )

  val otherClient = ClientGen.next

  val otherClientUser = ClientUser(
    OtherClientUid.id.toString,
    "shon@yandex.ru",
    Some("shon"),
    Some(otherClient.id),
    isMain = true
  )

  val agency = AgencyGen.next

  val agencyUser = ClientUser(
    AgencyUid.id.toString,
    "bob@yandex.ru",
    Some("bob"),
    Some(agency.id),
    isMain = true
  )

  val autoruClient = ClientGen.next

  val autoruClientWithAgency = {
    val propWithAgency = autoruClient.properties.copy(agencyId = Some(agency.id))
    autoruClient.copy(properties = propWithAgency)
  }

  val rolesMock = {
    val m = mock[RoleService]
    stub(m.get(_: User)(_: RequestContext)) {
      case (SuperUserUid, OperatorContext("test", SuperUserUid, _, _)) =>
        Success(UserRole(SuperUserUid, RoleService.Roles.SuperUser))
      case (ClientUid, OperatorContext("test", ClientUid, _, _)) =>
        Success(UserRole(ClientUid, RoleService.Roles.RegularUser))
      case (AgencyUid, OperatorContext("test", AgencyUid, _, _)) =>
        Success(UserRole(AgencyUid, RoleService.Roles.RegularUser))
      case (OtherUid, OperatorContext("test", OtherUid, _, _)) =>
        Success(UserRole(OtherUid, RoleService.Roles.RegularUser))
      case (OtherClientUid, OperatorContext("test", OtherClientUid, _, _)) =>
        Success(UserRole(OtherClientUid, RoleService.Roles.RegularUser))
      case (SuperUserAutoRuUid, OperatorContext("test", SuperUserAutoRuUid, _, _)) =>
        Success(UserRole(OtherClientUid, RoleService.Roles.SuperUser))
      case (AutoRuUser, OperatorContext("test", AutoRuUser, _, _)) =>
        Success(UserRole(AutoRuUser, RoleService.Roles.RegularUser))
    }

    m
  }

  val balanceMock = {
    implicit def success[T](opt: Option[T]): Try[Option[T]] = Success(opt)

    def asParams(uid: Uid, operator: Uid): (String, OperatorId) =
      (uid.id.toString, operator.id.toString)

    val m = mock[Balance]

    stub(m.getPassportByUid(_: String)(_: OperatorId))(
      Map[(String, OperatorId), Try[Option[ClientUser]]](
        asParams(ClientUid, ClientUid) -> Some(clientUser),
        asParams(AgencyUid, AgencyUid) -> Some(agencyUser),
        asParams(OtherUid, OtherUid) -> None,
        asParams(OtherClientUid, OtherClientUid) -> Some(otherClientUser),
        asParams(ClientUid, SuperUserUid) -> Some(clientUser),
        asParams(AgencyUid, SuperUserUid) -> Some(agencyUser),
        asParams(OtherUid, SuperUserUid) -> None,
        asParams(OtherClientUid, SuperUserUid) -> Some(otherClientUser)
      )
    )

    stub(m.getClientsById _)(
      Map(
        client.id -> Success(Some(client)),
        agency.id -> Success(Some(agency)),
        otherClient.id -> Success(Some(otherClient))
      )
    )

    m
  }

  val customersMock = {
    val m = mock[CustomerService]

    val autoruCustomer = Customer(
      header = CustomerHeader(autoruClient, UserResourceRef(AutoRuUser)),
      client = autoruClient,
      agency = None
    )

    val autoruCustomerWithAgency = Customer(
      header = CustomerHeader(autoruClientWithAgency, UserResourceRef(AutoRuUser)),
      client = autoruClientWithAgency,
      agency = None
    )

    val expected = Set(autoruCustomer, autoruCustomerWithAgency)

    stub(m.get(_: model_core.CustomerId)(_: RequestContext)) { case (customerId, _) =>
      expected.find(_.id == customerId) match {
        case Some(value) => Success(value)
        case None => Failure(new NoSuchElementException(s"There is no customer with $customerId"))
      }
    }

    m
  }

  val securityProviderMock = new SecurityProviderImpl(
    rolesMock,
    Some(balanceMock),
    Some(customersMock)
  )

  def expectDeny[A](f: => A) = Assertions.intercept[AccessDenyException] {
    f
  }

  implicit def uid2operatorContext(uid: Uid): OperatorContext = OperatorContext("test", uid)
}
