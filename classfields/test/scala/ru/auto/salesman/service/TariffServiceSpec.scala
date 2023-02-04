package ru.auto.salesman.service

import ru.auto.salesman.Task
import ru.auto.salesman.dao.TariffDao.Filter
import ru.auto.salesman.model.{Tariff, TariffTypes}
import ru.auto.salesman.test.model.gens._
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import ru.yandex.vertis.mockito.MockitoSupport
import zio.ZIO

object TariffServiceSpec extends MockitoSupport {

  object TariffServiceEmptyMock extends TariffService {

    def upsert(tariff: Tariff): Task[Unit] = ZIO.unit

    def delete(filter: Filter): Task[Unit] = ZIO.unit

    def get(filter: Filter): Task[Option[Tariff]] =
      ZIO.none
  }

  object TariffServicePromoMock extends TariffService {

    def upsert(tariff: Tariff): Task[Unit] = ZIO.unit

    def delete(filter: Filter): Task[Unit] = ZIO.unit

    def get(filter: Filter): Task[Option[Tariff]] =
      ZIO.some(TariffGen.next.copy(tariffType = TariffTypes.LuxaryMsk))
  }
}
