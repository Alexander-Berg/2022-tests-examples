package vertis.spamalot.dao.queries.configs

import com.yandex.ydb.table.query.Params
import ru.yandex.vertis.spamalot.model.{ReceiverId => ProtoReceiverId}
import vertis.spamalot.dao.ReceiverConfigStorage
import vertis.spamalot.dao.YdbConstants.{PartitionHashColumn, ReceiverIdColumn}
import vertis.ydb.queue.storage.Query

object DeleteQuery extends Query {

  override def query: String =
    s"""|${ReceiverIdColumn.declare[ProtoReceiverId]}
        |
        |DELETE FROM ${ReceiverConfigStorage.tableName}
        |WHERE $PartitionHashColumn = Digest::CityHash($$$ReceiverIdColumn)
        |  AND $ReceiverIdColumn = $$$ReceiverIdColumn;
        |
        |""".stripMargin

  def makeParams(receiverId: ProtoReceiverId): Params =
    Params.of(
      ReceiverIdColumn.param,
      receiverId
    )
}
