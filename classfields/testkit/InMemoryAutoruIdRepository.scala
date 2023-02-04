package auto.dealers.dealer_stats.storage.testkit

import auto.dealers.dealer_stats.model.{BalanceId, ClientId}
import auto.dealers.dealer_stats.storage.AutoruIdRepository
import auto.dealers.dealer_stats.storage.AutoruIdRepository.{AutoruIdFetchingError, AutoruIdRepository}
import auto.dealers.dealer_stats.storage.testkit.InMemoryAutoruIdRepository.AutoruIdFetcher
import cats.data.NonEmptySet
import zio.{Has, IO, URLayer, ZIO, ZLayer}

class InMemoryAutoruIdRepository(fetch: AutoruIdFetcher) extends AutoruIdRepository.Service {

  override def fetchByBalanceIds(
      balanceIds: NonEmptySet[BalanceId]): IO[AutoruIdFetchingError, Map[BalanceId, ClientId]] =
    ZIO.foreach(balanceIds.toSortedSet)(balanceId => fetch(balanceId).map(_.map(balanceId -> _))).map(_.flatten.toMap)

}

object InMemoryAutoruIdRepository {
  type AutoruIdFetcher = BalanceId => IO[AutoruIdFetchingError, Option[ClientId]]

  val test: URLayer[Has[Map[BalanceId, ClientId]], AutoruIdRepository] =
    ZLayer.fromService(data => new InMemoryAutoruIdRepository(req => ZIO.succeed(data.get(req))))

  val failing: URLayer[Has[AutoruIdFetchingError], AutoruIdRepository] =
    ZLayer.fromService(err => new InMemoryAutoruIdRepository(_ => ZIO.fail(err)))
}
