package auto.dealers.dealer_stats.storage.testkit

import auto.dealers.dealer_stats.model.{ClientId, Indicator, IndicatorType}
import auto.dealers.dealer_stats.storage.dao.WarehouseIndicatorDao
import auto.dealers.dealer_stats.storage.dao.WarehouseIndicatorDao.WarehouseIndicatorDao
import zio.{Chunk, Has, Task, UIO, ULayer, ZIO}
import zio.stm.TRef

class InMemoryWarehouseIndicatorDao(storage: TRef[Chunk[Indicator]]) extends WarehouseIndicatorDao.Service {

  override def batchUpsert(indicators: List[Indicator]): Task[Unit] =
    storage.update(_ ++ Chunk.fromIterable(indicators)).commit

  override def findByClientId(clientId: ClientId): Task[List[Indicator]] =
    storage.get.map(storage => storage.filter(i => i.clientId == clientId).toList).commit

  def dump: UIO[List[Indicator]] = storage.get.map(_.toList).commit

  def clean: UIO[Unit] = storage.set(Chunk.empty).commit
}

object InMemoryWarehouseIndicatorDao {

  def clean: ZIO[Has[InMemoryWarehouseIndicatorDao], Nothing, Unit] =
    ZIO.service[InMemoryWarehouseIndicatorDao].flatMap(_.clean)

  def dump: ZIO[Has[InMemoryWarehouseIndicatorDao], Nothing, List[Indicator]] =
    ZIO.service[InMemoryWarehouseIndicatorDao].flatMap(_.dump)

  val test: ULayer[WarehouseIndicatorDao with Has[InMemoryWarehouseIndicatorDao]] =
    TRef
      .makeCommit(Chunk.fromIterable(List.empty[Indicator]))
      .map(new InMemoryWarehouseIndicatorDao(_))
      .map(dao => Has.allOf[WarehouseIndicatorDao.Service, InMemoryWarehouseIndicatorDao](dao, dao))
      .toLayerMany
}
