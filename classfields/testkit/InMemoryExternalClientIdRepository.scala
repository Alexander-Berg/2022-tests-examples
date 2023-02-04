package auto.dealers.application.storage.testkit

import auto.dealers.application.model.ECreditId
import auto.dealers.application.storage.ExternalClientIdRepository
import auto.dealers.application.storage.ExternalClientIdRepository.{
  ExternalClientIdFetchingError,
  ExternalClientIdRepository
}
import auto.dealers.application.storage.testkit.InMemoryExternalClientIdRepository.eCreditIdFetcher
import zio.stm.{STM, TMap}
import zio.{Has, IO, UIO, URLayer, ZLayer}

class InMemoryExternalClientIdRepository(eCreditIdFetcher: eCreditIdFetcher)
  extends ExternalClientIdRepository.Service {

  override def eCreditId(
      clientId: Long): IO[ExternalClientIdFetchingError, Option[ECreditId]] = eCreditIdFetcher(clientId)
}

object InMemoryExternalClientIdRepository {
  type eCreditIdFetcher = Long => IO[ExternalClientIdFetchingError, Option[ECreditId]]

  val test: URLayer[Has[Map[Long, ECreditId]], ExternalClientIdRepository] = ZLayer.fromServiceM(create)

  val failing: URLayer[Has[ExternalClientIdFetchingError], ExternalClientIdRepository] =
    ZLayer.fromService(err => apply(_ => IO.fail(err)))

  def create(data: Map[Long, ECreditId]): UIO[InMemoryExternalClientIdRepository] =
    STM.atomically(TMap.fromIterable(data)).map { storage =>
      new InMemoryExternalClientIdRepository(clientId => storage.get(clientId).commit)
    }

  def apply(eCreditIdFetcher: eCreditIdFetcher): InMemoryExternalClientIdRepository =
    new InMemoryExternalClientIdRepository(eCreditIdFetcher)
}
