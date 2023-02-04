package common.zio.grpc.test.client

import java.util.concurrent.atomic.AtomicInteger
import com.google.protobuf.empty.Empty
import common.grpc.GrpcStatus
import common.zio.grpc.client.{GrpcClient, GrpcClientConfig, GrpcClientLive, RetryPolicy}
import common.zio.grpc.testkit.test_service.TestServiceGrpc.{TestService, TestServiceStub}
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc._
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.clock.Clock
import zio.{Has, ZIO, ZLayer, ZManaged}

object RetryClientInterceptorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("RetryClientInterceptor")(
      testM("Correct retry with unavailable code") {
        for {
          retriesCounter <- ZIO.service[AtomicInteger]
          _ <- ZIO.succeed(retriesCounter.set(0))
          client <- ZIO.service[GrpcClient.Service[TestService]]
          res <- client.call(_.testSend(Empty())).flip
          afterResponse = retriesCounter.get()
        } yield {
          assert(res)(isSubtype[GrpcStatus.Unavailable](anything)) &&
          assert(afterResponse)(equalTo(RetryPolicy.default.maxAttempts))
        }
      }
    ).provideCustomLayer {
      val retriesCounter = ZLayer.succeed(new AtomicInteger)
      val serviceName = InProcessServerBuilder.generateName()
      val grpcClient: ZLayer[Any, Nothing, Has[GrpcClient.Service[TestService]]] =
        retriesCounter >>> ZLayer.fromServiceManaged[AtomicInteger, Any, Nothing, GrpcClient.Service[TestService]] {
          counter =>
            ZManaged
              .makeEffect(InProcessChannelBuilder.forName(serviceName).directExecutor().build())(_.shutdownNow())
              .map(channel => GrpcClientLive(channel, new TestServiceStub(_)))
              .map { service =>
                service.withInterceptors(new ClientInterceptor {
                  override def interceptCall[ReqT, RespT](
                      method: MethodDescriptor[ReqT, RespT],
                      callOptions: CallOptions,
                      next: Channel): ClientCall[ReqT, RespT] = {
                    counter.incrementAndGet()
                    next.newCall(method, callOptions)
                  }
                })
              }
              .orDie
        }
      val config = ZIO.succeed(GrpcClientConfig("endpoint")).toLayer
      ((Clock.live ++ config ++ grpcClient) >>> GrpcClientLive.withRetries[TestService]) ++ retriesCounter
    }
}
