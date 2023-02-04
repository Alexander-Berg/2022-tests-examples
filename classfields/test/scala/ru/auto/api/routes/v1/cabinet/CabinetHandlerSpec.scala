package ru.auto.api.routes.v1.cabinet

import akka.http.scaladsl.model.StatusCodes.OK
import com.google.protobuf.{BoolValue, StringValue, Timestamp, UInt64Value}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.model.{AutoruUser, DealerUserRoles, RequestParams}
import ru.auto.api.model.ModelGenerators.{DealerAccessGroupGen, DealerSessionResultGen, DealerUserEssentialsGen}
import ru.auto.api.services.MockedClients
import ru.auto.api.services.cabinet.CheckAccessView
import ru.auto.api.util.{Request, RequestImpl}
import org.mockito.Mockito._
import ru.auto.api.auth.Application
import ru.auto.cabinet.ApiModel.{Customer, CustomerType, FindClientsRequest, FindClientsResponse}
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.vertis.tracing.Traced

class CabinetHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {
  private val checkAccessClientView = CheckAccessView(role = DealerUserRoles.Client)

  import CabinetHandlerSpec._

  val request: ApiModel.SessionResult => Request = session => {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.desktop)
    r.setUser(AutoruUser(session.getUser.getId.toLong))
    r.setTrace(Traced.empty)
    r
  }

  "GET /cabinet/agency/dealers" should {
    "return clients for agency" in {
      forAll(DealerSessionResultGen) { session =>

        val user = request(session).user.userRef.asPrivate

        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)

        val customer = Customer
          .newBuilder()
          .setId(user.uid)
          .setCustomerType(CustomerType.AGENCY)
          .build()

        val updatedFilter = {
          val filterBuilder = CabinetHandlerSpec.filterClientRequest.toBuilder
          val f = filterBuilder.getFilterBuilder

          f.setAgencyId(UInt64Value.of(customer.getId))

          filterBuilder.build()
        }

        when(cabinetApiClient.getUserCabinetCustomer(?)(?)).thenReturnF(customer)
        when(cabinetApiClient.findClients(?)(?)).thenReturnF(findClientsResponse)

        Post("/1.0/cabinet/agency/dealers", CabinetHandlerSpec.filterClientRequest) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
              responseAs[FindClientsResponse] shouldBe findClientsResponse
            }
          }

        verify(cabinetApiClient).getUserCabinetCustomer(eq(user))(?)
        verify(cabinetApiClient).findClients(eq(updatedFilter))(?)
      }
    }
  }

  "GET /cabinet/company/dealers" should {
    "return clients for company" in {
      forAll(DealerSessionResultGen) { session =>

        val user = request(session).user.userRef.asPrivate

        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)

        val customer = Customer
          .newBuilder()
          .setId(user.uid)
          .setCustomerType(CustomerType.COMPANY_GROUP)
          .build()

        val updatedFilter = {
          val filterBuilder = CabinetHandlerSpec.filterClientRequest.toBuilder
          val f = filterBuilder.getFilterBuilder

          f.setCompanyId(UInt64Value.of(customer.getId))

          filterBuilder.build()
        }

        when(cabinetApiClient.getUserCabinetCustomer(?)(?)).thenReturnF(customer)
        when(cabinetApiClient.findClients(?)(?)).thenReturnF(findClientsResponse)

        Post("/1.0/cabinet/company/dealers", CabinetHandlerSpec.filterClientRequest) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
              responseAs[FindClientsResponse] shouldBe findClientsResponse
            }
          }

        verify(cabinetApiClient).getUserCabinetCustomer(eq(user))(?)
        verify(cabinetApiClient).findClients(eq(updatedFilter))(?)
      }
    }
  }
}

object CabinetHandlerSpec {

  val filter = FindClientsRequest.Filter
    .newBuilder()
    .setMultipostingEnabled(BoolValue.of(false))
    .setOrigin(StringValue.of("origin"))
    .setPreset(FindClientsRequest.Filter.Preset.ALL)
    .build()

  val sorting = FindClientsRequest.Sorting
    .newBuilder()
    .setSortingField(FindClientsRequest.Sorting.SortingField.ORIGIN)
    .setSortingOrder(FindClientsRequest.Sorting.SortingOrder.ASC)

  val filterClientRequest = FindClientsRequest
    .newBuilder()
    .setFilter(filter)
    .setSorting(sorting)
    .build()

  val phone = FindClientsResponse.ClientDto.Phone
    .newBuilder()
    .setContactName("contact name")
    .setPhone(71234567890L)
    .setPhoneMask("1:3:6")
    .setCallFrom(123456)
    .setCallTill(123654)

  val clientDto = FindClientsResponse.ClientDto
    .newBuilder()
    .setName("name")
    .setId(1L)
    .setOrigin("origin")
    .setCreateDate(Timestamp.getDefaultInstance)
    .setPaidTill(Timestamp.getDefaultInstance)
    .addPhones(phone)

  val findClientsResponse = FindClientsResponse
    .newBuilder()
    .addClients(clientDto)
    .build()
}
