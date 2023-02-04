package vertis.spamalot.dao.queries.push_history

import vertis.spamalot.dao.PushHistoryStorage
import vertis.ydb.queue.storage.Query

object CleanQuery extends Query {

  override def query: String =
    s"""
       |DELETE FROM ${PushHistoryStorage.databaseTableName};
       |""".stripMargin
}
