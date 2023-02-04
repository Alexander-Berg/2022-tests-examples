package ru.yandex.vertis.moderation.util

import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.util.mysql.MySqlModes
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Spec to check mysql settings
  *
  * @author potseluev
  */
@Ignore("For manually running")
@RunWith(classOf[JUnitRunner])
class MySqlTest extends SpecBase with MySqlSpecBase {

  import ru.yandex.vertis.moderation.util.mysql.SqlParameters._

  override protected def schemaScripts: Seq[String] = Seq("/mysql-test.sql")

  private val NormalSizedData: Array[Byte] = Array.fill(65000)(0)

  private val TooLargeData: Array[Byte] = Array.fill(66000)(0)

  private def insert(data: Array[Byte]): Future[Unit] =
    database
      .run(
        sqlu"""
                   INSERT INTO `test_table` (`payload`)
                   VALUES ($data)
             """,
        MySqlModes.Write
      )
      .map(_ => ())

  before {
    database.run(sqlu"DELETE FROM `test_table`", MySqlModes.Write).futureValue
  }

  "MySql" should {

    "failed with MysqlDataTruncation when data is too large if INSERT is used" in {
      insert(TooLargeData).shouldCompleteWithException[MysqlDataTruncation]
    }

    "failed with MysqlDataTruncation when data is too large if UPDATE is used" in {
      insert(NormalSizedData).futureValue
      database
        .run(
          sqlu"""
                   UPDATE `test_table`
                   SET `payload` = $TooLargeData
             """,
          MySqlModes.Write
        )
        .shouldCompleteWithException[MysqlDataTruncation]
    }

    "failed with MysqlDataTruncation when data is too large data if ON DUPLICATE KEY UPDATE is used" in {
      database
        .run(
          sqlu"""
                   INSERT INTO `test_table` (`payload`)
                   VALUES ($TooLargeData)
                   ON DUPLICATE KEY UPDATE `payload` = $TooLargeData
           """,
          MySqlModes.Write
        )
        .shouldCompleteWithException[MysqlDataTruncation]
    }
  }

}
