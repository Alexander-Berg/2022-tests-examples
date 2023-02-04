package auto.dealers.calltracking.storage.testkit

import java.time.Instant

import auto.dealers.calltracking.storage.SynchronizationDao
import auto.dealers.calltracking.storage.SynchronizationDao._
import zio.stm.{TMap, TRef}
import zio.{Has, UIO, ULayer}

object TestSynchronizationDao {

  def make: UIO[SynchronizationDao.Service] =
    TMap.make[String, Instant]().map(new TestDao(_)).commit

  val live: ULayer[Has[Service]] = make.toLayer

  class TestDao(lastPollingTimes: TMap[String, Instant]) extends SynchronizationDao.Service {

    override def getLastPollingTime(pollingId: String): UIO[Option[Instant]] =
      lastPollingTimes.get(pollingId).commit

    override def updateLastPollingTime(pollingId: String, polling: Instant): UIO[Unit] =
      lastPollingTimes.put(pollingId, polling).commit
  }
}
