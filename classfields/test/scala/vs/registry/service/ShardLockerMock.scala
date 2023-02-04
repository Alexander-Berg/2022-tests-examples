package vs.registry.service

import vs.core.distribution.InstanceId
import vs.registry.domain.ShardId
import zio.*

object ShardLockerMock {

  private val mock: ZLayer[Any, Throwable, ShardLockerMock] = ZLayer.fromZIO(
    for {
      queue <- Queue.sliding[Unit](1)
    } yield ShardLockerMock(queue),
  )

  val live: ZLayer[Any, Throwable, ShardLockerMock & ShardLocker] =
    mock >+>
      ZLayer.fromZIO(
        for {
          impl <- ZIO.service[ShardLockerMock]
        } yield impl: ShardLocker,
      )

}

case class ShardLockerMock(queue: Queue[Unit]) extends ShardLocker {

  def releaseAll(instanceId: InstanceId): UIO[Unit] = queue.takeAll.unit

  def lockShards(instanceId: InstanceId, shards: Set[ShardId]): Task[Unit] =
    queue.offer(()).unit

  def waitForLock: UIO[Unit] = queue.take

}
