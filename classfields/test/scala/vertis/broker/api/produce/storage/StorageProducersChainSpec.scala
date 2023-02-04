package vertis.broker.api.produce.storage

import ru.yandex.vertis.generators.ProducerProvider
import vertis.broker.model.ModelGenerators
import vertis.zio.test.ZioSpecBase
import zio.ZIO

/** @author zvez
  */
class StorageProducersChainSpec extends ZioSpecBase with ProducerProvider {

  "StorageProducersChain" should {
    "return success from the first producer" in ioTest {
      for {
        producer <- AlwaysOkStorageProducer.make
        producerFb <- AlwaysOkStorageProducer.make
        chain = new StorageProducersChain(producer, List(producerFb))
        messages = ModelGenerators.messageSequence.take(10).toSeq
        _ <- ZIO.foreach(messages)(chain.write)
        _ <- check {
          producer.messageProcessed shouldBe 10
          producerFb.messageProcessed shouldBe 0
        }
      } yield ()
    }

    "fallback when main is failing" in {
      for {
        producerFb <- AlwaysOkStorageProducer.make
        chain = new StorageProducersChain(AlwaysFailProducer, List(producerFb))
        messages = ModelGenerators.messageSequence.take(10).toSeq
        _ <- ZIO.foreach(messages)(chain.write)
        _ <- check {
          producerFb.messageProcessed shouldBe 10
        }
      } yield ()
    }
  }

}
