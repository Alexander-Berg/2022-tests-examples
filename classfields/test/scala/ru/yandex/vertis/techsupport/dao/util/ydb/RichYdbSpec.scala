package ru.yandex.vertis.vsquality.techsupport.dao.util.ydb

import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.techsupport.util.ydb.YdbSpecBase
import ru.yandex.vertis.vsquality.utils.ydb_utils._
import ru.yandex.vertis.vsquality.utils.ydb_utils.model.{Query, QueryParams}

/**
  * @author potseluev
  */
class RichYdbSpec extends YdbSpecBase {

  before {
    ydb.runTx(ydb.execute("DELETE FROM appeals")).await
  }

  "Ydb.transaction" should {
    val yql = "INSERT INTO appeals (client_id, chat_provider, create_time) VALUES(null, null, null);"

    def getRowsCount: Long =
      ydb
        .runTx(ydb.execute("SELECT count(*) as count from appeals"))
        .map(_.resultSet.rowIterator.next().getColumn("count").getUint64)
        .await

    "run yql successfully" in {
      assume(getRowsCount == 0)
      val query = Query(yql, QueryParams())
      val transaction = ydb.transaction(List(query))
      ydb.runTx(transaction).await
      getRowsCount shouldBe 1
    }

    "rollback transaction if one of queries has failed" in {
      assume(getRowsCount == 0)
      val query = Query(yql, QueryParams())
      val transaction = ydb.transaction(List(query, query))
      ydb.runTx(transaction).shouldFail
      getRowsCount shouldBe 0
    }
  }
}
