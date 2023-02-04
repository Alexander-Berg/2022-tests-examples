package ru.yandex.realty.grpc.server

import io.grpc.{ManagedChannelBuilder, Server, ServerInterceptors, Status, StatusRuntimeException}
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.boilerplate.proto.api.internal.{BoilerplateServiceGrpc, InternalEcho}
import ru.yandex.realty.grpc.HeadObserver

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ExceptionHandlerInterceptorSpec extends AsyncSpecBase {
  val port = 8000

  val client: BoilerplateServiceGrpc.BoilerplateServiceStub = {
    val builder = ManagedChannelBuilder.forAddress("localhost", port)
    builder.usePlaintext()
    BoilerplateServiceGrpc.newStub(builder.build())
  }

  val service: Server = {
    val tracer: Tracer = NoopTracerFactory.create()
    val builder = NettyServerBuilder.forPort(port)
    val service = TestServer.bindService()
    builder.addService(
      ServerInterceptors.intercept(
        service,
        //interceptors call in reverse order!
        new ExceptionHandlerInterceptor,
        new LoggingServerInterceptor
      )
    )
    builder.build()
  }

  def mkObserver(): (HeadObserver[InternalEcho.Response], Future[InternalEcho.Response]) = {
    val o = new HeadObserver[InternalEcho.Response]
    o -> o.result
  }

  def request(text: String): InternalEcho.Request = InternalEcho.Request.newBuilder.setText(text).build()

  override protected def beforeAll(): Unit = {
    service.start()
  }

  override protected def afterAll(): Unit = {
    service.shutdownNow()
  }

  object TestServer extends BoilerplateServiceGrpc.BoilerplateServiceImplBase {
    override def echo(request: InternalEcho.Request, responseObserver: StreamObserver[InternalEcho.Response]): Unit =
      request.getText match {
        case "throw Exception" =>
          throw new Exception("unexpected exception")
        case _ =>
          responseObserver.onNext(InternalEcho.Response.newBuilder().setText("Ok").build())
          responseObserver.onCompleted()
      }
  }

  "ExceptionHandlerInterceptor" when {
    "successful request" in {
      val (observer, result) = mkObserver()
      client.echo(request("ping"), observer)
      result.futureValue.getText shouldBe "Ok"
    }

    "service throw Exception" in {
      val (observer, result) = mkObserver()
      client.echo(request("throw Exception"), observer)
      val ex = result.failed.futureValue.asInstanceOf[StatusRuntimeException]
      ex.getStatus.getCode shouldBe Status.Code.INTERNAL
      ex.getStatus.getDescription shouldBe null
    }

  }

}
