package vertis.spamalot.dao

import common.zio.logging.Logging
import ru.yandex.vertis.ydb.zio.TxRIO
import vertis.spamalot.dao.queries.channels.DeleteQuery
import vertis.spamalot.model.ReceiverId

trait ChannelsStorageTestMixin { self: ChannelsStorage =>

  def delete(receiverId: ReceiverId): TxRIO[Logging.Logging, Unit] =
    prepareAndExecute(
      DeleteQuery,
      DeleteQuery.makeParams(receiverId.proto)
    ).unit
}
