package auto.dealers.application.scheduler.test.kafka

import auto.common.clients.salesman.ProductCreationResult
import auto.common.clients.salesman.ProductDomain
import auto.common.clients.salesman.SalesmanClient.SalesmanClient
import auto.common.clients.salesman.testkit.{SalesmanClientEmpty, SalesmanClientMock}
import common.zio.kafka.ProducerConfig
import common.zio.kafka.scalapb.ScalaProtobufSerializer
import common.zio.kafka.testkit.TestKafka
import org.apache.kafka.common.serialization.StringSerializer
import auto.common.clients.salesman.testkit.{SalesmanClientEmpty, SalesmanClientMock}
import ru.auto.application.palma.proto.application_palma_model.CreditApplication
import auto.dealers.application.scheduler.kafka.processors.PalmaChangelogProcessor
import common.zio.ziokafka.scalapb.ScalapbSerde
import ru.auto.salesman.products.products.{ActiveProductNaturalKey, ProductRequest, ProductTariff}
import ru.yandex.vertis.palma.event.dictionary_event.MutationEvent
import ru.yandex.vertis.palma.event.dictionary_event.MutationEvent.ActionEnum
import zio.blocking.Blocking
import zio.duration._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serializer
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test._
import zio.{ULayer, ZIO, ZLayer}
import zio.test.mock.Expectation._

object PalmaChangelogProcessorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    val topic = ZLayer.succeed("application-credit-notification")
    val kafka = TestKafka.live
    val kafkaProducerConfig = kafka >>> ZLayer.fromServiceM(_ =>
      TestKafka.bootstrapServers.map(servers => ProducerConfig(servers, 30.seconds, Map.empty))
    )

    val kafkaProducerSettings = kafkaProducerConfig >>> ZLayer.fromService(kafkaProducerConfig =>
      ProducerSettings(kafkaProducerConfig.bootstrapServers)
        .withCloseTimeout(kafkaProducerConfig.closeTimeout)
        .withProperties(kafkaProducerConfig.properties)
    )

    val producer = kafkaProducerSettings ++ Blocking.any >>>
      Producer.live

    suite("PalmaChangelogProcessor")(
      testM("golden way") {
        val message = MutationEvent(
          dictionaryId = "auto/application/credit",
          action = ActionEnum.Action.CREATE,
          itemKey = "exists-new"
        )

        val salesman: ULayer[SalesmanClient] = SalesmanClientMock.CreateProduct(
          equalTo(
            ProductRequest(
              key = Some(
                ActiveProductNaturalKey(
                  domain = ProductDomain.Application.toString,
                  payer = "dealer:20101",
                  target = "user:1",
                  productType = "single"
                )
              ),
              tariff = ProductTariff.APPLICATION_CREDIT_SINGLE_TARIFF_CARS_NEW
            )
          ),
          value(ProductCreationResult.Created)
        )

        assertM(PalmaChangelogProcessor.processMessage(message))(isUnit)
          .provideCustomLayer(
            (salesman ++ CreditApplicationDictionaryServiceTest.layer ++ producer ++ Blocking.any ++ topic) >>> PalmaChangelogProcessor.live
          )
      },
      testM("golden way used") {
        val message = MutationEvent(
          dictionaryId = "auto/application/credit",
          action = ActionEnum.Action.CREATE,
          itemKey = "exists-used"
        )

        val salesman: ULayer[SalesmanClient] = SalesmanClientMock.CreateProduct(
          equalTo(
            ProductRequest(
              key = Some(
                ActiveProductNaturalKey(
                  domain = ProductDomain.Application.toString,
                  payer = "dealer:20101",
                  target = "user:1",
                  productType = "single"
                )
              ),
              tariff = ProductTariff.APPLICATION_CREDIT_SINGLE_TARIFF_CARS_USED
            )
          ),
          value(ProductCreationResult.Created)
        )

        assertM(PalmaChangelogProcessor.processMessage(message))(isUnit)
          .provideCustomLayer(
            (salesman ++ CreditApplicationDictionaryServiceTest.layer ++ producer ++ Blocking.any ++ topic) >>> PalmaChangelogProcessor.live
          )
      },
      testM("another dictionary") {
        val message = MutationEvent(
          dictionaryId = "encrypted/auto/booking",
          action = ActionEnum.Action.CREATE,
          itemKey = "exists"
        )

        val salesman: ULayer[SalesmanClient] = SalesmanClientEmpty.empty

        assertM(PalmaChangelogProcessor.processMessage(message))(isUnit)
          .provideCustomLayer(
            (salesman ++ CreditApplicationDictionaryServiceTest.layer ++ producer ++ Blocking.any ++ topic) >>> PalmaChangelogProcessor.live
          )
      },
      testM("another action type") {
        val message = MutationEvent(
          dictionaryId = "encrypted/auto/application/credit",
          action = ActionEnum.Action.UPDATE,
          itemKey = "exists"
        )

        val salesman: ULayer[SalesmanClient] = SalesmanClientEmpty.empty

        assertM(PalmaChangelogProcessor.processMessage(message))(isUnit)
          .provideCustomLayer(
            (salesman ++ CreditApplicationDictionaryServiceTest.layer ++ producer ++ Blocking.any ++ topic) >>> PalmaChangelogProcessor.live
          )
      },
      testM("not found") {
        val message = MutationEvent(
          dictionaryId = "encrypted/auto/application/credit",
          action = ActionEnum.Action.CREATE,
          itemKey = "not_exists"
        )

        val salesman: ULayer[SalesmanClient] = SalesmanClientEmpty.empty

        assertM(PalmaChangelogProcessor.processMessage(message))(isUnit)
          .provideCustomLayer(
            (salesman ++ CreditApplicationDictionaryServiceTest.layer ++ producer ++ Blocking.any ++ topic) >>> PalmaChangelogProcessor.live
          )
      }
    )
  }
}
