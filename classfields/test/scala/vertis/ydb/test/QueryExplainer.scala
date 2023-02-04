package vertis.ydb.test

import com.yandex.ydb.table.query.ExplainDataQueryResult
import common.zio.logging.SyncLogger
import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import vertis.ydb.dao.YdbStorageDao
import vertis.ydb.queue.storage.Query
import vertis.zio.BaseEnv
import zio.{RIO, UIO, ZLayer}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class QueryExplainer(ydb: YdbZioWrapper) extends YdbStorageDao {
  private val syncLogger = SyncLogger[QueryExplainer]
  private val ydbLayer = ZLayer.succeed(ydb)

  def explain(query: Query): RIO[BaseEnv, ExplainDataQueryResult] = {
    tx(ydb.explain(query.query)).provideSomeLayer[BaseEnv](ydbLayer)
  }

  def printExplain(query: Query): RIO[BaseEnv, Unit] =
    explain(query) >>= { res =>
      UIO(
        syncLogger.info(s"""Explaining ${query.name}:
        Ast:
         ${res.getQueryAst}
          Plain:
           ${res.getQueryPlan}""")
      )
    }
}
