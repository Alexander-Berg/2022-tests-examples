package ru.auto.salesman.dao

import org.scalatest.Inspectors
import ru.auto.salesman.dao.TariffDao.Actual
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.TariffTypes
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.yandex.vertis.generators.ProducerProvider.asProducer

trait TariffDaoSpec extends BaseSpec {

  protected def dao: TariffDao

  "TariffDao" should {

    "correctly add tariff" in {
      val tariff = TariffGen.next

      dao.upsert(tariff).success

      dao.get(Actual(tariff.clientId)).success.value.value shouldBe tariff
    }

    "correctly update tariff" in {
      val tariff = TariffGen.next.copy(tariffType = TariffTypes.LuxaryMsk)

      dao.upsert(tariff).success

      dao.get(Actual(tariff.clientId)).success.value.value shouldBe tariff

      val tariffChanged = TariffGen.next
        .copy(clientId = tariff.clientId, tariffType = TariffTypes.LuxaryMsk)

      dao.upsert(tariffChanged).success

      dao
        .get(Actual(tariff.clientId))
        .success
        .value
        .value shouldBe tariffChanged
    }

    "correctly get tariff by filter" in {
      val clients = ClientIdGen.next(10).toList

      val expected = clients.map { client =>
        val tariff = TariffGen.next.copy(clientId = client)
        dao.upsert(tariff).success.value

        client -> tariff
      }.toMap

      Inspectors.forEvery(clients) { client =>
        dao.get(Actual(client)).success.value.value shouldBe expected(client)
      }
    }

    "should not return future tariff by default" in {
      val tariff = TariffGen.next.copy(
        from = now().plusDays(3),
        to = now().plusDays(5)
      )

      dao.upsert(tariff).success

      dao.get(Actual(tariff.clientId)).success.value shouldBe empty
    }

    "should return future tariff if requested" in {
      val tariff = TariffGen.next.copy(
        from = now().plusDays(3),
        to = now().plusDays(5)
      )

      dao.upsert(tariff).success

      dao
        .get(Actual(tariff.clientId, now().plusDays(4)))
        .success
        .value
        .value shouldBe tariff
    }

    "correctly remove tariff" in {
      val tariff = TariffGen.next
      dao.upsert(tariff).success

      val filter = Actual(tariff.clientId)

      dao.get(filter).success.value.value shouldBe tariff

      dao.delete(filter).success

      dao.get(filter).success.value shouldBe empty
    }

  }

}
