package ru.auto.salesman.client.howmuch

import billing.common_model.Money
import billing.howmuch.price_service.{
  GetPricesRequest,
  GetPricesResponse,
  GetPricesResponseEntry,
  PatchPricesRequest,
  PriceServiceGrpc
}
import com.google.protobuf.empty.Empty
import io.grpc.{Server, ServerBuilder}
import org.joda.time.DateTime
import ru.auto.salesman.client.howmuch.model.MatrixId.CustomMatrixId
import ru.auto.salesman.client.howmuch.model.PriceRequest.PriceRequestEntry
import ru.auto.salesman.client.howmuch.model.PriceResponse.PriceResponseEntry
import ru.auto.salesman.{Env, Task}
import ru.auto.salesman.client.howmuch.model.{PriceRequest, PriceResponse, RuleId}
import ru.auto.salesman.model.criteria.Criterion
import ru.auto.salesman.model.criteria.CriteriaContext.CallCarsNewCriteriaContext
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.grpc.{GrpcClient, GrpcClientConfig}
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.global
import zio.{RManaged, ZIO, ZManaged}

import scala.concurrent.Future

class HowMuchClientSpec extends BaseSpec {
  import HowMuchClientSpec._

  private val grpcServer = new PriceServiceGrpc.PriceService {

    override def getPrices(
        request: GetPricesRequest
    ): Future[GetPricesResponse] =
      Future.successful(
        GetPricesResponse(
          List(
            GetPricesResponseEntry(
              TestEntryId.value,
              GetPricesResponseEntry.Result.Rule(
                GetPricesResponseEntry.Rule(testRuleId, Some(Money(testPrice)))
              )
            )
          )
        )
      )

    override def patchPrices(request: PatchPricesRequest): Future[Empty] = ???
  }

  private val grpcClient =
    GrpcClient(
      GrpcClientConfig("localhost:8080"),
      TestOperationalSupport
    )(PriceServiceGrpc.stub)

  private val client = new HowMuchGrpcClient(grpcClient)

  private val testRuleId = "rule1"
  private val testPrice = 1000L

  "HowMuchClientImpl" should {
    "get price" in {

      val expectedResult = PriceResponse(
        List(PriceResponseEntry(TestEntryId, RuleId(testRuleId), testPrice))
      )

      run {
        client.getPrice(TestRequest)
      } shouldBe expectedResult
    }
  }

  private def managed: RManaged[Env, Server] = {
    val acquire: Task[Server] = ZIO.effect {
      ServerBuilder
        .forPort(8080)
        .addService(PriceServiceGrpc.bindService(grpcServer, global))
        .build
        .start
    }
    val release = (s: Server) => ZIO.effectTotal(s.shutdown())

    ZManaged.make(acquire)(release)
  }

  private def run[T](f: => Task[T]): T =
    managed.use(_ => f).success.value
}

object HowMuchClientSpec {

  private val TestRequest = PriceRequest(
    List(
      PriceRequestEntry(
        CustomMatrixId("test"),
        CallCarsNewCriteriaContext(List(Criterion("key1", "value1")))
      )
    ),
    DateTime.now()
  )

  private val TestEntryId = TestRequest.entries.head.entryId

}
