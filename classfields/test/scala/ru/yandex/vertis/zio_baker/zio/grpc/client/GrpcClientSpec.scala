package ru.yandex.vertis.zio_baker.zio.grpc.client

import io.grpc.stub.AbstractStub
import io.grpc.Channel
import scalapb.grpc.AbstractService
import zio.test._
import zio.test.Assertion.isRight
import zio.test.environment.TestEnvironment

object GrpcClientSpec extends DefaultRunnableSpec {
  trait Foo extends AbstractService
  trait FooStub extends AbstractStub[FooStub] with Foo

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("constructors")(
      testM("live typechecks")(
        assertM(typeCheck("GrpcClient.live[Foo](??? : Channel => FooStub)"))(isRight)
      ),
      testM("liveWithTvm typechecks")(
        assertM(typeCheck("GrpcClient.liveWithTvm[Foo](??? : Channel => FooStub)"))(isRight)
      )
    )
}
