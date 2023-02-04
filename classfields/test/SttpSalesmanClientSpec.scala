package auto.common.clients.salesman.test

import auto.common.clients.salesman.JsonCodecs._
import auto.common.clients.salesman.{
  Campaign,
  PlacementPaymentModel,
  ProductCreationResult,
  SalesmanClient,
  SttpSalesmanClient
}
import common.scalapb.ScalaProtobuf
import common.zio.sttp.endpoint.Endpoint
import io.circe.parser.parse
import ru.auto.api.api_offer_model.{Category, Section}
import ru.auto.salesman.model.cashback.api_model.LoyaltyReportInfo
import ru.auto.salesman.products.products.{ActiveProductNaturalKey, ProductRequest}
import ru.auto.salesman.tariffs.credit_tariffs.{DealersWithActiveApplicationCredit, TariffScope}
import common.zio.sttp.Sttp
import common.zio.sttp.Sttp.ZioSttpBackendStub
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.{ByteArrayBody, RequestBody, Response}
import sttp.model.{Header, Method, StatusCode, Uri}
import zio.test.Assertion._
import zio.test._

import scala.io.Source

object SttpSalesmanClientSpec extends DefaultRunnableSpec {

  private val testServiceName = "test"
  private val basePath = "api/1.x/service/autoru"
  private val testClientId = 1L

  private def path(uri: Uri): String = {
    uri.path.mkString("/")
  }

  private def containCommonHeaders(headers: Seq[Header]): Boolean = {
    headers.contains(Header.accept("application/protobuf")) &&
    headers.contains(Header("X-Salesman-User", testServiceName))
  }

  private def bodyAs[T <: GeneratedMessage: GeneratedMessageCompanion](
      requestBody: RequestBody[_]): T = {
    requestBody match {
      case byteArray: ByteArrayBody =>
        ScalaProtobuf.fromBytes[T](byteArray.b)
      case _ =>
        throw new IllegalArgumentException(s"Illegal RequestBody: $requestBody")
    }
  }

  private def fromResource(filename: String): String = {
    Source.fromResource(filename)(scala.io.Codec.UTF8).getLines().mkString
  }

  private val SuccessCreditApplicationRequest = ProductRequest {
    Some {
      ActiveProductNaturalKey(
        domain = "application-credit",
        payer = "user:123",
        target = "cars:new",
        productType = "access"
      )
    }
  }

  private val AlreadyExistsCreditApplicationRequest = ProductRequest {
    Some {
      ActiveProductNaturalKey(
        domain = "application-credit",
        payer = "user:123",
        target = "cars:used",
        productType = "access"
      )
    }
  }

  private val stub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case req
        if path(req.uri) == s"$basePath/products/create" &&
          req.method == Method.POST &&
          req.headers.contains(Header("X-Salesman-User", testServiceName)) =>
      bodyAs[ProductRequest](req.body) match {
        case b if b == SuccessCreditApplicationRequest =>
          Response("Successfully created", StatusCode.Ok)
        case b if b == AlreadyExistsCreditApplicationRequest =>
          Response("Already exists", StatusCode.Conflict)
        case _ =>
          sys.error("Unreachable")
      }

    case req
        if path(req.uri) == s"$basePath/cashback/client/$testClientId/report/current" &&
          req.method == Method.GET &&
          containCommonHeaders(req.headers) =>
      Response.ok(LoyaltyReportInfo.defaultInstance.toByteArray)

    case req
        if path(req.uri) == s"$basePath/application-credit/tariffs/clients" &&
          req.method == Method.GET &&
          containCommonHeaders(req.headers) => {
      Response.ok {
        Right {
          ScalaProtobuf
            .fromJson[DealersWithActiveApplicationCredit] {
              fromResource("get_active_application_credit_dealers.json")
            }
            .groupedDealers
        }
      }
    }

    case req
        if path(req.uri).startsWith(s"$basePath/campaign/client") &&
          req.method == Method.GET &&
          req.headers.contains(Header.accept("application/json")) &&
          req.headers.contains(Header("X-Salesman-User", testServiceName)) =>
      Response.ok {
        parse(fromResource("get_dealer_campaign.json")).flatMap(_.as[List[Campaign]])
      }

    case req => Response(body = s"Wrong request: ${req.toCurl}", code = StatusCode.BadRequest)
  }

  private def clientFrom(stub: ZioSttpBackendStub) = {
    (Endpoint.testEndpointLayer ++ Sttp.fromStub(stub)) >>> SttpSalesmanClient.make(testServiceName)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SttpSalesmanClient")(
      testM("createProduct returns Created") {
        for {
          result <- SalesmanClient.createProduct(SuccessCreditApplicationRequest)
        } yield assert(result)(equalTo(ProductCreationResult.Created))
      },
      testM("createProduct returns AlreadyExists") {
        for {
          result <- SalesmanClient.createProduct(AlreadyExistsCreditApplicationRequest)
        } yield assert(result)(equalTo(ProductCreationResult.AlreadyExists))
      },
      testM("getDealersWithActiveCreditApplicationProduct returns grouped records") {
        for {
          result <- SalesmanClient.getDealersWithActiveCreditApplicationProduct
        } yield assert(result)(equalTo {
          Seq {
            DealersWithActiveApplicationCredit.GroupedDealers(
              tariffScope = TariffScope.CARS_USED,
              dealerIds = Seq("dealer:123", "dealer:321")
            )
          }
        })
      },
      testM("getDealerCampaign returns campaign") {
        for {
          result <- SalesmanClient.getDealerCampaign(20101, includeDisabled = false)
        } yield assert(result)(
          hasSameElements(
            List(
              Campaign(
                category = Category.CARS,
                section = Set(Section.NEW),
                enabled = true,
                paymentModel = PlacementPaymentModel.Calls
              ),
              Campaign(
                category = Category.MOTO,
                section = Set(Section.USED),
                enabled = true,
                paymentModel = PlacementPaymentModel.SingleWithCalls
              ),
              Campaign(
                category = Category.CARS,
                section = Set(Section.USED),
                enabled = true,
                paymentModel = PlacementPaymentModel.Quota
              ),
              Campaign(
                category = Category.TRUCKS,
                section = Set(Section.NEW, Section.USED),
                enabled = true,
                paymentModel = PlacementPaymentModel.Quota
              )
            )
          )
        )
      },
      testM("getLoyaltyReport returns LoyeltyReportInfo proto") {
        for {
          res <- SalesmanClient.getLoyaltyReport(testClientId)
        } yield assert(res)(isSome(equalTo(LoyaltyReportInfo.defaultInstance)))
      }
    ).provideCustomLayerShared(clientFrom(stub))
  }
}
