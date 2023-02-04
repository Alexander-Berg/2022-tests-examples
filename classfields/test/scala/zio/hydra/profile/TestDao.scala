package vertis.hydra.profile

import ru.yandex.hydra.profile.dao.counter.TokenCounterDAO
import zio.{Has, RIO, ZIO}

object TestDao {

  def get(objectId: String): RIO[Has[TokenCounterDAO], Int] = ZIO.accessM(_.get.get(objectId))

  def multiGet(objectIds: Set[String]): RIO[Has[TokenCounterDAO], Map[String, Int]] =
    ZIO.accessM(_.get.multiGet(objectIds))

  def add(objectId: String, token: String): RIO[Has[TokenCounterDAO], Unit] = ZIO.accessM(_.get.add(objectId, token))

}
