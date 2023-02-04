package ru.auto.salesman.service.client

import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import zio.ZIO
import zio.duration._

class ClientServiceImplWithCacheSpec extends BaseSpec {

  private val underlying = mock[ClientService]
  private val service = ClientServiceImplWithCache(1, 500.millis)(underlying)

  private val client = ClientRecordGen.next

  "ClientServiceImplWithCache" should {

    "cache underlying service" in {
      (underlying.getById _)
        .expects(client.clientId, false)
        .returningZ(Some(client))
        .once()

      (1 to 2).foreach { _ =>
        service
          .getById(client.clientId, withDeleted = false)
          .success
          .value shouldBe Some(client)
      }
    }

    "lookup again after ttl is over" in {
      (underlying.getById _)
        .expects(client.clientId, false)
        .returningZ(Some(client))
        .twice()

      val test =
        for {
          _ <- ZIO.foreach_(1 to 2)(_ => service.getById(client.clientId, false))
          _ <- ZIO.sleep(1.seconds)
          _ <- ZIO.foreach_(1 to 2)(_ => service.getById(client.clientId, false))
        } yield ()

      test.success.value
    }

  }

}
