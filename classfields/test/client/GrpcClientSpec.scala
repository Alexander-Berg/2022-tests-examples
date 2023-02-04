package common.zio.grpc.test.client

import common.zio.grpc.client.GrpcClientLive
import common.zio.grpc.testkit.test_service.TestServiceGrpc
import common.zio.grpc.testkit.{GrpcServerTestkit, TestServiceImpl}
import zio.test.Assertion._
import zio.test._
import zio.{Chunk, ZManaged}

object GrpcClientSpec extends DefaultRunnableSpec {

  def spec =
    suite("GrpcClientLive")(
      testM("client streaming") {
        {
          for {
            runtime <- ZManaged.runtime[Any]
            channel <- GrpcServerTestkit.start(
              TestServiceGrpc.bindService(new TestServiceImpl, runtime.platform.executor.asEC)
            )
            client = GrpcClientLive(channel, TestServiceGrpc.stub)

            expectedCount = 20

            res <- client.stream[Int](_.testStream(expectedCount, _)).runCollect.toManaged_
          } yield assert(res)(equalTo(Chunk.fromIterable(0 until expectedCount)))
        }.useNow
      }
    )
}
