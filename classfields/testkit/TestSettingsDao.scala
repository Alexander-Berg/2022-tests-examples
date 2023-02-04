package auto.dealers.calltracking.storage.testkit

import auto.dealers.calltracking.model.{ClientId, ClientSettings}
import auto.dealers.calltracking.storage.SettingsDao
import zio.stm.TMap
import zio.{Has, Task, UIO, ULayer}

object TestSettingsDao {
  def makeService: UIO[SettingsDao.Service] = TMap.make[Long, ClientSettings]().map(new TestDao(_)).commit

  val live: ULayer[Has[SettingsDao.Service]] = makeService.toLayer

  class TestDao(map: TMap[Long, ClientSettings]) extends SettingsDao.Service {

    override def getClientSettings(clientId: ClientId): Task[ClientSettings] =
      map.getOrElse(clientId.id, ClientSettings()).commit

    override def updateClientSettings(clientId: ClientId, settings: ClientSettings): Task[Unit] =
      map.put(clientId.id, settings).commit
  }
}
