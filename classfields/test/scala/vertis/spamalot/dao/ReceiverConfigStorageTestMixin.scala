package vertis.spamalot.dao

import common.zio.logging.Logging
import ru.yandex.vertis.ydb.zio.TxRIO
import vertis.spamalot.dao.queries.configs.DeleteQuery
import vertis.spamalot.model.ReceiverId

trait ReceiverConfigStorageTestMixin { self: ReceiverConfigStorage =>

  def delete(receiverId: ReceiverId): TxRIO[Logging.Logging, Unit] =
    prepareAndExecute(
      DeleteQuery,
      DeleteQuery.makeParams(receiverId.proto)
    ).unit
}
