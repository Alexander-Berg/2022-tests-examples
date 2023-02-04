package vertis.spamalot.dao.queries.channels

import com.yandex.ydb.table.query.Params
import ru.yandex.vertis.spamalot.model.{ReceiverId => ProtoReceiverId}
import vertis.spamalot.dao.ChannelsStorage
import vertis.spamalot.dao.YdbConstants.{PartitionHashColumn, ReceiverIdColumn}
import vertis.ydb.queue.storage.Query

/** Query for deleting number of unread messages for a user.
  *
  * @author tymur-lysenko
  */
object DeleteQuery extends Query {

  override def query: String =
    s"""|${ReceiverIdColumn.declare[ProtoReceiverId]}
        |
        |DELETE FROM ${ChannelsStorage.databaseTableName}
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
