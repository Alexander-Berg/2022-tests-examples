package vertis.spamalot.dao.queries.channels.old

import com.yandex.ydb.table.query.Params
import vertis.spamalot.convert.instances._
import vertis.spamalot.dao.OldChannelsStorage
import vertis.spamalot.dao.YdbConstants._
import vertis.spamalot.dao.model.storage.old.{StoredChannel => OldStoredChannel}
import vertis.spamalot.model.UserId
import vertis.ydb.queue.storage.Query

import java.time.Instant

/** Queries for upserting users' channels
  *
  * @author kusaeva
  */
object UpsertChannelQuery extends Query {

  override def query: String =
    s"""|${UserIdColumn.declare[UserId]}
        |${UnreadCountColumn.declare[Int]}
        |${UpdateTsColumn.declare[Instant]}
        |
        |
        |UPSERT INTO ${OldChannelsStorage.databaseTableName} (
        |    $PartitionHashColumn,
        |    $UserIdColumn,
        |    $UnreadCountColumn,
        |    $UpdateTsColumn
        |) VALUES (
        |    Digest::CityHash(${UserIdColumn.param}),
        |    ${UserIdColumn.param},
        |    ${UnreadCountColumn.param},
        |    ${UpdateTsColumn.param}
        |);
        |""".stripMargin

  def makeParams(channel: OldStoredChannel): Params = {
    toParams(
      UserIdColumn -> channel.userId,
      UnreadCountColumn -> channel.unreadCount,
      UpdateTsColumn -> channel.updateTs
    )
  }
}
