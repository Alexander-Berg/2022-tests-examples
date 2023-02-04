package ru.auto.salesman.client.auctions

import com.google.protobuf.empty.Empty
import io.grpc.{Server, ServerBuilder}
import org.joda.time.DateTime
import ru.auto.salesman._
import ru.auto.salesman.client.auctions.model.AuctionRequest
import ru.auto.salesman.model.criteria.CriteriaContext.CallCarsNewCriteriaContext
import ru.auto.salesman.model.criteria.Criterion._
import ru.auto.salesman.model.criteria._
import ru.auto.salesman.model.{AutoruDealer, ProductId}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.grpc.{GrpcClient, GrpcClientConfig}
import ru.yandex.vertis.ops.test.TestOperationalSupport
import vsmoney.auction.auction_bids._
import vsmoney.auction.auction_blocks.AuctionBlocksGrpc
import vsmoney.auction.common_model.Money
import vsmoney.auction.{auction_bids => proto}
import zio.{RManaged, Ref, ZIO, ZManaged}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class AuctionsClientSpec extends BaseSpec {

  import AuctionsClientSpec._

  private val auctionBidsGrpc =
    GrpcClient(
      GrpcClientConfig("localhost:8080"),
      TestOperationalSupport
    )(AuctionBidsGrpc.stub)

  private val auctionBlocksGrpc =
    GrpcClient(
      GrpcClientConfig("localhost:8080"),
      TestOperationalSupport
    )(AuctionBlocksGrpc.stub)

  private val client =
    new AuctionsClientImpl(auctionBidsGrpc, auctionBlocksGrpc)

  private val ctx = CallCarsNewCriteriaContext(
    List(
      Criterion("model", "bmw"),
      Criterion("region_id", "1"),
      Criterion("mark", "m3")
    )
  )
  private val product = ProductId.Call
  private val user = AutoruDealer(1)
  private val request = AuctionRequest(user, product, ctx)

  "AuctionClientImpl" should {

    "get bid by datetime" in {
      run { _ =>
        client
          .bidByDateTime(request, DateTime.now())
          .map(_.map(_.bid).contains(100L) shouldBe true)
      }
    }

    "criteria in request must be ordered" in {
      run { interceptor =>
        client.bidByDateTime(request, DateTime.now()) *>
          interceptor.get.map { proto =>
            val requestCriteria =
              proto.context
                .flatMap(_.context.criteriaContext.map(_.criteriaValues))
                .getOrElse(Seq.empty)
                .map(c => Criterion(c.key, c.value))

            val ordered = requestCriteria.sorted(callCriterionOrdering)

            (requestCriteria should contain).theSameElementsInOrderAs(ordered)
          }
      }
    }

  }

  private def managed(
      ref: Ref[BidByDateTimeRequest]
  ): RManaged[Env, Server] = {
    val acquire: Task[Server] = ZIO.effect {
      ServerBuilder
        .forPort(8080)
        .addService(AuctionBidsGrpc.bindService(new StubServer(ref), global))
        .build
        .start
    }
    val release = (s: Server) => ZIO.effectTotal(s.shutdown())

    ZManaged.make(acquire)(release)
  }

  private def run[T](f: Ref[BidByDateTimeRequest] => Task[T]): T =
    Ref
      .make(BidByDateTimeRequest.defaultInstance)
      .toManaged_
      .tap(managed)
      .use(ref => f(ref))
      .success
      .value

}

object AuctionsClientSpec {

  class StubServer(requestInterceptor: Ref[BidByDateTimeRequest])
      extends AuctionBidsGrpc.AuctionBids {

    override def placeBidBatch(request: BidRequestBatch): Future[Empty] =
      Future.successful(Empty())

    override def stopAuctionBatch(request: StopRequestBatch): Future[Empty] =
      Future.successful(Empty())

    def currentStateByContextBatch(
        request: CurrentStateBatchRequest
    ): Future[AuctionStates] =
      Future.successful(AuctionStates.defaultInstance)

    def placeBid(request: proto.BidRequest): Future[Empty] =
      Future.successful(Empty())

    def stopAuction(
        request: proto.StopRequest
    ): Future[Empty] =
      Future.successful(Empty())

    def bidByDateTime(
        request: BidByDateTimeRequest
    ): Future[BidByDateTimeResponse] = {
      val rs = proto.BidByDateTimeResponse.defaultInstance.copy(
        bid = Some(Money(100L))
      )

      requestInterceptor
        .set(request)
        .as(rs)
        .unsafeRunToFuture()
    }

    def allUserStateByContextBatch(
        request: AllUserStateBatchRequest
    ): Future[AllUserAuctionStates] =
      Future.successful(AllUserAuctionStates.defaultInstance)

    def auctionSettingsBatch(
        request: AuctionSettingBatchRequest
    ): Future[AuctionSettingsBatchResponse] =
      Future.successful(AuctionSettingsBatchResponse.defaultInstance)
  }

}
