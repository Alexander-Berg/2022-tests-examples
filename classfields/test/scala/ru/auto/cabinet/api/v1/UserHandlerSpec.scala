package ru.auto.cabinet.api.v1

import org.scalatest.OneInstancePerTest
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import ru.auto.cabinet.ApiModel.{Customer, CustomerPresets, CustomerType}
import ru.auto.cabinet.dao.entities.BalanceOrderIds
import ru.auto.cabinet.api.v1.view.BalanceOrderMarshaller._
import ru.auto.cabinet.security.SecurityProvider
import ru.auto.cabinet.service.ClientService
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.cabinet.test.TestUtil.RichOngoingStub

class UserHandlerSpec
    extends FlatSpec
    with HandlerSpecTemplate
    with OneInstancePerTest {

  private val auth = new SecurityMocks

  import auth._

  private val clientService = mock[ClientService]
  private val securityProvider = mock[SecurityProvider]

  private val route = wrapRequestMock {
    new UserHandler(securityProvider, clientService).route
  }

  val balanceIds = BalanceOrderIds(
    orderId = 1L,
    balanceClientId = 1L,
    clientId = 1L,
    agencyId = 1L
  )

  val customer = Customer
    .newBuilder()
    .setId(user1.userId)
    .setCustomerType(CustomerType.MANAGER)
    .build()

  "Get /client/user/{userId}" should "return client order" in {
    when(clientService.getClientOrderByUser(eq(user1.userId))(any()))
      .thenReturnF(balanceIds)

    Get(s"/client/user/${user1.userId}") ~> route ~> check {
      responseAs[BalanceOrderIds] shouldBe balanceIds
    }
  }

  "Get /user/{userId}/customer" should "return customer" in {
    when(securityProvider.getCustomer(eq(user1.userId))(any()))
      .thenReturnF(Some(customer))

    Get(s"/user/${user1.userId}/customer") ~> route ~> check {
      import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._
      responseAs[Customer] shouldBe customer
    }
  }

  "GET /user/{userId}/customer/clients/presets" should "return clients counters" in {
    when(securityProvider.getCustomer(eq(user1.userId))(any()))
      .thenReturnF(Some(customer))
    when(clientService.getCustomerPresets(eq(customer))(any()))
      .thenReturnF(CustomerPresets.getDefaultInstance)

    Get(s"/user/${user1.userId}/customer/clients/presets") ~> route ~> check {
      import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._
      responseAs[CustomerPresets] shouldBe (CustomerPresets.getDefaultInstance)
    }
  }
}
