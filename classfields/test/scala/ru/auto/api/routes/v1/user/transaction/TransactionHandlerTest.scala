package ru.auto.api.routes.v1.user.transaction

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import ru.auto.api.ApiSuite
import ru.auto.api.managers.product.ProductManager
import ru.auto.api.model.ModelGenerators.PrivateUserRefGen
import ru.auto.api.model.gen.BasicGenerators.readableString
import ru.auto.api.model.gen.SalesmanModelGenerators._
import ru.auto.api.model.salesman.{Prolongable, TransactionId}
import ru.auto.api.services.MockedPassport
import ru.auto.api.services.MockedClients
import ru.auto.api.util.ManagerUtils

class TransactionHandlerTest extends ApiSuite with MockedClients with MockedPassport {

  override lazy val productManager: ProductManager = mock[ProductManager]

  when(passportManager.getClientId(?)(?)).thenReturnF(None)

  test("make transaction prolongable") {
    forAll(PrivateUserRefGen, TransactionIdGen, SalesmanDomainGen) { (user, transactionId, domain) =>
      when(productManager.setProlongable(?, TransactionId(any()), Prolongable(any()))(?))
        .thenReturnF(ManagerUtils.SuccessResponse)
      Put(s"/1.0/user/transaction/${transactionId.value}/prolongable?domain=$domain") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(productManager).setProlongable(
            eq(domain),
            TransactionId(eq(transactionId.value)),
            Prolongable(eq(true))
          )(?)
        }
      reset(productManager)
      verifyNoMoreInteractions(productManager)
    }
  }

  test("make transaction non-prolongable") {
    forAll(PrivateUserRefGen, TransactionIdGen, SalesmanDomainGen) { (user, transactionId, domain) =>
      when(productManager.setProlongable(?, TransactionId(any()), Prolongable(any()))(?))
        .thenReturnF(ManagerUtils.SuccessResponse)
      Delete(s"/1.0/user/transaction/${transactionId.value}/prolongable?domain=$domain") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(productManager).setProlongable(
            eq(domain),
            TransactionId(eq(transactionId.value)),
            Prolongable(eq(false))
          )(?)
        }
      reset(productManager)
      verifyNoMoreInteractions(productManager)
    }
  }

  test("respond with 400 if domain missing") {
    forAll(PrivateUserRefGen, readableString, Gen.oneOf(Put, Delete)) { (user, transactionId, method) =>
      method(s"/1.0/user/transaction/$transactionId/prolongable") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
        }
    }
  }
}
