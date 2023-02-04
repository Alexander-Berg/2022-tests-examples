package vertis.spamalot.dao

import common.zio.logging.Logging
import ru.yandex.vertis.ydb.zio.TxRIO
import vertis.spamalot.dao.model.storage.old.{StoredChannel => OldStoredChannel}
import vertis.spamalot.dao.queries.channels.old.UpsertChannelQuery

trait OldChannelsStorageTestMixin { self: OldChannelsStorage =>

  def upsert(channel: OldStoredChannel): TxRIO[Logging.Logging, Unit] = {
    prepareAndExecute(
      UpsertChannelQuery,
      UpsertChannelQuery.makeParams(channel)
    ).unit
  }
}
