package auto.dealers.application.scheduler.test.kafka

import common.palma.Palma
import common.palma.testkit.MockPalma
import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.GrpcClientLive
import ru.auto.application.palma.proto.application_palma_model.CreditApplication
import io.grpc._
import ru.auto.api.api_offer_model.{Category, Section}
import zio.{Has, ULayer, ZLayer}

import scala.concurrent.Future

object CreditApplicationDictionaryServiceTest {

  def layer: ULayer[Has[Palma.Service]] = ZLayer.fromEffect {
    for {
      palma <- MockPalma.make
      _ <- palma
        .create(
          CreditApplication(
            id = "exists-used",
            dealerId = 20101,
            category = Category.CARS,
            section = Section.USED,
            userId = 1
          )
        )
        .orDie
      _ <- palma
        .create(
          CreditApplication(
            id = "exists-new",
            dealerId = 20101,
            category = Category.CARS,
            section = Section.NEW,
            userId = 1
          )
        )
        .orDie
    } yield palma
  }

}
