package common.zio.grpc.testkit

import com.google.protobuf.empty.Empty
import com.google.protobuf.wrappers.Int32Value
import common.zio.grpc.testkit.test_service.TestServiceGrpc.TestService
import io.grpc.stub.StreamObserver

import scala.concurrent.Future

class TestServiceImpl extends TestService {
  override def testSend(request: Empty): Future[Empty] = Future.successful(Empty.defaultInstance)

  override def testStream(request: Int, responseObserver: StreamObserver[Int]): Unit = {
    (0 until request).foreach { v =>
      responseObserver.onNext(v)
    }
    responseObserver.onCompleted()
  }
}
