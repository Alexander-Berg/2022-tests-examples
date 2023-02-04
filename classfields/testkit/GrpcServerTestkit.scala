package common.zio.grpc.testkit

import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, ServerServiceDefinition}
import zio.ZManaged

object GrpcServerTestkit {

  def start[R](service: ServerServiceDefinition): ZManaged[R, Throwable, ManagedChannel] = {
    for {
      name <- ZManaged.succeed(InProcessServerBuilder.generateName)
      _ <- ZManaged
        .makeEffect {
          InProcessServerBuilder
            .forName(name)
            .addService(service)
            .build()
            .start()
        }(_.shutdown.awaitTermination)
      channel <- ZManaged
        .makeEffect(InProcessChannelBuilder.forName(name).build())(_.shutdownNow())
    } yield channel
  }
}
