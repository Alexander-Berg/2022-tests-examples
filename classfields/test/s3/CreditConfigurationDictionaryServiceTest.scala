package auto.dealers.application.scheduler.test.s3

import com.google.protobuf.empty.Empty
import common.palma.Palma
import common.palma.testkit.MockPalma
import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.GrpcClientLive
import io.grpc.stub.StreamObserver
import ru.auto.application.palma.proto.application_palma_model.CreditConfiguration
import auto.dealers.application.scheduler.test.s3.ApplicationsSearchIndexUploaderSpecOps.TimeoutException
import zio.{Has, ULayer, ZIO, ZLayer}

import scala.concurrent.Future

object CreditConfigurationDictionaryServiceTest {

  def buildLayer(
      configurations: Seq[CreditConfiguration],
      hasExceptionRisen: Boolean = false): ULayer[Has[Palma.Service]] =
    ZLayer.fromEffect(for {
      palma <- MockPalma.make
      _ <- ZIO.foreach(configurations)(palma.create(_)).orDie
    } yield palma)

}
