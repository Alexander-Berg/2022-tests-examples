package auto.dealers.dealer_stats.storage.testkit

import auto.dealers.dealer_stats.model.{ClientId, DailyBalance, DealerBalance}
import auto.dealers.dealer_stats.storage.dao.DealerBalanceDao
import auto.dealers.dealer_stats.storage.dao.DealerBalanceDao.{DealerBalanceDao, DealerBalanceError}
import zio.stm.TRef
import zio.{Chunk, Has, IO, NonEmptyChunk, UIO, ULayer, ZIO}

import java.time.LocalDate

class InMemoryDealerBalanceDao(storage: TRef[Chunk[DealerBalance]]) extends DealerBalanceDao.Service {

  override def insert(balances: NonEmptyChunk[DealerBalance]): IO[DealerBalanceError, Unit] =
    storage.update(_ ++ balances).commit

  override def balancesByDay(
      from: LocalDate,
      to: LocalDate,
      clientId: ClientId): IO[DealerBalanceError, List[DailyBalance]] =
    storage.get
      .map(
        _.filter(db => db.clientId == clientId && !(db.date.isBefore(from) || db.date.isAfter(to)))
          .groupBy(_.date)
          .map { case (_, dailyBalances) => dailyBalances.maxBy(_.timestamp) }
          .toList
          .sortBy(_.date)
          .map(db => DailyBalance(db.date, db.balance))
      )
      .commit

  override def lastBalance(before: LocalDate, clientId: ClientId): IO[DealerBalanceError, Option[Long]] =
    storage.get
      .map(
        _.filter(db => db.clientId == clientId && db.date.isBefore(before))
          .sortBy(_.timestamp)
          .lastOption
          .map(_.balance)
      )
      .commit

  def dump: UIO[List[DealerBalance]] = storage.get.map(_.toList).commit
  def clean: UIO[Unit] = storage.set(Chunk.empty).commit
}

object InMemoryDealerBalanceDao {

  def clean: ZIO[Has[InMemoryDealerBalanceDao], Nothing, Unit] = ZIO.service[InMemoryDealerBalanceDao].flatMap(_.clean)

  def dump: ZIO[Has[InMemoryDealerBalanceDao], Nothing, List[DealerBalance]] =
    ZIO.service[InMemoryDealerBalanceDao].flatMap(_.dump)

  val test: ULayer[DealerBalanceDao with Has[InMemoryDealerBalanceDao]] =
    TRef
      .makeCommit(Chunk.fromIterable(List.empty[DealerBalance]))
      .map(new InMemoryDealerBalanceDao(_))
      .map(dao => Has.allOf[DealerBalanceDao.Service, InMemoryDealerBalanceDao](dao, dao))
      .toLayerMany
}
