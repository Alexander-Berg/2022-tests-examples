package ru.yandex.vertis.telepony.dao.yql

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.{AonAddAction, AonChangeAction, AonDeleteSourceAction, AonUpdateVerdictAction}
import ru.yandex.vertis.telepony.yql.TestYqlServer
import ru.yandex.vertis.telepony.dao.jdbc.api._

import scala.annotation.nowarn
import scala.concurrent.Future

/**
  * @author tolmach
  */
trait YqlAonBlacklistDaoSpecBase extends TestYqlServer { this: SpecBase =>

  // Need access to yt cluster to read more than 1k rows
  protected val YtBatchSize = 1000

  protected def batchInsert(tablePath: String, actions: Seq[AonChangeAction]) = {
    val rows = actions.map {
      case AonUpdateVerdictAction(source, verdict) =>
        s"""'UPDATE', '${source.callerId.value}', '$verdict'"""
      case AonAddAction(source, verdict) =>
        s"""'ADD', '${source.callerId.value}', '$verdict'"""
      case AonDeleteSourceAction(source) =>
        s"""'DELETE', '${source.callerId.value}', 'empty'"""
    }

    val parts = rows.map { row =>
      s"""
          INSERT INTO
            `$tablePath` (action, source, verdict)
          SELECT
            $row;
       """
    }
    val q = parts.mkString("\n")

    val query = sql"#$q"

    // stupid because https://yql.yandex-team.ru/docs/yt/syntax/insert_into fail and i dont know why
    // insert per request is slow. i think all insert in one query is better
    yqlClient.executeUpdate("stupid bulk insert", query).futureValue
  }

  @nowarn
  protected def checkBatchEquality(expected: Seq[AonChangeAction])(batch: Seq[AonChangeAction]): Future[Unit] = Future {
    batch should contain theSameElementsAs expected
  }

}
