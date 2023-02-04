package ru.auto.salesman.client.banker.impl

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.scalamock.scalatest.MockFactory
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.proto.user.ModelProtoFormats
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.BasicSalesmanGenerators
import ru.auto.salesman.test.model.gens.user.BankerApiModelGenerators
import ru.auto.salesman.util.sttp.SttpClientImpl
import ru.auto.salesman.util.sttp.SttpProtobufSupport.SttpProtoException.PaymentRequiredException
import ru.yandex.vertis.banker.model.ApiModel
import ru.yandex.vertis.banker.model.ApiModel._
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.protobuf.ProtobufUtils
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

class BankerSttpClientSpec extends BaseSpec with BankerApiModelGenerators {

  import BankerSttpClientSpec._

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val testApiModelAccount: Account = ApiModel.Account.newBuilder().build()

  val api = new BankerSttpClient(
    baseUrl = runServer(
      concat(
        createAccountRoute(ApiModel.Account.newBuilder().build()),
        getAccountsRoute(ApiModel.Account.newBuilder().build()),
        getAccountInfoRoute,
        executeAccountRequestRoute,
        getPaymentMethodsRoute,
        bankerPayRoute,
        payRecurrentRoute
      )
    ).toString,
    service = "test",
    backend = SttpClientImpl(TestOperationalSupport)
  )

  "BankerSttpClient" should {
    val yandexPassportId = Some(readableString.next)

    "call create account" in {
      api
        .createAccount(ApiModel.Account.newBuilder().setUser("test").build())
        .success
        .value shouldBe testApiModelAccount
    }

    "call get accounts" in {
      api
        .getAccounts("test-user")
        .success
        .value shouldBe List(testApiModelAccount)
    }

    "call get account" in {
      api
        .getAccount("test-user")
        .success
        .value shouldBe Option(testApiModelAccount)
    }

    "call getAccountInfo" in {
      val res = ApiModel.AccountInfo.newBuilder().build()
      api
        .getAccountInfo("test-user", "dd")
        .success
        .value shouldBe res
    }

    "call executeAccountRequest" in {
      api
        .executeAccountRequest(
          "test-user",
          ApiModel.AccountConsumeRequest.newBuilder().setAccount("dd").build()
        )
        .success
        .value shouldBe ApiModel.Transaction.getDefaultInstance
    }

    "forward error from call executeAccountRequest " in {
      api
        .executeAccountRequest(
          "userBalance500",
          ApiModel.AccountConsumeRequest.newBuilder().setAccount("dd").build()
        )
        .failure
        .exception shouldBe a[PaymentRequiredException]
    }

    "call getPaymentMethods" in {
      api
        .getPaymentMethods("test-user", PaymentSystemId.YANDEXKASSA_V3, yandexPassportId)
        .success
        .value shouldBe Iterable(ApiModel.PaymentMethod.getDefaultInstance)
    }

    "call getCard" in {
      api
        .getCards("test-user", PaymentSystemId.YANDEXKASSA_V3, yandexPassportId)
        .success
        .value shouldBe Nil
    }

    "call pay in banker sttp" in {
      api
        .pay(
          "test",
          ApiModel.PaymentMethod
            .newBuilder()
            .setId("id")
            .setPsId(ApiModel.PaymentSystemId.YANDEXKASSA_V3)
            .build(),
          PaymentRequest.Source.getDefaultInstance,
          yandexPassportId
        )
        .success
        .value
        .getId shouldBe "form1"
    }

    "error processing from banker sttp" in {
      api
        .pay(
          "test",
          ApiModel.PaymentMethod
            .newBuilder()
            .setPsId(ApiModel.PaymentSystemId.YANDEXKASSA_V3)
            .setId("test33")
            .build(),
          PaymentRequest.Source.getDefaultInstance,
          yandexPassportId
        )
        .success
        .value
        .getId shouldBe "form1"
    }

    "call pay recurrent" in {
      api
        .payRecurrentWithTrust(
          "test",
          ApiModel.RecurrentPaymentSource.getDefaultInstance,
          yandexPassportId.get
        )
        .success
        .value
        .getStatus shouldBe RecurrentPaymentResponse.RecurrentPaymentStatus.IN_PROGRESS
    }
  }

}

object BankerSttpClientSpec
    extends MockFactory
    with BasicSalesmanGenerators
    with ModelProtoFormats
    with ProtobufSupport
    with ProtobufUtils {

  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  val CorrectForm: PaymentRequest.Form =
    PaymentRequest.Form.newBuilder().setId("form1").build()

  private def createAccountRoute(account: ApiModel.Account): Route =
    post {
      path(
        "api" / "1.x" / "service" / "test" / "customer" / Segment / "account"
      ) { _ =>
        complete(account)
      }
    }

  private def getAccountsRoute(account: ApiModel.Account): Route =
    get {
      path(
        "api" / "1.x" / "service" / "test" / "customer" / Segment / "account"
      ) { _ =>
        complete(List(account))
      }
    }

  private def getAccountInfoRoute: Route =
    get {
      path(
        "api" / "1.x" / "service" / "test" / "customer" / Segment / "account" / Segment / "info"
      ) { case (_, _) =>
        complete(ApiModel.AccountInfo.newBuilder().build())
      }
    }

  private def executeAccountRequestRoute: Route =
    put {
      path(
        "api" / "1.x" / "service" / "test" / "customer" / Segment / "account" / Segment / "consume"
      ) { case (userId, _) =>
        userId match {
          case "userBalance500" =>
            complete(
              akka.http.scaladsl.model.StatusCodes.PaymentRequired -> ApiModel.ApiError
                .newBuilder()
                .build()
            )
          case _ =>
            complete(ApiModel.Transaction.getDefaultInstance)
        }
      }
    }

  private def getPaymentMethodsRoute: Route =
    get {
      path(
        "api" / "1.x" / "service" / "test" / "customer" / Segment / "method" / "gate" / Segment
      ) { (_, _) =>
        complete(Iterable(ApiModel.PaymentMethod.getDefaultInstance))
      }
    }

  private def bankerPayRoute: Route =
    post {
      path(
        "api" / "1.x" / "service" / "test" / "customer" / Segment / "payment" / Segment / "method" / Segment
      ) { case (_, _, _) =>
        val response = ApiModel.PaymentRequest.Form
          .newBuilder()
          .setId("form1")
          .build()
        complete(response)

      }
    }

  private def payRecurrentRoute: Route =
    post {
      path(
        "api" / "1.x" / "service" / "test" / "customer" / Segment / "payment" / Segment / "recurrent"
      ) { case (_, _) =>
        val response = ApiModel.RecurrentPaymentResponse
          .newBuilder()
          .setStatus(RecurrentPaymentResponse.RecurrentPaymentStatus.IN_PROGRESS)
          .build()
        complete(response)

      }
    }

}
